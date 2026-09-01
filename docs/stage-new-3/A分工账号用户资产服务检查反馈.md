# A 分工账号与用户资产服务检查反馈

> 对应分支：`ms/identity-asset`
> 对应服务：`identity-asset-service`
> 对应数据库：`aituan_identity`
> 检查目的：判断 A 服务是否完成微服务拆分、是否符合统一标准、是否可以和其他服务尤其 C 服务对接。

## 1. 总结论

A 分支已经完成了 `identity-asset-service` 的主体拆分，完成度较高，具备继续集成联调的基础。

但是，当前还不能直接判定为“完全按统一标准完成、可以无审查直接合入、可以和 C 服务无缝对接”。主要原因是：Gateway 路由存在基路径漏转发风险，测试覆盖不足，部署与 Secret 准备不完整，并且分支里包含了部分非 A 职责范围的 Docker/K8s 文件，后续合并时容易和 B/C/D 分支冲突。

建议 A 同学先按本文的 P0/P1 项修正，再进入全员集成分支。

## 2. 已完成内容

### 2.1 分支基线基本正确

检查结果显示 A 分支包含 `origin/microservices-main` 的微服务地基：

```text
A_CONTAINS_MICROSERVICES_MAIN=true
```

这说明 A 分支至少是基于统一微服务工程地基继续开发的，比没有对齐地基的分支更适合后续合入。

### 2.2 服务模块基本完整

A 分支已经实现了独立的 `identity-asset-service`，包括：

- 独立 Maven 模块；
- 服务名：`identity-asset-service`；
- 服务端口：`8081`；
- 独立数据库口径：`aituan_identity`；
- 独立 Flyway migration 和 seed；
- 复用 `common-contract`；
- Actuator 健康检查；
- Dockerfile；
- K8s YAML；
- CI/CD workflow；
- A 分工交付说明文档。

### 2.3 业务范围覆盖较完整

A 服务覆盖的职责包括：

- 认证；
- 用户资料；
- 地址；
- 收藏；
- 会员；
- 优惠券；
- 站内消息；
- 商家账号 provision/deactivate 内部能力。

这些职责基本符合统一分工文档中 A 服务的边界。

### 2.4 内部接口基本覆盖统一标准

A 分支已提供统一标准要求的内部接口，包括：

```text
GET  /internal/users/{userId}/summary
GET  /internal/users/{userId}/addresses/{addressId}/snapshot
GET  /internal/users/{userId}/home-summary
GET  /internal/users/{userId}/preference-signals
POST /internal/coupons/quote
POST /internal/coupons/{couponId}/use
POST /internal/coupons/{couponId}/release
POST /internal/members/{userId}/growth
POST /internal/messages
POST /internal/merchant-accounts/provision
POST /internal/merchant-accounts/{accountId}/deactivate
GET  /internal/metrics/platform/users
```

这些接口是 C 服务交易下单链路后续需要调用的基础，例如地址快照、优惠券报价、优惠券使用/释放、会员成长值和站内消息。

## 3. 需要修正的问题

### P0-1. Gateway 路由存在基路径漏转发风险

当前 Gateway 对部分后台接口只配置了 `/**` 子路径，例如：

```text
/api/admin/users/**
/api/admin/operation/member-levels/**
/api/admin/operation/coupon-templates/**
```

这样可能无法覆盖无尾路径接口，例如：

```text
/api/admin/users
/api/admin/operation/member-levels
/api/admin/operation/coupon-templates
```

如果 Controller 中有列表、新增等基路径接口，Gateway 可能不会转发到 A 服务。

建议改为同时覆盖基路径和子路径：

```text
/api/admin/users,/api/admin/users/**
/api/admin/operation/member-levels,/api/admin/operation/member-levels/**
/api/admin/operation/coupon-templates,/api/admin/operation/coupon-templates/**
```

这是对接前建议优先修复的问题。

### P0-2. 分支包含非 A 职责范围的 Docker/K8s 文件

A 分支中除了 `identity-asset-service` 自己的 Docker/K8s 文件外，还包含 B/C/D 服务相关文件，例如：

```text
deploy/microservices/merchant-catalog-service/Dockerfile
deploy/microservices/trade-fulfillment-service/Dockerfile
deploy/microservices/engagement-platform-service/Dockerfile
k8s/11-merchant-catalog-service.yaml
k8s/12-trade-fulfillment-service.yaml
k8s/13-engagement-platform-service.yaml
```

这会导致后续和 B/C/D 分支合并时产生职责越界和冲突风险。

建议：

- A 分支只保留 `identity-asset-service` 相关 Docker/K8s；
- B/C/D 的 Docker/K8s 由对应成员分支提供；
- 最终全量 K8s 文件在集成分支统一合并。

### P1-1. CI/CD 范围过大，不利于判断 A 服务本身是否通过

