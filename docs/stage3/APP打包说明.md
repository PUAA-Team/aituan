# 爱团 APP 打包说明（Stage 3）

## 1. 目标

用户端 Flutter APP 源码仍保留在仓库内：

```text
apps/user_app/
```

为了避免 C 盘空间不足，所有预览构建、APK 打包、Flutter 依赖缓存和 Gradle 缓存都放到 D 盘英文路径执行。

## 2. D 盘路径约定

| 用途 | 路径 |
| --- | --- |
| 预览运行工作目录 | `D:/aituan_build/user_app_preview/` |
| APK 打包工作目录 | `D:/aituan_build/user_app/` |
| APK 输出目录 | `D:/aituan_release/apk/` |
| Flutter Pub 缓存 | `D:/aituan_cache/pub/` |
| Gradle 缓存 | `D:/aituan_cache/gradle/` |

## 3. 本地预览

在项目根目录运行：

```powershell
.\scripts\dev\start_user_app.ps1
```

脚本会把 `apps/user_app/` 同步到 `D:/aituan_build/user_app_preview/`，然后在 D 盘工作目录执行：

```powershell
flutter pub get
flutter run
```

## 4. 生成 Debug APK

在项目根目录运行：

```powershell
.\scripts\build\build_android_apk.ps1
```

脚本会：

1. 将源码同步到 `D:/aituan_build/user_app/`。
2. 设置 `PUB_CACHE=D:/aituan_cache/pub/`。
3. 设置 `GRADLE_USER_HOME=D:/aituan_cache/gradle/`。
4. 执行 `flutter pub get`、`flutter analyze`、`flutter test`。
5. 执行 `flutter build apk --debug`。
6. 将 APK 复制到：

```text
D:/aituan_release/apk/aituan-user-debug.apk
```

7. 删除 `D:/aituan_build/user_app/` 临时构建目录，只保留最终 APK。

## 5. 清理冗余构建目录

普通清理：

```powershell
.\scripts\release\clean_build_artifacts.ps1
```

如果需要连依赖缓存也清理：

```powershell
.\scripts\release\clean_build_artifacts.ps1 -IncludeCache
```

默认不会删除 `D:/aituan_release/apk/` 下的 APK 成品。

## 6. 当前说明

当前 Flutter APP 按真实前端框架建设，使用本地 Mock 数据作为临时数据源；搜索、模块、商家、订单等页面按未来后端接口边界组织，代码中保留 `AppRepository` 与 `AppApiClient`，后续接后端时替换数据源即可。
