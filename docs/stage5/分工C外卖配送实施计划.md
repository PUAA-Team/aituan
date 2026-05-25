# Stage 5 分工 C 外卖配送实施计划

## 1. 文档状态

- 所属阶段：Stage 5
- 所属分工：成员 C，外卖供给、点餐交易、配送履约
- 当前状态：已完成编码实施与全端构建验证
- 代码分支：`stage5-c-takeaway-delivery`
- 文档目标：在动工前明确现状、缺口、接口口径、实施顺序、验收标准和风险降级，确认后再修改业务代码。

## 2. 分工 C 总目标

本分工负责外卖业务从用户点餐到配送完成的端到端闭环：

```text
用户端浏览外卖商家
  -> 加购商品
  -> 确认订单
  -> mock 支付
  -> 商家端接单 / 拒单 / 备餐 / 出餐
  -> 后端模拟配送推进
  -> 用户端查看订单详情和配送跟踪
  -> 后台端查看订单和配送状态
  -> 订单完成后进入评价入口
```

本阶段不接真实支付、真实骑手端、真实地图轨迹和真实配送平台。配送可退化为状态时间线和预计送达说明。

## 3. 现状调研结论

### 3.1 用户端 Flutter 现状

关键文件：

- `apps/user_app/lib/features/merchant/presentation/takeaway_merchant_page.dart`
- `apps/user_app/lib/features/merchant/presentation/takeaway_cart_sheet.dart`
- `apps/user_app/lib/features/merchant/presentation/takeaway_merchant_sections.dart`
- `apps/user_app/lib/features/merchant/presentation/takeaway_merchant_widgets.dart`
- `apps/user_app/lib/features/checkout/presentation/checkout_page.dart`
- `apps/user_app/lib/features/order/presentation/orders_page.dart`
- `apps/user_app/lib/features/order/presentation/takeaway_order_detail_page.dart`
- `apps/user_app/lib/features/home/data/backend_app_repository.dart`
- `apps/user_app/lib/app/router.dart`
- `apps/user_app/lib/app/route_args.dart`
- `apps/user_app/lib/core/constants/route_constants.dart`
- `apps/user_app/lib/shared/enums/business_type.dart`

已具备：

- 外卖商家页能加载门店、展示分类商品、加减购、打开购物车弹层。
- 登录后能从外卖商家页进入确认订单页。
- 确认订单页已接入预览、创建订单、mock 支付。
- 订单列表能进入外卖订单详情。
- 外卖订单详情已能拉取订单详情并展示内嵌配送时间线。
- `backendRepository.fetchDeliveryTimeline(orderId)` 已存在，但当前没有独立配送跟踪页面使用。
- 页面已具备刷新和返回后更新的基础能力。

主要缺口：

- 没有独立 `/delivery/tracking/:orderId` 配送跟踪页。
- 购物车仅是外卖商家页内存状态，没有持久化购物车接口。
- 购物车弹层能力较浅，缺清空、弹层内加减、库存/售罄提醒。
- 外卖商家页信息不够完整，缺起送价、配送范围、包装费/服务费、营业状态、商家公告等真实外卖决策信息。
- 确认订单页缺地址选择、备注强化、支付方式选择、配送不可达提示、金额刷新提示。
- 外卖订单状态仍依赖粗粒度 `unpaid / pending / used` 展示，缺接单、备餐、出餐、配送中、送达等细阶段展示。
- 缺取消、拒单、催单、联系商家、再来一单等入口或占位。
- 部分 mock 字母 ID 进入后端链路时可能不可用，需确保真实后端数据路径优先。

### 3.2 后端现状

关键文件：