A 分支的 `microservices-ci.yml` 和 `microservices-deploy.yml` 做成了四服务矩阵：

```text
identity-asset-service
merchant-catalog-service
trade-fulfillment-service
engagement-platform-service
```

这更像最终集成分支的流水线，不适合作为 A 单人分支自己的验证。因为 B/C/D 服务在 A 分支里不一定是最新版，其他服务失败会影响 A 服务判断。

建议 A 分支先保留或新增独立的 A 服务验证流程，只验证：

```text
common-contract
identity-asset-service
api-gateway identity route test
identity Flyway H2/MySQL smoke
identity Docker build
identity K8s dry-run
```

四服务矩阵建议放到最终集成分支。

### P1-2. Deploy workflow 有掩盖失败的问题

A 的部署 workflow 中存在类似：

```bash
kubectl apply -f k8s/11-merchant-catalog-service.yaml || true
kubectl apply -f k8s/12-trade-fulfillment-service.yaml || true
kubectl apply -f k8s/13-engagement-platform-service.yaml || true
```

`|| true` 会吞掉失败，可能导致流水线显示成功，但实际某些服务没有部署成功。

建议：

- A 分支不要部署其他服务；
- 集成分支部署失败不要用 `|| true` 掩盖；
- 失败时上传原始日志和 K8s 诊断信息。

### P1-3. K8s Secret 和数据库准备说明不足

A 服务真实部署需要准备：

```text
aituan_identity 数据库
identity 服务数据库账号
JWT Secret
内部服务调用 token
K8s Secret
```

这些不能提交真实值，但应提供示例 Secret 或部署说明，说明 key 名称和创建方式。否则后续 `kubectl apply` 后 Pod 可能因为 Secret 缺失启动失败。

### P1-4. 测试覆盖不足

A 分支已有一些内部接口幂等测试，但还不足以覆盖验收要求。

建议补充：

- 公开认证接口测试；
- 用户资料接口测试；
- 地址接口测试；
- 收藏接口测试；
- 优惠券领取/查询/使用边界测试；
- 会员等级接口测试；
- 站内消息接口测试；
- Gateway identity route test；
- H2 `MODE=MySQL` 空库迁移测试；
- MySQL 8 空库迁移 smoke test。

### P2-1. 文档格式存在 trailing whitespace

`git diff --check` 检查发现 A 文档中有行尾空格：

```text
docs/stage-new-3/分工A-账号与用户资产服务拆分交付说明.md:3: trailing whitespace.
docs/stage-new-3/分工A-账号与用户资产服务拆分交付说明.md:4: trailing whitespace.
docs/stage-new-3/分工A-账号与用户资产服务拆分交付说明.md:5: trailing whitespace.
```

建议合入前清理。

### P2-2. 头像 public URL 链路需要确认

A 服务包含用户头像相关接口，后续需要和文件服务或静态资源域名对齐：

- 上传后的文件归属哪个服务；
- public URL 如何生成；
- Gateway 是否转发 `/api/common/files/**`；
- 是否依赖 D 服务或旧单体文件能力。

这不是 A 服务主体拆分的阻塞项，但全链路联调前需要确认。

## 4. 和 C 服务对接需要关注的接口

C 服务交易下单链路后续会依赖 A 服务这些能力：

```text
GET  /internal/users/{userId}/summary
GET  /internal/users/{userId}/addresses/{addressId}/snapshot
POST /internal/coupons/quote
POST /internal/coupons/{couponId}/use
POST /internal/coupons/{couponId}/release
POST /internal/members/{userId}/growth
POST /internal/messages
```

对接前请重点确认：

1. 请求字段和响应字段是否和 C 服务 client DTO 一致；
2. 金额类型统一使用数字，不要字符串和数字混用；
3. 优惠券使用接口必须支持幂等；
4. 优惠券释放接口必须支持订单失败回滚；
5. 会员成长值接口重复调用不能重复加成长值；
6. 站内消息接口失败不能影响订单主流程；
7. 内部接口错误码统一使用 `ApiResponse` 格式。

## 5. 建议修复顺序

建议 A 同学按以下顺序处理：

1. 修 Gateway 基路径漏转发问题；
2. 清理非 A 职责范围的 B/C/D Docker/K8s 文件，或和全员确认这些文件放到集成分支统一维护；
3. 调整 A 分支 CI，只保留 A 服务和 identity 路由相关验证；
4. 补 K8s Secret 示例和数据库准备说明；
5. 补公开接口测试、Gateway route test、Flyway H2/MySQL smoke；
6. 清理文档 trailing whitespace；

## 6. 当前判断

A 服务主体拆分可以认为“基本完成”，但还不能直接认为“完全符合统一标准”。

合入建议：

```text
可以继续作为 A 服务分支修正，不建议直接无审查合入 microservices-main。
```
