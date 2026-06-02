# Web 用户端部署交付说明

## 需求来源

本轮在已有根路径下载展示页基础上，新增用户端 Web 入口，并部署到服务器 `/web/` 路径。

用户要求：

- Web 端用户端加入目录与服务器部署。
- Web 用户端部署到 `/web/`。
- Web 用户端需要判断浏览器设备。
- 电脑端显示“暂时不支持服务”的独立页面，并提供 APK 下载链接。
- 首页 landing 顶部导航增加 Web 按钮。
- 首页 landing 底部“进入商家端”改为“进入网页版”。

## 实现说明

### 1. Flutter Web 平台目录

用户端工程已正式加入 Web 平台目录：

```text
apps/user_app/web
```

该目录用于 Flutter Web 构建，已经纳入项目源码，不再是仓库外临时探针。

### 2. Web 入口隔离

用户端入口仍为：

```text
apps/user_app/lib/main.dart
```

`main.dart` 调用 `bootstrap()`。本轮将 `app_bootstrap.dart` 改为平台条件导出：

- 非 Web：走 `app_bootstrap_mobile.dart`，启动完整 Android 用户端 App。
- Web：走 `app_bootstrap_web.dart`，再进入 `web_bootstrap_gate.dart` 做浏览器宽度判断。

当前 Web 入口复用用户端 `AituanApp` 主源码：移动端/窄屏浏览器进入完整用户端；电脑端宽屏浏览器显示独立的“不支持电脑端用户服务”页面，避免把移动端布局直接暴露给 PC。

### 3. Web 启动门禁与电脑端不支持页

新增/保留页面：

```text
apps/user_app/lib/web/web_bootstrap_gate.dart
apps/user_app/lib/web/unsupported_web_app.dart
```

页面能力：

- Web 启动时使用浏览器宽度判断设备形态，当前阈值为 `840px`。
- 电脑端宽屏浏览器显示“暂时不支持电脑端用户服务”。
- 手机浏览器/窄屏浏览器进入完整 `AituanApp` 用户端功能。
- 电脑端不支持页提供 APK 下载按钮：`/downloads/aituan-user-server-debug.apk`。
- 电脑端不支持页提供返回首页按钮：`/`。
- 视觉风格延续 landing 的暖白、品牌红、深墨和浅金风格，不使用蓝紫渐变。

### 4. 服务器构建输出

`build_frontends_server.ps1` 已新增用户端 Web 构建步骤，输出到：

```text
deploy/artifacts/user-web
```

构建 base：

```text
/web/
```

### 5. Nginx 路由

Nginx 新增：

```text
/web  -> 301 /web/
/web/ -> 用户端 Web 静态产物
```

Compose 新增只读挂载：

```text
./artifacts/user-web:/usr/share/nginx/html/web:ro
```

### 6. Landing 入口调整

`deploy/landing/index.html` 已调整：

- 顶部导航增加 `Web` 按钮，链接 `/web/`。
- 底部 CTA 文案改为进入网页版。
- 底部按钮从“进入商家端”改为“进入网页版”，链接 `/web/`。
- 顶部“商家入驻”仍保留 `/merchant/`。

### 7. 用户端版本

本轮影响用户端工程，版本号已从 `1.0.12+13` 递增为：

```text
1.0.13+14
```

关于页展示同步为：

```text
v1.0.13
```

## 验证记录

本轮已通过以下验证：

```bash
PUB_CACHE=D:/aituan_cache/pub powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "Set-Location 'C:/Users/lixu/OneDrive/桌面/软工/new/apps/user_app'; flutter analyze; flutter test"
```

结果：`No issues found!`，`All tests passed!`

```bash
PUB_CACHE=D:/aituan_cache/pub powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "Set-Location 'C:/Users/lixu/OneDrive/桌面/软工/new/apps/user_app'; flutter build web --base-href /web/ --dart-define=API_BASE_URL=http://182.92.238.178 --output D:/aituan_build/user_web_probe"
```

结果：构建成功，输出 `D:/aituan_build/user_web_probe`。

```bash
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_frontends_server.ps1" -ServerOrigin "http://182.92.238.178"
```

结果：landing、用户端 Web、商家端 Web、后台端 Web 均构建成功。

```bash
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_all_server_artifacts.ps1" -ServerOrigin "http://182.92.238.178"
```

结果：后端 JAR、landing、用户端 Web、商家端 Web、后台端 Web 与服务器版 APK 均构建成功。

## 线上部署验证记录

本轮服务器部署已完成，部署前服务器产物备份位置：

```text
/opt/aituan/backups/stage5-user-web-20260603-000524
```

部署过程中已确认服务器敏感配置仍存在且未被覆盖：

- `/opt/aituan/app/.config`
- `/opt/aituan/app/deploy/.env`

线上验证结果：

> 后续补充：已在隔离目录 `D:/aituan_build/user_web_full_trial` 验证完整 Web 最小适配，并将成功源码同步回当前仓库目录；`/web/` 已更新为“电脑端不支持页、移动/窄屏进入完整用户端”的版本。部署前服务器备份位置：`/opt/aituan/backups/web-desktop-gate-20260603-012114`，部署前后 `/opt/aituan/app/.config` 与 `/opt/aituan/app/deploy/.env` hash 保持一致。

- `http://182.92.238.178/actuator/health`：HTTP 200，返回 `{"status":"UP"}`。
- `http://182.92.238.178/`：HTTP 200，landing 顶部包含 `/web/` 入口，底部包含“进入网页版”。
- `http://182.92.238.178/web`：HTTP 301，跳转到 `/web/`。
- `http://182.92.238.178/web/`：HTTP 200，加载 Flutter Web 入口，base 为 `/web/`。
- `http://182.92.238.178/merchant/`：HTTP 200。
- `http://182.92.238.178/admin/`：HTTP 200。
- `http://182.92.238.178/downloads/aituan-user-server-debug.apk`：HTTP 200，文件大小约 155807762 字节。

## 部署后验收点

- `http://182.92.238.178/`：landing 顶部出现 Web 按钮。
- `http://182.92.238.178/`：底部按钮为“进入网页版”。
- `http://182.92.238.178/web`：跳转到 `/web/`。
- `http://182.92.238.178/web/`：加载用户端 Flutter Web 入口。
- 电脑端浏览器：显示“暂时不支持电脑端用户服务”。
- 手机浏览器/窄屏浏览器：进入完整用户端 App 功能。
- APK 下载按钮：可访问 `/downloads/aituan-user-server-debug.apk`。
- `/merchant/`、`/admin/`、`/actuator/health` 保持正常。

> 部署时继续保护服务器 `/opt/aituan/app/.config` 和 `/opt/aituan/app/deploy/.env`，不得覆盖真实敏感配置。