- `services/backend/src/main/java/com/aituan/discovery/DiscoveryController.java`
- `services/backend/src/main/java/com/aituan/trade/TradeController.java`
- `services/backend/src/main/java/com/aituan/trade/TradeService.java`
- `services/backend/src/main/java/com/aituan/trade/TradeRepository.java`
- `services/backend/src/main/java/com/aituan/trade/TradeOpsController.java`
- `services/backend/src/main/java/com/aituan/trade/TradeDeliveryScheduler.java`
- `services/backend/src/main/java/com/aituan/trade/TradeDtos.java`
- `database/migrations/V002__init_merchant_catalog.sql`
- `database/migrations/V003__init_trade_review_message.sql`
- `database/migrations/V004__init_indexes.sql`
- `database/seeds/R__seed_demo_data.sql`

已有表和字段：

- `cart`、`cart_item` 表已经存在。
- `order_main` 已有 `display_status`、`payment_status`、`fulfillment_status`。
- `delivery_task` 已有 `current_stage`、`current_stage_text`、`eta_minutes`、`next_tick_at`、`completed_at`。
- `delivery_track_node` 已有配送节点。
- `order_state_log` 已有状态流转日志表。
- `sys_audit_log` 已有审计表。

已有接口和能力：

- 用户端发现接口：首页、推荐、模块、搜索、门店详情、商品详情。
- 用户端交易接口：确认订单预览、创建订单、mock 支付、订单列表、订单详情、配送时间线。
- 支付后外卖订单会创建 `delivery_task` 和时间线节点。
- 已有 `/api/merchant/trade` 与 `/api/admin/trade` 的订单详情、配送推进、券码核销接口。
- 定时任务 `TradeDeliveryScheduler` 可推进到期配送任务。

主要缺口和风险：

- `cart` 表存在，但没有购物车增删改查 Controller。
- 外卖商品、SKU、库存、配送规则没有商家端维护接口。
- 商家端订单动作没有拆分：缺接单、拒单、开始备餐、出餐、完成等明确接口。
- 当前支付后直接把外卖订单推进到 `fulfillment_status = delivering`，并插入 `accepted` 节点，缺“待商家接单”。
- 当前 `advanceDelivery` 只有 `accepted -> preparing -> delivering -> delivered`，无法表达待接单、拒单、出餐、待配送、送达后完成等阶段。
- 配送完成时 `display_status` 写成 `used`，前端通过展示层修正为“已完成”，但数据语义仍不够清晰。
- `order_state_log` 和 `sys_audit_log` 已建表但当前外卖状态动作未形成完整写入闭环。
- 商家端接口目前未校验订单归属，MERCHANT 角色可能查看或推进非所属商家的订单，存在权限风险。
- 库存在创建订单时即扣减，未支付取消或超时关闭暂未回补，本阶段可先保留风险说明，不做复杂库存锁。

### 3.3 商家端和后台端现状

调研结论：当前仓库未发现完整商家端 Web 或后台端 Web 工程。

已有：

- 文档中已规划商家端和后台端路由。
- 后端已存在少量 `/api/merchant/trade` 和 `/api/admin/trade` 接口。
- 数据库已有商家、门店、商品、配送规则、订单、配送任务、审计相关表。

缺失：

- 没有 `apps/merchant_web` 或 `apps/admin_web` 工程。
- 没有商家端登录、外卖商品维护、配送规则维护、外卖订单处理页面。
- 没有后台端外卖订单治理、配送状态查看、外卖统计页面。
- 没有商家端和后台端 Web 的构建、运行、联调说明。

建议：Stage5-C 若得到确认，应先建立最小可演示 Web 骨架；技术栈按 Stage1 架构规划采用 Vue 3 + Vite + TypeScript，具体创建工程前按高风险操作规则再次确认。

## 4. 本阶段范围

### 4.1 必做范围

#### 用户端 APP

- 外卖商家页信息增强。
- 外卖购物车体验增强。
- 外卖确认订单增强。
- 外卖订单详情状态增强。
- 独立配送跟踪页。
- 订单返回刷新和下拉刷新继续保持。

#### 后端

- 外卖购物车接口。
- 外卖订单状态流转细化。
- 商家外卖商品和配送规则接口。
- 商家外卖订单处理接口。
- 配送时间线和配送推进接口增强。
- 后台外卖订单查询、状态查看和简单人工干预接口。
- 外卖统计接口。
- 状态日志和审计日志写入。
- 商家订单归属权限校验。

