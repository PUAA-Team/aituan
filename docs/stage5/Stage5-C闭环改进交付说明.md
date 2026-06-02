# Stage5-C 外卖闭环与控制台改进交付说明

## 交付范围

本阶段围绕分工 C 的外卖配送闭环，同时补齐用户端、商家端、后台端在演示和联调中暴露出的缺口：

1. 用户端外卖体验：地址管理、下单地址选择、头像上传、订单取消/催单、配送跟踪、履约状态中文化。
2. 商家端控制台：订单处理、商品/服务管理、分类管理、图片上传、门店资料、自动/手动接单、配送规则、券码核销。
3. 后台端控制台：平台总览、订单治理、商户门店治理、用户状态、商品治理、配送任务、公告运营、平台配置与审计日志。
4. 后端能力：文件上传、本地图片存储、商家资料、商品治理、后台治理接口、用户外卖取消/催单接口。
5. 演示数据：补齐门店图、商品图、订单条目图、收藏图、头像图和后台公告图。

## 当前已完成能力

### 用户端 APP

- 外卖订单不再展示英文履约状态，使用中文节点和外卖专属状态文案。
- 配送履约记录只展示已到达节点和当前节点，不提前展示未发生节点。
- 外卖订单详情支持真实调用后端取消订单和催单接口。
- 外卖结算页加载用户地址，支持选择默认地址，无地址时阻止提交。
- 我的页支持头像上传，退出登录后清空本地资料并显示游客态。
- 地址列表、地址编辑、默认地址设置、删除地址已经接入后端。

### 商家端控制台

- 采用成熟控制台布局：左侧导航、顶部工具栏、表格、抽屉/弹窗、筛选区。
- 商品/服务管理支持按业务类型、状态、关键词筛选。
- 支持新建、编辑商品/服务，支持上传商品封面和上下架。
- 支持门店资料维护、门店封面上传。
- 配送与履约页整合自动/手动接单、配送规则、券码核销。
- 订单页支持查看详情和按当前履约阶段推进外卖订单。

### 平台后台控制台

- 平台总览展示今日订单、交易额、异常订单、商户、用户、商品和配送中任务。
- 订单治理支持订单筛选、详情查看、配送推进和异常标记。
- 商户门店页支持商户状态、门店开关、门店封面治理。
- 用户页支持账号状态治理。
- 商品治理支持按门店、业务类型、状态、关键词筛选和上下架。
- 配送任务页支持全局自动推进配置、单任务推进、暂停、恢复、异常。
- 公告运营支持新建、编辑、发布、下线。
- 设置审计页支持平台配置维护和操作日志查询。

### 后端

- 新增统一图片上传能力，默认本地目录为 `D:/aituan_runtime/uploads`。
- 新增 `file_asset` 表记录上传文件，保留后续替换对象存储/图床的扩展点。
- 用户头像、商家门店封面、商品封面、后台门店封面均复用统一上传服务。
- 商家端接口按当前登录商户解析门店，避免手工输入其他商家门店 ID。
- 后台端补齐订单、商户、用户、商品、配送、公告、配置和审计接口。
- 用户端外卖取消/催单接口写入订单状态日志和审计日志。

## 演示数据补齐

`database/seeds/R__seed_demo_data.sql` 已补齐以下演示字段：

| 数据类型 | 覆盖范围 | 说明 |
| --- | --- | --- |
| 用户头像 | `user_profile.avatar_url` | 默认用户头像有可展示图片 |
| 门店封面 | 14 个演示门店 | 用户端、商家端、后台端均可展示门店图 |
| 商品/服务封面 | 27 个商品/服务 | 覆盖外卖、团购、酒店、娱乐、电影、丽人、景点、按摩 |
| 订单条目图 | 演示订单明细 | 订单详情中商品条目不再空图 |
| 收藏封面 | 6 条收藏 | 收藏页覆盖门店和商品两种类型 |
| 平台公告图 | 3 条公告 | 后台公告运营有发布态演示数据 |
| 平台配置 | `upload_storage_type=local` | 记录当前上传存储实现，预留后续对象存储替换 |

所有种子数据继续使用 `ON DUPLICATE KEY UPDATE`，不清空真实数据，不覆盖表结构。

## 构建与验证命令

后续最终验证按以下命令执行：

```bash
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_backend.ps1"
PUB_CACHE="D:/aituan_cache/pub" flutter analyze
PUB_CACHE="D:/aituan_cache/pub" flutter test
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_android_apk.ps1"
npm --prefix apps/merchant_web --cache "D:/aituan_cache/npm" run build
npm --prefix apps/admin_web --cache "D:/aituan_cache/npm" run build
```

## 预期交付产物

- 后端 JAR：`D:/aituan_release/backend/aituan-backend.jar`
- 用户端 APK：`D:/aituan_release/apk/aituan-user-debug.apk`
- 商家端构建产物：`apps/merchant_web/dist/`
- 后台端构建产物：`apps/admin_web/dist/`

## 最终验证结果

| 验证项 | 结果 |
| --- | --- |
| 后端构建 | 通过，输出 `D:/aituan_release/backend/aituan-backend.jar` |
| 后端 demo smoke | 通过，覆盖用户端首页/收藏/订单、商家端当前门店/商品、后台端总览/公告 |
| Flutter format | 通过，`75 files (0 changed)` |
| Flutter analyze | 通过，`No issues found` |
| Flutter test | 通过，`All tests passed` |
| Android APK 打包 | 通过，输出 `D:/aituan_release/apk/aituan-user-debug.apk` |
| 商家端 Web 构建 | 通过，输出 `apps/merchant_web/dist/` |
| 后台端 Web 构建 | 通过，输出 `apps/admin_web/dist/` |
| 临时目录清理 | 通过，`D:/aituan_build/user_app` 已删除 |
