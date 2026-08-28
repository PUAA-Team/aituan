# Stage 9 端到端业务场景测试（UC01-UC07）

## 1. 背景与目标

对应 8.26-27 任务分工第 3 项：完成前 7 个端到端业务场景测试（UC01-UC07），
覆盖“从页面或接口入口走完一个完整业务流程”，并在 CI 中自动执行、失败即停。

实现方案：

- 测试工程：`tests/e2e`（Playwright 1.62 + TypeScript）
- 统一入口：`tests/e2e/playwright.config.ts`
- 本地数据库：H2（`e2e` profile，`services/backend/src/main/resources/application-e2e.yml`）
- CI 数据库：MySQL 8 service（`.github/workflows/ci.yml` 的 `e2e` job）
- 用户端：Flutter Web 无障碍语义树驱动
- 商家端 / 后台端：Vue Web 普通 DOM 驱动
- 用户侧复杂业务：API 客户端 + 前端展示断言

## 2. 用例覆盖清单

| 测试编号 | 业务场景 | 自动化文件 | 入口方式 |
| --- | --- | --- | --- |
| E2E-TC01 | 用户注册、登录并进入受保护业务 | `specs/uc01-auth-flow.spec.ts` | Flutter Web 界面 + API |
| E2E-TC02 | 用户搜索筛选商家/商品并查看详情 | `specs/uc02-discovery-flow.spec.ts` | Flutter Web 界面 + API |
| E2E-TC03 | 用户维护个人资料、地址和收藏 | `specs/uc03-account-assets-flow.spec.ts` | API + Flutter Web 展示 |
| E2E-TC04 | 用户完成外卖点单、优惠结算和模拟支付 | `specs/uc04-takeaway-order-flow.spec.ts` | API 闭环 + Flutter Web 展示 |
| E2E-TC05 | 商家处理外卖订单并推进配送履约 | `specs/uc05-delivery-fulfillment-flow.spec.ts` | 商家 Vue Web + API 闭环 |
| E2E-TC06 | 用户购买非外卖服务并获得券码凭证 | `specs/uc06-service-voucher-flow.spec.ts` | API 出券 + 用户展示 + 商家核销 |
| E2E-TC07 | 用户预约到店/团购服务并由商家核销确认 | `specs/uc07-booking-redeem-flow.spec.ts` | API 预约 + 商家 Vue Web 确认 |

## 3. 本地运行

```powershell
cd D:\aituan\tests\e2e
.\scripts\run-e2e-local.ps1
```

脚本构建后端（H2 + Flyway + seed）、Flutter Web、商家端、后台端，启动 8080 后端与
8090 静态服务（`/api` 反向代理），然后运行 Playwright。详细说明见
`tests/e2e/README.md`。

## 4. CI 接入

`ci.yml` 新增 `e2e` job：

- MySQL 8 service 自动建库并健康检查；
- 构建后端 JAR、用户端 Flutter Web、商家端/后台端 Vue Web；
- 启动后端 `e2e` profile（连接 MySQL）；
- 启动静态服务并验证 `/web/`、`/merchant/`、`/admin/` 可访问；
- `npx playwright test`，任何用例失败都会使 CI 失败；
- 上传 HTML 报告与 `test-results` 产物。

## 5. 测试数据与幂等性

- 演示账号：`demo_user`、`demo_takeaway_merchant`、`demo_groupbuy_merchant`、
  `demo_massage_merchant`，密码 `123456`。
- 注册用例使用时间戳生成唯一手机号/邮箱；下单用例每次创建新订单。
- 本地 H2 每次全新；CI MySQL 每次 job 全新，重复运行不互相污染。

## 6. 验证结果口径

E2E 报告包含：测试总数、通过数、失败数、失败原因、运行环境（浏览器、端口、数据库）、
视频/截图/trace 产物。追溯编号 `E2E-TC01` 至 `E2E-TC07` 供追溯表使用。