#### 商家端 Web

- 商家登录。
- 商家工作台。
- 外卖商品列表和编辑。
- 配送规则维护。
- 外卖订单列表和详情。
- 接单、拒单、备餐、出餐、完成等操作。

#### 后台端 Web

- 后台登录。
- 外卖订单列表和详情。
- 配送状态查看。
- 简单人工干预。
- 外卖统计卡片。

#### 数据和演示

- 外卖商家、分类、商品、SKU、配送规则补充。
- 外卖订单覆盖待付款、待接单、备餐中、待配送、配送中、已送达、已完成等状态。
- 配送时间线节点覆盖不同阶段。
- 商家端和后台端可直接看到演示数据。

### 4.2 本阶段不做

- 真实微信/支付宝支付。
- 真实骑手端。
- 真实地图轨迹。
- 真实配送平台接入。
- 高并发库存锁、库存冻结和库存流水。
- 完整退款审核和财务资金流；本分工 C 只做外卖取消/拒单/异常备注等状态占位。
- 完整后台治理系统。
- 完整 AI 能力。
- 非外卖券码、预约和核销，这部分归分工 D。

## 5. 状态口径设计

### 5.1 用户端展示状态

| 展示状态 | 说明 | 来源建议 |
| --- | --- | --- |
| 待付款 | 已创建但未支付 | `payment_status = unpaid` |
| 待商家接单 | 已支付，等待商家处理 | `fulfillment_status = merchant_pending` |
| 商家已接单 | 商家已接受订单 | `fulfillment_status = accepted` |
| 备餐中 | 商家正在制作 | `delivery_task.current_stage = preparing` |
| 待配送 | 商家已出餐，等待模拟配送 | `delivery_task.current_stage = ready_for_delivery` |
| 配送中 | 模拟配送中 | `delivery_task.current_stage = delivering` |
| 已送达 | 模拟配送已送达，等待完成 | `delivery_task.current_stage = delivered` |
| 已完成 | 订单完成，可评价 | `display_status = used` 且 `order_type = takeaway` 时 UI 显示“已完成” |
| 已取消 | 订单取消或商家拒单 | 需要新增/复用取消状态 |

说明：

- 为避免大范围破坏现有用户端订单筛选，本阶段可以继续保留 `display_status` 的粗粒度：`unpaid / pending / used`。
- 外卖细阶段主要使用 `fulfillment_status` 和 `delivery_task.current_stage` 表达。
- 前端展示时按 `order_type = takeaway` 区分“已完成”，不要显示“已使用”。
- 是否新增 `cancelled / refunded` 等 `display_status`，需要在编码前结合现有筛选和订单页再确认。

### 5.2 后端建议状态流转

```text
created
  -> merchant_pending       支付成功，等待商家接单
  -> accepted               商家接单
  -> preparing              备餐中
  -> ready_for_delivery     已出餐，等待配送
  -> delivering             配送中
  -> delivered              已送达
  -> completed              已完成
```

异常流：

```text
created -> cancelled               未支付取消或超时关闭
merchant_pending -> merchant_rejected / cancelled
accepted / preparing -> cancelled 或 abnormal
ready_for_delivery / delivering -> abnormal -> delivered / completed
```

### 5.3 商家动作

| 动作 | 建议接口 | 前置状态 | 后置状态 |
| --- | --- | --- | --- |
| 设置接单模式 | `GET/POST /api/merchant/trade/stores/{storeId}/takeaway-setting` | 门店外卖配置 | `manual / auto` |
| 接单 | `POST /api/merchant/trade/orders/{orderId}/accept` | `merchant_pending` | `accepted` |
| 拒单 | `POST /api/merchant/trade/orders/{orderId}/reject` | `merchant_pending` | `merchant_rejected / cancelled` |
| 开始备餐 | `POST /api/merchant/trade/orders/{orderId}/prepare` | `accepted` | `preparing` |
| 出餐 | `POST /api/merchant/trade/orders/{orderId}/ready` | `preparing` | `ready_for_delivery` |
| 开始配送 | `POST /api/merchant/trade/orders/{orderId}/dispatch` 或后台推进 | `ready_for_delivery` | `delivering` |
| 标记送达 | `POST /api/merchant/trade/orders/{orderId}/deliver` 或后台推进 | `delivering` | `delivered` |
| 完成订单 | `POST /api/merchant/trade/orders/{orderId}/complete` 或系统自动 | `delivered` | `completed` |

