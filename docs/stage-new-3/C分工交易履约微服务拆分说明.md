# C 分工交易履约微服务拆分说明

> 日期：2026-08-31
> 分支：`microservice-c-trade-fulfillment`
> 服务：`trade-fulfillment-service`
> 目标库：`aituan_trade`

## 1. 文档定位

本文是 C 分工开始拆交易与履约微服务前的边界冻结文档，用来统一以下内容：

- C 服务负责哪些 API；
- C 服务负责哪些业务表；
- 哪些路径虽然带有 `trade` 或 `delivery`，但不归 C 服务；
- 从单体迁移时需要重点改造的跨服务依赖；
- 第一轮实现与验证范围。

当前只先推进工程基建和独立库迁移，不一次性搬完整订单业务链路。

## 2. C 服务职责边界

C 服务中文名：交易与履约服务。

英文工程名：`trade-fulfillment-service`。

默认端口：`8083`。

主要负责：

- 购物车；
- 结算预览；
- 订单创建、查询、取消；
- mock 支付；
- 退款主流程；
- 非外卖券码生成、查询、核销；
- 预约记录与商家确认；
- 外卖配送任务、配送时间线、配送状态推进；
- 订单状态流转日志。

## 3. C 服务负责的数据表

目标逻辑库为 `aituan_trade`。C 服务只拥有以下 11 张表：

| 表 | 用途 |
| --- | --- |
| `cart` | 用户按门店分组的购物车 |
| `cart_item` | 购物车商品明细 |
| `order_main` | 订单主体、金额、状态和快照 |
| `order_item` | 订单商品或服务快照 |
| `order_payment_record` | mock 支付流水 |
| `order_voucher` | 非外卖电子券码与核销状态 |
| `order_booking_record` | 预约信息与确认状态 |
| `order_refund_record` | 退款申请、结果和补偿轨迹 |
| `order_state_log` | 订单状态流转日志 |
| `delivery_task` | 外卖配送模拟任务 |
| `delivery_track_node` | 配送时间线节点 |

禁止在 C 服务里直接读写其他服务表，例如：

- `user_address`；
- `user_coupon`；
- `coupon_template`；
- `member_growth_log`；
- `merchant_store`；
- `merchant_delivery_rule`；
- `catalog_item`；
- `catalog_sku`；
- `review_record`；
- `support_station_message`；
- `sys_audit_log`。

## 4. API 追溯清单

### 4.1 用户端交易 API

| 方法 | 路径 | 当前单体入口 | 目标归属 | 第一轮是否迁移 |
| --- | --- | --- | --- | --- |
| GET | `/api/app/trade/payment-methods` | `TradeController` | C 服务 | 阶段 2 |
| GET | `/api/app/trade/cart` | `TradeController` | C 服务 | 阶段 2 |
| DELETE | `/api/app/trade/cart` | `TradeController` | C 服务 | 阶段 2 |
| POST | `/api/app/trade/cart/items` | `TradeController` | C 服务 | 阶段 2 |
| PUT | `/api/app/trade/cart/items/{itemId}` | `TradeController` | C 服务 | 阶段 2 |
| DELETE | `/api/app/trade/cart/items/{itemId}` | `TradeController` | C 服务 | 阶段 2 |
| POST | `/api/app/trade/checkout/preview` | `TradeController` | C 服务 | 阶段 2 |
| GET | `/api/app/trade/orders` | `TradeController` | C 服务 | 阶段 3 |
| POST | `/api/app/trade/orders` | `TradeController` | C 服务 | 阶段 3 |
| GET | `/api/app/trade/orders/{orderId}` | `TradeController` | C 服务 | 阶段 3 |
| POST | `/api/app/trade/orders/{orderId}/pay` | `TradeController` | C 服务 | 阶段 3 |
| POST | `/api/app/trade/orders/{orderId}/refund` | `TradeController` | C 服务 | 阶段 3 |
| POST | `/api/app/trade/orders/{orderId}/cancel` | `TradeController` | C 服务 | 阶段 3 |
| POST | `/api/app/trade/orders/{orderId}/remind` | `TradeController` | C 服务 | 阶段 3 |
| PUT | `/api/app/trade/orders/{orderId}/delivery-address` | `TradeController` | C 服务 | 阶段 3 |
| GET | `/api/app/trade/orders/{orderId}/delivery/timeline` | `TradeController` | C 服务 | 阶段 3 |
| GET | `/api/app/trade/orders/{orderId}/booking` | `TradeController` | C 服务 | 阶段 3 |
| POST | `/api/app/trade/orders/{orderId}/booking` | `TradeController` | C 服务 | 阶段 3 |

