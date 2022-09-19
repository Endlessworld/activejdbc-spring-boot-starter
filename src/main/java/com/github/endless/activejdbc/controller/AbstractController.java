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
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.javalite.activejdbc.Model;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

/**
 * 基本接口
 *
 * @author Endless
 */
@SuppressWarnings("all")
@EnableModel(value = {})
public abstract class AbstractController<T extends Model, V extends BaseModelVO> {

    /**
     * 获取第一个泛型参数的实际类型
     */
    public Class<Model> modelClass() {
        return (Class<Model>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    @ApiOperation("增加")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public Response<V> createIt(@RequestBody V input) {
        return Response.respone(ContextHelper.createIt(modelClass(), input));
    }

    @ApiOperation("根据ID删除 一条")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "对应主键id", required = true, dataType = "Long", paramType = "path")})
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public Response<V> deleteTagById(@PathVariable(name = "id") Long id) {
        return Response.respone(ContextHelper.deleteTagById(modelClass(), id));
    }

    @ApiOperation("批量新增或修改（有id则根据id修改,否则新增）")
    @RequestMapping(value = "/batchCreateOrUpdate", method = RequestMethod.PUT)
    public Response<?> createOrUpdate(@RequestBody List<V> rows) {
        return Response.respone(ContextHelper.batchCreateOrUpdateForVO(modelClass(), rows));
    }

    @ApiOperation("根据ID数组批量删除")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "对应主键id", required = true, allowMultiple = true, dataType = "Long", paramType = "query")})
    @RequestMapping(value = "/array", method = RequestMethod.DELETE)
    public Response<List<Model>> deleteTagByArray(@RequestParam(name = "id") Long[] id) {
        return Response.respone(ContextHelper.deleteTagById(modelClass(), id));
    }

    @ApiOperation("编辑 根据主键覆盖剩余字段")
    @RequestMapping(value = "", method = RequestMethod.PUT)
    public Response<V> saveItById(@RequestBody V input) {
        return Response.respone(ContextHelper.saveItById(modelClass(), input));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageSize", defaultValue = "10", value = "分页参数-每页条数", dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "pageNum", defaultValue = "1", value = "分页参数-当前页码", dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "sort", value = "分页参数-排序字段名，对应表结构必须包含该字段，多个以英文逗号隔开", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "分页参数-排序方式，与排序字段对应，多个以英文逗号隔开", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "isEqual", defaultValue = "false", value = "是否全等匹配,不填默认为false(true/false)", dataType = "boolean", paramType = "query"),
            @ApiImplicitParam(name = "isOr", defaultValue = "false", value = "是否使用或运算,多个查询条件以or连接,不填默认为false(true:or,false:and)", dataType = "boolean", paramType = "query"),
            @ApiImplicitParam(name = "input", value = "参数，此处应为键值对，name取对应表字段名", dataType = "String", paramType = "query")})
    @ApiOperation("分页查询 ")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public Response<PageQuery<V>> pageQuery(@RequestParam Map<String, Object> input, boolean isEqual) {
        return Response.respone(ContextHelper.pageQuery(modelClass(), input, isEqual));
    }

    @ApiOperation("高级筛选")
    @RequestMapping(value = "/filter", method = RequestMethod.POST)
    public Response<PageQuery<Map<String, Object>>> query(@RequestBody QueryFilter<V> queryFilter) {
        return Response.respone(queryFilter.search(modelClass()));
    }

    @ApiOperation("根据id加载一条")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "对应主键id", required = true, dataType = "Long", paramType = "path")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public Response<V> findById(@PathVariable(name = "id") Long id) {
        return Response.respone(ContextHelper.findById(modelClass(), id));
    }

    @ApiOperation("根据id加载一条,并加载所有子表数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "对应主键id", required = true, dataType = "Long", paramType = "path")})
    @RequestMapping(value = "/{id}/includeAll", method = RequestMethod.GET)
    public Response<Map<String, Object>> includeAll(@PathVariable(name = "id") Long id) {
        return Response.respone(ContextHelper.includeAllChildren(ContextHelper.findById(modelClass(), id)).toMap());
    }

}
