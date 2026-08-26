# 爱团端到端测试（UC01-UC07）

本目录是爱团 8.26 任务分工第 3 项的端到端测试工程，覆盖前 7 个业务场景：

| 编号 | 业务场景 | 自动化入口 |
| --- | --- | --- |
| UC01 | 用户注册、登录并进入受保护业务 | 用户端 Flutter Web 界面 + API |
| UC02 | 用户搜索筛选商家/商品并查看详情 | 用户端 Flutter Web 界面 + API |
| UC03 | 用户维护个人资料、地址和收藏 | API + 用户端 Flutter Web 展示 |
| UC04 | 用户完成外卖点单、优惠结算和模拟支付 | API 闭环 + 用户端订单页展示 |
| UC05 | 商家处理外卖订单并推进配送履约，用户接收订单状态消息 | 商家端 Vue Web 界面 + API 闭环 |
| UC06 | 用户购买非外卖服务并获得券码凭证 | API 出券 + 用户端展示 + 商家端核销 |
| UC07 | 用户预约到店/团购服务并由商家核销确认 | API 预约 + 商家端确认 |

## 技术栈

- Playwright 1.62 + TypeScript
- 后端 Spring Boot 运行在 `e2e` profile（本地默认 H2，CI 使用 MySQL 8）
- 用户端 Flutter Web 使用无障碍语义树驱动（浏览器自动化可读、可输入、可点击）
- 商家端 / 后台端 Vue Web 为普通 DOM，直接驱动

## 本地运行（Windows / PowerShell）

前置要求：JDK 17、Maven、Node.js、Flutter、Microsoft Edge（或设置
`PLAYWRIGHT_BROWSER_PATH`）。

```powershell
cd D:\aituan\tests\e2e
.\scripts\run-e2e-local.ps1
```

脚本会：

1. 构建后端 JAR（输出 `.aituan-local/e2e/backend`，不占用 C 盘）。
2. 构建用户端 Flutter Web、商家端 Web、后台端 Web（输出 `.aituan-local/e2e/web`）。
3. 启动后端（8080，H2 + Flyway 迁移 + seed 数据，验证码回显）和静态服务（8090，`/api` 反向代理）。
4. 运行 `npx playwright test`，失败时退出码非 0。

只运行单个场景：

```powershell
npx playwright test uc04-takeaway-order-flow
```

如需使用本机已有环境（后端 8080、静态服务 8090 已启动）直接运行测试：

```powershell
npm ci
$env:E2E_API_ORIGIN="http://127.0.0.1:8080"
$env:E2E_WEB_ORIGIN="http://127.0.0.1:8090"
$env:E2E_ARTIFACTS_ROOT="D:\aituan\.aituan-local\e2e\web"
$env:PLAYWRIGHT_BROWSER_PATH="C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
npm test
```

## CI

`.github/workflows/ci.yml` 中新增 `e2e` job：使用 MySQL 8 service，构建后端与三个前端，
启动后端 `e2e` profile，运行 Playwright，上传 HTML 报告与 JSON 结果。E2E 失败会使 CI 失败，
满足“测试失败流水线即停”。

## 测试数据

- 演示账号：`demo_user` / `demo_takeaway_merchant` / `demo_groupbuy_merchant` /
  `demo_massage_merchant`，密码均为 `123456`。
- UC01 注册使用唯一手机号和邮箱，其余用例创建独立订单，重复运行不会相互覆盖。
- 本地 H2 每次启动为全新库；CI MySQL 每次 job 也是全新容器。

## 报告与追溯

- HTML 报告：`tests/e2e/playwright-report/index.html`
- JSON 结果：`tests/e2e/test-results/e2e-results.json`
- 用例编号对应追溯表：`E2E-TC01` 至 `E2E-TC07`
