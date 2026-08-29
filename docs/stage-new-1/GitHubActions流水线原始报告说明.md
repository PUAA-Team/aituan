# GitHub Actions 流水线原始报告说明

## 1. 背景

为便于课程验收、部署追溯和故障排查，GitHub Actions 流水线现在会在每次运行结束后生成“流水线原始报告”并上传为 artifact。

该报告不是替代 GitHub Actions 页面上的完整日志，而是把本次运行的关键上下文、job 状态、提交信息、触发事件 payload 和 GitHub Actions API 原始响应集中归档，方便下载留存。

## 2. 已覆盖的流水线

| Workflow | 报告 artifact 名称 | 触发场景 | 保留时间 |
| --- | --- | --- | --- |
| `aituan-ci` | `aituan-ci-pipeline-raw-report` | PR / 手动 CI | 30 天 |
| `aituan-deploy` | `aituan-deploy-pipeline-raw-report` | main push / 手动部署 | 30 天 |
| `aituan-android-apk` | `aituan-android-apk-pipeline-raw-report` | 手动 APK 打包 / 可选上传服务器 | 30 天 |

## 3. 报告内容

每个报告 artifact 中包含：

```text
pipeline-raw-report.md   # 面向人工阅读的流水线原始报告摘要
event-payload.json       # GitHub 触发事件原始 payload
run-detail.json          # GitHub Actions run 原始元数据，若 API 权限可用
run-jobs.json            # 本次 run 的 jobs 原始 JSON，若 API 权限可用
git-head.txt             # 当前 checkout 的提交 hash
git-status.txt           # 当前 checkout 工作区状态
*.err                    # GitHub API 拉取失败时的错误输出，便于排查权限问题
```

`pipeline-raw-report.md` 会记录：

- Workflow / Job / Run ID / Run Attempt。
- 触发事件、分支、提交 SHA、执行人、仓库、Runner OS。
- 部署目标、是否部署、镜像仓库、镜像标签、APK 名称等白名单字段。
- 本次 run 中各关键 job 的结果，例如 `success`、`failure`、`skipped`。

## 4. 实现位置

| 文件 | 说明 |
| --- | --- |
| `scripts/verify/write_pipeline_raw_report.sh` | 统一生成原始报告的脚本 |
| `.github/workflows/ci.yml` | 增加 `pipeline_raw_report` job |
| `.github/workflows/deploy.yml` | 增加 `pipeline_raw_report` job |
| `.github/workflows/android-apk.yml` | 增加 `pipeline_raw_report` job |

三个 workflow 都增加了 `actions: read` 权限，用于通过 GitHub CLI 读取当前 run 元数据和 jobs 列表。即使 API 读取失败，报告摘要、事件 payload 和 git 信息仍会上传，失败原因写入对应 `.err` 文件。

## 5. 查看与下载方式

1. 打开 GitHub 仓库。
2. 进入 `Actions`。
3. 选择对应 workflow，例如 `aituan-deploy`。
4. 打开一次具体 run。
5. 在页面底部 `Artifacts` 区域下载：
   - `aituan-deploy-pipeline-raw-report`
   - `aituan-ci-pipeline-raw-report`
   - `aituan-android-apk-pipeline-raw-report`

## 6. 注意事项

- 报告不主动导出 Secrets，也不读取服务器 `.env`、kubeconfig、SSH 私钥等敏感内容。
- 完整逐行日志仍以 GitHub Actions 页面原生日志为准。
- 部署失败时，报告 job 使用 `if: always()`，会尽量继续运行并上传报告，方便定位失败阶段。
- K8s / Compose 部署原有诊断日志仍保留；本报告用于补充 run 级别原始上下文。