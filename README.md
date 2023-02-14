# acivejdbc-spring-boot-starter
### 简介
该项目提供基于orm框架activejdbc的功能扩展。

项目将在springboot项目启动时、遍历容器中的数据源。

然后自动扫描数据库中每一张表的元数据 包括表名、主键、字段名、字段类型等。

在这个基础上使用JavaAssist字节码生成框架 动态生成所有必要的字节码并注入到容器中。

最终 本项目会为数据库的每一张表提供一套通用的CRUD接口:
- 单条新增
- 单条修改
- 单条查询
- 单条删除
- 批量新增
- 批量修改
- 批量删除
- 分页查询

从而消除Java后端开发中重复性的简单接口开发工作。使后端开发者从重复性的开发任务中解放出来，专注与业务功能开发。

基于以上特性，该项目期望淘汰一些代码生成工具和插件。

### 演示项目地址 
* https://github.com/Endlessworld/active-api

### 引入方式
2021-10-06 1.0 版本正式发布到中央仓库
```
<dependency>
  <groupId>cn.ipfs-files</groupId>
  <artifactId>activejdbc-spring-boot-starter</artifactId>
  <version>1.0.1.RELEASE</version>
</dependency>
```

### 在线接口地址
* https://y29bdwkxrh.apifox.cn/api-62175699

# 整库通用数据接口

Base URLs:

* <a href="http://127.0.0.1:9999">本地环境: http://127.0.0.1:9999</a>

## GET 分页查询

GET /model/{model-name}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|model-name|path|string| 是 |表名|
|isEqual|query|string| 否 |是否全等匹配,不填默认为false(true/false)|
|order|query|string| 否 |分页参数-排序方式，与排序字段对应，多个以英文逗号隔开|
|pageNum|query|string| 否 |分页参数-当前页码|
|pageSize|query|string| 否 |分页参数-每页条数|
|sort|query|string| 否 |分页参数-排序字段名，对应表结构必须包含该字段，多个以英文逗号隔开|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {
    "orderBys": "string",
    "pageNum": 0,
    "pageSize": 0,
    "rows": [
      {}
    ],
    "total": 0
  },
  "message": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|Inline|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Unauthorized|Inline|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Forbidden|Inline|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|Not Found|Inline|

### 返回数据结构

<a id="opIdeditUsingPUT"></a>

## PUT 编辑 根据主键覆盖剩余字段

PUT /model/{model-name}

> Body 请求参数

```json
"string"
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|model-name|path|string| 是 |表名|
|body|body|string| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {},
  "message": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|Inline|
|201|[Created](https://tools.ietf.org/html/rfc7231#section-6.3.2)|Created|Inline|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Unauthorized|Inline|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Forbidden|Inline|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|Not Found|Inline|

### 返回数据结构

<a id="opIdaddUsingPOST"></a>

## POST 新增

POST /model/{model-name}

> Body 请求参数

```json
"string"
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|model-name|path|string| 是 |表名|
|body|body|string| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {},
  "message": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|Inline|
|201|[Created](https://tools.ietf.org/html/rfc7231#section-6.3.2)|Created|Inline|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Unauthorized|Inline|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Forbidden|Inline|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|Not Found|Inline|

### 返回数据结构

<a id="opIddeleteTagByArrayUsingDELETE"></a>

## DELETE 根据ID数组批量删除

DELETE /model/{model-name}/array

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|model-name|path|string| 是 |none|
|id|query|integer| 是 |对应主键id|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": [
    {
      "compositeKeys": [
        "string"
      ],
      "frozen": true,
      "id": {},
      "idName": "string",
      "longId": 0,
      "modified": true,
      "new": true,
      "valid": true
    }
  ],
  "message": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|Inline|
