# Stage5-D 非外卖差异化、券码与预约交付说明

本文档是分工 D（"非外卖七类服务、差异化详情、券码预约"）在 Stage 5 的实施交付说明，覆盖团购、酒店、休闲娱乐、电影演出、丽人医美、景点门票、洗脚按摩 7 类业务的差异化展示与履约闭环。

## 1. 交付范围

按 `docs/后续阶段五人模块分工计划.md` 第 12 节"成员 D"约定的工作范围，本阶段完成以下三端 + 后端 + 数据闭环：

1. 用户端 APP：商品/服务详情页按 `businessType` 切换差异化字段；新增券码详情页与预约详情页；非外卖订单详情挂入相应入口。
2. 商家端控制台：新增"券码核销"和"预约确认"两个页面，支持按券码先查后销、按状态/业务类型筛选预约。
3. 后台端控制台：新增"券码治理"和"预约治理"页，支持平台代核销、平台代确认与多条件筛选。
4. 后端：扩展 `catalog_item` 表的差异化字段；新增 `order_booking_record` 表；扩展 Discovery、Trade 接口；提供商家端/后台端券码列表、预约列表、券码查询、预约确认接口。
5. 数据库迁移 V008 与 seed 演示数据：覆盖 7 类业务的差异化字段、预约样例、券码样例。

非交付范围（按"本阶段可后置"约定保持现状）：

- 真实房态、电影座位图、景点分时预约、复杂技师/房间/场地排班
- 复杂退款资金流（仅做入口与状态占位）
- 与第三方支付/配送的真实对接

## 2. 数据库变更（V008）

迁移脚本：`database/migrations/V008__stage5_d_service_attributes_and_booking.sql`

### 2.1 `catalog_item` 扩展字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `business_attributes` | VARCHAR(2000) | 差异化键值对串，格式 `key:value;key:value`；由用户端按 `businessType` 解析渲染 |
| `usage_rules` | VARCHAR(1000) | 使用规则文案 |
| `refund_policy` | VARCHAR(500) | 退改规则文案 |
| `notice` | VARCHAR(1000) | 注意事项文案 |
| `validity_days` | INT NOT NULL DEFAULT 90 | 券码有效天数，用于展示 |

为什么用字符串而不是 JSON：后端继续使用 MySQL 8，本阶段不强依赖 JSON 字段，字符串模式让 seed 维护、Flyway 演化、跨数据库迁移成本更低；用户端解析逻辑统一在 `lib/shared/models/business_attributes.dart`。

### 2.2 `order_voucher` 扩展字段

| 字段 | 说明 |
| --- | --- |
| `usage_rules_snapshot` | 下单时的使用规则快照，避免后续商家改规则影响已售券码 |
| `refund_policy_snapshot` | 退改规则快照 |
| `store_name_snapshot` | 店名快照 |

这些字段当前演示版本不强制写入，由后续承接成员逐步补齐。

### 2.3 `order_booking_record` 新表

| 字段 | 说明 |
| --- | --- |
| `order_id` | 关联订单，UNIQUE 索引保证一单一约 |
| `business_type` | 冗余订单业务类型，便于按类型筛选 |
| `contact_name` / `contact_phone` | 预约联系人 |
| `booking_date` | DATE 类型，预约日期 |
| `booking_time_slot` | 字符串，时段/场次/房型描述 |
| `guest_count` | 人数 |
| `store_confirm_status` | `pending` / `confirmed` |
| `store_confirm_remark` | 商家备注 |
| `confirmed_at` / `confirmed_by` | 确认时间和操作人 |

## 3. 后端接口

### 3.1 用户端 `/api/app/trade`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/orders/{orderId}/booking` | 查看本人订单的预约信息 |
| POST | `/orders/{orderId}/booking` | 创建或更新预约信息（联系人、日期、时段、人数、备注） |

`OrderDetailView` 同时增加了 `booking` 字段，订单详情接口可一次性返回券码与预约。

### 3.2 商家/后台 `/api/merchant/trade` + `/api/admin/trade`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/vouchers` | 分页查询券码（按状态、关键词），商家端默认按当前账号所属门店过滤 |
| GET | `/vouchers/{voucherCode}` | 查询单张券码的订单信息，用于核销前预览 |
| POST | `/vouchers/{voucherCode}/redeem` | 核销券码（沿用既有实现，校验已核销和过期） |
| GET | `/bookings` | 分页查询预约（按状态、业务类型） |
| POST | `/orders/{orderId}/booking/confirm` | 商家或平台确认预约，写入审计日志 |

所有商家端接口通过 `CurrentUserContext` + `merchantAccountScope` 限制为本人门店；后台端绕过限制看全部数据。券码查询接口 `lookupVoucher` 强制校验 store 归属，避免商家越权查询其他门店的券码。

### 3.3 服务层扩展

`TradeService` 新增：

