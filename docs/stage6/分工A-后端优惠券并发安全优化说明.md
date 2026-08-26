# 分工A-后端优惠券并发安全优化说明

## 1. 背景

此前用户领取优惠券的流程为：先读取模板库存，再插入用户优惠券，最后执行 `issued_qty + 1`。该流程在高并发场景下存在竞态窗口，多个请求可能同时读取到相同库存，从而造成优惠券超发。

本次针对领取接口做最小化并发安全增强，保证限量优惠券不会突破 `total_qty`。

## 2. 改造范围

代码文件：

```text
services/backend/src/main/java/com/aituan/coupon/CouponRepository.java
services/backend/src/main/java/com/aituan/coupon/CouponService.java
services/backend/src/test/java/com/aituan/stage6/Stage6AccountAssetApiIntegrationTest.java
```

相关接口：

```text
POST /api/app/account/coupons/{templateId}/claim
```

## 3. 实现要点

### 3.1 事务内锁定模板行

新增 Repository 方法：

```text
findTemplateForUpdate(long id)
```

领取优惠券时在 `@Transactional` 内使用 `for update` 锁定对应 `coupon_template` 行，避免多个请求同时基于同一份库存快照继续执行。

### 3.2 库存增加改为条件更新

将无条件 `issued_qty = issued_qty + 1` 改为条件更新：

```sql
update coupon_template
set issued_qty = issued_qty + 1, updated_at = current_timestamp
where id = ? and is_deleted = 0 and (total_qty = 0 or issued_qty < total_qty)
```

如果受影响行数为 0，则说明库存已经不可领取，返回业务错误：

```text
优惠券已领完
```

### 3.3 领取顺序

当前领取顺序：

1. 在事务内锁定优惠券模板。
2. 校验模板状态、有效期和库存。
3. 校验用户领取上限。
4. 条件增加 `issued_qty`。
5. 插入 `user_coupon` 快照。

如果插入用户优惠券失败，事务会回滚，库存增加也随之回滚。

## 4. 测试覆盖

新增集成测试：

```text
couponClaimStopsAtTemplateInventoryLimit
```

覆盖流程：

1. 管理员创建一张 `totalQty = 1` 的测试优惠券。
2. 用户第一次领取成功。
3. 用户第二次领取返回业务错误 `优惠券已领完`。
4. 后台查询模板，确认 `issuedQty = 1`，没有超过 `totalQty`。

## 5. 验证结果

执行命令：

```bash
JAVA_HOME='D:/soft/OracleJdk/Jdk21/jdk-21.0.8' \
PATH='D:/soft/OracleJdk/Jdk21/jdk-21.0.8/bin':$PATH \
mvn -f 'C:/Users/baozh/Downloads/aituan-stage5-c-takeaway-delivery/aituan-stage5-c-takeaway-delivery/services/backend/pom.xml' \
  -Dmaven.repo.local=D:/aituan_cache/m2 \
  -Dbackend.build.directory=D:/aituan_build/backend-target \
  test
```

结果：

```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 6. 附带修正

新增库存边界测试时发现 `JdbcGeneratedKeys` 在 H2 下使用 `Statement.RETURN_GENERATED_KEYS` 可能返回 `id`、`created_at`、`updated_at` 多个生成列，导致 `GeneratedKeyHolder.getKey()` 报错。

已同步修正为明确只回取 `id` 列：

```text
connection.prepareStatement(sql, new String[] {"id"})
```

该修正保持 MySQL/H2 测试环境下的插入主键回取口径一致。

## 7. 注意事项

- 本次未修改数据库表结构和 Flyway 迁移文件。
- 本次未修改 Flutter 用户端界面或逻辑，因此无需重新打包 APP。
- 当前测试覆盖库存边界，非大规模压力测试；如需验证极高并发，可后续单独增加多用户并发压测脚本。
