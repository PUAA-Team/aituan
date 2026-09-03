# 视频 04：依赖故障隔离（前端主画面 + SSH 录制版）

> 目标成片：约 60 秒。主画面必须是用户端，终端只用来证明 B 服务真实下线和恢复。
> 服务器脚本已放在 `/tmp/aituan-fault-demo/record_catalog_fault_demo.sh`，登录服务器后可以直接运行。

## 1. 这段视频最终要证明什么

完整过程为：

```text
B 商品服务正常（1/1）
  → 用户购物车中有“藤椒鸡腿堡 ×2”
  → SSH 脚本把 B 缩到 0/0
  → 用户端仍显示 C 保存的购物车快照和黄色备用结果提示
  → 新增和结算受限，但移除仍成功
  → “评价”等不依赖 B 的业务仍可打开
  → B 恢复到 1/1
  → 点击重新检测，黄色提示消失，新增恢复
```

这不是前端伪造的提示。黄色提示、商品名称、数量和金额均来自 C 的真实响应字段：

```text
catalogAvailable=false
notice=商品服务暂不可用，已显示最近一次购物车快照……
items=藤椒鸡腿堡 ×2
amount=39.80
```

## 2. 已经完成的前端更新

用户端已经接入 C 的购物车查询、新增、改数量、移除和清空接口，并实现以下故障态：

- 页面顶部显示黄色“商品服务异常 · 已启用备用结果”；
- 黄色区域说明正在显示最近一次购物车快照，并说明其他业务仍可访问；
- 商品、数量和金额继续显示；
- 商品加号置灰，提交按钮显示“服务恢复后结算”；
- 购物车弹窗显示“移除商品”，故障期间仍可执行；
- 页面右上角有“刷新门店和购物车”按钮，故障注入后不必离开当前页面；
- B 恢复后点击“重新检测”，提示消失且新增、结算恢复。

本地验证结果：

- `flutter analyze`：0 问题；
- `flutter test`：68 项全部通过；
- 新增的购物车接口与故障界面测试：5 项全部通过；
- Flutter Web 与生产域名版 Android Debug APK 均构建成功；
- 服务器真实预演通过：B `1 → 0 → 1`，备用快照、写保护、移除、A/C/D 隔离和恢复断言全部为真。

注意：录制前必须先把包含本次前端更新的提交通过 CI/CD 部署到线上。若线上仍没有右上角刷新按钮，说明录制用的还是旧 Web 镜像，不能开始录制。

## 3. 录制前一次性准备

### 3.1 浏览器

用户端 Web 只在窄屏提供完整功能。二选一：

1. Chrome 开发者工具打开设备模式，设置 `430 × 850`；
2. 使用本次构建的 Android APK 在模拟器或手机上录制。

浏览器访问：

```text
https://aituan.2b.gs/web/
```

登录演示账号：

```text
账号：demo_user
密码：123456
```

不要展示真实手机号、地址、Token 或 kubeconfig。

### 3.2 画面布局

推荐 1920×1080 录屏：

- 左侧约 70%：430×850 的用户端页面；
- 右侧约 30%：SSH 终端，字号至少 22 px；
- 浏览器地址栏保留 `aituan.2b.gs`；
- 终端不要开启其他日志滚动窗口。

### 3.3 服务器只读预检

在录制电脑执行：

```bash
ssh aituan-new
cd /tmp/aituan-fault-demo

kubectl get nodes
kubectl -n aituan get deployment/merchant-catalog-service \
  deployment/trade-fulfillment-service hpa/merchant-catalog-service
curl -ksS https://aituan.2b.gs/actuator/health | jq .
```

开始前必须看到：

- Node 为 `Ready`；
- B `merchant-catalog-service` 为 `1/1`；
- C `trade-fulfillment-service` 为 `1/1`；
- B HPA 的 `REFERENCE` 为 `Deployment/merchant-catalog-service`；
- 公网健康为 `UP`。

服务器脚本校验：

```bash
cd /tmp/aituan-fault-demo
bash -n ./record_catalog_fault_demo.sh
sha256sum ./record_catalog_fault_demo.sh
```

当前已上传脚本只允许 root 执行，不需要再次复制。若本地脚本之后有改动，再重新上传：

```bash
scp scripts/experiments/record_catalog_fault_demo.sh \
  aituan-new:/tmp/aituan-fault-demo/record_catalog_fault_demo.sh
```

## 4. 正式录制只需要这一条启动命令

SSH 登录服务器后执行：

```bash
cd /tmp/aituan-fault-demo
./record_catalog_fault_demo.sh
```

脚本有三个阶段和三个暂停点，看到提示后切换浏览器操作，再回终端按回车。不要使用全自动的 `run_catalog_fault_experiment.sh` 录前端，因为它会在你切到浏览器前自动恢复 B。

## 5. 三个暂停点怎么操作

### 暂停点一：正常基线

终端会显示：

```text
[步骤 1/3] 建立正常购物车快照。
商品服务正常
塔斯汀中国汉堡
藤椒鸡腿堡
数量 2
金额 39.8
merchant-catalog-service 1/1
```

此时在用户端：

1. 登录 `demo_user`；
2. 进入外卖门店“塔斯汀中国汉堡”；
3. 点击底部购物车；
4. 确认显示“藤椒鸡腿堡 ×2”和金额 `￥39.80`；
5. 关闭购物车，但不要退出当前商家页；
6. 开始正式录屏，再回终端按回车。

### 暂停点二：B 已下线

终端会显示：