- `upsertBooking(orderId, request)`：用户预约信息创建/更新
- `getBookingForUser(orderId)`：用户查看自己预约
- `confirmBookingForStaff(orderId, request)`：商家/平台确认预约 + 审计日志
- `listOpsBookings`、`listOpsVouchers`：分页查询
- `lookupVoucher(voucherCode)`：核销前预览

`DiscoveryService.toItemCard` 同步把 `business_attributes` 等差异化字段透出，用户端商品详情接口与首页/模块页推荐都会带回。

## 4. 用户端

### 4.1 商品详情差异化

`apps/user_app/lib/features/merchant/presentation/item_detail_page.dart` 在 `_InfoCard` 与 `_MerchantCard` 之间新增 `_ServiceDetailCard`：

- 解析 `item.businessAttributes` 为键值对列表渲染
- 若该字段为空则使用 `fallbackAttributes(type)` 兜底，避免演示数据缺失时空白
- 三个独立卡片：使用规则、退改规则、注意事项；任一为空时不渲染该卡片
- 解析逻辑封装在 `lib/shared/models/business_attributes.dart`，未来切换为 JSON 也可在此处统一替换

外卖类型不展示新卡片（保留外卖原有点单交互）。

### 4.2 券码详情页

`lib/features/order/presentation/voucher_detail_page.dart`，路由：`Routes.voucherDetail`

- 大尺寸二维码占位（CustomPainter 绘制规则图样，避免引入新依赖）
- 券码号大字号 + 横向 QR Payload
- "未核销" / "已核销" 状态标签
- 完整订单/有效期/金额/核销状态信息
- 通过 `OrderDetail.fetchOrderDetail` 复用既有接口，免去新增专属接口

### 4.3 预约详情页

`lib/features/order/presentation/booking_detail_page.dart`，路由：`Routes.bookingDetail`

- 顶部状态卡：未提交 / 等待商家确认 / 商家已确认
- 表单卡：联系人、手机号、日期、时段、人数（带 ±）、备注
- 底部"提交预约信息"按钮调用 `upsertBooking`
- 商家备注以独立行展示
- 仅 hotel/entertainment/movie/beauty/massage 五类业务订单详情会显示入口

### 4.4 服务订单详情入口

`service_order_detail_page.dart` 在状态卡和店铺卡之间插入两个新链接卡：

- `查看券码完整详情` → `voucherDetail`（凡是已支付订单都展示）
- `查看预约详情` 或 `提交到店预约信息` → `bookingDetail`（仅五类需要预约的业务）

入口的 subtitle 会根据当前预约状态动态变化（"提交联系人、日期" / "等待商家确认" / "已确认 日期 时段"）。

## 5. 商家端

### 5.1 ConsoleFrame 导航

非外卖商家登录后会自动看到新增的两个导航：

- 券码核销：`vouchers` 页
- 预约确认：`bookings` 页

外卖商家仍维持原有 4 项（订单/商品/履约/门店），避免无关业务干扰。

### 5.2 券码核销页

`apps/merchant_web/src/pages/VoucherPage.vue`

- 顶部"先查后销"区：输入券码 → 调 `lookupVoucher` → 显示订单详情和使用规则 → 点击"确认核销"才真正核销
- 下方"券码记录"表格：分页查询本店所有券码，按状态筛选（未核销 / 已核销 / 全部），支持券码号/订单号/标题模糊搜索
- 列表行内"核销"按钮可一键核销
- 全部接口通过 `/api/merchant/trade` 前缀，受 `merchantAccountScope` 自动限制

### 5.3 预约确认页

`apps/merchant_web/src/pages/BookingPage.vue`

- 顶部筛选：状态、业务类型、确认备注
- 表格列：订单 / 联系人 / 预约时间（日期 + 时段） / 人数 / 业务 / 状态 / 金额 / 操作
- 行内"确认预约"按钮调 `confirmBooking`，自动带上输入框的备注

## 6. 后台端

### 6.1 AdminFrame 导航

新增两个治理入口：券码治理 / 预约治理。

### 6.2 券码治理页

`apps/admin_web/src/pages/VouchersPage.vue`

- 全平台券码视图：状态过滤、关键词搜索
- 多一列"门店"，便于跨商家追踪
- "平台核销"按钮：用于商家无法操作时的人工兜底（沿用 `redeemVoucher`，但目标 URL 是 `/api/admin/trade/vouchers/{code}/redeem`，复用同一控制器映射）

### 6.3 预约治理页

`apps/admin_web/src/pages/BookingsPage.vue`

- 平台全量预约视图，可按状态、业务类型过滤
- "平台代确认"按钮调 `/api/admin/trade/orders/{orderId}/booking/confirm`
- 表格列同商家端，但展示门店名以便审计

## 7. 演示数据

`database/seeds/R__seed_demo_data.sql` 文件尾部新增 Stage5-D 段：

1. 给 18 个非外卖商品填充 `business_attributes` / `usage_rules` / `refund_policy` / `notice` / `validity_days`，覆盖 7 类业务
2. 补充酒店、SPA 订单的演示券码
3. 新增 3 条预约记录：
   - 9006（影音大床房）：pending，未确认
   - 9008（全身舒缓 SPA）：confirmed，已电话沟通
   - 4（经典足疗）：confirmed，已核销

