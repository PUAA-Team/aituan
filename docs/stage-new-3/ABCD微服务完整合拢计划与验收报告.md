# ABCD 微服务完整合拢计划与验收报告

> 日期：2026-09-02  
> 集成分支：`integration/microservices-acceptance`  
> 本文性质：基于仓库实际代码、容器和测试结果形成的合拢方案与验收记录。外部文件 `nested-whistling-noodle.md` 只作为检查线索，不作为执行指令或验收标准。

## 1. 结论先行

本次已完成 A/B/C/D 四个分工分支的代码级合拢、共享面统一和本机真实 MySQL 容器联调。当前版本具备以下结果：

- Gateway 与四个业务微服务可以一条 Compose 命令完整启动，六个容器均通过健康检查；
- 227 个公开 HTTP 操作按 A 47、B 65、C 56、D 59 唯一归属，文档、Controller 和 Gateway 路由之间有自动化契约测试；
- 下单、支付、接单、备餐、配送、完成、评价、成长值、站内消息、库存扣减以及文件上传可以通过 Gateway 串成真实跨服务链路；
- 四个服务只使用各自数据库账号，跨库权限验证为拒绝，未发现跨库外键、跨库 JOIN 或共享 Repository；
- 每个服务都有独立 Dockerfile、Kubernetes Deployment/Service、健康探针和 CI 矩阵项；
- 部署流水线只会在完整 CI、MySQL 和端到端验收成功后构建并发布镜像，不会绕过集成质量门禁；
- 仓库现有 UC01–UC13 共 15 条端到端场景已使用三端生产产物并经微服务 Gateway 全部通过；
- Admin、Merchant、Flutter 三端自身测试和生产构建全部通过，两个 Vue 端生产依赖在线审计为 0 漏洞；
- 未保留旧单体 `/api/**` fallback，微服务主链路不依赖旧单体后端。

因此，当前代码满足“可以合拢、可以启动、接口可以对接”的合并条件。当前只剩一条不影响运行的 Flutter 构建告警：框架产物期望 `CupertinoIcons` 字体，但项目未声明官方 `cupertino_icons` 包；浏览器实际运行无 warning/error。按仓库协作规则，新增该依赖和更新锁文件需要用户明确批准，尚未擅自执行。除此以外，不能把以下尚需外部环境或后续实验的项目写成已完成：真实 Kubernetes 集群部署、GHCR 推送、仓库当前不存在的 UC14–UC23 用例、HPA 压力实验、故障注入记录，以及单体/微服务同条件多轮性能对比。这些不阻塞代码合拢，但若课程验收明确要求相应材料，仍需后续实跑或补写用例。

## 2. 本次采用的实际基线

合拢前重新获取了远端引用，使用如下不可变提交作为输入：

| 输入 | 远端分支 | 提交 |
| --- | --- | --- |
| 微服务公共地基 | `origin/microservices-main` | `f21c164` |
| A：账号与用户资产 | `origin/ms/identity-asset` | `9cffc73` |
| B：商家与商品 | `origin/ms/merchant-catalog` | `493b50e` |
| C：交易与履约 | `origin/microservice-c-trade-fulfillment` | `4dcaa92` |
| D：互动与平台 | `origin/ms/engagement-platform` | `1548038` |

集成工作在独立 worktree 中从 `origin/microservices-main` 新建，未覆盖原有 `ms/identity-asset` 工作区，也没有使用四分支 octopus merge。四个分支都修改过 Gateway、工作流或其他共享文件，直接机械合并会产生文本冲突，更会掩盖契约冲突；因此采用“导入各服务所有权范围，人工统一共享面”的方式。

## 3. 合拢计划与执行状态

