# Stage6 分工A · 后端接口测试交付说明

## 1. 文档状态

- 所属阶段：Stage 6（后续五人模块分工）· 成员 A · 账号/用户资产/会员消息
- 对应计划：`docs/stage6/分工A-账号与用户资产会员消息实施计划.md` 第 1 轮「后端 + 数据」接口冒烟验收
- 当前状态：已补齐后端集成测试；`mvn test` 已通过（5 个用例）
- 范围边界：本文件主要记录后端集成测试与 H2 测试环境兼容修正；优惠券库存边界测试用于验证并发安全优化，不新增 API 路径。

## 2. 交付范围

### 2.1 后端集成测试

新增 `SpringBootTest + MockMvc` 集成测试，走真实 Spring Security 过滤器、H2 数据库、Flyway 迁移与 seed 数据。

测试文件：

```text
services/backend/src/test/java/com/aituan/stage6/Stage6AccountAssetApiIntegrationTest.java
```

覆盖用例：

| 用例 | 覆盖点 |
| --- | --- |
| `adminProfileAllowsAdminAndRejectsUser` | 管理员资料接口 ADMIN 可访问；USER 访问 `/api/admin/account/profile` 被 403 拦截 |
| `memberInfoShowsDemoUserGrowthProgress` | 演示用户会员信息：普通会员、成长值 128、距银卡会员还差 172、进度 43% |
| `couponApisExposeDemoCouponsAndOrderOptions` | 我的可用券、领券中心状态、下单可用券抵扣金额 |
| `couponClaimStopsAtTemplateInventoryLimit` | 限量优惠券领取库存边界，确认 `issuedQty` 不超过 `totalQty` |
| `messageReadApiMarksSingleMessageRead` | 站内消息按订单类型筛选；单条消息已读前后状态变化 |

### 2.2 H2 测试迁移兼容修正

首次运行集成测试时，SpringBootTest 启动失败，原因是 `V008__delivery_location_quote.sql` 使用 MySQL 的 `AFTER` 列位置语法，H2 `MODE=MySQL` 仍不支持。

已按确认后的最小方案修正：

- 去掉 `AFTER estimated_minutes` / `AFTER address_snapshot` / `AFTER delivery_distance_km`。
- 将 `order_main` 一次添加两列拆成两条 `ALTER TABLE`。
- 不改变字段名、类型、默认值和业务含义；只放弃 MySQL 中的列显示顺序指定。

改动文件：

```text
database/migrations/V008__delivery_location_quote.sql
```

## 3. 文件清单

### 新增

| 文件 | 说明 |
| --- | --- |
| `services/backend/src/test/java/com/aituan/stage6/Stage6AccountAssetApiIntegrationTest.java` | Stage6 分工A 后端接口集成测试 |
| `docs/stage6/分工A-后端接口测试交付说明.md` | 本交付说明 |

### 改动

| 文件 | 说明 |
| --- | --- |
| `database/migrations/V008__delivery_location_quote.sql` | 去掉 H2 不兼容的 MySQL `AFTER` 列位置语法 |
| `services/backend/src/main/java/com/aituan/coupon/CouponRepository.java` | 优惠券领取增加模板行锁和条件库存更新 |
| `services/backend/src/main/java/com/aituan/coupon/CouponService.java` | 调整领取顺序，库存不足时返回业务错误 |
| `services/backend/src/main/java/com/aituan/common/jdbc/JdbcGeneratedKeys.java` | 明确只回取 `id` 生成列，兼容 H2 多生成列场景 |
| `docs/stage6/分工A-账号与用户资产会员消息实施计划.md` | 同步 seed 会员门槛口径：银卡 300，演示用户普通会员 |
| `docs/stage6/分工A-后台管理员资料交付说明.md` | 同步后端验证口径：已有集成测试，不再是无测试源 |
| `docs/ReadMe.md` | 增加本交付说明索引 |

## 4. 验证结果

### 4.1 后端测试命令

本机默认 Maven 仍走 JDK 11，会报 `不支持发行版本 17`。本次未修改系统环境变量，只对命令临时指定 JDK 21，并使用 D 盘 Maven 缓存与构建目录：

```bash
cd services/backend
JAVA_HOME='D:/soft/OracleJdk/Jdk21/jdk-21.0.8' \
PATH='D:/soft/OracleJdk/Jdk21/jdk-21.0.8/bin':$PATH \
mvn -Dmaven.repo.local=D:/aituan_cache/m2 -Dbackend.build.directory=D:/aituan_build/backend-target test
```

结果：

```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 4.2 已处理的验证失败

1. 首次测试启动失败：`V008__delivery_location_quote.sql` 的 MySQL `AFTER` 语法不兼容 H2。已修正迁移脚本后通过 Flyway 迁移。
2. 第二次测试断言失败：订单类站内消息 seed 实际为 6 条，不是 2 条。已修正测试断言为 `total=6`，并用 `messageId=2` 验证单条已读。
3. 新增优惠券库存边界测试时，H2 对 `Statement.RETURN_GENERATED_KEYS` 返回多个生成列，导致 `GeneratedKeyHolder.getKey()` 报错。已修正为明确只回取 `id` 列。

## 5. 手动复验建议

1. 运行上述 `mvn test` 命令，确认 5 个测试全部通过。
2. 登录后台管理员 `demo_admin / 123456`，访问「管理员资料」，确认只读资料页正常。
3. 登录用户 `demo_user / 123456`，确认会员中心显示普通会员、成长值 128、距银卡 172。
4. 进入优惠券页与下单选券页，确认可用券与抵扣金额展示正常。
5. 进入消息页，点击订单类消息或执行单条已读，确认未读状态变化。

## 6. 潜在问题与说明

- 修改既有 Flyway 版本迁移文件后，如果某个持久化 MySQL 环境已经执行过旧版 V008，Flyway 可能出现 checksum 不一致；课程演示/本地测试环境可重建库或执行 Flyway repair 后继续。
- H2 测试环境只用于后端接口集成测试，不替代 MySQL 生产兼容验证。
- 当前测试覆盖 Stage6 分工A 的关键接口冒烟、角色拦截与限量优惠券库存边界；插入 ID 回取方式已在 `docs/stage6/分工A-后端插入ID回取优化说明.md` 中完成优化说明，优惠券库存并发安全已在 `docs/stage6/分工A-后端优惠券并发安全优化说明.md` 中完成优化说明。
