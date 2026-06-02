# Stage6 分工A · 后台管理员资料交付说明

## 1. 文档状态

- 所属阶段：Stage 6（后续五人模块分工）· 成员 A · 账号/用户资产/会员消息
- 对应计划：`docs/后续阶段五人模块分工计划.md` §9.3「商家账号资料和后台管理员资料」
- 当前状态：编码完成；后端编译验证通过；后台 Web 构建通过
- 范围边界：本次只补齐后台管理员自己的资料只读查看。`iam_account.login_name` 是登录凭据之一，本次不提供昵称编辑，避免破坏管理员登录名；密码修改、手机号换绑、权限分配等高风险能力均不扩展。

## 2. 交付范围

### 2.1 后端接口

新增后台管理员资料接口，均受 `/api/admin/**` 的 ADMIN 角色保护：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/admin/account/profile` | 查询当前管理员账号资料 |

当前管理员资料直接复用 `iam_account`：

- `account_no`：账号编号，只读展示。
- `account_type`：固定为 `ADMIN`，只读展示。
- `login_name`：作为后台管理员显示名称只读展示；它也是登录凭据之一，本次不允许在资料页修改。
- `phone` / `email` / `status` / `created_at` / `last_login_at`：只读展示。

### 2.2 后台 Web 页面

新增「管理员资料」导航入口和页面：

- 左侧导航新增「管理员资料」。
- 页面左侧展示显示名称、账号编号、账号类型和账号状态。
- 页面右侧展示手机号、邮箱、创建时间、最近登录时间。
- 资料加载失败时沿用后台全局 notice 提示。

## 3. 文件清单

### 新增

| 文件 | 说明 |
| --- | --- |
| `apps/admin_web/src/pages/AdminProfilePage.vue` | 后台管理员资料页 |
| `docs/stage6/分工A-后台管理员资料交付说明.md` | 本交付说明 |

### 改动

| 文件 | 说明 |
| --- | --- |
| `services/backend/src/main/java/com/aituan/admin/AdminController.java` | 新增管理员资料查询接口 |
| `services/backend/src/main/java/com/aituan/admin/AdminService.java` | 新增管理员资料查询业务逻辑 |
| `services/backend/src/main/java/com/aituan/admin/AdminRepository.java` | 新增 `iam_account` 查询 |
| `services/backend/src/main/java/com/aituan/admin/AdminDtos.java` | 新增管理员资料 DTO |
| `apps/admin_web/src/api.ts` | 新增管理员资料查询 API 封装 |
| `apps/admin_web/src/types.ts` | 新增 `AdminProfile` 与页面枚举 |
| `apps/admin_web/src/App.vue` | 注册管理员资料页与页面标题 |
| `apps/admin_web/src/components/AdminFrame.vue` | 左侧导航新增管理员资料入口 |
| `docs/ReadMe.md` | 增加本交付说明索引 |

## 4. 验证结果

### 4.1 后端验证

首次直接执行 Maven 时失败：当前默认 Maven 使用 `D:\soft\jdk-11`，项目要求 Java 17，因此报错 `不支持发行版本 17`。

随后仅对本次命令临时指定本机 JDK 21（不修改系统环境变量）重新验证：

```bash
cd services/backend
JAVA_HOME='D:/soft/OracleJdk/Jdk21/jdk-21.0.8' \
PATH='D:/soft/OracleJdk/Jdk21/jdk-21.0.8/bin':$PATH \
mvn -Dmaven.repo.local=D:/aituan_cache/m2 -Dbackend.build.directory=D:/aituan_build/backend-target test
```

结果：`BUILD SUCCESS`。

后续已补充 Stage6 分工A 后端接口集成测试，覆盖管理员资料角色拦截、会员进度、优惠券查询/下单可用券、消息单条已读；详见 `docs/stage6/分工A-后端接口测试交付说明.md`。

### 4.2 后台 Web 构建

```bash
cd apps/admin_web
npm run build
```

结果：`vue-tsc --noEmit` 与 `vite build` 均通过。

## 5. 手动验收建议

1. 启动后端和后台 Web。
2. 使用演示管理员账号 `demo_admin / 123456` 登录后台。
3. 点击左侧「管理员资料」。
4. 确认页面能看到账号编号 `A202605170001`、账号类型 `ADMIN`、手机号和邮箱。
5. 确认页面没有昵称保存按钮，避免误改 `login_name` 登录凭据。

## 6. 后续增强项

- 管理员密码修改、手机号/邮箱换绑、管理员账号管理属于权限与安全相关能力，后续如需实现应单独确认方案。
- 当前仅支持当前登录管理员只读查看自己的账号资料，不提供修改其他管理员资料的能力。