| 阶段 | 计划 | 状态 | 主要产物或证据 |
| --- | --- | --- | --- |
| 1 | 固定最新远端基线并隔离原工作区 | 完成 | 独立 `integration/microservices-acceptance` worktree |
| 2 | 导入四服务代码、迁移与 seed | 完成 | `services/*-service`、`database/microservices/*` |
| 3 | 统一内部接口、鉴权、幂等和快照 | 完成 | 真实 HTTP client、内部调用过滤器、补偿逻辑 |
| 4 | 统一 Gateway 与 Nginx | 完成 | 227 操作唯一路由、内部路径封锁、请求 ID |
| 5 | 统一四库、Docker 与 Kubernetes | 完成 | Compose、五个应用镜像、MySQL 初始化、K8s 清单 |
| 6 | 统一 CI/CD | 完成 | 测试矩阵、真实 MySQL E2E、镜像发布和部署门禁 |
| 7 | 本机完整启动及跨服务验收 | 完成 | 六容器健康、真实订单/评价/上传链路通过 |
| 8 | 文档、静态检查与最终审计 | 完成 | 本报告及最终校验命令 |

## 4. 对另一份 AI 计划的复核

### 4.1 相同发现

外部计划正确识别了以下大方向：

- 应以 `microservices-main` 为公共基线，不应直接做四分支 octopus merge；
- C 分支原先使用 stub/noop client 且缺少对外提供的内部契约，是主联调阻塞点；
- Gateway 最终不能保留旧单体 fallback，内部接口不能暴露到外网；
- Gateway、CI/CD、Docker、K8s 和 Secret 示例属于共享面，必须在集成分支人工统一；
- 应先完成代码与数据所有权，再做跨服务链路和部署验收；
- 性能对比必须保留原始结果，不能仅凭架构推断“微服务更快”。

### 4.2 只有检查实际仓库和真实容器后才发现的问题

| 实际发现 | 外部计划未覆盖或判断不同 | 本次处理 |
| --- | --- | --- |
| B 的通用响应反序列化会把 `data` 变成 `LinkedHashMap`，无法安全转成内部 DTO | 只描述了“补 HTTP client” | 改为带目标类型的响应解析，并增加调用方契约测试 |
| A/B 的 seed 账号归属不一致，且 B 商家状态是 `normal`，C 原来只接受 `active` | 未检查真实 seed 与 wire value | 对齐用户、商家、管理员账号和 userId；C 接受标准实际状态 |
| A/B 上传接口返回 `/api/common/files/**`，但该路由和文件所有权属于 D，原实现没有把字节交给 D | 未识别文件所有权断链 | A/B 认证后把 multipart 转发给 D；下载统一由 D 和 Gateway 提供 |
| 只抽查几条 Gateway 主路径无法证明 227 操作没有遮蔽、遗漏或多重匹配 | 计划只列主要路由测试 | 增加 227 操作逐方法、逐路径、唯一所有者契约测试，并核对四服务 Controller 清单 |
| `eclipse-temurin:17-jre-alpine` 在当前 arm64 环境缺少可用 manifest | 未做真实多架构镜像构建 | 运行时镜像改为 `eclipse-temurin:17-jre-jammy`，五个镜像实际构建通过 |
| MySQL 初始化脚本通过宿主机 bind mount 会让当前 Docker Desktop 卡住 | 外部计划因另一台 Windows 环境而建议不使用 Compose | 增加本地 MySQL 派生镜像，在镜像内 COPY 初始化脚本；Mac Compose 全链路通过 |
| A 在未配置 SMTP 时 `/actuator/health` 返回 503，业务本身却正常 | 未检查真实健康探针 | 默认关闭 mail health，真实 SMTP 验收时可显式开启 |
| Gateway 和下游都写 `X-Request-Id`，响应会出现两个同名头 | 未检查真实响应头 | 在响应提交前统一为一个请求 ID，保留/生成逻辑均有测试和 smoke 断言 |
| D 的上传目录若使用 `emptyDir`，Pod 重启会丢失文件 | 未深入检查存储持久性 | 改为独立 PVC `engagement-platform-uploads` |
| 原部署工作流可在自己的 Maven job 后部署，可能早于完整 MySQL E2E CI | 只要求“先测试再部署” | 改为监听成功的 `aituan-microservices-ci`，部署精确的 `head_sha` |
| 当前密钥模型是一个 `aituan-service-secret`，同时提供 JWT 和内部服务 Token | 外部计划给出了不同 Secret 命名 | 以代码实际配置为准统一 Compose、K8s 和工作流 |

