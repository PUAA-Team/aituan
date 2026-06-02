# 爱团 API 分组设计（Stage 1）

## 1. 文档目标

本文档用于细化爱团平台后端接口分组方案，统一三端调用边界、命名风格、鉴权方式、响应结构和关键接口清单，作为后续 Controller 拆分、接口联调、测试用例和 Swagger 文档的基线。

本文档与以下文档配套使用：

- `docs/stage1/代码架构设计.md`
- `docs/stage1/数据库表设计.md`

## 2. 总体设计原则

1. **按调用方分域**：用户端、商家端、平台后台分开前缀，避免权限边界混乱。
2. **按业务模块分组**：每组接口只围绕一个领域，不做跨域“大接口”。
3. **优先打通 P0 业务闭环**：先打通登录、搜索、下单、支付、接单、配送、评价、审核，前端可先用 Mock 数据源预览。
4. **统一返回结构**：三端接口响应格式一致，降低前端接入成本。
5. **核心动作幂等**：下单、支付回调、接单、审核等接口需支持幂等控制。
6. **非核心能力可降级**：地图、AI、推荐等失败时给出可识别降级结果，不阻断主链路。

## 3. 基础约定

## 3.1 URL 前缀规划

建议统一使用以下前缀：

| 前缀 | 调用方 | 说明 |
| --- | --- | --- |
| `/api/open` | 未登录调用方 | 登录、邮箱验证码、公开查询等开放接口 |
| `/api/app` | 用户端 APP | 消费者业务接口 |
| `/api/merchant` | 商家端 | 商家经营接口 |
| `/api/admin` | 平台后台 | 平台治理和运营接口 |
| `/api/common` | 三端复用 | 文件上传、字典、公共配置 |

> 当前阶段不单独暴露 `/api/internal`，配送推进、状态轮询等由后端定时任务直接调用服务层完成。

## 3.2 HTTP 方法约定

| 方法 | 用途 |
| --- | --- |
| `GET` | 查询列表或详情 |
| `POST` | 创建、提交、复杂查询、动作执行 |
| `PUT` | 整体更新 |
| `PATCH` | 局部更新、状态切换 |
| `DELETE` | 逻辑删除或解除关系 |