### 4.2 商家端与后台交易 API

| 方法 | 路径 | 当前单体入口 | 目标归属 | 第一轮是否迁移 |
| --- | --- | --- | --- | --- |
| GET | `/api/merchant/trade/orders` | `TradeOpsController` | C 服务 | 阶段 4 |
| GET | `/api/admin/trade/orders` | `TradeOpsController` | C 服务 | 阶段 4 |
| GET | `/api/merchant/trade/orders/stats` | `TradeOpsController` | C 服务 | 阶段 4 |
| GET | `/api/admin/trade/orders/stats` | `TradeOpsController` | C 服务 | 阶段 4 |
| GET | `/api/merchant/trade/orders/{orderId}` | `TradeOpsController` | C 服务 | 阶段 4 |
| GET | `/api/admin/trade/orders/{orderId}` | `TradeOpsController` | C 服务 | 阶段 4 |
| POST | `/api/merchant/trade/orders/{orderId}/accept` | `TradeOpsController` | C 服务 | 阶段 4 |
| POST | `/api/merchant/trade/orders/{orderId}/reject` | `TradeOpsController` | C 服务 | 阶段 4 |
| POST | `/api/merchant/trade/orders/{orderId}/prepare` | `TradeOpsController` | C 服务 | 阶段 4 |
| POST | `/api/merchant/trade/orders/{orderId}/ready` | `TradeOpsController` | C 服务 | 阶段 4 |
| POST | `/api/merchant/trade/orders/{orderId}/delivery/advance` | `TradeOpsController` | C 服务 | 阶段 4 |
| POST | `/api/merchant/trade/orders/{orderId}/complete` | `TradeOpsController` | C 服务 | 阶段 4 |
| POST | `/api/merchant/trade/orders/{orderId}/abnormal` | `TradeOpsController` | C 服务 | 阶段 4 |
| POST | `/api/merchant/trade/orders/{orderId}/refund` | `TradeOpsController` | C 服务 | 阶段 4 |
| GET | `/api/merchant/trade/vouchers` | `TradeOpsController` | C 服务 | 阶段 3 |
| GET | `/api/admin/trade/vouchers` | `TradeOpsController` | C 服务 | 阶段 3 |
| GET | `/api/merchant/trade/vouchers/{voucherCode}` | `TradeOpsController` | C 服务 | 阶段 3 |
| GET | `/api/admin/trade/vouchers/{voucherCode}` | `TradeOpsController` | C 服务 | 阶段 3 |
| POST | `/api/merchant/trade/vouchers/{voucherCode}/redeem` | `TradeOpsController` | C 服务 | 阶段 3 |
| POST | `/api/admin/trade/vouchers/{voucherCode}/redeem` | `TradeOpsController` | C 服务 | 阶段 3 |
| GET | `/api/merchant/trade/bookings` | `TradeOpsController` | C 服务 | 阶段 3 |
| GET | `/api/admin/trade/bookings` | `TradeOpsController` | C 服务 | 阶段 3 |
| POST | `/api/merchant/trade/orders/{orderId}/booking/confirm` | `TradeOpsController` | C 服务 | 阶段 3 |
| POST | `/api/admin/trade/orders/{orderId}/booking/confirm` | `TradeOpsController` | C 服务 | 阶段 3 |

### 4.3 后台配送任务 API

| 方法 | 路径 | 当前单体入口 | 目标归属 | 第一轮是否迁移 |
| --- | --- | --- | --- | --- |
| GET | `/api/admin/delivery/tasks` | `AdminController` | C 服务 | 阶段 4 |
| GET | `/api/admin/delivery/tasks/{taskId}` | `AdminController` | C 服务 | 阶段 4 |
| POST | `/api/admin/delivery/tasks/{taskId}/advance` | `AdminController` | C 服务 | 阶段 4 |
| POST | `/api/admin/delivery/tasks/{taskId}/pause` | `AdminController` | C 服务 | 阶段 4 |
| POST | `/api/admin/delivery/tasks/{taskId}/resume` | `AdminController` | C 服务 | 阶段 4 |
| POST | `/api/admin/delivery/tasks/{taskId}/abnormal` | `AdminController` | C 服务 | 阶段 4 |