### 5.4 后台动作

| 动作 | 建议接口 | 说明 |
| --- | --- | --- |
| 查询外卖订单 | `GET /api/admin/trade/takeaway/orders` | 支持状态、商家、时间筛选 |
| 查看订单详情 | `GET /api/admin/trade/orders/{orderId}` | 可复用或增强现有接口 |
| 查看配送状态 | `GET /api/admin/trade/orders/{orderId}/delivery/timeline` | 展示配送任务和节点 |
| 人工推进 | `POST /api/admin/trade/orders/{orderId}/delivery/advance` | 增强现有接口，写审计 |
| 标记异常 | `POST /api/admin/trade/orders/{orderId}/abnormal` | 可先做占位状态和备注 |
| 查看统计 | `GET /api/admin/trade/takeaway/summary` | 今日订单、成交额、待处理、取消数 |

## 6. 详细实施清单

### 6.1 第 0 步：确认计划

- [x] 用户确认本文档范围、状态口径和实施顺序。
- [x] 用户确认是否创建 `apps/merchant_web` 和 `apps/admin_web` 最小 Web 工程。
- [x] 用户确认 Web 技术栈沿用 Vue 3 + Vite + TypeScript，并强调现代化、响应式、成熟大厂风格，不用蓝紫渐变和过多圆角。
- [x] 用户确认商家端和后台端本阶段只做外卖相关最小闭环。

### 6.2 第 1 步：文档与接口口径落地

- [x] 更新 `docs/ReadMe.md` 索引。
- [x] 在本文档中记录最终确认的状态枚举。
- [x] 明确用户端、商家端、后台端外卖相关接口清单。
- [x] 明确需要新增的迁移和 seed 范围。

验收：

- [x] 文档能指导编码，不存在“待确认但未标注”的关键口径。

### 6.3 第 2 步：后端状态和接口增强

涉及文件初步预计：

- `services/backend/src/main/java/com/aituan/trade/TradeController.java`
- `services/backend/src/main/java/com/aituan/trade/TradeOpsController.java`
- `services/backend/src/main/java/com/aituan/trade/TradeService.java`
- `services/backend/src/main/java/com/aituan/trade/TradeRepository.java`
- `services/backend/src/main/java/com/aituan/trade/TradeDtos.java`
- `services/backend/src/main/java/com/aituan/trade/TradeDeliveryScheduler.java`
- `database/migrations/Vxxx__stage5_takeaway_delivery.sql`
- `database/seeds/R__seed_demo_data.sql`

任务：

- [x] 支付成功后外卖订单进入 `merchant_pending`，不再直接“商家已接单”；门店设置为自动接单时直接进入 `accepted`。
- [x] 新增商家自动接单/手动接单配置接口。
- [x] 新增商家接单、拒单、备餐、出餐、完成等接口。
- [x] 增强配送推进逻辑，支持 `ready_for_delivery -> delivering -> delivered -> completed`。
- [x] 为商家端订单接口增加所属商家校验。
- [x] 为后台端订单接口增加管理员权限边界。
- [x] 在关键状态变化时写入 `order_state_log`。
- [x] 在商家动作、后台人工干预时写入 `sys_audit_log`。
- [x] 新增或增强外卖订单列表接口，支持状态筛选和分页。
- [x] 新增外卖统计接口。
- [x] 新增购物车接口，至少支持查询、加购、改数量、删除、清空。
- [x] 本阶段复用已有 `cart` / `cart_item`、履约、审计表，无需新增 Flyway 迁移。

验收：