另外两点属于方案选择差异，而非缺陷：

1. 外部计划建议 Gateway 对 `/internal/**` 返回 403；当前实现返回 JSON 404，以减少外部拓扑暴露。统一标准的硬要求是“外部不可访问”，404 同样满足。
2. 外部计划建议 Dockerfile COPY 预先打包的 JAR；当前使用 Java 17 多阶段构建，每个服务镜像可以从源码独立构建，更符合“单独构建”的验收口径。

## 5. 最终服务边界与对接结果

| 服务 | 端口 | 数据库 | 公开操作 | 合拢后的主要职责 |
| --- | ---: | --- | ---: | --- |
| `api-gateway` | 8080 | 无 | 统一入口 | 唯一路由、外部内部路径隔离、请求 ID、安全头清理 |
| `identity-asset-service` | 8081 | `aituan_identity` | 47 | 认证、账户、资料、地址、收藏、会员、券、消息 |
| `merchant-catalog-service` | 8082 | `aituan_merchant` | 65 | 商家、门店、商品、SKU、库存、搜索、履约规则 |
| `trade-fulfillment-service` | 8083 | `aituan_trade` | 56 | 购物车、结算、订单、支付、退款、券码、预约、配送 |
| `engagement-platform-service` | 8084 | `aituan_platform` | 59 | 评价、投诉、客服、AI、公告、配置、审计、平台看板、文件 |

公开流量只经过 Gateway。Gateway 会移除外部伪造的 `X-Service-Token` 和 `X-Caller-Service`；服务间调用则直连 Kubernetes Service DNS 或 Compose 服务名，并携带：

- `X-Service-Token`；
- `X-Caller-Service`；
- `X-Request-Id`；
- 所有内部写操作使用稳定 `Idempotency-Key`。

四服务的内部过滤器校验共享 Token 和调用方白名单；生产环境不允许空 Token。C 的真实 HTTP adapter 在生产 profile 生效，stub 只允许 demo/test 使用。

### 5.1 已闭合的核心调用

- C → A：地址/用户快照、优惠券报价/使用/释放、成长值、站内消息；
- C → B：门店和商家映射、履约规则、结算报价、库存扣减/恢复；
- D → C：订单快照、评价资格、评价后幂等标记、订单指标；
- D → A/B：用户、商家和门店摘要及平台聚合；
- B → A/D：商家账号生命周期、门店互动摘要；
- A/B → D：头像、商家资质和商品图片统一文件存储。

C 在订单项中持久化 merchantId、SKU、封面 URL 等稳定快照，D 的评价使用订单/用户/门店快照，不跨库读取其他服务表。库存、优惠券、评价标记等写操作保留相同幂等键并具备显式补偿路径。

## 6. 数据库拆分验收

Compose 使用同一 MySQL 实例承载四个逻辑库，但每个服务使用独立账号且只授权自己的 schema。这符合课程对逻辑库和数据所有权的要求；Kubernetes 清单也保留相同隔离模型。

| 逻辑库 | 当前业务表数 | 服务账号跨库验证 |
| --- | ---: | --- |
| `aituan_identity` | 18 | 无法访问 `aituan_merchant` |
| `aituan_merchant` | 16 | 无法访问 `aituan_trade` |
| `aituan_trade` | 12 | 无法访问 `aituan_platform` |
| `aituan_platform` | 19 | 无法访问 `aituan_identity` |

