# 上传图片

## OpenAPI Specification

```yaml
openapi: 3.0.1
info:
  title: ''
  description: ''
  version: 1.0.0
paths:
  /api/v1/upload:
    post:
      summary: 上传图片
      deprecated: false
      description: ''
      tags:
        - 旧版本接口
      parameters:
        - name: Accept
          in: header
          description: ''
          required: false
          example: application/json
          schema:
            type: string
        - name: Authorization
          in: header
          description: ''
          required: false
          example: Bearer {{token}}
          schema:
            type: string
            default: Bearer {{token}}
      requestBody:
        content:
          multipart/form-data:
            schema:
              type: object
              properties:
                file:
                  format: binary
                  type: string
                  description: 图片
                  example: file:///Users/company/Downloads/落魄程序员在线炒粉.gif
                token:
                  description: 临时上传 Token
                  example: ''
                  type: string
                permission:
                  type: integer
                  description: 权限，1=公开，0=私有
                  example: 0
                strategy_id:
                  description: 储存策略ID
                  example: '1'
                  type: string
                album_id:
                  description: 相册ID
                  example: ''
                  type: string
                expired_at:
                  description: 图片过期时间(yyyy-MM-dd HH:mm:ss)
                  example: ''
                  type: string
              required:
                - file
      responses:
        '200':
          description: ''
          content:
            application/json:
              schema:
                type: object
                properties: {}
              example:
                status: true
                message: 上传成功
                data:
                  key: 21
                  name: 落魄程序员在线炒粉
                  pathname: 20240719/6a39702c8347047c6749854a40831de0.gif
                  origin_name: 落魄程序员在线炒粉.gif
                  size: 465.1474609375
                  mimetype: image/gif
                  extension: gif
                  md5: 6a39702c8347047c6749854a40831de0
                  sha1: 570bdc9ae184db710ee74824a15725d5ed3db589
                  links:
                    url: >-
                      http://localhost/20240719/6a39702c8347047c6749854a40831de0.gif
                    html: >-
                      &lt;img
                      src="http://localhost/20240719/6a39702c8347047c6749854a40831de0.gif"
                      alt="落魄程序员在线炒粉.gif" title="落魄程序员在线炒粉.gif" /&gt;
                    bbcode: >-
                      [img]http://localhost/20240719/6a39702c8347047c6749854a40831de0.gif[/img]
                    markdown: >-
                      ![落魄程序员在线炒粉.gif](http://localhost/20240719/6a39702c8347047c6749854a40831de0.gif)
                    markdown_with_link: >-
                      [![落魄程序员在线炒粉.gif](http://localhost/20240719/6a39702c8347047c6749854a40831de0.gif)](http://localhost/20240719/6a39702c8347047c6749854a40831de0.gif)
                    thumbnail_url: >-
                      http://localhost/20240719/6a39702c8347047c6749854a40831de0.gif
                    delete_url: ''
          headers: {}
          x-apifox-name: 成功
      security: []
      x-apifox-folder: 旧版本接口
      x-apifox-status: released
      x-run-in-apifox: https://app.apifox.com/web/project/4596809/apis/api-195745209-run
components:
  schemas: {}
  securitySchemes: {}
servers:
  - url: http://127.0.0.1:8000/api/v2
    description: 开发环境
security: []

```



# 生成临时上传token

## OpenAPI Specification

```yaml
openapi: 3.0.1
info:
  title: ''
  description: ''
  version: 1.0.0
paths:
  /api/v1/images/tokens:
    post:
      summary: 生成临时上传token
      deprecated: false
      description: ''
      tags:
        - 旧版本接口
      parameters:
        - name: Accept
          in: header
          description: ''
          required: false
          example: application/json
          schema:
            type: string
        - name: Authorization
          in: header
          description: ''
          required: false
          example: Bearer {{token}}
          schema:
            type: string
            default: Bearer {{token}}
      requestBody:
        content:
          application/x-www-form-urlencoded:
            schema:
              type: object
              properties:
                num:
                  description: 生成数量，最大 100
                  example: '1'
                  type: string
                seconds:
                  description: 有效期(秒)，最大 2626560 (一个月)
                  example: '120'
                  type: string
              required:
                - num
                - seconds
      responses:
        '200':
          description: ''
          content:
            application/json:
              schema:
                type: object
                properties:
                  status:
                    type: boolean
                  message:
                    type: string
                  data:
                    type: object
                    properties:
                      tokens:
                        type: array
                        items:
                          type: object
                          properties:
                            token:
                              type: string
                            expired_at:
                              type: string
                    required:
                      - tokens
                required:
                  - status
                  - message
                  - data
              example:
                status: true
                message: 生成成功
                data:
                  tokens:
                    - token: <LSKYPRO_TOKEN_EXAMPLE>
                      expired_at: '2024-07-19 15:17:43'
          headers: {}
          x-apifox-name: 成功
      security: []
      x-apifox-folder: 旧版本接口
      x-apifox-status: released
      x-run-in-apifox: https://app.apifox.com/web/project/4596809/apis/api-195791421-run
components:
  schemas: {}
  securitySchemes: {}
servers:
  - url: http://127.0.0.1:8000/api/v2
    description: 开发环境
security: []

```

