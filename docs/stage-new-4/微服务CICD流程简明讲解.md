# 微服务 CI/CD 流程简明讲解

## 1. 一句话说明

微服务流水线分为两个 GitHub Actions：`microservices-ci.yml` 负责“代码能否合并”，`microservices-deploy.yml` 负责“同一份已通过 CI 的代码能否制品化并部署”。PR 自动跑 CI；`main` 的 CI 成功后，CD 才会自动触发。

## 2. 完整流程

`PR/main push → CI 并行测试 → 四库五服务三端 E2E → Compose/K8s 清单校验 → 7 个镜像试构建 → main 同 SHA 触发 CD → 再次质量门禁 → 推送 7 个 SHA 镜像 → 部署 k3s → 线上验收`

CI 主要做以下检查：

1. 静态回归与旧单体兼容测试，防止拆分时丢失原功能。
2. `microservice-unit` 调用独立单元测试脚本，`microservice-integration-api` 调用独立集成/API 测试脚本；原 `service-test` 仍对 Gateway、A/B/C/D 分别执行 Maven `clean verify`，作为重复的全量兜底。商家端、管理端执行 Vitest 和覆盖率；用户端执行 `flutter analyze`、Flutter 测试和覆盖率。
3. 执行离线消费者契约测试；随后启动 MySQL 8、四个独立 schema、Gateway 和 A/B/C/D，再执行真实提供者契约、跨服务 smoke、全部 12 项跨库拒绝检查。
4. 构建三端页面，通过 Gateway 运行 UC01—UC13 的 13 个 Playwright 文件、15 条 E2E 用例。
5. 渲染并校验 Compose/Kubernetes 清单，试构建 Gateway、A/B/C/D、MySQL、Web 共 7 个镜像。任一步失败，CI 失败，不能进入 CD。

CD 只接受“当前精确提交 SHA 已有成功完整 CI”的版本。它会复跑静态检查、全部 Java 测试和三端测试/构建，然后向 GHCR 推送 7 个带 `sha-<commit>` 的不可变镜像。生产目标为 k3s：先更新并等待 MySQL，再部署六个 Deployment，等待滚动发布，最后检查 5 个 HPA、5 个服务健康与版本、7 个实际镜像、三端页面、三角色登录和 APK 下载，并上传部署证据。Compose 是可选部署目标，不会与 k3s 同时执行。

## 3. 测试按课程三分类放在哪里

| 分类 | 代码位置 | 流水线位置 | 说明 |
| --- | --- | --- | --- |
| 单元测试 | `scripts/verify/test_microservices_unit.sh` | CI `microservice-unit`；CD `quality-gate` 复跑 | 不启动完整系统，验证 Gateway 与 A/B/C/D 的类、方法、规则和异常分支 |
| 集成/API 测试 | `scripts/verify/test_microservices_integration_api.sh` | CI `microservice-integration-api`；CD `quality-gate` 复跑 | 验证 Controller、API、契约、迁移和服务边界；真实四库集成另在 `mysql-four-schema-smoke` 完成 |
| 端到端测试 | `scripts/verify/test_microservices_e2e.sh`、`tests/e2e/specs/uc01...uc13` | CI `mysql-four-schema-smoke` 的 `Run gateway-backed E2E scenarios` | 启动五服务和三端，从 Gateway 走 13 个 UC、15 条完整场景；CD 必须校验同 SHA 的该 CI 已成功 |
| 部署后 smoke | CD `Verify all internal services and public three-end site` | CD `deploy-kubernetes` | 验证部署可用，不属于上面三类业务测试，也不能代替 E2E |

要点：CD 会明确复跑单元和集成/API 两个分类脚本，但不会重复启动另一套四库环境跑 Playwright；它用“同 SHA 的完整 CI（包含 E2E）必须成功”作为硬门禁，再执行全量测试和部署后 smoke，因此 E2E 仍是自动发布链路中的必经步骤。

更详细的后端测试类、当前缺口和课程逐项判断见 `docs/stage-new-4/微服务测试三分类与课程要求对照.md`。分类 job 失败会直接阻断四库 E2E、镜像构建和 CD；原 `service-test` 的重复执行是额外兜底。

本地手动入口（使用项目统一的 Java 17）：`bash scripts/verify/test_microservices_unit.sh`、`bash scripts/verify/test_microservices_integration_api.sh`。两个脚本均先清理旧报告，避免分类统计互相混入。E2E 需要先启动 18080 Gateway、A/B/C/D 和 8090 三端静态站，再运行 `bash scripts/verify/test_microservices_e2e.sh`。

## 4. 答辩时怎么演示

1. 打开 Actions 中的 `aituan-microservices-ci`，依次指出 `microservice-unit`、`microservice-integration-api` 和 `mysql-four-schema-smoke` 中调用 E2E 脚本的步骤，再指出 `service-test` 是重复的五服务矩阵兜底，`docker-build` 是 7 镜像构建验证。
2. 打开同一提交触发的 `aituan-microservices-deploy`，指出 `quality-gate → publish-images → deploy-kubernetes` 的依赖关系。
3. 展示 CD 中的 `image_tag=sha-<commit>`、线上健康/版本检查和 deployment artifact，证明“测试、镜像、部署”对应同一个提交。

配置源：`.github/workflows/microservices-ci.yml`、`.github/workflows/microservices-deploy.yml`。