## 3.3 统一响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": "2026-05-08T12:00:00+08:00",
  "requestId": "req_202605081200001234"
}
```

### 字段说明

| 字段 | 说明 |
| --- | --- |
| `code` | 业务码，0 表示成功 |
| `message` | 接口结果说明 |
| `data` | 业务数据 |
| `timestamp` | 服务端响应时间 |
| `requestId` | 请求追踪 ID |

## 3.4 分页结构

列表接口统一支持：

- `page`：页码，从 1 开始
- `pageSize`：每页数量
- `sortBy`：排序字段
- `sortDirection`：`asc` / `desc`

建议统一分页响应：

```json
{
  "list": [],
  "page": 1,
  "pageSize": 20,
  "total": 100,
  "hasNext": true
}
```

## 3.5 时间与金额约定

1. 时间统一使用 ISO 8601 字符串。
2. 金额字段统一使用两位小数的数值或字符串，字段命名统一为：
   - `price`
   - `originalPrice`
   - `discountAmount`
   - `payableAmount`
3. 坐标统一使用 `longitude`、`latitude`。

## 3.6 鉴权约定

### Token 传递

统一通过请求头传递：

```text
Authorization: Bearer <token>
```

### 角色说明

| 角色码 | 说明 |
| --- | --- |
| `USER` | 消费者 |
| `MERCHANT` | 商家经营人员 |
| `ADMIN` | 平台管理员/运营人员 |

### 路由守卫对应规则

- `/api/app/**`：默认要求 `USER`
- `/api/merchant/**`：默认要求 `MERCHANT`
- `/api/admin/**`：默认要求 `ADMIN`
- `/api/open/**`：匿名可访问
- `/api/common/**`：按接口具体权限判定

## 3.7 幂等约定

以下接口建议支持 `Idempotency-Key`：

- 提交订单
- 发起支付
- 支付结果确认
- 商家接单/拒单
- 券码核销
- 审核通过/驳回

请求头示例：

```text
Idempotency-Key: 9b7e7f55-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

## 3.8 文件上传约定

建议统一上传接口：

- `POST /api/common/upload/image`

返回结构建议：

```json
{
  "fileUrl": "https://.../demo.png",
  "fileName": "demo.png",
  "fileSize": 12345
}
```

## 3.9 错误码分段建议

| 范围 | 含义 |
| --- | --- |
| `0` | 成功 |
| `1000-1999` | 通用请求错误 |
| `2000-2999` | 鉴权与权限错误 |
| `3000-3999` | 用户侧业务错误 |
| `4000-4999` | 商家侧业务错误 |
| `5000-5999` | 订单与支付错误 |
| `6000-6999` | 审核与运营错误 |
| `7000-7999` | AI 与外部依赖错误 |

## 4. 接口分组总览

| 分组 | 前缀 | 主要调用方 | P0 |
| --- | --- | --- | --- |
| 开放认证组 | `/api/open/auth` | 三端 | 是 |
| 公共资源组 | `/api/common` | 三端 | 是 |
| 用户账户组 | `/api/app/account` | 用户端 | 是 |
| 用户首页搜索组 | `/api/app/discovery` | 用户端 | 是 |
| 用户交易组 | `/api/app/trade` | 用户端 | 是 |
| 用户互动组 | `/api/app/interaction` | 用户端 | 是 |
| 用户会员消息组 | `/api/app/member` `/api/app/message` | 用户端 | 是 |
| 用户 AI 组 | `/api/app/ai` | 用户端 | 否 |
| 商家入驻门店组 | `/api/merchant/store` | 商家端 | 是 |
| 商家商品组 | `/api/merchant/catalog` | 商家端 | 是 |
| 商家订单组 | `/api/merchant/order` | 商家端 | 是 |
| 商家经营互动组 | `/api/merchant/ops` | 商家端 | 是 |
| 商家 AI 组 | `/api/merchant/ai` | 商家端 | 否 |
| 后台治理组 | `/api/admin/governance` | 平台后台 | 是 |
| 后台运营配置组 | `/api/admin/operation` | 平台后台 | 是 |
| 后台权限日志组 | `/api/admin/system` | 平台后台 | 是 |
| 后台 AI 治理组 | `/api/admin/ai` | 平台后台 | 否 |

## 5. 开放认证组 `/api/open/auth`

## 5.1 目标

处理用户端手机号/邮箱登录、邮箱验证码、注册、找回密码、退出前准备与公开登录校验等匿名入口。

## 5.2 接口清单

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `POST` | `/api/open/auth/email-code` | 发送邮箱验证码 | 是 |
| `POST` | `/api/open/auth/user/login/password` | 用户手机号/邮箱 + 密码登录 | 是 |
| `POST` | `/api/open/auth/user/login/email-code` | 用户邮箱验证码登录 | 是 |
| `POST` | `/api/open/auth/user/register` | 用户注册 | 是 |
| `POST` | `/api/open/auth/user/password/reset` | 用户找回/重置密码 | 是 |
| `POST` | `/api/open/auth/merchant/login` | 商家登录 | 是 |
| `POST` | `/api/open/auth/admin/login` | 后台管理员登录 | 是 |
| `POST` | `/api/open/auth/logout` | 退出登录 | 是 |
| `GET` | `/api/open/auth/token/check` | 校验 token 有效性 | 是 |

### 5.3 说明

1. 用户端首包支持手机号 + 密码登录，也支持邮箱 + 密码登录。
2. 邮箱验证码主要用于邮箱登录、注册和找回密码；当前阶段手机号不接短信验证码。
3. 商家端和后台可继续支持账号密码登录。
4. 开发环境允许固定邮箱验证码或控制台打印验证码。

## 6. 公共资源组 `/api/common`

## 6.1 接口清单

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `POST` | `/api/common/upload/image` | 上传图片 | 是 |
| `GET` | `/api/common/dicts/{dictType}` | 查询字典 | 是 |
| `GET` | `/api/common/config/public` | 查询公开配置 | 是 |
| `GET` | `/api/common/categories` | 查询平台类目树 | 是 |
| `GET` | `/api/common/tags` | 查询标签列表 | 否 |

## 7. 用户账户组 `/api/app/account`

## 7.1 范围

用户资料、地址、收藏、个人中心摘要。

## 7.2 接口清单

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `GET` | `/api/app/account/profile` | 查询个人资料 | 是 |
| `PUT` | `/api/app/account/profile` | 更新个人资料 | 是 |
| `GET` | `/api/app/account/home-summary` | 个人中心摘要 | 是 |
| `GET` | `/api/app/account/addresses` | 地址列表 | 是 |
| `POST` | `/api/app/account/addresses` | 新增地址 | 是 |
| `PUT` | `/api/app/account/addresses/{addressId}` | 编辑地址 | 是 |
| `PATCH` | `/api/app/account/addresses/{addressId}/default` | 设为默认地址 | 是 |
| `DELETE` | `/api/app/account/addresses/{addressId}` | 删除地址 | 是 |
| `GET` | `/api/app/account/favorites` | 收藏列表 | 是 |
| `POST` | `/api/app/account/favorites` | 新增收藏 | 是 |
| `DELETE` | `/api/app/account/favorites/{favoriteId}` | 取消收藏 | 是 |

## 8. 用户首页搜索组 `/api/app/discovery`

## 8.1 范围

首页展示、八个模块展示、搜索、推荐、商品/服务详情、两套商家详情。

## 8.2 接口清单

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `GET` | `/api/app/discovery/home` | 首页聚合数据 | 是 |
| `GET` | `/api/app/discovery/banners` | Banner 列表 | 是 |
| `GET` | `/api/app/discovery/recommendations` | 推荐位列表，支持首页猜你喜欢分页加载 | 是 |
| `GET` | `/api/app/discovery/modules/{moduleCode}` | 模块展示页数据，返回模块搜索占位、热门推荐、按类型过滤的商铺聚合精选 | 是 |
| `GET` | `/api/app/discovery/stores/search` | 搜索任意内容并统一返回店铺卡结果 | 是 |
| `GET` | `/api/app/discovery/stores/nearby` | 附近商家 | 是 |
| `GET` | `/api/app/discovery/stores/{storeId}` | 商家详情；外卖返回短商家信息和点单数据，非外卖返回完整商家信息和商品/服务列表 | 是 |
| `GET` | `/api/app/discovery/stores/{storeId}/items` | 商家商品/服务列表，返回分类字段供前端分组和分类锚点滚动 | 是 |
| `GET` | `/api/app/discovery/items/{itemId}` | 商品/服务详情，返回商品图文、规则、价格、所属店铺入口 | 是 |
| `GET` | `/api/app/discovery/items/{itemId}/schedules` | 场次/库存/预约资源（预约、门票等服务使用） | 否 |

### 8.3 搜索参数建议

| 参数 | 说明 |
| --- | --- |
| `keyword` | 搜索词，可命中商家、商品、外卖商品、团购服务、门票、预约项目等 |
| `categoryId` | 类目 |
| `sortBy` | 排序字段：distance / score / sales / price |
| `distanceKm` | 距离筛选 |
| `minPrice` / `maxPrice` | 价格区间 |
| `tagCodes` | 标签 |
| `longitude` / `latitude` | 用户坐标 |
| `page` / `pageSize` | 分页参数 |

### 8.4 搜索返回结构建议

1. 搜索结果首层统一返回店铺卡，而不是把商品结果和店铺结果拆成两条独立流。
2. 搜索结果顶部分类第一位为“问小爱”，后续为系统模块：外卖、团购、酒店、休闲娱乐、电影演出、丽人医美、景点门票、洗脚；筛选栏只保留当前订单地点、排序方式、筛选，并支持下拉选择。
3. 每张店铺卡下返回命中的商品/服务横滑小卡列表，至少包含图片、名称、价格/起价、业务类型标记、简短卖点或规则摘要。
4. 当命中外卖商家或外卖商品时，前端点击店铺或商品都进入外卖商家点单页，因此接口需要在摘要中明确 `itemType=takeaway` 和 `storeId`。
5. 当命中非外卖商家时，前端点击店铺进入非外卖商家页；当命中非外卖商品/服务时，前端点击商品进入 `/items/{itemId}` 商品详情页，商品详情页再提供店铺入口。
6. 当命中的是店铺、标签或商家名称时，命中商品列表默认返回该店热门商品，用于搜索结果页小卡展示。

## 9. 用户交易组 `/api/app/trade`

## 9.1 范围

购物车、下单、订单、支付、配送、券码、预约。

## 9.2 购物车接口

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `GET` | `/api/app/trade/carts/current` | 查询当前购物车 | 是 |
| `POST` | `/api/app/trade/carts/items` | 加入购物车 | 是 |
| `PATCH` | `/api/app/trade/carts/items/{cartItemId}` | 修改数量/选项 | 是 |
| `DELETE` | `/api/app/trade/carts/items/{cartItemId}` | 删除购物项 | 是 |
| `DELETE` | `/api/app/trade/carts/{cartId}/clear` | 清空购物车 | 是 |

## 9.3 下单与试算接口

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `POST` | `/api/app/trade/checkout/preview` | 订单试算，返回商品金额、配送费、服务费、优惠、应付金额 | 是 |
| `POST` | `/api/app/trade/orders` | 提交订单，包含支付方式选择 | 是 |
| `GET` | `/api/app/trade/orders` | 订单列表，支持全部、未支付、待完成、未使用、已使用筛选 | 是 |
| `GET` | `/api/app/trade/orders/{orderId}` | 订单详情；外卖返回配送履约信息，非外卖返回券码/二维码核销信息 | 是 |
| `PATCH` | `/api/app/trade/orders/{orderId}/cancel` | 取消订单 | 是 |
| `PATCH` | `/api/app/trade/orders/{orderId}/confirm` | 确认完成 | 是 |

## 9.4 支付接口

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `GET` | `/api/app/trade/payment-methods` | 查询可用支付方式，当前返回模拟支付，后续可扩展微信/支付宝 | 是 |
| `POST` | `/api/app/trade/orders/{orderId}/pay` | 发起支付，当前 `paymentMode=mock` 时直接模拟支付成功 | 是 |
| `GET` | `/api/app/trade/orders/{orderId}/payment` | 查询支付结果 | 是 |
| `POST` | `/api/app/trade/payments/{paymentNo}/retry` | 重新发起支付 | 是 |

## 9.5 配送接口

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `GET` | `/api/app/trade/orders/{orderId}/delivery` | 查询配送信息 | 是 |
| `GET` | `/api/app/trade/orders/{orderId}/delivery/timeline` | 查询配送时间线 | 是 |

## 9.6 预约与券码接口

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `GET` | `/api/app/trade/bookings` | 我的预约列表 | 否 |
| `GET` | `/api/app/trade/bookings/{bookingId}` | 预约详情 | 否 |
| `PATCH` | `/api/app/trade/bookings/{bookingId}/reschedule` | 改期 | 否 |
| `GET` | `/api/app/trade/vouchers` | 我的券码列表 | 否 |
| `GET` | `/api/app/trade/vouchers/{voucherId}` | 券码详情，返回券码号、二维码载荷、有效期和核销状态 | 否 |
| `POST` | `/api/app/trade/refunds` | 提交退款申请 | 否 |

## 10. 用户互动组 `/api/app/interaction`

## 10.1 范围

评价、点赞、举报、咨询会话。

## 10.2 接口清单

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `GET` | `/api/app/interaction/reviews/pending` | 已使用且可评价订单列表 | 是 |
| `POST` | `/api/app/interaction/reviews` | 发布评价 | 是 |
| `GET` | `/api/app/interaction/reviews/mine` | 我的评价 | 是 |
| `POST` | `/api/app/interaction/reviews/{reviewId}/votes` | 点赞/有用投票 | 否 |
| `POST` | `/api/app/interaction/reviews/{reviewId}/reports` | 举报评价 | 是 |
| `GET` | `/api/app/interaction/sessions` | 客服会话列表 | 是 |
| `POST` | `/api/app/interaction/sessions` | 创建咨询会话 | 是 |
| `GET` | `/api/app/interaction/sessions/{sessionId}` | 会话详情 | 是 |
| `POST` | `/api/app/interaction/sessions/{sessionId}/messages` | 发送消息 | 是 |

## 11. 用户会员消息组

## 11.1 `/api/app/member`

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `GET` | `/api/app/member/profile` | 查询会员资料 | 是 |
| `GET` | `/api/app/member/levels` | 会员等级说明 | 是 |
| `GET` | `/api/app/member/growth-records` | 烟火值流水 | 是 |
| `GET` | `/api/app/member/coupons` | 我的优惠券 | 是 |
| `POST` | `/api/app/member/coupons/{couponId}/receive` | 领取优惠券 | 否 |

## 11.2 `/api/app/message`

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `GET` | `/api/app/message/station` | 站内消息列表 | 是 |
| `PATCH` | `/api/app/message/station/{messageId}/read` | 单条已读 | 是 |
| `PATCH` | `/api/app/message/station/read-all` | 全部已读 | 否 |

## 12. 用户 AI 组 `/api/app/ai`

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `POST` | `/api/app/ai/plan` | 智能生活规划 | 否 |
| `POST` | `/api/app/ai/search-assist` | 智能搜索辅助 | 否 |
| `POST` | `/api/app/ai/recommend-explain` | 推荐理由解释 | 否 |
| `GET` | `/api/app/ai/history` | AI 历史记录 | 否 |

## 13. 商家入驻门店组 `/api/merchant/store`

## 13.1 范围

商家入驻、资质提交、门店资料、营业状态、经营首页。

## 13.2 接口清单

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `GET` | `/api/merchant/store/profile` | 查询商家主体资料 | 是 |
| `POST` | `/api/merchant/store/apply` | 提交入驻申请 | 是 |
| `PUT` | `/api/merchant/store/profile` | 更新商家主体资料 | 是 |
| `GET` | `/api/merchant/store/qualifications` | 资质列表 | 是 |
| `POST` | `/api/merchant/store/qualifications` | 提交资质材料 | 是 |
| `GET` | `/api/merchant/store/branches` | 门店列表 | 是 |
| `POST` | `/api/merchant/store/branches` | 新增门店 | 否 |
| `PUT` | `/api/merchant/store/branches/{storeId}` | 编辑门店 | 是 |
| `PATCH` | `/api/merchant/store/branches/{storeId}/status` | 修改营业状态 | 是 |
| `PUT` | `/api/merchant/store/branches/{storeId}/delivery-rule` | 配送规则维护 | 是 |
| `GET` | `/api/merchant/store/dashboard/summary` | 经营总览摘要 | 是 |

## 14. 商家商品组 `/api/merchant/catalog`

## 14.1 范围

商品、服务、库存、场次、上下架。

## 14.2 接口清单

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `GET` | `/api/merchant/catalog/items` | 商品/服务列表 | 是 |
| `POST` | `/api/merchant/catalog/items` | 新增商品/服务 | 是 |
| `GET` | `/api/merchant/catalog/items/{itemId}` | 商品/服务详情 | 是 |
| `PUT` | `/api/merchant/catalog/items/{itemId}` | 编辑商品/服务 | 是 |
| `PATCH` | `/api/merchant/catalog/items/{itemId}/status` | 上下架 | 是 |
| `GET` | `/api/merchant/catalog/items/{itemId}/skus` | SKU 列表 | 是 |
| `POST` | `/api/merchant/catalog/items/{itemId}/skus` | 新增 SKU | 是 |
| `PUT` | `/api/merchant/catalog/skus/{skuId}` | 编辑 SKU | 是 |
| `PATCH` | `/api/merchant/catalog/skus/{skuId}/stock` | 修改库存 | 是 |
| `GET` | `/api/merchant/catalog/items/{itemId}/schedules` | 场次/预约资源列表 | 否 |
| `POST` | `/api/merchant/catalog/items/{itemId}/schedules` | 新增场次/资源 | 否 |
| `PUT` | `/api/merchant/catalog/schedules/{scheduleId}` | 编辑场次/资源 | 否 |

## 15. 商家订单组 `/api/merchant/order`

## 15.1 范围

订单处理、退款审核、核销、预约改期。

## 15.2 接口清单

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `GET` | `/api/merchant/order/orders` | 订单列表 | 是 |
| `GET` | `/api/merchant/order/orders/{orderId}` | 订单详情 | 是 |
| `PATCH` | `/api/merchant/order/orders/{orderId}/accept` | 接单 | 是 |
| `PATCH` | `/api/merchant/order/orders/{orderId}/reject` | 拒单 | 是 |
| `PATCH` | `/api/merchant/order/orders/{orderId}/prepare` | 开始备餐/准备服务 | 是 |
| `PATCH` | `/api/merchant/order/orders/{orderId}/complete` | 标记完成 | 否 |
| `GET` | `/api/merchant/order/refunds` | 退款申请列表 | 否 |
| `PATCH` | `/api/merchant/order/refunds/{refundId}/approve` | 同意退款 | 否 |
| `PATCH` | `/api/merchant/order/refunds/{refundId}/reject` | 拒绝退款 | 否 |
| `POST` | `/api/merchant/order/vouchers/verify` | 券码核销 | 否 |
| `POST` | `/api/merchant/order/bookings/verify` | 预约核销 | 否 |
| `PATCH` | `/api/merchant/order/bookings/{bookingId}/reschedule` | 商家改期 | 否 |

## 16. 商家经营互动组 `/api/merchant/ops`

## 16.1 范围

评价回复、咨询会话、营销配置、经营数据。

## 16.2 接口清单

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `GET` | `/api/merchant/ops/reviews` | 评价列表 | 是 |
| `POST` | `/api/merchant/ops/reviews/{reviewId}/reply` | 回复评价 | 是 |
| `GET` | `/api/merchant/ops/sessions` | 客服会话列表 | 是 |
| `GET` | `/api/merchant/ops/sessions/{sessionId}` | 会话详情 | 是 |
| `POST` | `/api/merchant/ops/sessions/{sessionId}/messages` | 回复用户消息 | 是 |
| `GET` | `/api/merchant/ops/campaigns` | 营销活动列表 | 否 |
| `POST` | `/api/merchant/ops/campaigns` | 新增营销活动 | 否 |
| `PUT` | `/api/merchant/ops/campaigns/{campaignId}` | 编辑营销活动 | 否 |
| `GET` | `/api/merchant/ops/analytics/dashboard` | 商家看板 | 是 |
| `GET` | `/api/merchant/ops/analytics/hot-items` | 热销商品分析 | 否 |
| `GET` | `/api/merchant/ops/analytics/trends` | 销售趋势 | 否 |

## 17. 商家 AI 组 `/api/merchant/ai`

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `POST` | `/api/merchant/ai/copy-generate` | 商品文案生成 | 否 |
| `POST` | `/api/merchant/ai/tag-generate` | 标签生成 | 否 |
| `POST` | `/api/merchant/ai/reply-suggest` | 评价/客服回复建议 | 否 |
| `POST` | `/api/merchant/ai/campaign-suggest` | 活动文案建议 | 否 |

## 18. 后台治理组 `/api/admin/governance`

## 18.1 范围

商家审核、评价审核、投诉工单、订单干预。

## 18.2 接口清单

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/governance/merchant-audits` | 商家审核列表 | 是 |
| `GET` | `/api/admin/governance/merchant-audits/{auditId}` | 商家审核详情 | 是 |
| `PATCH` | `/api/admin/governance/merchant-audits/{auditId}/approve` | 审核通过 | 是 |
| `PATCH` | `/api/admin/governance/merchant-audits/{auditId}/reject` | 审核驳回 | 是 |
| `GET` | `/api/admin/governance/reviews` | 评价审核列表 | 是 |
| `GET` | `/api/admin/governance/reviews/{reviewId}` | 评价审核详情 | 是 |
| `PATCH` | `/api/admin/governance/reviews/{reviewId}/approve` | 评价通过 | 是 |
| `PATCH` | `/api/admin/governance/reviews/{reviewId}/hide` | 评价屏蔽 | 是 |
| `GET` | `/api/admin/governance/complaints` | 投诉工单列表 | 是 |
| `GET` | `/api/admin/governance/complaints/{ticketId}` | 投诉工单详情 | 是 |
| `PATCH` | `/api/admin/governance/complaints/{ticketId}/assign` | 分派工单 | 是 |
| `PATCH` | `/api/admin/governance/complaints/{ticketId}/close` | 结案 | 是 |
| `GET` | `/api/admin/governance/orders` | 平台订单查询 | 是 |
| `GET` | `/api/admin/governance/orders/{orderId}` | 平台订单详情 | 是 |
| `PATCH` | `/api/admin/governance/orders/{orderId}/intervene` | 订单人工干预 | 否 |

## 19. 后台运营配置组 `/api/admin/operation`

## 19.1 范围

类目、标签、Banner、推荐位、会员等级、活动配置、平台看板。

## 19.2 接口清单

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/operation/dashboard` | 平台经营看板 | 是 |
| `GET` | `/api/admin/operation/categories` | 类目列表 | 是 |
| `POST` | `/api/admin/operation/categories` | 新增类目 | 否 |
| `PUT` | `/api/admin/operation/categories/{categoryId}` | 编辑类目 | 否 |
| `GET` | `/api/admin/operation/tags` | 标签列表 | 否 |
| `GET` | `/api/admin/operation/banners` | Banner 列表 | 是 |
| `POST` | `/api/admin/operation/banners` | 新增 Banner | 是 |
| `PUT` | `/api/admin/operation/banners/{bannerId}` | 编辑 Banner | 是 |
| `PATCH` | `/api/admin/operation/banners/{bannerId}/status` | 发布/停用 Banner | 是 |
| `GET` | `/api/admin/operation/recommendations` | 推荐位列表 | 否 |
| `POST` | `/api/admin/operation/recommendations` | 新增推荐位 | 否 |
| `GET` | `/api/admin/operation/member-levels` | 会员等级列表 | 是 |
| `POST` | `/api/admin/operation/member-levels` | 新增会员等级 | 否 |
| `PUT` | `/api/admin/operation/member-levels/{levelId}` | 编辑会员等级 | 否 |
| `GET` | `/api/admin/operation/campaigns` | 平台活动列表 | 否 |
| `POST` | `/api/admin/operation/campaigns` | 新增平台活动 | 否 |

## 20. 后台权限日志组 `/api/admin/system`

## 20.1 范围

系统参数、角色权限、字典、审计日志、请求日志。

## 20.2 接口清单

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/system/configs` | 系统配置列表 | 是 |
| `PUT` | `/api/admin/system/configs/{configId}` | 更新系统配置 | 是 |
| `GET` | `/api/admin/system/roles` | 角色列表 | 是 |
| `POST` | `/api/admin/system/roles` | 新增角色 | 否 |
| `PUT` | `/api/admin/system/roles/{roleId}` | 编辑角色 | 否 |
| `PUT` | `/api/admin/system/roles/{roleId}/permissions` | 分配权限 | 是 |
| `GET` | `/api/admin/system/accounts` | 后台账号列表 | 否 |
| `PUT` | `/api/admin/system/accounts/{accountId}/roles` | 分配账号角色 | 否 |
| `GET` | `/api/admin/system/audit-logs` | 审计日志列表 | 是 |
| `GET` | `/api/admin/system/audit-logs/{logId}` | 审计日志详情 | 是 |
| `GET` | `/api/admin/system/request-logs` | 请求日志列表 | 否 |
| `GET` | `/api/admin/system/dicts/{dictType}` | 字典项列表 | 否 |

## 21. 后台 AI 治理组 `/api/admin/ai`

| 方法 | 路径 | 说明 | P0 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/ai/skills` | Skill 注册表 | 否 |
| `POST` | `/api/admin/ai/skills` | 新增 Skill 配置 | 否 |
| `PUT` | `/api/admin/ai/skills/{skillId}` | 编辑 Skill 配置 | 否 |
| `PATCH` | `/api/admin/ai/skills/{skillId}/status` | 启停 Skill | 否 |
| `GET` | `/api/admin/ai/invoke-logs` | AI 调用日志 | 否 |
| `PUT` | `/api/admin/ai/fallback-rules/{ruleId}` | 调整降级规则 | 否 |

## 22. 接口与后端模块映射

| API 分组 | 后端模块 |
| --- | --- |
| `/api/open/auth` | `auth/` |
| `/api/common` | `common/` `catalog/` `notification/` |
| `/api/app/account` | `user/` |
| `/api/app/discovery` | `search/` `merchant/` `catalog/` `marketing/` |
| `/api/app/trade` | `cart/` `order/` `order/delivery/` `member/` |
| `/api/app/interaction` | `review/` `support/` |
| `/api/app/member` | `member/` |
| `/api/app/message` | `notification/` |
| `/api/app/ai` | `ai/` |
| `/api/merchant/store` | `merchant/` |
| `/api/merchant/catalog` | `catalog/` |
| `/api/merchant/order` | `order/` |
| `/api/merchant/ops` | `review/` `support/` `marketing/` `statistic/` |
| `/api/merchant/ai` | `ai/` |
| `/api/admin/governance` | `admin/` `review/` `order/` |
| `/api/admin/operation` | `admin/` `marketing/` `member/` `statistic/` |
| `/api/admin/system` | `permission/` `admin/` `notification/` `common/log` |
| `/api/admin/ai` | `ai/` |

## 23. P0 联调优先顺序

建议按以下顺序联调：

1. `/api/open/auth/*`
2. `/api/app/account/*`
3. `/api/app/discovery/*`
4. `/api/app/trade/carts/*`
5. `/api/app/trade/checkout/preview`
6. `/api/app/trade/orders/*`
7. `/api/app/trade/orders/{orderId}/pay`
8. `/api/merchant/order/orders/*`
9. `/api/app/trade/orders/{orderId}/delivery/*`
10. `/api/app/interaction/reviews/*`
11. `/api/admin/governance/merchant-audits/*`
12. `/api/admin/governance/reviews/*`
13. `/api/admin/operation/dashboard`

## 24. 风险与简化说明

1. 当前将首页、搜索、推荐统一归到 `/api/app/discovery`，是为了减少前期接口分散；后期如果复杂度升高，可拆成 `home`、`search`、`recommend` 三组。
2. 预约、券码、退款接口当前放在 `/api/app/trade` 和 `/api/merchant/order` 下，是为了维持统一交易边界，不额外新建独立大模块。
3. AI 接口全部定义为辅助能力，任何失败都不应阻断订单、支付、审核等核心流程。
4. 平台统计接口当前挂在运营组下，后续若图表和报表数量变大，可单独拆 `/api/admin/statistics`。

## 25. 下一步建议

在接口设计确认后，建议继续细化：

1. 请求/响应 DTO 字段清单
2. 订单状态流转接口时序图
3. 审核与投诉工单状态机
4. AI Skills 输入输出协议