- [x] 用户支付外卖订单后，后端状态为待商家接单；自动接单门店进入已接单。
- [x] 商家接单后，用户端详情能看到状态变化。
- [x] 商家备餐、出餐、配送推进后，配送时间线节点依次点亮。
- [x] 后台人工推进能记录审计。
- [x] 非所属商家不能操作其他商家订单。

### 6.4 第 3 步：用户端 APP 外卖链路增强

涉及文件初步预计：

- `apps/user_app/lib/app/router.dart`
- `apps/user_app/lib/core/constants/route_constants.dart`
- `apps/user_app/lib/app/route_args.dart`
- `apps/user_app/lib/features/home/data/backend_app_repository.dart`
- `apps/user_app/lib/features/merchant/presentation/takeaway_merchant_page.dart`
- `apps/user_app/lib/features/merchant/presentation/takeaway_cart_sheet.dart`
- `apps/user_app/lib/features/merchant/presentation/takeaway_merchant_sections.dart`
- `apps/user_app/lib/features/merchant/presentation/takeaway_merchant_widgets.dart`
- `apps/user_app/lib/features/checkout/presentation/checkout_page.dart`
- `apps/user_app/lib/features/order/presentation/takeaway_order_detail_page.dart`
- 新增 `apps/user_app/lib/features/order/presentation/delivery_tracking_page.dart`

任务：

- [x] 外卖商家页展示起送价、配送费、预计送达、配送范围、营业状态、公告。
- [x] 商品卡展示库存或售罄状态，售罄不可加购。
- [x] 购物车弹层支持加减数量、删除单项、清空。
- [x] 购物车金额、配送费、实付金额与后端 preview 对齐。
- [x] 确认订单页增强地址、备注、支付方式、费用明细展示。
- [x] 订单详情按 `fulfillmentStatus` 展示外卖细状态。
- [x] 订单详情增加“配送跟踪”入口。
- [x] 新增配送跟踪页，展示当前阶段、节点时间线、地址文字信息。
- [x] 配送跟踪页支持下拉刷新。
- [x] 保持返回订单列表后自动刷新。

验收：

- [x] 用户能从外卖商家页完成加购和提交订单。
- [x] 支付后订单详情显示“待商家接单”。
- [x] 商家推进状态后，用户端刷新能看到接单、备餐、配送中、已送达、已完成。
- [x] 配送跟踪页可进入、可刷新、无地图时正常展示文字时间线。

### 6.5 第 4 步：商家端最小外卖闭环

前置确认：如果用户确认创建 Web 工程，则新增商家端最小工程；如果后续其他成员已经创建统一商家端工程，则在统一工程内添加外卖模块。

建议目录：

- `apps/merchant_web/`

建议页面：

- `/login`
- `/dashboard`
- `/catalog/goods`
- `/catalog/goods/create`
- `/catalog/goods/:itemId/edit`
- `/store/delivery-rule`
- `/orders`
- `/orders/:orderId`

任务：

- [x] 商家登录和 token 保存。
- [x] 商家工作台展示待接单、备餐中、配送中等统计。
- [x] 外卖商品列表支持查看、筛选、上下架。
- [x] 商品编辑支持标题、副标题、价格、库存、状态基础字段。
- [x] 配送规则维护支持起送价、配送费、预计时长、配送范围说明。
- [x] 门店接单模式支持手动接单 / 自动接单切换。
- [x] 外卖订单列表支持状态筛选。
- [x] 外卖订单详情展示商品、金额、备注、地址、时间线。
- [x] 订单列表支持接单、拒单、备餐、出餐、完成。

验收：

- [x] 用户支付后，商家端订单列表能看到待接单。
- [x] 商家点击接单后，用户端订单状态变为商家已接单。
- [x] 商家推进备餐/出餐后，用户端配送时间线变化。
- [x] 商家不能查看或操作非本店订单。

### 6.6 第 5 步：后台端外卖治理最小闭环

前置确认：如果用户确认创建 Web 工程，则新增后台端最小工程；如果后续其他成员已经创建统一后台端工程，则在统一工程内添加外卖治理模块。

