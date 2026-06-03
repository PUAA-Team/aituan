# Stage 6 分工 A 账号与用户资产会员消息实施计划

## 1. 文档状态

- 所属阶段：Stage 6（后续五人模块分工阶段）
- 所属分工：成员 A，账号 / 用户资产 / 会员消息
- 当前状态：主体功能已完成并完成交付记录；后端接口集成测试已补齐；后续主要剩余服务收藏真实化、消息跳转扩展、优惠券并发安全等跨模块增强项
- 依据文档：`docs/后续阶段五人模块分工计划.md` §8 / §9、`docs/stage1/代码架构设计.md`、`docs/stage1/数据库表设计.md`、Stage5 现状代码
- 文档目标：在动工前明确成员 A 范围内的现状、缺口、表设计、接口契约、跨人协作边界、实施顺序与验收标准，作为后续编码依据。

### 1.1 已确认范围决策

| 决策点 | 结论 |
| --- | --- |
| 优惠券 | 做完整闭环：模板 + 用户券 + 领取 + 可用/已用/失效 + 下单抵扣 + 后台配置 |
| 会员等级 | 新建真实多等级体系（等级表 + 权益 + 成长进度），不再只用 `user_profile` 字符串字段 |
| Web 端范围 | 只做后台端会员等级配置页 + 优惠券模板配置页，并补 `admin_web` 401 自动登出；**不做商家端会员营销/发券** |
| 权限粒度 | 沿用现有角色拦截（USER/MERCHANT/ADMIN），`iam_permission` 细粒度权限后置 |

## 2. 主线目标

负责用户身份与个人资产的端到端闭环：用户登录后能管理资料、地址、收藏、优惠券、会员与消息；后台可配置会员等级与优惠券模板。复用 Stage5 已完成的登录 / 地址 / 收藏 / 消息骨架，不重复造轮子。

## 3. 现状结论（已完成 / 真实缺口）

### 3.1 已完成可复用

- 后端：用户/商家/后台三端登录、JWT、401 失效返回、角色拦截；账号资料查询/编辑、头像上传；地址增删改查 + 默认地址；收藏 query/save/delete（按 type）；消息列表 + 单条已读；后台用户管理、审计日志写入与查询。
- 用户端 APP：登录/注册/验证码/改密/退出；个人资料展示；地址列表/增删改/默认 + checkout 已接入选址；消息列表。
- 商家端/后台端 Web：登录与 token 已由 Stage5-C 完成；`admin_web` 已有 UsersPage / SettingsPage（含审计日志）/ AnnouncementsPage；`merchant_web` 已有 StorePage（商家与门店资料）。
- 数据库：`iam_account`(account_type)、`user_profile`(member_level_name, growth_value)、`user_address`(is_default、经纬度)、`user_favorite`(favorite_type: store/item)、`support_station_message`(message_type, read_status, related_order_id)、`iam_role` 体系、`sys_audit_log` 均已存在。

### 3.2 真实缺口（本阶段要做）

- 用户端：资料编辑页、手机/邮箱脱敏；收藏分类 Tab + 取消按钮 + 刷新；优惠券页（三态）；会员中心页（等级/成长进度/权益）；消息分类筛选 + 已读视觉 + 点击跳转。
- 后端：会员等级体系（等级表 + 用户会员信息 + 后台配置）；优惠券体系（模板表 + 用户券 + 领取/查询/下单抵扣 + 后台配置）；消息按 type 筛选 + 全部已读；资料脱敏。
- 后台 Web：会员等级配置页、优惠券模板配置页、`admin_web` 401 自动登出。
- 数据/seed：会员等级、优惠券模板、用户券、消息跳转字段及对应演示数据。

## 4. 本阶段范围

### 4.1 必做

- 用户端：资料编辑、脱敏、收藏分类增强、优惠券页、会员中心、消息分类/已读/跳转、checkout 选券入口。
- 后端：`member` 包、`coupon` 包、`message` 增强、`account` 脱敏。
- 后台 Web：会员等级配置、优惠券模板配置、401 登出。
- 数据：`V009` 迁移 + 幂等 seed。

### 4.2 本阶段不做 / 后置