```text
[步骤 2/3] 将商品服务从 1 个副本缩到 0。
merchant-catalog-service 0/0
trade-fulfillment-service 1/1
catalogAvailable false
已返回备用快照
其他业务检查：账号 A code=0，交易 C code=0，互动 D code=0
```

此时在用户端按顺序做：

1. 保持在同一个商家页，点击右上角刷新按钮；
2. 拍到黄色“商品服务异常 · 已启用备用结果”；
3. 拍到“最近一次购物车快照”和“其他业务仍可正常访问”；
4. 商品加号应为置灰状态，底部按钮应显示“服务恢复后结算”；
5. 打开购物车，拍到原商品、数量、金额和禁用的“商品服务恢复后可结算”；
6. 点击“移除商品”，拍到“故障隔离生效：商品已成功移除”；
7. 点击页面“评价”标签，拍到评价正常加载，证明 D 未被带崩；
8. 返回“下单”标签，再回终端按回车。

重要：B 下线期间不要刷新整个浏览器，也不要返回首页。只点击商家页右上角的刷新按钮；否则首页本身依赖 B，画面会被无关错误打断。

### 暂停点三：B 已恢复

终端会等待 B rollout，并显示：

```text
[步骤 3/3] 恢复商品服务。
merchant-catalog-service 1/1
trade-fulfillment-service 1/1
catalogAvailable true
```

此时在用户端：

1. 点击黄色提示右侧“重新检测”；
2. 拍到黄色提示消失；
3. 拍到商品加号恢复为红色可点击状态；
4. 点击一次加号，确认购物车数量变为 1；
5. 停止录屏；
6. 回终端按最后一次回车，脚本会清空演示购物车。

终端最后必须显示：

```text
录制流程完成：B 与 HPA 已恢复，demo_user 的演示购物车已清空。
```

## 6. 60 秒成片剪辑表

| 时间 | 主画面 | 终端角落 | 配音 / 字幕 |
| ---: | --- | --- | --- |
| 0:00—0:08 | 正常购物车：商品 ×2、￥39.80 | B `1/1` | “商品服务正常时，交易服务保存购物车商品快照。” |
| 0:08—0:17 | 保持用户端商家页 | 按回车；B 变为 `0/0`，C 仍 `1/1` | “现在真实停止 B 商品服务。” |
| 0:17—0:31 | 点右上角刷新，黄色备用结果和原购物车仍在 | `catalogAvailable=false` | “C 没有崩溃，而是返回最近一次持久化快照和明确提示。” |
| 0:31—0:42 | 加号/结算置灰；打开购物车并移除商品 | A/C/D `code=0` | “依赖实时价格库存的操作被保护，但 C 自有的移除仍可完成，其他服务保持可用。” |
| 0:42—0:48 | 点击“评价”，内容正常加载 | 保留 B `0/0` | “不依赖 B 的互动业务不受影响。” |
| 0:48—0:55 | 回到下单页 | 按回车；B rollout 到 `1/1` | “恢复商品服务，C 不需要重启。” |
| 0:55—1:00 | 点“重新检测”，黄色提示消失，加号恢复 | `catalogAvailable=true` | “依赖恢复后，用户操作自动恢复正常。” |

等待 Pod 启动的部分可以 4—6 倍加速，但以下画面不能剪掉：触发回车、B `0/0`、黄色备用结果、移除成功、A/C/D 正常、B `1/1`、黄色提示消失。

## 7. 录完后的恢复检查

```bash
kubectl -n aituan get hpa/merchant-catalog-service \
  -o jsonpath='{.spec.scaleTargetRef.name}{"\n"}'
kubectl -n aituan get deployment/merchant-catalog-service \
  deployment/trade-fulfillment-service hpa/merchant-catalog-service
curl -ksS https://aituan.2b.gs/actuator/health | jq .
```

正确结果：HPA 目标为 `merchant-catalog-service`，B/C 均为 `1/1`，公网健康为 `UP`。B 刚恢复时 HPA CPU 可能短暂显示 `<unknown>`，等待约 30—60 秒重新采样即可。

如果 SSH 意外断开，脚本的退出钩子会尝试恢复；仍需重新连接并核对。发现 HPA 目标异常或 B 未恢复时执行：

```bash
kubectl -n aituan patch hpa/merchant-catalog-service \
  --type=merge \
  -p '{"spec":{"scaleTargetRef":{"name":"merchant-catalog-service"}}}'
kubectl -n aituan scale deployment/merchant-catalog-service --replicas=1
kubectl -n aituan rollout status deployment/merchant-catalog-service --timeout=300s
```

## 8. 常见问题

| 现象 | 原因与处理 |
| --- | --- |
| 页面没有右上角刷新按钮 | 线上仍是旧 Web 镜像；先部署本次前端更新。 |
| 故障后没有黄色提示 | 不要刷新整个浏览器；停留在商家页，点击右上角刷新按钮。 |
| 购物车不是“藤椒鸡腿堡 ×2” | 重新启动脚本，步骤 1 会先清空并建立固定基线。 |
| B 很快自己变回 1 | 没有用录制脚本，或 HPA 未临时解绑；停止录制并恢复后重来。 |
| 移除后恢复阶段购物车为空 | 这是预期结果；恢复后点击加号即可证明写操作恢复。 |
| HPA CPU 显示 `<unknown>` | B 新 Pod 刚启动尚未采样；副本和健康正常时等待 30—60 秒。 |
| 脚本退出 | 不能使用失败的一半拼接；先核对 B/HPA 恢复，再从步骤 1 完整重录。 |