---

接口URL

```
https://p.2b.gs/api/v1
```

验证方式

当前版本接口采用 「HTTP 基本验证」的方式验证授权，获取到 token 后，通过设置请求 header 标头来验证请求(Bearer Token)，例如：**"Authorization": "Bearer 1|key"**

如果未设置 Authorization 的情况下请求上传接口，将会被视为游客上传。

公共请求 headers 说明

| 字段          | 类型   | 说明                            |
| :------------ | :----- | :------------------------------ |
| Authorization | String | 授权 Token，例如：Bearer 1\|key |
| *Accept       | String | 必须设置为 application/json     |

公共响应 headers 说明

| 字段                  | 类型    | 说明                       |
| :-------------------- | :------ | :------------------------- |
| X-RateLimit-Limit     | Integer | 当前客户端一分钟内请求配额 |
| X-RateLimit-Remaining | Integer | 当前客户端剩余请求配额     |

响应状态码 HTTP Status Code 说明

| 状态码 | 说明                   |
| :----- | :--------------------- |
| 401    | 未登录或授权失败       |
| 403    | 管理员关闭了接口功能   |
| 429    | 超出请求配额，请求受限 |
| 500    | 服务端出现异常         |

文档中接口的请求参数，使用红色「*」符号标注，则表示为必传项。

授权相关

生成 Token

```
/tokens
```

请求参数(Body)

| 字段      | 类型   | 说明 |
| :-------- | :----- | :--- |
| *email    | String | 邮箱 |
| *password | String | 密码 |

返回参数

| 字段    | 类型    | 说明                |
| :------ | :------ | :------------------ |
| status  | Boolean | 状态，true 或 false |
| message | String  | 描述信息            |
| data    | Object  | 数据                |
| token   | String  | Token               |

清空 Token

```
/tokens
```

返回参数

| 字段    | 类型    | 说明                |
| :------ | :------ | :------------------ |
| status  | Boolean | 状态，true 或 false |
| message | String  | 描述信息            |
| data    | Object  | 数据                |

用户资料

```
/profile
```

返回参数

| 字段          | 类型    | 说明                |
| :------------ | :------ | :------------------ |
| status        | Boolean | 状态，true 或 false |
| message       | String  | 描述信息            |
| data          | Object  | 数据                |
| name          | String  | 用户名              |
| avatar        | String  | 头像地址            |
| email         | String  | 邮箱地址            |
| capacity      | Float   | 总容量              |
| used_capacity | Float   | 已使用容量          |
| url           | String  | 个人主页地址        |
| image_num     | Integer | 图片数量            |
| album_num     | Integer | 相册数量            |
| registered_ip | String  | 注册 IP             |

策略相关

策略列表

```
/strategies
```

请求参数(Query)

| 字段    | 类型   | 说明       |
| :------ | :----- | :--------- |
| keyword | String | 筛选关键字 |

返回参数

| 字段       | 类型     | 说明                |
| :--------- | :------- | :------------------ |
| status     | Boolean  | 状态，true 或 false |
| message    | String   | 描述信息            |
| data       | Object   | 数据                |
| strategies | Object[] | 策略数据            |
| id         | Integer  | 策略 ID             |
| name       | String   | 策略名称            |

图片相关

上传图片

```
/upload
```

Headers

| 字段          | 类型   | 说明                           |
| :------------ | :----- | :----------------------------- |
| *Content-Type | String | 需要设置为 multipart/form-data |

请求参数(Body)

| 字段        | 类型    | 说明       |
| :---------- | :------ | :--------- |
| *file       | File    | 图片文件   |
| strategy_id | Integer | 储存策略ID |