- `iam_permission` 细粒度（按钮级/资源级）权限。
- 成长值变动明细表（成长进度先用当前 `growth_value` + 等级阈值计算）。
- 商家端会员管理、商家发券、会员定向通知。
- 收藏「服务(service)」分类的真实化（依赖成员 D 服务模型，先做 store/item，service 留扩展占位）。
- token 刷新机制、密码强度策略、登录失败锁定、登录日志（均非本分工本阶段范围）。
- 真实退款资金流；优惠券只做发放/使用/失效，不做财务结算。

## 5. 数据库设计（V009 + seed）

> 新增迁移 `database/migrations/V009__stage6_member_coupon.sql`，从 V008 之后递增；已执行迁移不回改。引擎、字符集、命名风格、状态枚举大小写以现有迁移为准，下方为草案。

### 5.1 新增 / 变更表

```sql
-- 会员等级定义
CREATE TABLE member_level (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  level_code       VARCHAR(32)  NOT NULL,         -- NORMAL/SILVER/GOLD/PLATINUM
  level_name       VARCHAR(64)  NOT NULL,         -- 普通会员/银卡会员/金卡会员/铂金会员
  min_growth_value INT          NOT NULL DEFAULT 0,-- 达到该等级所需成长值
  benefits         JSON         NULL,             -- 权益条目数组 [{title, desc}]
  icon_url         VARCHAR(255) NULL,
  color            VARCHAR(16)  NULL,
  sort_order       INT          NOT NULL DEFAULT 0,
  status           VARCHAR(16)  NOT NULL DEFAULT 'enabled',
  created_at       DATETIME     NOT NULL,
  updated_at       DATETIME     NOT NULL,
  UNIQUE KEY uk_level_code (level_code)
);

-- 优惠券模板
CREATE TABLE coupon_template (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  name             VARCHAR(64)  NOT NULL,
  type             VARCHAR(24)  NOT NULL,         -- full_reduction 满减 / discount 折扣
  face_value       DECIMAL(10,2) NOT NULL,        -- 满减=减免金额；折扣=折扣率(0.85)
  threshold_amount DECIMAL(10,2) NOT NULL DEFAULT 0, -- 满减门槛/最低订单金额
  business_scope   VARCHAR(32)  NOT NULL DEFAULT 'all', -- 本阶段先 all
  valid_kind       VARCHAR(16)  NOT NULL,         -- absolute 固定日期 / relative 领后N天
  valid_start      DATETIME     NULL,
  valid_end        DATETIME     NULL,
  valid_days       INT          NULL,
  total_qty        INT          NOT NULL DEFAULT 0, -- 0 表示不限
  issued_qty       INT          NOT NULL DEFAULT 0,
  per_user_limit   INT          NOT NULL DEFAULT 1,
  status           VARCHAR(16)  NOT NULL DEFAULT 'enabled', -- enabled/disabled
  created_at       DATETIME     NOT NULL,
  updated_at       DATETIME     NOT NULL
);

-- 用户优惠券（与成员 D 的 order_voucher 严格区分，不复用）
CREATE TABLE user_coupon (
  id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_id        BIGINT      NOT NULL,
  user_id            BIGINT      NOT NULL,
  status             VARCHAR(16) NOT NULL DEFAULT 'unused', -- unused/used/expired
  claimed_at         DATETIME    NOT NULL,
  expire_at          DATETIME    NOT NULL,
  used_at            DATETIME    NULL,
  used_order_id      BIGINT      NULL,
  -- 规则快照，防模板后续改动影响已领券
  type_snapshot      VARCHAR(24)  NOT NULL,
  face_value_snapshot DECIMAL(10,2) NOT NULL,
  threshold_snapshot DECIMAL(10,2) NOT NULL DEFAULT 0,
  created_at         DATETIME    NOT NULL,
  KEY idx_user_status (user_id, status),
  KEY idx_template (template_id)
);

-- 站内消息增加泛化跳转字段（保留 related_order_id 兼容）
ALTER TABLE support_station_message
  ADD COLUMN related_target_type VARCHAR(16) NULL,  -- order/review
  ADD COLUMN related_target_id   BIGINT      NULL;
```

### 5.2 关键状态枚举

- `member_level.status`：enabled / disabled
- `coupon_template.type`：full_reduction / discount；`status`：enabled / disabled
- `user_coupon.status`：unused（可用）/ used（已用）/ expired（失效）
- 消息跳转 `related_target_type`：order / review

