# 跨服务消费者驱动契约测试

契约基于已确定的服务边界以及远端账号、商家服务实现分支中的 `/internal/**` 接口，当前覆盖：

1. 交易履约服务读取账号服务的地址快照。
2. 交易履约服务读取商家服务的门店快照。
3. 交易履约服务向商家服务请求只读结算报价。

每份契约同时校验 HTTP 方法、路径、`X-Request-Id`、`X-Caller-Service`、`X-Service-Token`、请求 JSON 和响应关键字段。结算报价不会扣库存，可以反复执行。

## 离线消费者验证

```powershell
node tests/contracts/contract-test.mjs
```

脚本会启动仅监听 `127.0.0.1` 随机端口的临时 HTTP Provider，严格检查消费者请求，然后返回契约示例响应。这一步不需要启动微服务，适合每次提交和 CI 运行。

## 在线 Provider 验证

服务实现合并并启动后：

```powershell
$env:CONTRACT_SERVICE_TOKEN = '本地内部服务令牌'
node tests/contracts/contract-test.mjs --live `
  --provider identity-asset-service=http://127.0.0.1:18081 `
  --provider merchant-catalog-service=http://127.0.0.1:18082
```

在线模式会向真实 Provider 发送相同的只读请求并校验响应。报告默认写入 `tests/contracts/results/`，不会写入服务令牌。

当前 `origin/microservices-main` 只有空服务骨架，账号、商家、交易代码仍位于不同功能分支，互动服务尚未落地。因此本轮可以完成并运行离线消费者契约；在服务代码合并前，不能把离线结果表述成“真实四服务联调通过”。
