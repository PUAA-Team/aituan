# Stage6 分工A · 用户端资料收藏消息优惠券交付说明

## 1. 文档状态

- 所属阶段：Stage 6（后续五人模块分工）· 成员 A · 账号/用户资产/会员消息
- 对应计划：`docs/stage6/分工A-账号与用户资产会员消息实施计划.md` 第 2 轮「用户端」剩余任务
- 当前状态：编码完成；`dart analyze lib test`、`flutter --no-version-check analyze`、`flutter --no-version-check test` 通过；APK 已构建产出 `D:\aituan_release\apk\aituan-user-debug.apk`
- 范围边界：本次完成用户端资料编辑、收藏增强、消息增强和 checkout 选券入口；后续已在交易下单链路中接入 `couponId` 试算、下单核销与取消回退，当前文档口径同步为真实抵扣已联调。

## 2. 交付范围

### 2.1 资料编辑

- 新增 `/profile/edit` 资料编辑页。
- 「我的」页顶部按钮从“退出”调整为“编辑”，退出登录移到底部工具区。
- 资料编辑页支持修改昵称，手机号和邮箱展示后端脱敏结果。
- 当前阶段暂不支持 APP 内换绑手机号/邮箱，页面中已说明。

### 2.2 收藏增强

- 「我的收藏」增加分类筛选：全部 / 店铺 / 商品 / 服务。
- 店铺和商品收藏接入后端 `favoriteType` 查询。
- 收藏卡片增加取消收藏按钮，取消后本地列表即时刷新。
- 服务收藏先保留扩展入口和空状态说明，等待成员 D 的服务模型继续真实化。

### 2.3 消息增强

- 消息页增加分类筛选：全部 / 订单 / 系统 / 互动。
- 接入后端消息 `type` 查询、单条已读、全部已读接口。
- 未读消息使用红色弱底和“未读”标记区分。
- 订单类消息支持根据 `relatedTargetType=order` / `relatedTargetId` 跳转订单详情；无法识别目标时给出提示。

### 2.4 checkout 选券入口

- 确认订单页增加「优惠券」卡片。
- 新增 `/coupons/select` 选券页，按当前订单金额调用 `/api/app/account/coupons/usable-for-order`。
- 选择优惠券后在确认订单页展示选择结果。
- 选择优惠券后确认订单页会重新调用 `checkout/preview` 试算优惠与实付金额，提交订单时携带 `couponId`，最终金额以后端返回为准。

## 3. 文件清单

### 新增

| 文件 | 说明 |
| --- | --- |
| `apps/user_app/lib/features/profile/presentation/profile_edit_page.dart` | 资料编辑页 |
| `apps/user_app/lib/features/coupon/presentation/coupon_selector_page.dart` | 下单选券页 |
| `docs/stage6/分工A-用户端资料收藏消息优惠券交付说明.md` | 本交付说明 |

### 改动

| 文件 | 说明 |
| --- | --- |
| `apps/user_app/lib/core/constants/route_constants.dart` | 新增 `/profile/edit`、`/coupons/select` 路由常量 |
| `apps/user_app/lib/app/router.dart` | 注册资料编辑页和选券页，并加入登录保护 |
| `apps/user_app/lib/features/profile/presentation/profile_page.dart` | 顶部编辑入口、底部退出入口 |
| `apps/user_app/lib/features/favorite/presentation/favorite_page.dart` | 分类筛选、取消收藏、服务分类占位 |
| `apps/user_app/lib/features/message/presentation/message_page.dart` | 分类、已读、全部已读、订单跳转 |
| `apps/user_app/lib/shared/models/message_item.dart` | 消息模型增加 id/type/unread/跳转目标等字段，并兼容旧 mock 构造 |
| `apps/user_app/lib/features/home/data/backend_app_repository.dart` | 消息筛选/已读接口、PATCH 请求封装 |
| `apps/user_app/lib/features/coupon/data/coupon_repository.dart` | 增加下单可用券接口与 `OrderCouponOption` 模型 |
| `apps/user_app/lib/features/checkout/presentation/checkout_page.dart` | 增加优惠券入口和选择结果展示 |
| `apps/user_app/test/member_coupon_models_test.dart` | 增加 `OrderCouponOption.fromApi` 解析单测 |
| `docs/ReadMe.md` | 增加本交付说明索引 |

## 4. 验证结果

### 4.1 Dart 静态分析

```bash
cd apps/user_app
dart analyze lib test
```

结果：`No issues found!`

### 4.2 Flutter 单元测试

```bash
cd apps/user_app
flutter --no-version-check test
```

结果：`All tests passed!`，共 7 个测试通过。

### 4.3 Flutter analyze

```bash
cd apps/user_app
flutter --no-version-check analyze
```

结果：`No issues found!`

### 4.4 APK 打包

打包命令：

```bash
cd C:/Users/baozh/Downloads/aituan-stage5-c-takeaway-delivery/aituan-stage5-c-takeaway-delivery
GRADLE_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897' powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_android_apk.ps1
```

结果：

- `flutter analyze`：通过
- `flutter test`：通过
- `flutter build apk --debug`：通过
- APK 产物：`D:\aituan_release\apk\aituan-user-debug.apk`
- 构建脚本已在复制 APK 后清理 `D:\aituan_build\user_app` 工作目录。

## 5. 手动验收建议

1. 登录演示用户后进入「我的」页。
2. 点击顶部「编辑」进入资料编辑页，修改昵称并保存。
3. 进入「收藏」，切换全部 / 店铺 / 商品 / 服务，取消一条收藏后确认列表刷新。
4. 进入「消息」，切换订单 / 系统 / 互动，点击全部已读后确认未读标识消失。
5. 点击订单类消息，确认可跳转对应订单详情。
6. 进入任一商品/服务下单页，点击「优惠券」，选择可用券后返回确认订单页查看选择结果。

## 6. 后续待联调事项

- checkout 优惠券抵扣已完成与交易下单链路联调：`checkout/preview` 会按 `couponId` 重新试算，`createOrder` 会携带 `couponId` 并在后端完成核销；取消外卖订单或商家拒单时会回退优惠券。
- 服务收藏分类目前是扩展占位，等待成员 D 的服务模型和收藏类型稳定后再接真实服务收藏。
- 消息跳转当前主要覆盖订单；评价/客服等目标需要成员 E 对应页面和路由完成后继续扩展。