### 5.3 seed 覆盖（`R__seed_demo_data.sql`，幂等，固定 ID + ON DUPLICATE KEY UPDATE）

- `member_level`：4 个等级（普通 0 / 银卡 300 / 金卡 800 / 铂金 2000）+ 各自权益条目。
- 演示用户按 `growth_value=128` 落在普通会员，距银卡会员还差 172 成长值，用于验证成长进度。
- `coupon_template`：3-5 个（满减、折扣、不同门槛/有效期/上下架态）。
- `user_coupon`：给演示用户发券，覆盖 unused / used / expired 三态。
- `support_station_message`：补 `related_target_type` / `related_target_id`（订单类消息回填 target_type=order）。
- `sys_audit_log`：补少量演示记录。

## 6. 后端接口契约

> 沿用现有「按域四件套」风格（`XxxController` / `XxxService` / `XxxRepository` / `XxxDtos`），新增 `com.aituan.member`、`com.aituan.coupon` 两个包。统一走 `ApiResponse`，分页走 `PageResponse`，角色由现有 `SecurityConfig` 路径前缀拦截。

### 6.1 会员（member 包）

| 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/app/account/member/info` | USER | 当前等级、成长值、距下一级进度、权益列表 |
| GET | `/api/admin/operation/member-levels` | ADMIN | 等级列表 |
| POST | `/api/admin/operation/member-levels` | ADMIN | 新增等级 |
| PUT | `/api/admin/operation/member-levels/{id}` | ADMIN | 编辑等级与权益 |

`member/info` 响应关键字段：`currentLevel{code,name,iconUrl,color}`、`growthValue`、`nextLevel{name,minGrowthValue}`、`progressPercent`、`benefits[]`。

### 6.2 优惠券（coupon 包）

| 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/app/account/coupons?status=usable\|used\|expired` | USER | 我的券（三态） |
| GET | `/api/app/coupons/available` | USER | 可领取的券 |
| POST | `/api/app/account/coupons/{templateId}/claim` | USER | 领取（校验限领、库存、上架） |
| GET | `/api/app/account/coupons/usable-for-order` | USER | 下单可用券（入参订单金额/上下文） |
| GET | `/api/admin/operation/coupon-templates` | ADMIN | 模板列表 |
| POST | `/api/admin/operation/coupon-templates` | ADMIN | 新增模板 |
| PUT | `/api/admin/operation/coupon-templates/{id}` | ADMIN | 编辑模板 |

校验点：领取校验 `per_user_limit`、`total_qty`/`issued_qty`、`status`；使用校验门槛、过期、归属、状态；并发下需对 issued_qty 与 user_coupon.status 做幂等/乐观控制。

### 6.3 消息增强 / 资料脱敏