建议目录：

- `apps/admin_web/`

建议页面：

- `/login`
- `/dashboard`
- `/orders`
- `/orders/:orderId`
- `/orders/delivery`

任务：

- [x] 后台登录和 token 保存。
- [x] 后台首页展示外卖状态统计。
- [x] 外卖订单列表支持状态筛选。
- [x] 外卖订单详情展示商家、商品、金额、状态流转。
- [x] 配送状态查看展示 `delivery_task` 和 `delivery_track_node`。
- [x] 人工推进配送状态。
- [x] 标记异常、填写备注、取消/拒单状态占位。
- [x] 不实现完整退款审核和财务退款流，只保留与外卖履约相关的异常入口。
- [x] 后台干预写入审计日志，并在后台侧栏说明留痕口径。

验收：

- [x] 后台能查看外卖订单和配送状态。
- [x] 后台人工推进后，用户端配送跟踪同步变化。
- [x] 后台操作写入审计日志。

### 6.7 第 6 步：演示数据补齐

涉及文件：

- `database/seeds/R__seed_demo_data.sql`

任务：

- [x] 外卖商家至少 3-5 个，覆盖不同配送费、起送价、预计时长。
- [x] 每个外卖商家至少 2-3 个分类。
- [x] 每个分类至少 2-4 个商品。
- [x] 商品覆盖正常可售、售罄、不同库存、不同销量。
- [x] 外卖订单覆盖：待付款、待商家接单、商家已接单、备餐中、待配送、配送中、已送达、已完成。
- [x] 配送时间线覆盖不同节点。
- [x] 商家端和后台端默认账号能看到相关订单。

验收：

- [x] demo profile 启动后不需要手工造数据即可演示完整外卖链路。
- [x] seed 幂等，多次执行不重复制造脏数据。

### 6.8 第 7 步：验证和构建

后端验证：

- [x] Maven 构建通过。
- [x] 后端 jar 构建通过。
- [x] demo profile 启动通过。
- [x] health 冒烟通过。
- [x] 外卖订单、购物车、商家端、后台端接口冒烟通过。

用户端验证：

- [x] `dart format apps/user_app/lib apps/user_app/test`
- [x] `flutter analyze apps/user_app`
- [x] `cd apps/user_app && flutter test`
- [x] Android APK 打包通过。

商家端/后台端验证：

- [x] 商家端和后台端 Web 构建通过。
- [x] 商家端外卖订单处理、商品和配送规则接口演示通过。
- [x] 后台端外卖治理详情和配送时间线演示通过。

最终产物：

```text
D:/aituan_release/backend/aituan-backend.jar
D:/aituan_release/apk/aituan-user-debug.apk
```

## 7. 接口清单草案

### 7.1 用户端接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/app/trade/cart?storeId={storeId}` | 查询当前用户某店购物车 |
| `POST` | `/api/app/trade/cart/items` | 加购商品 |
| `PUT` | `/api/app/trade/cart/items/{itemId}` | 修改数量 |
| `DELETE` | `/api/app/trade/cart/items/{itemId}` | 删除购物项 |
| `DELETE` | `/api/app/trade/cart?storeId={storeId}` | 清空某店购物车 |
| `POST` | `/api/app/trade/checkout/preview` | 订单预览，沿用现有接口并增强费用字段 |
| `POST` | `/api/app/trade/orders` | 创建订单，沿用现有接口 |
| `POST` | `/api/app/trade/orders/{orderId}/pay` | mock 支付，沿用现有接口但调整支付后状态 |
| `GET` | `/api/app/trade/orders/{orderId}` | 订单详情，沿用并增强外卖细阶段 |
| `GET` | `/api/app/trade/orders/{orderId}/delivery/timeline` | 配送跟踪，沿用并增强字段 |

