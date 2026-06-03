# Stage6 分工A · 后台会员优惠券与下单抵扣交付说明

## 1. 文档状态

- 所属阶段：Stage 6（后续五人模块分工）· 成员 A · 账号/用户资产/会员消息
- 对应计划：`docs/stage6/分工A-账号与用户资产会员消息实施计划.md` 第 3 轮「后台 Web + 联调 + 交付」
- 当前状态：编码完成；后端测试、后台 Web 构建、Flutter analyze/test、APK 打包均已通过
- 完成日期：2026-06-01

## 2. 交付范围

### 2.1 后台 Web

后台端新增两个运营配置页面：

- **会员等级**：维护等级编码、等级名称、成长值门槛、等级颜色、排序、状态和权益条目。
- **优惠券模板**：维护券名称、满减/折扣类型、面额、门槛、业务范围、有效期、库存、限领、状态。

同时补齐：

- 后台左侧导航入口。
- `api.ts` 接入 `/api/admin/operation/member-levels` 和 `/api/admin/operation/coupon-templates`。
- `types.ts` 补会员等级、权益条目、优惠券模板类型。
- `admin_web` 401/403 自动清 token 并返回登录态。

### 2.2 下单优惠券真实抵扣

用户端确认订单页的优惠券选择从“只记录选择”升级为真实联调：

1. 选择优惠券后重新调用 `checkout/preview` 试算。
2. 后端 `CheckoutPreviewRequest` 增加 `couponId`。
3. 后端 `CreateOrderRequest` 增加 `couponId`。
4. `TradeService` 通过 `CouponService.calcDiscount(...)` 计算抵扣金额。
5. 创建订单成功后调用 `CouponService.redeem(...)` 标记用户券已使用。
6. 用户取消外卖订单或商家拒单时，通过订单号回退已核销优惠券。
7. 用户端创建订单时把所选 `couponId` 传给后端，订单实付金额以后端返回为准。

## 3. 文件清单

### 3.1 后台 Web 新增

| 文件 | 说明 |
| --- | --- |
| `apps/admin_web/src/pages/MemberLevelsPage.vue` | 会员等级配置页 |
| `apps/admin_web/src/pages/CouponTemplatesPage.vue` | 优惠券模板配置页 |
| `apps/admin_web/package-lock.json` | 本地安装依赖后生成的锁文件 |

### 3.2 后台 Web 改动

| 文件 | 说明 |
| --- | --- |
| `apps/admin_web/src/App.vue` | 注册会员等级/优惠券模板页面，监听登录过期事件 |
| `apps/admin_web/src/components/AdminFrame.vue` | 左侧导航新增会员等级和优惠券模板入口 |
| `apps/admin_web/src/api.ts` | 新增会员/优惠券后台接口，401/403 自动登出 |
| `apps/admin_web/src/types.ts` | 新增会员等级、权益、优惠券模板类型 |
| `apps/admin_web/src/styles.css` | 补启用态标签、权益编辑器样式 |

### 3.3 后端与用户端改动

| 文件 | 说明 |
| --- | --- |
| `services/backend/src/main/java/com/aituan/trade/TradeDtos.java` | 预览/下单请求增加 `couponId` |
| `services/backend/src/main/java/com/aituan/trade/TradeService.java` | 交易链路接入优惠券试算、核销、取消回退 |
| `services/backend/src/main/java/com/aituan/coupon/CouponService.java` | 增加按订单回退用户券方法 |
| `services/backend/src/main/java/com/aituan/coupon/CouponRepository.java` | 增加 `used_order_id` 回退 SQL |
| `apps/user_app/lib/features/home/data/backend_app_repository.dart` | 预览/下单请求传 `couponId` |
| `apps/user_app/lib/features/checkout/presentation/checkout_page.dart` | 选择优惠券后重新试算金额，提交订单带券 |

## 4. 验证结果

### 4.1 后端测试

本机默认 `mvn` 使用 Java 11，会报 `不支持发行版本 17`。本次未修改系统环境变量，使用 IntelliJ 自带 JBR 21 临时执行：

```bash
export JAVA_HOME="/c/Program Files/JetBrains/IntelliJ IDEA Community Edition 2025.2.1/jbr"
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f services/backend/pom.xml -Dmaven.repo.local=D:/aituan_cache/m2 -q test
```

结果：通过。

### 4.2 后台 Web 构建

首次构建前 `apps/admin_web` 本地依赖缺失，经用户确认后执行：

```bash
npm --prefix apps/admin_web --cache "D:/aituan_cache/npm" install
npm --prefix apps/admin_web --cache "D:/aituan_cache/npm" run build
```

结果：通过，输出 `apps/admin_web/dist/`。

### 4.3 Flutter 用户端验证

```bash
cd apps/user_app
PUB_CACHE="D:/aituan_cache/pub" flutter --no-version-check analyze
PUB_CACHE="D:/aituan_cache/pub" flutter --no-version-check test
```

结果：

- `flutter analyze`：通过，`No issues found!`
- `flutter test`：通过，`All tests passed!`，共 7 个测试。

### 4.4 后端 jar 构建

使用临时 JBR 21 和 D 盘 Maven 缓存构建：

```bash
export JAVA_HOME="/c/Program Files/JetBrains/IntelliJ IDEA Community Edition 2025.2.1/jbr"
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f "C:/Users/baozh/Downloads/aituan-stage5-c-takeaway-delivery/aituan-stage5-c-takeaway-delivery/services/backend/pom.xml" \
  -Dmaven.repo.local="D:/aituan_cache/m2" \
  -Dbackend.build.directory="D:/aituan_build/backend/target" \
  -DskipTests clean package
```

结果：通过，输出：

```text
D:/aituan_release/backend/aituan-backend.jar
```

### 4.5 APK 打包

使用项目脚本在 D 盘构建，Gradle 注入 Clash 代理参数：

```bash
GRADLE_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897' \
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_android_apk.ps1
```

结果：通过，输出：

```text
D:/aituan_release/apk/aituan-user-debug.apk
```

脚本完成后已清理 `D:/aituan_build/user_app`；后端构建命令也已清理 `D:/aituan_build/backend`。

## 5. 手动验收建议

1. 启动后端 jar，登录后台管理员账号。
2. 后台进入「会员等级」，新增或编辑一个会员等级，确认权益条目保存后刷新仍存在。
3. 后台进入「优惠券模板」，新增或编辑一个满减券/折扣券，确认状态、库存、有效期展示正确。
4. 用户端登录演示用户，进入优惠券页领取可用券。
5. 进入商品/服务下单页，选择优惠券后确认订单页应重新试算优惠和实付金额。
6. 提交订单后，该用户券应变为已用；取消可取消的外卖订单或商家拒单后，应回退为未使用。
7. 后台 token 过期或权限失效时，应自动清 token 并回到登录态。

## 6. 潜在问题与说明

- 当前优惠券抵扣按 `商品金额 + 配送费` 作为试算金额；如后续需要“仅商品金额可抵扣”，需调整 `TradeService` 中试算基准。
- `admin_web` 依赖已本地安装到 `apps/admin_web/node_modules`，便于后续继续构建；如果 C 盘空间紧张，可在确认后删除并在下次构建前重新 `npm install`。
- 本机项目脚本 `scripts/build/build_backend.ps1` 默认写死 `D:\tools\jdk-17.0.18+8`，该路径当前不存在；本次用 IntelliJ JBR 21 临时构建，未修改系统环境变量。
