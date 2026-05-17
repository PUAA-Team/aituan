# 开发环境

- Windows / PowerShell
- 已经安装Andriod Studio、Andriod SDK、Flutter、Java、Python

# 工作流程

全程中文对话。

1. **先查后动**：改代码前先找相关文档，不确定就调查清楚或直接问用户
2. **先说再做**：编码前描述方案等批准，需求不明先澄清
3. **拆大为小**：改动超 3 个文件的任务先分解
4. **参考先例**：实现功能优先去 GitHub 搜参考代码
5. **写完必跑**：代码完成后验证通过再交付
6. **收尾检查**：完成后列出潜在问题；发现 bug 先写复现测试再修

## 确认机制

- **重大变更须确认**：文件结构增删、新功能、新页面UI、核心算法、新依赖、API 定义——先提方案问”您同意吗？”，批准后再动
- **局部优化可自主**：函数内部重构、命名优化等不影响外部调用的可以直接做，报告里说明即可

## 遇到问题必须停

命令报错、测试不过、发现逻辑漏洞——立即停止，报告：遇到了什么、原计划是什么、建议怎么办。

# 编码规范

如无必要，勿增实体（指不必要的复杂度和重复代码；可复用的公共组件应积极创建）。

遵循 KISS、YAGNI、DRY、SOLID。编码风格与代码库保持一致，优先复用已有函数。

# 文档规范

所有文档放在`docs`文件夹下面，设计总体设计的部分放在该文件夹下。后续每一个步骤开发时，应该新建一个`docs/stage[x]`文件夹，并交付本阶段说明与部署说明markdown文档，文档均需保持索引放在docs/ReadMe.md。

# 项目背景

- 开工前必读：`docs/ReadMe.md`，里面包含项目介绍和已有文档索引
- 新增前端/后端/功能设计需要写到文档里，需要通过文件夹分类文档，并添加到索引中
- 遇到问题多查文档库，解决后也往文档库里记录
- 任何接下来需要用户自己做的事情都要写好完整文档，包括编译、后端部署等，尽可能完成你可以完成的所有功能，比如打包后APP，APP打包最好在D盘完成，减少C盘占用。
- 当前 C 盘空间特别少；后端本地测试、打包、运行、Gradle 缓存、临时日志等不要放在 C 盘，优先同步到 D 盘英文路径下的项目子目录（例如 `D:/aituan_build/`、`D:/aituan_cache/`、`D:/aituan_runtime/`、`D:/aituan_release/`），不要直接散落在 D 盘根目录。
- 本机没有 Docker Compose 环境；后端日常本地测试不要依赖 Docker Compose，应在 D 盘手动构建实时测试。Docker Compose 只作为后续部署方式提供。
- 用户明确说“之后再做”的功能不要只留在对话里，必须记录到 `docs/后续功能待办.md`，不要写散在某个 stage 文档里。
- 后端 Stage4 开发在 `stage4-dev` 分支进行；开发过程中要经常提交小步 commit，但未经用户明确确认，不能在 `main` 主分支上提交。
- 每次前端重构或影响用户端界面的改动完成后，都必须重新打包 APP，优先在 D 盘英文路径执行打包，并在交付时明确给出 APK 完整路径。
- 每次完成 APP 打包后，都要清理本次打包产生的冗余构建目录与临时文件，只保留最终需要交付的 APK 成品。
- 所有代码新的设计都要服从高内聚低耦合的设计理念，严禁形成单个代码文件上千行，严格遵守项目结构，提高代码的可扩展性与简洁性、可维护性。
- 所有代码的必要位置请使用中文注释，不要太多，只在必要位置使用。

# 高风险操作（须确认）

以下操作执行前必须告知用户并获得确认：

| 类别     | 示例                                  |
| -------- | ------------------------------------- |
| 文件系统 | 删除文件/目录、批量修改、覆盖系统文件 |
| 版本控制 | git commit / push / reset --hard      |
| 系统配置 | 环境变量、全局配置、权限变更          |
| 数据操作 | 删数据、改表结构、批量更新            |
| 网络请求 | 含敏感数据的请求、调用生产环境 API    |
| 包管理   | 全局安装/卸载、更新核心依赖           |

补充要求：
- 凡是安装开发环境、修改环境变量、修改系统路径前，必须先展示将要修改的具体内容，征得用户同意。
- 执行前必须做好备份，并明确告诉用户备份位置或回滚方式。
- 优先采用影响范围最小的方式，避免破坏用户已有命令与环境。

确认格式：
> ⚠️ 危险操作：[操作内容]，影响范围：[说明]，风险：[后果]。确认执行？

# Other Requirement

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