### 7.2 商家端接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/merchant/trade/orders` | 外卖订单列表，支持 `fulfillmentStatus`、分页 |
| `GET` | `/api/merchant/trade/orders/stats` | 外卖状态统计 |
| `GET` | `/api/merchant/trade/orders/{orderId}` | 外卖订单详情，增强所属商家校验 |
| `POST` | `/api/merchant/trade/orders/{orderId}/accept` | 接单 |
| `POST` | `/api/merchant/trade/orders/{orderId}/reject` | 拒单 |
| `POST` | `/api/merchant/trade/orders/{orderId}/prepare` | 开始备餐 |
| `POST` | `/api/merchant/trade/orders/{orderId}/ready` | 出餐/待配送 |
| `POST` | `/api/merchant/trade/orders/{orderId}/delivery/advance` | 推进模拟配送 |
| `POST` | `/api/merchant/trade/orders/{orderId}/complete` | 完成订单或确认完成 |
| `GET` | `/api/merchant/trade/stores/{storeId}/takeaway-setting` | 查询手动/自动接单模式 |
| `POST` | `/api/merchant/trade/stores/{storeId}/takeaway-setting` | 设置手动/自动接单模式 |
| `GET` | `/api/merchant/trade/stores/{storeId}/items` | 外卖商品列表 |
| `POST` | `/api/merchant/trade/stores/{storeId}/items/{itemId}` | 编辑外卖商品基础字段 |
| `POST` | `/api/merchant/trade/stores/{storeId}/items/{itemId}/status` | 上下架/售罄 |
| `GET` | `/api/merchant/trade/stores/{storeId}/delivery-rule` | 查询配送规则 |
| `POST` | `/api/merchant/trade/stores/{storeId}/delivery-rule` | 更新配送规则 |

### 7.3 后台端接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/admin/trade/orders` | 外卖订单治理列表，支持 `fulfillmentStatus`、分页 |
| `GET` | `/api/admin/trade/orders/stats` | 外卖状态统计 |
| `GET` | `/api/admin/trade/orders/{orderId}` | 订单详情，内含配送时间线 |
| `POST` | `/api/admin/trade/orders/{orderId}/delivery/advance` | 人工推进配送 |
| `POST` | `/api/admin/trade/orders/{orderId}/abnormal` | 标记异常占位并写备注 |

## 8. 需要确认的决策点

编码前需要用户确认：

1. 是否同意在本分支创建商家端 Web 最小工程 `apps/merchant_web/`。结论：同意。
2. 是否同意在本分支创建后台端 Web 最小工程 `apps/admin_web/`。结论：同意。
3. Web 技术栈是否按文档规划使用 Vue 3 + Vite + TypeScript。结论：同意；前端需要现代化、美观、响应式布局，避免蓝色/紫色等渐变色，不做炫酷风格，不使用过多圆角，整体多参考成熟大厂管理端并保留爱团项目风味。
4. 外卖 `display_status` 是否继续保持粗粒度，细阶段通过 `fulfillment_status` + `delivery_task.current_stage` 表达。结论：采用此方案。通俗说明：订单列表筛选继续沿用现有“待付款 / 进行中 / 已完成”粗分类，外卖详情、商家端和后台端再用更细字段展示“待接单、备餐中、待配送、配送中、已送达”等阶段，避免改动过大导致现有订单列表失效。
5. 配送跟踪是否暂不做地图，只做时间线和文字地址。结论：同意，先做状态时间线和文字地址。
6. 库存是否本阶段只做基础扣减和售罄展示，不处理未支付取消回补、冻结库存和并发锁。结论：同意，但代码和表设计要保留后续扩展空间；复杂库存冻结、回补、并发锁和库存流水继续记录到长期待办。
7. 后台人工干预是否只做“推进/标记异常/备注”，不做完整退款和财务流程。结论：后台人工干预要做，但退款与成员 D/E 的非外卖履约、售后投诉存在交叉；本分工 C 只负责外卖订单的异常标记、配送推进、取消/拒单状态占位和备注记录，不实现完整退款资金流、退款审核流程和财务结算。

## 9. 风险和降级方案

