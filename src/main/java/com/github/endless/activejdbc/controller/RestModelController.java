/*
 * Copyright 2021. Endless All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */

package com.github.endless.activejdbc.controller;

import com.github.endless.activejdbc.annotation.EnableModel;
import com.github.endless.activejdbc.core.ContextHelper;
import com.github.endless.activejdbc.domains.BaseModelVO;
import com.github.endless.activejdbc.query.PageQuery;
import com.github.endless.activejdbc.query.QueryFilter;
import com.github.endless.activejdbc.query.Response;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.javalite.activejdbc.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 提供统一crud接口
 *
 * @author Endless
 */
@RestController
@RequestMapping("model")
@Api(tags = "基础数据接口")
@SuppressWarnings("all")
public class RestModelController {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields(new String[]{});
    }
    private ThreadLocal<String> tableName = new ThreadLocal<>();

    @ModelAttribute
    public void attribute(@PathVariable("model-name") String tableName) {
        this.tableName.set(tableName);
    }

    public Class<Model> modelClass() {
        return ContextHelper.modelClass(tableName.get());
    }

    /**
     * 新增
     */
    @EnableModel
    @ApiOperation("新增")
    @ApiImplicitParams({@ApiImplicitParam(name = "model-name", value = "表名", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "input", value = "参数，此处应为键值对，name取对应表字段名，不可包含主键", required = true, dataType = "object", paramType = "body")})
    @RequestMapping(value = "{model-name}", method = RequestMethod.POST)
    public Response<Map<String, Object>> add(@RequestBody Map<String, Object> input) {
        return Response.respone(ContextHelper.createIt(modelClass(), input).toMap());
    }

    /**
     * 标记删除
     */
    @EnableModel
    @ApiOperation("根据主键逻辑删除")
    @ApiImplicitParams({@ApiImplicitParam(name = "model-name", value = "表名", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "id", value = "对应主键id", required = true, dataType = "Long", paramType = "path")})
    @RequestMapping(value = "/{model-name}/{id}", method = RequestMethod.DELETE)
    public Response<Map<String, Object>> delete(@PathVariable(name = "id") Long id) {
        return Response.respone(ContextHelper.deleteTagById(modelClass(), id).toMap());
    }

    @EnableModel
    @ApiOperation("批量新增或修改（有id则根据id修改,否则新增）")
    @ApiImplicitParams({@ApiImplicitParam(name = "model-name", value = "表名", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "input", value = "参数，此处应为键值对，name取对应表字段名", required = true, dataType = "object", paramType = "body")})
    @RequestMapping(value = "/{model-name}/batchCreateOrUpdate", method = RequestMethod.PUT)
    public Response<List<Model>> createOrUpdate(@RequestBody List<Map> rows) {
        return Response.respone(ContextHelper.batchCreateOrUpdateForMap(modelClass(), rows));
    }
    @EnableModel
    @ApiOperation("根据ID数组批量删除")
    @ApiImplicitParams({@ApiImplicitParam(name = "id", value = "对应主键id", required = true, allowMultiple = true, dataType = "Long", paramType = "query")})
    @RequestMapping(value = "/{model-name}/array", method = RequestMethod.DELETE)
    public Response<List<Model>> deleteTagByArray(@RequestParam(name = "id") Long[] id) {
        return Response.respone(ContextHelper.deleteTagById(modelClass(), id));
    }

    ;

    /**
     * 编辑 根据主键覆盖剩余字段
     */
    @EnableModel
    @ApiOperation("编辑 根据主键覆盖剩余字段")
    @ApiImplicitParams({@ApiImplicitParam(name = "model-name", value = "表名", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "input", value = "参数，此处应为键值对，name取对应表字段名", required = true, dataType = "object", paramType = "body")})
    @RequestMapping(value = "/{model-name}", method = RequestMethod.PUT)
    public Response<Map<String, Object>> edit(@RequestBody Map<String, Object> input) {
        return Response.respone(ContextHelper.saveItById(modelClass(), input).toMap());
    }

    /**
     * 分页查询
     */
    @EnableModel
    @ApiImplicitParams({@ApiImplicitParam(name = "model-name", value = "表名", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "pageSize", defaultValue = "10", value = "分页参数-每页条数", dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "pageNum", defaultValue = "1", value = "分页参数-当前页码", dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "sort", value = "分页参数-排序字段名，对应表结构必须包含该字段，多个以英文逗号隔开", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "分页参数-排序方式，与排序字段对应，多个以英文逗号隔开", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "isEqual", value = "是否全等匹配,不填默认为false(true/false)", dataType = "boolean", paramType = "query"),
            @ApiImplicitParam(name = "input", value = "参数，此处应为键值对，name取对应表字段名", dataType = "String", paramType = "query")})
    @ApiOperation("分页查询 ")
    @RequestMapping(value = "/{model-name}", method = RequestMethod.GET)
    public Response<PageQuery> list(@RequestParam Map<String, Object> input, boolean isEqual) {
        return Response.respone(ContextHelper.pageQuery(modelClass(), input, isEqual));
    }
    @EnableModel
    @ApiImplicitParams({@ApiImplicitParam(name = "model-name", value = "表名", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "pageSize", defaultValue = "10", value = "分页参数-每页条数", dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "pageNum", defaultValue = "1", value = "分页参数-当前页码", dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "sort", value = "分页参数-排序字段名，对应表结构必须包含该字段，多个以英文逗号隔开", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "分页参数-排序方式，与排序字段对应，多个以英文逗号隔开", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "isEqual", value = "是否全等匹配,不填默认为false(true/false)", dataType = "boolean", paramType = "query"),
            @ApiImplicitParam(name = "input", value = "参数，此处应为键值对，name取对应表字段名", dataType = "String", paramType = "query")})
    @ApiOperation("分页查询-并加载所有子表 ")