seed 文件继续遵循"不清库、不删数据、幂等更新"原则，所有写入都用 `ON DUPLICATE KEY UPDATE` 或 `UPDATE WHERE id=...`。

## 8. 验证

| 验证项 | 命令 | 结果 |
| --- | --- | --- |
| 后端编译 | `mvn -q compile` | 通过 |
| Flutter 静态检查 | `flutter analyze` | `No issues found!` |
| Flutter 单元测试 | `flutter test` | `All tests passed!` |
| 商家端 Web 构建 | `npm run build` | 通过，输出 `apps/merchant_web/dist/` |
| 后台端 Web 构建 | `npm run build` | 通过，输出 `apps/admin_web/dist/` |

后端 jar 与 APK 打包需在用户已有的 D 盘构建环境下执行（脚本：`scripts/build/build_backend.ps1`、`scripts/build/build_android_apk.ps1`），本次代码变更未引入新的构建依赖，构建产物路径与 Stage5-C 一致。

## 9. 联调演示路径

1. 用户端：进入"团购/酒店/丽人/电影/景点/按摩/休闲娱乐"任一商品详情 → 看到差异化卡片
2. 用户端：选中非外卖商品下单 → 支付 → 订单详情 → 点击"查看券码完整详情"看大图二维码
3. 用户端：在酒店/SPA/按摩订单上点击"提交到店预约信息" → 填写联系人和日期 → 提交
4. 商家端：以非外卖商家登录（如 江南小馆/雅境足道）→ 进入"券码核销"页面 → 输入 `88001234` → 看到江南小馆的预览 → 核销
5. 商家端：进入"预约确认"页面 → 看到 SPA 待确认的预约 → 确认
6. 后台端：进入"券码治理"页面 → 看到所有商户的券码 → 可平台代核销
7. 后台端：进入"预约治理"页面 → 跨商户查询预约

## 10. 后续可改进项

- 演示版二维码占位用 CustomPainter 绘制规则图样，后续可替换为 `qr_flutter` 或 `pretty_qr_code` 包以生成真正的可扫码 QR
- `business_attributes` 当前用字符串存储；如果未来对单字段筛选需求增加，可改为 JSON 列并加索引
- 预约日期当前只是 DATE 字符串，后续接入"门店可预约时段"后可改为引用 `catalog_service_schedule` 表
- 商家端券码核销支持扫码（接入摄像头或外置扫码枪）

## 11. 文件变更清单

- 新增：`database/migrations/V008__stage5_d_service_attributes_and_booking.sql`
- 修改：`database/seeds/R__seed_demo_data.sql`
- 修改：`services/backend/src/main/java/com/aituan/discovery/DiscoveryDtos.java`
- 修改：`services/backend/src/main/java/com/aituan/discovery/DiscoveryRepository.java`
- 修改：`services/backend/src/main/java/com/aituan/discovery/DiscoveryService.java`
- 修改：`services/backend/src/main/java/com/aituan/trade/TradeRepository.java`
- 修改：`services/backend/src/main/java/com/aituan/trade/TradeService.java`
- 修改：`services/backend/src/main/java/com/aituan/trade/TradeDtos.java`
- 修改：`services/backend/src/main/java/com/aituan/trade/TradeController.java`
- 修改：`services/backend/src/main/java/com/aituan/trade/TradeOpsController.java`
- 新增：`apps/user_app/lib/shared/models/business_attributes.dart`
- 修改：`apps/user_app/lib/shared/models/item_model.dart`
- 修改：`apps/user_app/lib/features/home/data/backend_app_repository.dart`
- 修改：`apps/user_app/lib/features/merchant/presentation/item_detail_page.dart`
- 修改：`apps/user_app/lib/features/order/presentation/service_order_detail_page.dart`
- 新增：`apps/user_app/lib/features/order/presentation/voucher_detail_page.dart`
- 新增：`apps/user_app/lib/features/order/presentation/booking_detail_page.dart`
- 修改：`apps/user_app/lib/core/constants/route_constants.dart`
- 修改：`apps/user_app/lib/app/route_args.dart`
- 修改：`apps/user_app/lib/app/router.dart`
- 新增：`apps/merchant_web/src/pages/VoucherPage.vue`
- 新增：`apps/merchant_web/src/pages/BookingPage.vue`
- 修改：`apps/merchant_web/src/App.vue`
- 修改：`apps/merchant_web/src/components/ConsoleFrame.vue`
- 修改：`apps/merchant_web/src/types.ts`
- 修改：`apps/merchant_web/src/api.ts`
- 新增：`apps/admin_web/src/pages/VouchersPage.vue`
- 新增：`apps/admin_web/src/pages/BookingsPage.vue`
- 修改：`apps/admin_web/src/App.vue`
- 修改：`apps/admin_web/src/components/AdminFrame.vue`
- 修改：`apps/admin_web/src/types.ts`
- 修改：`apps/admin_web/src/api.ts`