|204|[No Content](https://tools.ietf.org/html/rfc7231#section-6.3.5)|No Content|Inline|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Unauthorized|Inline|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Forbidden|Inline|

### 返回数据结构

<a id="opIdcreateOrUpdateUsingPUT"></a>

## PUT 批量新增或修改（有id则根据id修改,否则新增）

PUT /model/{model-name}/batchCreateOrUpdate

> Body 请求参数

```json
"string"
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|model-name|path|string| 是 |表名|
|body|body|string| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": [
    {
      "compositeKeys": [
        "string"
      ],
      "frozen": true,
      "id": {},
      "idName": "string",
      "longId": 0,
      "modified": true,
      "new": true,
      "valid": true
    }
  ],
  "message": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|Inline|
|201|[Created](https://tools.ietf.org/html/rfc7231#section-6.3.2)|Created|Inline|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Unauthorized|Inline|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Forbidden|Inline|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|Not Found|Inline|

### 返回数据结构

<a id="opIdqueryUsingPOST"></a>

## POST 高级筛选

POST /model/{model-name}/filter

> Body 请求参数

```json
{
  "columns": [
    "string"
  ],
  "pageNum": 1,
  "pageSize": 10,
  "querys": [
    [
      {
        "name": "id",
        "operation": "EQUAL",
        "value": "1"
      }
    ]
  ],
  "sorter": [
    {
      "direction": "ASC",
      "property": "id"
    }
  ],
  "view": {}
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|model-name|path|string| 是 |表名|
|body|body|any| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {
    "orderBys": "string",
    "pageNum": 0,
    "pageSize": 0,
    "rows": [
      {
        "property1": {},
        "property2": {}
      }
    ],
    "total": 0
  },
  "message": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|Inline|
|201|[Created](https://tools.ietf.org/html/rfc7231#section-6.3.2)|Created|Inline|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Unauthorized|Inline|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Forbidden|Inline|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|Not Found|Inline|

### 返回数据结构

<a id="opIdfindFirstUsingGET"></a>

## GET 加载符合条件的第一条

GET /model/{model-name}/findFirst

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|model-name|path|string| 是 |表名|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {},
  "message": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|Inline|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Unauthorized|Inline|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Forbidden|Inline|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|Not Found|Inline|

### 返回数据结构

<a id="opIdqueryTreeUsingGET"></a>

## GET 分页查询-并加载所有子表

GET /model/{model-name}/include

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|model-name|path|string| 是 |表名|
|isEqual|query|string| 否 |是否全等匹配,不填默认为false(true/false)|
|order|query|string| 否 |分页参数-排序方式，与排序字段对应，多个以英文逗号隔开|
|pageNum|query|string| 否 |分页参数-当前页码|
|pageSize|query|string| 否 |分页参数-每页条数|
|sort|query|string| 否 |分页参数-排序字段名，对应表结构必须包含该字段，多个以英文逗号隔开|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {
    "orderBys": "string",
    "pageNum": 0,
    "pageSize": 0,
    "rows": [
      {}
    ],
    "total": 0
  },
  "message": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|Inline|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Unauthorized|Inline|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Forbidden|Inline|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|Not Found|Inline|

### 返回数据结构

<a id="opIdfindByIdUsingGET"></a>

## GET 根据主键加载一条

GET /model/{model-name}/{id}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |对应主键id|
|model-name|path|string| 是 |表名|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {},
  "message": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|Inline|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Unauthorized|Inline|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Forbidden|Inline|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|Not Found|Inline|

### 返回数据结构

<a id="opIddeleteUsingDELETE"></a>

## DELETE 根据主键逻辑删除

DELETE /model/{model-name}/{id}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |对应主键id|
|model-name|path|string| 是 |表名|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {},
  "message": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|Inline|
|204|[No Content](https://tools.ietf.org/html/rfc7231#section-6.3.5)|No Content|Inline|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Unauthorized|Inline|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Forbidden|Inline|

### 返回数据结构

# 数据模型

<h2 id="tocS_Response«PageQuery«Map«string,object»»»">Response«PageQuery«Map«string,object»»»</h2>

<a id="schemaresponse«pagequery«map«string,object»»»"></a>
<a id="schema_Response«PageQuery«Map«string,object»»»"></a>
<a id="tocSresponse«pagequery«map«string,object»»»"></a>
<a id="tocsresponse«pagequery«map«string,object»»»"></a>

```json
{
  "code": 0,
  "data": {
    "orderBys": "string",
    "pageNum": 0,
    "pageSize": 0,
    "rows": [
      {
        "property1": {},
        "property2": {}
      }
    ],
    "total": 0
  },
  "message": "string"
}

```

Response«PageQuery«Map«string,object»»»

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|data|[PageQuery«Map«string,object»»](#schemapagequery%c2%abmap%c2%abstring%2cobject%c2%bb%c2%bb)|false|none||none|
|message|string|false|none||none|

<h2 id="tocS_PageQuery«Map«string,object»»">PageQuery«Map«string,object»»</h2>

<a id="schemapagequery«map«string,object»»"></a>
<a id="schema_PageQuery«Map«string,object»»"></a>
<a id="tocSpagequery«map«string,object»»"></a>
<a id="tocspagequery«map«string,object»»"></a>

```json
{
  "orderBys": "string",
  "pageNum": 0,
  "pageSize": 0,
  "rows": [
    {
      "property1": {},
      "property2": {}
    }
  ],
  "total": 0
}

```

PageQuery«Map«string,object»»

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|orderBys|string|false|none||none|
|pageNum|integer(int32)|false|none||none|
|pageSize|integer(int32)|false|none||none|
|rows|[object]|false|none||none|
|» **additionalProperties**|object|false|none||none|
|total|integer(int64)|false|none||none|

<h2 id="tocS_QueryFilter«BaseModelVO»">QueryFilter«BaseModelVO»</h2>

<a id="schemaqueryfilter«basemodelvo»"></a>
<a id="schema_QueryFilter«BaseModelVO»"></a>
<a id="tocSqueryfilter«basemodelvo»"></a>
<a id="tocsqueryfilter«basemodelvo»"></a>

```json
{
  "columns": [
    "string"
  ],
  "pageNum": 1,
  "pageSize": 10,
  "querys": [
    [
      {
        "name": "id",
        "operation": "EQUAL",
        "value": "1"
      }
    ]
  ],
  "sorter": [
    {
      "direction": "ASC",
      "property": "id"
    }
  ],
  "view": {}
}

```

QueryFilter«BaseModelVO»

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|columns|[string]|false|none||返回字段|
|pageNum|integer(int32)|false|none||页号|
|pageSize|integer(int32)|false|none||分页大小|
|querys|[array]|false|none||查询条件组,各条件组之间以and链接<br> 同一组条件之间以or链接|
|sorter|[[FieldSort](#schemafieldsort)]|false|none||字段排序|
|view|[BaseModelVO](#schemabasemodelvo)|false|none||可作为查询参数的字段列表|

<h2 id="tocS_BaseModelVO">BaseModelVO</h2>

<a id="schemabasemodelvo"></a>
<a id="schema_BaseModelVO"></a>
<a id="tocSbasemodelvo"></a>
<a id="tocsbasemodelvo"></a>

```json
{}

```

BaseModelVO

### 属性

*None*

<h2 id="tocS_FieldSort">FieldSort</h2>

<a id="schemafieldsort"></a>
<a id="schema_FieldSort"></a>
<a id="tocSfieldsort"></a>
<a id="tocsfieldsort"></a>

```json
{
  "direction": "ASC",
  "property": "id"
}

```

FieldSort

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|direction|string|false|none||排序方向|
|property|string|false|none||排序字段|

#### 枚举值

|属性|值|
|---|---|
|direction|ASC|
|direction|DESC|

<h2 id="tocS_QueryField">QueryField</h2>

<a id="schemaqueryfield"></a>
<a id="schema_QueryField"></a>
<a id="tocSqueryfield"></a>
<a id="tocsqueryfield"></a>

```json
{
  "name": "id",
  "operation": "EQUAL",
  "value": "1"
}

```

QueryField

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|name|string|false|none||当前表包含的字段名|
|operation|string|true|none||比较符 <br /> [EQUAL,等于,=] <br />[NOT_EQUAL: 不等于,!=] <br />[LESS: 小于,<] <br />[LESS_EQUAL: 小于等于,<=] <br />[GREAT: 大于,>] <br />[GREAT_EQUAL: 大于等于,>=] <br />[LIKE: 包含,like] <br />[NOT_LIKE: 不包含,not like] <br />[LEFT_LIKE: 以...开始,like] <br />[NOT_LEFT_LIKE: 不以...开始,not like] <br />[RIGHT_LIKE: 以...结束,like] <br />[NOT_RIGHT_LIKE: 不以...结束,not like] <br />[IS_NULL: 为null,is null] <br />[NOT_NULL: 不为null,is not null] <br />[IS_EMPTY: 空字符串, == ''] <br />[NOT_EMPTY: 非空字符串,!= ''] <br />[IN: 在...中,in] <br />[NOT_IN: 不在...中,not in] <br />[BETWEEN: 在...之间,between] <br />[NOT_BETWEEN: 不在...之间,not between] <br />[EQUAL_IGNORE_CASE: 等于忽略大小写,=] <br />[REGEXP: 正则表达式匹配,REGEXP]|
|value|object|false|none||字段值: 数字，日期，字符串，数组  [1,2,3]/'value'/123/'2020-10-01'|

#### 枚举值

|属性|值|
|---|---|
|operation|BETWEEN|
|operation|EQUAL|
|operation|EQUAL_IGNORE_CASE|
|operation|GREAT|
|operation|GREAT_EQUAL|
|operation|IN|
|operation|IS_EMPTY|
|operation|IS_NULL|
|operation|LEFT_LIKE|
|operation|LESS|
|operation|LESS_EQUAL|
|operation|LIKE|
|operation|NOT_BETWEEN|
|operation|NOT_EMPTY|
|operation|NOT_EQUAL|
|operation|NOT_IN|
|operation|NOT_LEFT_LIKE|
|operation|NOT_LIKE|
|operation|NOT_NULL|
|operation|NOT_RIGHT_LIKE|
|operation|REGEXP|
|operation|RIGHT_LIKE|

<h2 id="tocS_Response«List«Model»»">Response«List«Model»»</h2>

<a id="schemaresponse«list«model»»"></a>
<a id="schema_Response«List«Model»»"></a>
<a id="tocSresponse«list«model»»"></a>
<a id="tocsresponse«list«model»»"></a>

```json
{
  "code": 0,
  "data": [
    {
      "compositeKeys": [
        "string"
      ],
      "frozen": true,
      "id": {},
      "idName": "string",
      "longId": 0,
      "modified": true,
      "new": true,
      "valid": true
    }
  ],
  "message": "string"
}

```

Response«List«Model»»

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|data|[[Model](#schemamodel)]|false|none||none|
|message|string|false|none||none|

<h2 id="tocS_Model">Model</h2>

<a id="schemamodel"></a>
<a id="schema_Model"></a>
<a id="tocSmodel"></a>
<a id="tocsmodel"></a>

```json
{
  "compositeKeys": [
    "string"
  ],
  "frozen": true,
  "id": {},
  "idName": "string",
  "longId": 0,
  "modified": true,
  "new": true,
  "valid": true
}

```

Model

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|compositeKeys|[string]|false|none||none|
|frozen|boolean|false|none||none|
|id|object|false|none||none|
|idName|string|false|none||none|
|longId|integer(int64)|false|none||none|
|modified|boolean|false|none||none|
|new|boolean|false|none||none|
|valid|boolean|false|none||none|

<h2 id="tocS_Response«Map«string,object»»">Response«Map«string,object»»</h2>

<a id="schemaresponse«map«string,object»»"></a>
<a id="schema_Response«Map«string,object»»"></a>
<a id="tocSresponse«map«string,object»»"></a>
<a id="tocsresponse«map«string,object»»"></a>

```json
{
  "code": 0,
  "data": {},
  "message": "string"
}

```

Response«Map«string,object»»

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|data|object|false|none||none|
|message|string|false|none||none|

<h2 id="tocS_Response«PageQuery»">Response«PageQuery»</h2>

<a id="schemaresponse«pagequery»"></a>
<a id="schema_Response«PageQuery»"></a>
<a id="tocSresponse«pagequery»"></a>
<a id="tocsresponse«pagequery»"></a>

```json
{
  "code": 0,
  "data": {
    "orderBys": "string",
    "pageNum": 0,
    "pageSize": 0,
    "rows": [
      {}
    ],
    "total": 0
  },
  "message": "string"
}

```

Response«PageQuery»

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|data|[PageQuery](#schemapagequery)|false|none||none|
|message|string|false|none||none|

<h2 id="tocS_PageQuery">PageQuery</h2>

<a id="schemapagequery"></a>
<a id="schema_PageQuery"></a>
<a id="tocSpagequery"></a>
<a id="tocspagequery"></a>

```json
{
  "orderBys": "string",
  "pageNum": 0,
  "pageSize": 0,
  "rows": [
    {}
  ],
  "total": 0
}

```

PageQuery

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|orderBys|string|false|none||none|
|pageNum|integer(int32)|false|none||none|
|pageSize|integer(int32)|false|none||none|
|rows|[object]|false|none||none|
|total|integer(int64)|false|none||none|