返回参数

| 字段               | 类型    | 说明                |
| :----------------- | :------ | :------------------ |
| status             | Boolean | 状态，true 或 false |
| message            | String  | 描述信息            |
| data               | Object  | 数据                |
| key                | String  | 图片唯一密钥        |
| name               | String  | 图片名称            |
| pathname           | String  | 图片路径名          |
| origin_name        | String  | 图片原始名          |
| size               | Float   | 图片大小，单位 KB   |
| mimetype           | String  | 图片类型            |
| extension          | String  | 图片拓展名          |
| md5                | String  | 图片 md5 值         |
| sha1               | String  | 图片 sha1 值        |
| links              | Object  | 链接                |
| url                | String  | 图片访问 url        |
| html               | String  | -                   |
| bbcode             | String  | -                   |
| markdown           | String  | -                   |
| markdown_with_link | String  | -                   |
| thumbnail_url      | String  | 缩略图 url          |

图片列表

```
/images
```

请求参数(Query)

| 字段       | 类型    | 说明                                                         |
| :--------- | :------ | :----------------------------------------------------------- |
| page       | Integer | 页码                                                         |
| order      | String  | 排序方式，newest=最新，earliest=最早，utmost=最大，least=最小 |
| permission | String  | 权限，public=公开的，private=私有的                          |
| album_id   | Integer | 相册 ID                                                      |
| keyword    | String  | 筛选关键字                                                   |

返回参数

| 字段         | 类型     | 说明                                    |
| :----------- | :------- | :-------------------------------------- |
| status       | Boolean  | 状态，true 或 false                     |
| message      | String   | 描述信息                                |
| data         | Object   | 数据                                    |
| current_page | Integer  | 当前所在页页码                          |
| last_page    | Integer  | 最后一页页码                            |
| per_page     | Integer  | 每页展示数据数量                        |
| total        | Integer  | 图片总数量                              |
| data         | Object[] | 图片列表                                |
| key          | String   | 图片唯一密钥                            |
| name         | String   | 图片名称                                |
| origin_name  | String   | 图片原始名称                            |
| pathname     | String   | 图片路径名                              |
| size         | Float    | 图片大小，单位 KB                       |
| width        | Integer  | 图片宽度                                |
| height       | Integer  | 图片高度                                |
| md5          | String   | 图片 md5 值                             |
| sha1         | String   | 图片 sha1 值                            |
| human_date   | String   | 上传时间(友好格式)                      |
| date         | String   | 上传日期(yyyy-MM-dd HH:mm:ss)           |
| links        | Object   | 链接，与上传接口返回参数中的 links 相同 |

删除图片

```
/images/:key
```

请求参数(Params)

| 字段 | 类型   | 说明     |
| :--- | :----- | :------- |
| *key | String | 图片密钥 |

返回参数

| 字段    | 类型    | 说明                |
| :------ | :------ | :------------------ |
| status  | Boolean | 状态，true 或 false |
| message | String  | 描述信息            |
| data    | Object  | 数据                |

相册相关

相册列表

```
/albums
```

请求参数(Query)

| 字段    | 类型    | 说明                                                         |
| :------ | :------ | :----------------------------------------------------------- |
| page    | Integer | 页码                                                         |
| order   | String  | 排序方式，newest=最新，earliest=最早，most=图片最多，least=图片最少 |
| keyword | String  | 筛选关键字                                                   |

返回参数

| 字段         | 类型     | 说明                |
| :----------- | :------- | :------------------ |
| status       | Boolean  | 状态，true 或 false |
| message      | String   | 描述信息            |
| data         | Object   | 数据                |
| current_page | Integer  | 当前所在页页码      |
| last_page    | Integer  | 最后一页页码        |
| per_page     | Integer  | 每页展示数据数量    |
| total        | Integer  | 图片总数量          |
| data         | Object[] | 相册列表            |
| id           | Integer  | 相册自增 ID         |
| name         | String   | 相册名称            |
| intro        | String   | 相册简介            |
| image_num    | Integer  | 相册图片数量        |

删除相册

```
/albums/:id
```

请求参数(Params)

| 字段 | 类型   | 说明        |
| :--- | :----- | :---------- |
| *id  | String | 相册自增 ID |

返回参数

| 字段    | 类型    | 说明                |
| :------ | :------ | :------------------ |
| status  | Boolean | 状态，true 或 false |
| message | String  | 描述信息            |
| data    | Object  | 数据                |