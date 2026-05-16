# Stage 2 HTML 原型查看说明

## 1. 原型文件位置

HTML 原型放在：

```text
docs/stage2/demo/
```

当前包含以下文件：

- `index.html`：原型入口页
- `splash_demo.html`：启动页原型
- `login_demo.html`：登录 / 注册 / 找回密码原型
- `home_demo.html`：首页原型
- `module_demo.html`：八个业务模块展示页原型
- `search_demo.html`：搜索页原型
- `search_result_demo.html`：搜索结果页原型
- `merchant_detail_demo.html`：外卖商家点单页原型，包含购物车上拉面板
- `service_merchant_demo.html`：非外卖商家页原型
- `item_detail_demo.html`：商品/服务详情页原型
- `checkout_demo.html`：确认订单页原型，包含支付方式选择
- `orders_demo.html`：订单列表页原型
- `order_detail_demo.html`：外卖订单详情页原型
- `service_order_detail_demo.html`：非外卖订单详情页原型，包含二维码和券码核销区
- `message_demo.html`：消息页原型
- `favorite_demo.html`：收藏页原型
- `profile_demo.html`：我的页原型
- `review_publish_demo.html`：发布评价页原型
- `demo.css`：原型共用样式

## 2. 查看方式

### 方式一：直接双击打开

直接打开以下文件即可：

```text
docs/stage2/demo/index.html
```

### 方式二：使用本地静态服务查看

如果您希望通过浏览器地址访问，可以在项目根目录执行：

```powershell
python -m http.server 8000
```

然后在浏览器打开：

```text
http://localhost:8000/docs/stage2/demo/index.html
```

## 3. 原型用途说明

这些 HTML 文件用于确认当前 App 页面结构、信息层级和主交互逻辑。它们不是 Flutter 代码，但页面之间已经通过链接串联，便于按真实使用路径查看：

1. 登录或游客进入
2. 首页浏览和八个模块入口
3. 模块展示页、搜索页与搜索结果页
4. 外卖商家点单或非外卖商家浏览
5. 商品/服务详情页
6. 外卖购物车上拉面板
7. 确认订单与模拟支付
8. 外卖订单详情、非外卖订单详情、订单列表、评价
9. 消息、收藏、我的页

## 4. 建议查看顺序

1. `splash_demo.html`
2. `login_demo.html`
3. `home_demo.html`
4. `module_demo.html`
5. `search_demo.html`
6. `search_result_demo.html`
7. `merchant_detail_demo.html`
8. `service_merchant_demo.html`
9. `item_detail_demo.html`
10. `checkout_demo.html`
11. `orders_demo.html`
12. `order_detail_demo.html`
13. `service_order_detail_demo.html`
14. `review_publish_demo.html`
15. `message_demo.html`
16. `favorite_demo.html`
17. `profile_demo.html`

## 5. 当前确认口径

- 底部导航为：首页 / 消息 / 订单 / 我的。
- 我的页保留订单、消息和收藏入口。
- 首页八个模块入口进入模块展示页。
- 搜索页和搜索结果页分离；搜索结果页按“店铺卡 + 命中商品/服务小卡”展示。
- 搜索结果顶部分类使用系统八个模块；筛选栏只保留地点、排序方式、筛选。
- 外卖和其他七个模块页面与业务逻辑分开。
- 外卖商家页是点单页，点击外卖商家或外卖商品都进入该页。
- 非外卖商家页展示更完整商家信息，商品/服务点击进入商品详情页。
- 外卖详情页使用购物车上拉面板，但提交后仍进入确认订单页。
- 当前只启用模拟支付，后续可扩展真实支付方式。
- 订单列表聚合状态为：未支付 / 待完成 / 未使用 / 已使用。
- 非外卖订单支付后进入未使用状态，订单详情展示二维码和券码号。
- 主题配色采用肯德基红方案：主色 `#E4002B`，深红 `#B80022`，浅红底 `#FFF0F2`，不使用渐变。
- 首页八个入口展示“洗脚”，业务承接洗脚按摩相关服务。
- 个人页暂不展示最近订单栏目。
- 页面文案使用爱团体系命名，不出现外部平台品牌文案。