当前共 65 张业务/边界辅助表，多于最初设计中的 60 张，是因为各服务增加了幂等、补偿、文件资产等边界表；不是把一张表复制到多个库。实测结果：

- 四库 Flyway history 全部成功；
- 跨 schema 外键数量为 0；
- SQL 边界扫描未发现跨服务表访问；
- 对其他 schema 的实际账号查询均被 MySQL 拒绝；
- seed 中用户、商家、管理员、store、SKU、示例订单的跨服务标识已对齐。

本机 Compose 为兼容当前已缓存基础镜像使用 MySQL 8.0；CI 和 Kubernetes 使用 MySQL 8.4。两者均属于标准要求的 MySQL 8 系列，Flyway 已同时在 H2 `MODE=MySQL` 和真实 MySQL 8 验证。

## 7. Docker、Kubernetes 与 CI/CD

### 7.1 Docker Compose

关键文件：

- `deploy/docker-compose.microservices.yml`；
- `deploy/microservices/mysql/Dockerfile`；
- `deploy/microservices/{api-gateway,identity-asset-service,merchant-catalog-service,trade-fulfillment-service,engagement-platform-service}/Dockerfile`；
- `deploy/microservices/acceptance-smoke.sh`。

五个 Java 应用镜像均采用 Java 17 多阶段构建、非 root 用户（UID 10001）运行，并有独立健康检查。MySQL 派生镜像只负责携带四库初始化脚本，不包含真实密码。

### 7.2 Kubernetes

`k8s/microservices/` 当前可渲染为：

| Kind | 数量 |
| --- | ---: |
| Namespace | 1 |
| ConfigMap | 2 |
| Deployment | 5 |
| Service | 6 |
| StatefulSet | 1 |
| PersistentVolumeClaim | 1 |
| HorizontalPodAutoscaler | 1 |
| Ingress | 1 |

五个应用 Deployment 均有 startup/readiness/liveness 探针、资源 requests/limits 和独立镜像。D 文件存储使用 PVC。Secret 文件只提供占位示例，不进入 kustomization，也不包含真实凭据。

### 7.3 CI/CD

`.github/workflows/microservices-ci.yml` 的门禁顺序为：

1. 五服务 Maven 测试矩阵；
2. MySQL 8.4 四库、四账号初始化；
3. 启动 Gateway 与 A/B/C/D；
4. 执行真实跨服务 acceptance smoke 和数据库边界检查；
5. 构建 Admin、Merchant、Flutter Web 生产产物，经 18080 Gateway 执行现有 15 条 UC E2E；
6. 渲染 Compose/K8s；
7. 前序全部通过后才并行构建五个 Docker 镜像。

`.github/workflows/microservices-deploy.yml` 只在上述 CI 成功后触发，使用触发 CI 的精确提交 SHA 构建并推送五个 GHCR 镜像，然后更新 K8s Deployment、等待 rollout、检查 Gateway health；失败时上传诊断资料。真实部署仍需仓库配置 `KUBE_CONFIG`、GHCR 权限和目标集群 Secret。

## 8. 本机真实验收证据

### 8.1 自动化测试

最终完整 Maven reactor 测试覆盖 Gateway、四个微服务、公共契约和单体回归基线。微服务侧包含单元、Controller/API、内部契约、Flyway、边界与补偿测试；A/B/C/D 各自还有公开接口 inventory 测试，防止 227 操作的文档与 Controller 漂移。

| 模块 | 测试数 | 失败 | 错误 | 跳过 |
| --- | ---: | ---: | ---: | ---: |
| Gateway | 4 | 0 | 0 | 0 |
| A：账号与用户资产 | 29 | 0 | 0 | 1 |
| B：商家与商品 | 21 | 0 | 0 | 0 |
| C：交易与履约 | 36 | 0 | 0 | 1 |
| D：互动与平台 | 22 | 0 | 0 | 0 |
| 微服务侧合计 | 112 | 0 | 0 | 2 |
| 旧单体回归基线 | 96 | 0 | 0 | 1 |

