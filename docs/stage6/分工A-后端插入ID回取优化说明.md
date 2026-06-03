# 分工A-后端插入 ID 回取优化说明

## 1. 背景

交接文档中记录部分新增逻辑仍使用 `select max(id)` 获取刚插入记录 ID。该方式在并发插入场景下可能取到其他请求插入的记录 ID，影响后续关联数据写入。

本次将后端源码中的 `select max(id)` 回取方式统一替换为更稳妥的插入主键回取方式。

## 2. 改造范围

新增公共工具：

```text
services/backend/src/main/java/com/aituan/common/jdbc/JdbcGeneratedKeys.java
```

替换范围覆盖以下文件中的全部 `select max(id)`：

```text
services/backend/src/main/java/com/aituan/account/AccountRepository.java
services/backend/src/main/java/com/aituan/admin/AdminRepository.java
services/backend/src/main/java/com/aituan/auth/AuthRepository.java
services/backend/src/main/java/com/aituan/catalog/CatalogRepository.java
services/backend/src/main/java/com/aituan/common/file/FileStorageService.java
services/backend/src/main/java/com/aituan/coupon/CouponRepository.java
services/backend/src/main/java/com/aituan/interaction/InteractionRepository.java
services/backend/src/main/java/com/aituan/member/MemberRepository.java
services/backend/src/main/java/com/aituan/merchant/MerchantRepository.java
services/backend/src/main/java/com/aituan/trade/TradeRepository.java
```

## 3. 实现要点

1. 普通 `insert` 场景统一使用 Spring JDBC `GeneratedKeyHolder` 回取数据库生成的自增主键。
2. 新增 `JdbcGeneratedKeys.insertAndReturnId(...)`，避免每个 Repository 重复编写 `PreparedStatement.RETURN_GENERATED_KEYS` 样板代码。
3. `user_favorite` 是 `insert ... on duplicate key update` 特殊场景，不直接使用 `GeneratedKeyHolder`：
   - 先执行 upsert。
   - 再按唯一键 `(user_id, favorite_type, target_id)` 查询真实记录 ID。
4. 保持原有 SQL 字段、参数、业务含义不变，只调整插入后 ID 回取方式。

## 4. 验证结果

### 4.1 残留扫描

已扫描后端源码：

```text
services/backend/src/main/java
```

结果：未发现残留 `select max(id)`。

### 4.2 后端测试

执行命令：

```bash
JAVA_HOME='D:/soft/OracleJdk/Jdk21/jdk-21.0.8' \
PATH='D:/soft/OracleJdk/Jdk21/jdk-21.0.8/bin':$PATH \
mvn -f 'C:/Users/baozh/Downloads/aituan-stage5-c-takeaway-delivery/aituan-stage5-c-takeaway-delivery/services/backend/pom.xml' \
  -Dmaven.repo.local=D:/aituan_cache/m2 \
  -Dbackend.build.directory=D:/aituan_build/backend-target \
  test
```

最终结果：

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

后续优惠券库存边界测试加入后，当前整体后端测试已增至 5 个用例，详见 `docs/stage6/分工A-后端优惠券并发安全优化说明.md`。

## 5. 注意事项

- 本次未修改数据库表结构和 Flyway 迁移文件。
- 本次未修改 Flutter 用户端界面或逻辑，因此无需重新打包 APP。
- 构建缓存和后端构建目录继续使用 D 盘路径，避免占用 C 盘空间。
