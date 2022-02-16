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
### 引入方式
2021-10-06 1.0 版本正式发布到中央仓库
```
<dependency>
  <groupId>cn.ipfs-files</groupId>
  <artifactId>activejdbc-spring-boot-starter</artifactId>
  <version>1.0.1.RELEASE</version>
</dependency>
```