MySQL smoke 测试在没有显式 MySQL 环境变量时会跳过，这是有意设计；真实 MySQL 迁移和端到端链路已经由 Compose 实跑，并在 CI 中作为必过门禁，不依赖这些可选测试替代。

### 8.2 Compose 启动

本机实际启动并健康的组件为：

- MySQL；
- Gateway：`localhost:18080`；
- A：`localhost:18081`；
- B：`localhost:18082`；
- C：`localhost:18083`；
- D：`localhost:18084`。

五个 `/actuator/health` 均返回 `UP`。Gateway 外部访问 `/internal/**` 返回 JSON 404；普通请求响应中只有一个 `X-Request-Id`。

### 8.3 跨服务业务链路

`deploy/microservices/acceptance-smoke.sh` 在真实容器和真实 MySQL 上完成以下步骤：

1. 用户、商家、管理员三种账号从 Gateway 登录；
2. 调用 B 的发现/商品数据；
3. C 创建订单，并用相同幂等键重放；
4. 支付、商家接单、备餐、出餐、两次配送推进、确认完成；
5. D 校验 C 的评价资格并发布评价；
6. C 幂等标记已评价；
7. A 增加评价成长值并产生站内消息；
8. A 和 B 分别上传文件到 D，再从 Gateway 下载并核对 SHA-256 完全一致。

最终自动 smoke 结果：

```json
{"status":"PASS","gatewayInternalBlocked":true,"requestIdCount":1,"orderId":9002,"reviewId":1,"fileForwardingVerified":true}
```

另一次人工订单链路验证了订单 9002 最终为已支付、已使用、已完成、已评价；B 的 SKU 库存从 498 降至 496，并产生库存幂等记录；D 保存评价；A 保存成长值 `+5` 和站内消息。停止 B 服务期间 A/C/D health 仍保持 `UP`，恢复 B 后其健康检查重新通过，证明单服务故障不会导致其余进程一起崩溃。

### 8.4 第二轮独立复核与修复

在首次合拢后又从空库进行了第二轮独立复核，实际发现并修复了以下问题：

- D 和旧单体客服/投诉代码使用 `Long == Long` 比较用户 ID，ID 超出 JVM 缓存范围时会误判为无权限；改为 `Objects.equals`，并新增 5001 用户 ID 的回归测试；
- C 的退款流程未同步取消到店预约；补齐 Repository 更新和用户/商家退款两条调用，并先用集成测试复现 `pending`，修复后断言为 `cancelled`；
- Flutter Web E2E 直接 `fill()` 不会同步 `TextEditingController`，坐标点击也可能不触发提交；改为显式聚焦、逐字键盘事件、语义按钮点击并等待登录接口响应；
- UC08、UC11 的测试数据仍引用单体旧 ID；按四库 seed 改为券 8001、订单 9001；
- 两个 Vue 端的 Vite/PostCSS/Nanoid 存在高危公告；Vite 升至 7.3.6并更新锁文件后，两个项目 `npm audit --omit=dev` 均为 0 漏洞；
- Flutter 桌面预览的 Emoji 触发 Noto 缺字警告；改用 Material 图标并增加图标回归断言，重新构建后的三端浏览器控制台均无 warning/error。

最终从新建 MySQL 卷冷启动后，四库 Flyway 记录分别为 identity 9/9、merchant 3/3、trade 3/3、platform 3/3，失败数均为 0；跨 schema 外键为 0，identity 账号读取 merchant schema 被拒绝。随后 acceptance smoke 通过；最后一次重建前端产物后，仓库现有 15 条 E2E 在 20.1 秒内全部通过。容器最近日志未发现真实 `ERROR`、`FATAL`、Exception、OOM 或 Access denied（预期的跨库拒绝测试除外）。

## 9. 对统一标准的逐项结论

