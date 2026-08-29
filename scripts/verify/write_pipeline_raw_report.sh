#!/usr/bin/env bash
set -euo pipefail

report_dir="${1:-.aituan-report/pipeline-raw-report}"
mkdir -p "${report_dir}"

report_file="${report_dir}/pipeline-raw-report.md"

value_or_unset() {
  local value="${1:-}"
  if [[ -z "${value}" ]]; then
    printf '未设置'
  else
    printf '%s' "${value}"
  fi
}

{
  echo "# ${AITUAN_REPORT_TITLE:-GitHub Actions 流水线原始报告}"
  echo
  echo "## 运行信息"
  echo
  echo "| 字段 | 值 |"
  echo "| --- | --- |"
  echo "| Workflow | $(value_or_unset "${GITHUB_WORKFLOW:-}") |"
  echo "| Job | $(value_or_unset "${GITHUB_JOB:-}") |"
  echo "| Job 状态 | $(value_or_unset "${AITUAN_JOB_STATUS:-}") |"
  echo "| Run ID | $(value_or_unset "${GITHUB_RUN_ID:-}") |"
  echo "| Run Attempt | $(value_or_unset "${GITHUB_RUN_ATTEMPT:-}") |"
  echo "| Event | $(value_or_unset "${GITHUB_EVENT_NAME:-}") |"
  echo "| Ref | $(value_or_unset "${GITHUB_REF:-}") |"
  echo "| SHA | $(value_or_unset "${GITHUB_SHA:-}") |"
  echo "| Actor | $(value_or_unset "${GITHUB_ACTOR:-}") |"
  echo "| Repository | $(value_or_unset "${GITHUB_REPOSITORY:-}") |"
  echo "| Runner OS | $(value_or_unset "${RUNNER_OS:-}") |"
  echo "| 生成时间 UTC | $(date -u +'%Y-%m-%dT%H:%M:%SZ') |"
  echo
  echo "## 部署 / 构建关键信息"
  echo
  echo "| 字段 | 值 |"
  echo "| --- | --- |"
  echo "| 部署目标 | $(value_or_unset "${AITUAN_DEPLOY_TARGET:-}") |"
  echo "| 是否部署 | $(value_or_unset "${AITUAN_SHOULD_DEPLOY:-}") |"
  echo "| 镜像仓库 | $(value_or_unset "${AITUAN_IMAGE_REGISTRY:-}") |"
  echo "| 镜像标签 | $(value_or_unset "${AITUAN_IMAGE_TAG:-}") |"
  echo "| APK 名称 | $(value_or_unset "${AITUAN_APK_NAME:-}") |"
  echo "| API Origin | $(value_or_unset "${AITUAN_API_ORIGIN:-}") |"
  echo
  echo "## 随报告上传的原始文件"
  echo
  echo "- \`event-payload.json\`：GitHub 触发事件原始 payload。"
  echo "- \`run-jobs.json\`：通过 GitHub Actions API 获取的本次 run jobs 原始 JSON（若 token 权限可用）。"
  echo "- \`run-detail.json\`：通过 GitHub Actions API 获取的本次 run 元数据原始 JSON（若 token 权限可用）。"
  echo "- \`git-head.txt\` / \`git-status.txt\`：当前 checkout 的提交与工作区状态。"
  echo
  echo "> 注意：报告只记录 GitHub 运行上下文和白名单字段，不主动导出 Secrets。完整逐行日志仍以 GitHub Actions 页面原生日志为准。"
} > "${report_file}"

if [[ -n "${GITHUB_EVENT_PATH:-}" && -f "${GITHUB_EVENT_PATH}" ]]; then
  cp "${GITHUB_EVENT_PATH}" "${report_dir}/event-payload.json"
fi

if command -v git >/dev/null 2>&1; then
  git rev-parse HEAD > "${report_dir}/git-head.txt" 2>&1 || true
  git status --short > "${report_dir}/git-status.txt" 2>&1 || true
fi

if [[ -n "${GH_TOKEN:-}" && -n "${GITHUB_REPOSITORY:-}" && -n "${GITHUB_RUN_ID:-}" ]] && command -v gh >/dev/null 2>&1; then
  gh api "/repos/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}" \
    > "${report_dir}/run-detail.json" 2> "${report_dir}/run-detail.err" || true
  gh api "/repos/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}/jobs?per_page=100" \
    > "${report_dir}/run-jobs.json" 2> "${report_dir}/run-jobs.err" || true
fi