| 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/app/message/station?type=...` | USER | 现有接口增加 type 筛选参数 |
| PATCH | `/api/app/message/station/read-all` | USER | 全部已读 |
| GET | `/api/app/account/profile` | USER | 现有接口返回值增加 `maskedPhone` / `maskedEmail` 展示字段 |

脱敏只用于展示字段，原始值仍可用于编辑提交；脱敏在服务层产出，前端直接展示。

## 7. 与成员 C 的协作边界（优惠券下单抵扣）

优惠券归 A，但「下单抵扣」发生在成员 C 的 `TradeService`。约定（**第 0 轮需与 C 当面敲定签名与调用时机**）：

- A 提供能力（`coupon` 包内）：
  - `CouponCalcResult calcDiscount(Long userId, Long userCouponId, BigDecimal orderAmount, OrderContext ctx)` — 返回可抵扣金额或不可用原因。
  - `void redeem(Long userCouponId, Long orderId)` — 下单成功后标记 used 并回填 used_order_id。
  - 可选：`void release(Long userCouponId)` — 订单取消/超时关闭时回退为 unused（与 C 的取消流程对齐后再做）。
- C 负责集成：`checkout/preview` 与创建订单接口增加 `couponId` 入参，preview 阶段调 `calcDiscount` 展示抵扣，下单阶段校验并 `redeem`。
- 降级方案：若联调未就绪，用户端优惠券页与领券先上线，checkout 暂「只展示可用券不抵扣」，抵扣随联调轮补齐。

## 8. 用户端实施（Flutter）

遵守单文件 ≤200 行、复杂页面拆 `presentation/widgets/`；新增方法集中在 `features/home/data/backend_app_repository.dart`。

### 8.1 新增页面与路由

| 页面 | 文件 | 路由常量 |
| --- | --- | --- |
| 资料编辑 | `features/profile/presentation/profile_edit_page.dart` | `/profile/edit` |
| 会员中心 | `features/member/presentation/member_center_page.dart` | `/member` |
| 优惠券（三 Tab） | `features/coupon/presentation/coupon_page.dart` | `/coupons` |

### 8.2 改造页面

- 收藏页：分类 Tab（店铺/商品，service 占位）+ 取消收藏按钮 + 取消后刷新。
- 消息页：分类筛选 + 已读/未读视觉 + 点击按 `related_target_type` 跳转订单/评价。
- checkout：优惠券选择入口（与 C 联调，未就绪则只展示）。
- profile：手机/邮箱脱敏展示、会员入口接 `/member`。

## 9. 后台 Web 实施（admin_web）

- 新增 `src/pages/MemberLevelsPage.vue`：等级列表 + 新增/编辑（含权益条目编辑）。
- 新增 `src/pages/CouponTemplatesPage.vue`：模板列表 + 新增/编辑（面额、门槛、有效期、限领、上下架）。
- `src/api.ts` / `src/types.ts` 补对应请求与类型，`components/AdminFrame.vue` 导航加入口。
- 补 `admin_web` 401 自动登出，对齐 `merchant_web` 现有处理。

## 10. 分轮实施清单与验收

### 第 0 轮 接口口径
- [ ] 本计划文档落定并挂索引。
- [ ] 与成员 C 敲定优惠券抵扣方法签名与调用时机。

### 第 1 轮 后端 + 数据
- [ ] `V009` 迁移：member_level / coupon_template / user_coupon / 消息跳转字段。
- [ ] seed 幂等补充上述演示数据。
- [ ] `member` 包：会员 info + 后台等级配置。
- [ ] `coupon` 包：领券 / 我的券 / 可用券 + 后台模板配置 + 抵扣能力（供 C 调用）。
- [ ] `message` 筛选 + 全部已读；`account` 资料脱敏。
- 验收：领券、查券（三态）、会员 info、消息筛选/全部已读 接口冒烟通过（D 盘运行）。

### 第 2 轮 用户端
- [ ] 资料编辑页、会员中心页、优惠券页。
- [ ] 收藏分类/取消、消息分类/已读/跳转、profile 脱敏与会员入口。
- [ ] checkout 优惠券入口（与 C 联调）。
- 验收：`dart format`、`flutter analyze`、`flutter test` 通过 + 手动链路走通。

### 第 3 轮 后台 Web + 联调 + 交付
- [ ] `admin_web` 会员等级配置页、优惠券模板配置页、401 登出。
- [ ] 全链路联调：后台配等级/券模板 → 用户端领券/看会员 → 下单抵扣（与 C）→ 消息跳转。
- [ ] 重打 APK、Web build，补文档与 `docs/ReadMe.md` 索引，清理构建冗余。
- 验收：总体链路全通，APK 与 jar 路径明确。

## 11. 风险与降级

| 风险 | 影响 | 降级方案 |
| --- | --- | --- |
| 优惠券抵扣跨人耦合（与 C） | 两边开工受阻 | 第 0 轮先约定签名；未就绪时 checkout 只展示不抵扣 |
| 优惠券/会员涉及金额 | 越权/超发/超用 | 后端强校验门槛、限领、库存、过期、归属；前端隐藏不算数 |
| 会员等级影响现有 profile 展示 | 旧字段语义冲突 | `member_level_name` 保留兼容，新体系按等级表计算后回填展示 |
| 收藏 service 分类依赖成员 D | 范围外阻塞 | 本阶段只做 store/item，service 占位留扩展 |
| C 盘空间不足 | 构建失败 | 构建/缓存/打包继续走 D 盘英文路径，打包后清理 |

## 12. 产物与文档

- 产物路径沿用约定：
  - `D:/aituan_release/backend/aituan-backend.jar`
  - `D:/aituan_release/apk/aituan-user-debug.apk`
- 构建、缓存、运行、打包优先 D 盘英文路径；打包后清理冗余构建目录，仅保留交付 APK。
- 本文档已挂入 `docs/ReadMe.md` 索引；各轮完成后补充交付记录。
