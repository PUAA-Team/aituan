# AI 助手交付说明

## 范围

- 平台客服 AI：用户创建平台客服会话后，AI 先根据订单、优惠券、投诉/评价入口等上下文整理回复；用户点击“转人工”或发送“转人工”后切换为人工。
- 用户端爱团助手：个人中心新增“爱团助手”入口，支持订单、优惠券、投诉举报、评价和平台客服引导。
- 后端 skill：将订单查询、优惠券查询、投诉/评价治理入口做成独立 skill，由 agent 统一编排。

## 配置

配置从 `.config` 或环境变量读取，不提交真实密钥。

```properties
aituan.ai.enabled=false
aituan.ai.api-url=http://cliapi.2b.gs
aituan.ai.api-key=
aituan.ai.model=pp/gpt-5.5
aituan.ai.timeout-seconds=20
aituan.ai.max-tokens=800
aituan.ai.temperature=0.25
```

本地需要真实模型调用时，将 `aituan.ai.enabled` 改为 `true` 并填写 `aituan.ai.api-key`。

## 降级策略

- AI 未开启、密钥为空、接口不可达或模型返回为空时，自动使用本地关键词与 skill 查询结果回复。
- 降级回复仍保留转人工、投诉入口、订单/优惠券引导，不影响核心客服流程。

## 用户端入口

- 个人中心 -> 爱团助手。
- 助手回复卡片可跳转到订单、优惠券、投诉、评价、客服会话等既有页面。
- 输入框支持回车发送。

## 后端接口

- `POST /api/app/ai/assistant/message`
- 请求：

```json
{
  "content": "帮我看看最近订单",
  "conversationId": "可选"
}
```

- 响应包含 `reply`、`cards`、`quickActions`、`usedSkills`、`modelUsed`。