| 风险 | 影响 | 降级方案 |
| --- | --- | --- |
| 商家端和后台端工程不存在 | 工期增加 | 先建最小骨架，只做外卖相关页面，不做全量后台系统 |
| 外卖状态改动影响已有订单筛选 | 用户端订单列表异常 | 保留 `display_status` 粗粒度，细阶段单独展示 |
| 商家动作和定时配送推进冲突 | 状态跳跃 | 商家动作和 scheduler 都走同一服务方法，状态机校验前置状态 |
| 权限归属未处理 | 商家越权操作 | 商家端接口必须校验订单所属门店或商家 |
| 购物车持久化影响现有内存购物车 | 前端改动较大 | 先保留内存购物车，后接后端购物车接口；接口失败可回退本地态 |
| seed 覆盖状态多 | 幂等脚本复杂 | 使用固定 ID 和 `ON DUPLICATE KEY UPDATE`，不清库 |
| C 盘空间不足 | 构建失败 | 继续使用 D 盘缓存、构建和发布目录 |

## 10. 用户确认后第一批动工建议

确认后建议按以下顺序动工：

1. 后端状态机和商家动作接口。
2. 外卖演示 seed。
3. 用户端外卖订单详情和配送跟踪页。
4. 商家端最小订单处理页面。
5. 后台端最小订单治理页面。
6. 购物车持久化和商品/配送规则维护。
7. 全链路联调、构建、打包。

原因：先把状态和订单动作打通，用户端、商家端、后台端才能围绕同一条演示链路开发，避免页面先做完但后端状态不支持。

## 11. 最终交付记录

### 11.1 已完成范围

- 后端：外卖细状态机、手动/自动接单、商家订单动作、后台人工推进/异常、购物车最小接口、商品上下架/编辑、配送规则维护、状态日志和审计日志写入。
- 用户端 APP：外卖商家信息、库存/售罄、购物车弹层、起送价和配送费、确认订单备注/支付方式、外卖细状态、配送跟踪页、返回刷新和下拉刷新。
- 商家端 Web：Vue 3 + Vite + TypeScript 最小工作台，支持订单处理、订单详情时间线、商品管理、配送规则、接单模式切换。
- 后台端 Web：Vue 3 + Vite + TypeScript 外卖治理页，支持统计、状态筛选、订单详情、配送时间线、人工推进、异常标记和审计留痕说明。
- 演示数据：外卖商家、商品/SKU、配送规则、订单状态、配送时间线节点覆盖完整演示链路。

### 11.2 验证记录

- 后端构建：`scripts/build/build_backend.ps1`，通过，产物 `D:/aituan_release/backend/aituan-backend.jar`。
- 后端基础冒烟：`D:/aituan_runtime/backend/stage5_c_backend_smoke.ps1`，通过。
- 购物车接口冒烟：`D:/aituan_runtime/backend/stage5_c_cart_smoke.ps1`，通过。
- 商家端接口冒烟：`D:/aituan_runtime/backend/stage5_c_merchant_smoke.ps1`，通过。
- 后台端接口冒烟：`D:/aituan_runtime/backend/stage5_c_admin_smoke.ps1`，通过。
- 用户端格式化：`dart format apps/user_app/lib apps/user_app/test`，通过，0 个文件变更。
- 用户端静态分析：`flutter analyze apps/user_app`，通过。
- 用户端测试：`cd apps/user_app && flutter test`，通过。
- 用户端 APK 打包：`scripts/build/build_android_apk.ps1`，通过，产物 `D:/aituan_release/apk/aituan-user-debug.apk`。
- 商家端 Web 构建：`npm run build --prefix apps/merchant_web`，通过。
- 后台端 Web 构建：`npm run build --prefix apps/admin_web`，通过。

### 11.3 产物路径

```text
D:/aituan_release/backend/aituan-backend.jar
D:/aituan_release/apk/aituan-user-debug.apk
```

### 11.4 保留边界

- 本阶段仍不做真实支付、真实骑手端、真实地图轨迹、真实退款财务流和复杂库存冻结/回补；这些事项继续按 `docs/后续功能待办.md` 处理。