## 5. 明确不归 C 服务的路径

| 路径 | 目标归属 | 原因 |
| --- | --- | --- |
| `/api/merchant/trade/stores/**` | `merchant-catalog-service` | 操作门店、商品、接单设置、配送规则主数据 |
| `/api/admin/trade/stores/**` | `merchant-catalog-service` | 操作门店、商品、接单设置、配送规则主数据 |
| `/api/admin/delivery/settings` | `engagement-platform-service` 或平台配置能力 | 是平台配送模拟设置，不是具体配送任务 |
| `/api/app/account/coupons/**` | `identity-asset-service` 或资产/运营能力 | 用户优惠券资产归属不在 C 服务 |
| `/api/admin/operation/coupon-templates/**` | `identity-asset-service` 或平台/资产能力 | 优惠券模板管理不在 C 服务 |

Gateway 后续配置时必须优先匹配这些排除路径，不能简单把所有 `/api/**/trade/**` 转发到 C 服务。

## 6. 跨服务依赖改造清单

| 旧依赖 | 旧访问方式 | C 服务新接口 | 第一轮处理 |
| --- | --- | --- | --- |
| 用户地址 | 直接查 `user_address` | `IdentityClient` | stub / 后续 HTTP adapter |
| 商品与 SKU | 直接查 `catalog_item`、`catalog_sku` | `CatalogClient` | stub / 后续 HTTP adapter |
| 库存 | 直接更新 `catalog_sku` | `InventoryClient` | stub，禁止直写 catalog 库 |
| 门店与配送规则 | 直接查 `merchant_store`、`merchant_delivery_rule` | `CatalogClient` 或 `FulfillmentRuleClient` | stub |
| 优惠券 | 直接调用 `CouponService` / 查优惠券表 | `CouponClient` | stub / 后续 HTTP adapter |
| 会员成长 | 直接调用 `MemberService` | `MemberGrowthClient` 或事件 | no-op stub |
| 站内消息 | 直接调用 `StationMessagePublisher` | `MessageClient` 或事件 | no-op stub |
| 商家权限 | 直接查商家/门店关系 | `MerchantAuthClient` | stub |
| 地图距离 | 直接调用 `MapDistanceService` | `DistanceClient` | local/stub |
| 审计日志 | 直接写 `sys_audit_log` | `AuditClient` | no-op/log stub |

原则：C 服务内部业务代码只依赖这些接口，不直接依赖其他服务的 Repository、Service 或数据库表。

## 7. 第一轮实施范围

第一轮只做阶段 0 和阶段 1：

1. 本文档和 API 追溯清单。
2. `trade-fulfillment-service` 补齐独立运行需要的依赖。
3. 新增 H2 test profile。
4. 新增 C 服务独立 Flyway schema。
5. 新增最小 seed 或测试 fixture。
6. 新增最小 context/migration 测试，证明服务可以独立启动和空库迁移。

第一轮暂不迁真实业务 API，不改 Gateway 路由，不删除 legacy 交易代码。

## 8. 验证命令

```powershell
mvn -f services/pom.xml "-Dmaven.repo.local=D:\aituan_cache\m2" -pl common-contract,trade-fulfillment-service -am test
```

```powershell
mvn -f services/pom.xml "-Dmaven.repo.local=D:\aituan_cache\m2" -pl trade-fulfillment-service -am "-DskipTests" package
```

如后续改动影响 legacy 后端，再补跑：

```powershell
mvn -f services/pom.xml "-Dmaven.repo.local=D:\aituan_cache\m2" -pl backend -Dtest=TradeApiIntegrationTest,TradeLifecycleApiIntegrationTest,VoucherBookingApiIntegrationTest,TradeServiceBusinessRuleTest,TradeGrowthServiceTest test
```

## 9. 暂停点

遇到以下情况必须暂停并确认：

- H2 或 MySQL 空库迁移失败；
- C 服务必须直接访问其他服务表才能继续；
- 需要修改其他成员服务的接口定义；
- 需要删除 legacy `services/backend/src/main/java/com/aituan/trade/**`；
- 需要配置真实 MySQL 账号、Secret、K8s、CI/CD 或生产环境变量；
- 需要提交或推送阶段性改动。