| 统一标准完成定义 | 结论 | 证据 |
| --- | --- | --- |
| 四个独立业务微服务 | 通过 | 四个 Maven 模块、启动类和独立职责 |
| 单独构建、测试和启动 | 通过 | Maven 测试、五个独立 Dockerfile、六容器健康 |
| 四个独立逻辑库 | 通过 | 四 schema、四账号、跨库权限拒绝 |
| 业务表按归属迁移 | 通过 | 65 张实际表、Flyway 全成功、无跨库 FK/SQL |
| Gateway 按资源路由 | 通过 | 227 个操作唯一匹配 |
| `/internal/**` 外部不可访问 | 通过 | Gateway 与 Nginx 双层封锁 |
| 核心跨服务链路走内部接口 | 通过 | 真实订单到评价、成长值、消息、库存、文件链路 |
| 独立 Docker/K8s | 通过 | 五 Dockerfile、五 Deployment、六 Service |
| CI 构建、测试、镜像、部署、健康检查 | 通过（定义完成） | 工作流语法和本机等价步骤通过；真实运行待推送后由 GitHub 执行 |
| 仓库现有 UC01–UC13 Gateway 回归 | 通过 | 15/15 Playwright 场景使用三端生产产物经 18080 Gateway 通过 |
| 课程若另要求 UC14–UC23 | 待补用例 | 当前仓库没有对应 spec，不能伪造为已执行 |

## 10. 启动与复验

在仓库根目录执行：

```bash
docker compose -f deploy/docker-compose.microservices.yml up -d --build
docker compose -f deploy/docker-compose.microservices.yml ps
deploy/microservices/acceptance-smoke.sh
```

查看失败日志：

```bash
docker compose -f deploy/docker-compose.microservices.yml logs --tail=200 api-gateway
docker compose -f deploy/docker-compose.microservices.yml logs --tail=200 identity-asset-service
docker compose -f deploy/docker-compose.microservices.yml logs --tail=200 merchant-catalog-service
docker compose -f deploy/docker-compose.microservices.yml logs --tail=200 trade-fulfillment-service
docker compose -f deploy/docker-compose.microservices.yml logs --tail=200 engagement-platform-service
```

运行代码测试和部署清单校验：

```bash
mvn -B -f services/pom.xml clean test
docker compose -f deploy/docker-compose.microservices.yml config --quiet
kubectl kustomize k8s/microservices
bash -n deploy/microservices/acceptance-smoke.sh
git diff --check
```

## 11. 合拢后仍需执行的课程验收工作

这些工作应基于本集成版本继续做，不能回到任一 A/B/C/D 分支单独取结果：

1. 若课程明确要求 UC14–UC23，先按课程定义补齐仓库中不存在的场景，再统一经 Gateway 实跑并生成接口—用例—结果追溯表；
2. 在真实 Kubernetes 集群配置 Secret，执行部署工作流并保存 Pod、Service、Ingress、rollout 和 health 原始输出；
3. 对 HPA 做加压扩容/降压缩容实验，保存时间线和吞吐、平均/P95、错误率；
4. 进行下游超时/停机故障实验，记录用户可见错误、隔离范围、恢复时间和日志 requestId；
5. 单体与微服务在同一机器、同一批数据、同一压力脚本下，对 2–3 个主接口分别运行至少 3 次，保存原始结果和机器配置；
6. 根据实测结果解释差异，不预设微服务必须更快。

性能、HPA 和故障实验在 `后五天微服务拆分实施方案.md` 中本就定义为后续独立阶段。本次已提供 HPA 清单和服务隔离基础，但没有把未执行的实验伪装为完成。

## 12. 版本控制状态

本报告生成时尚未执行 commit 或 push。原因是仓库协作规范要求版本控制写操作必须在执行前获得明确确认。确认后应将当前集成工作树作为一个可审查的合拢提交推送到新的远端集成分支，不应覆盖 A/B/C/D 原分支。