//    @ApiIgnore
    @RequestMapping(value = "/{model-name}/include", method = RequestMethod.GET)
    public Response<PageQuery> queryTree(@RequestParam Map<String, Object> input, boolean isEqual) {
        return Response.respone(ContextHelper.includePageQuery(modelClass(), input, isEqual, ContextHelper.getChildrenClass(modelClass())));
    }
    @EnableModel
    @ApiOperation("高级筛选")
    @ApiImplicitParam(name = "model-name", value = "表名", required = true, dataType = "String", paramType = "path")
    @RequestMapping(value = "/{model-name}/filter", method = RequestMethod.POST)
    public Response<PageQuery<Map<String, Object>>> query(@RequestBody QueryFilter<BaseModelVO> queryFilter) {
        return Response.respone(queryFilter.search(modelClass()));
    }

    /**
     * 根据id加载
     */
    @EnableModel
    @ApiOperation("根据主键加载一条")
    @ApiImplicitParams({@ApiImplicitParam(name = "model-name", value = "表名", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "id", value = "对应主键id", required = true, dataType = "Long", paramType = "path")})
    @RequestMapping(value = "/{model-name}/{id}", method = RequestMethod.GET)
    public Response<Map<String, Object>> findById(@PathVariable(name = "id") Long id) {
        return Response.respone(ContextHelper.findById(modelClass(), id).toMap());
    }

    /**
     * 加载符合条件的第一条
     */
    @EnableModel
    @ApiOperation("加载符合条件的第一条")
    @ApiImplicitParams({@ApiImplicitParam(name = "model-name", value = "表名", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "input", value = "参数，此处应为键值对，name取对应表字段名", required = true, dataType = "String", paramType = "query")})
    @RequestMapping(value = "/{model-name}/findFirst", method = RequestMethod.GET)
    public Response<Map<String, Object>> findFirst(@RequestParam Map<String, Object> input) {
        final Model model = ContextHelper.findFirst(modelClass(), input);
        return Response.respone(ContextHelper.assertNotNull(model).toMap());
    }
}
