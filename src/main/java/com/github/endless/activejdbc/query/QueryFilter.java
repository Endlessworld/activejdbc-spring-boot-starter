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

package com.github.endless.activejdbc.query;

import com.github.endless.activejdbc.constant.Keys;
import com.github.endless.activejdbc.core.ContextHelper;
import com.github.endless.activejdbc.core.Paginator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.ModelDelegate;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 朱领埔
 */
@Data
@SuppressWarnings("all")
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "通用查询器")
public class QueryFilter<V> {

    @ApiModelProperty(name = "page", notes = "页号", example = "1")
    private Integer pageNum = 1;

    @ApiModelProperty(name = "pageSize", notes = "分页大小", example = "10")
    private Integer pageSize = 10;

    @ApiModelProperty(name = "sorter", notes = "字段排序")
    private List<FieldSort> sorter = new ArrayList<FieldSort>();

    @ApiModelProperty(name = "columns", notes = "返回字段")
    private List<String> columns;

    @ApiModelProperty(name = "query", notes = "查询条件组,各条件组之间以and链接<br> 同一组条件之间以or链接")
    private List<List<QueryField>> querys = new ArrayList<List<QueryField>>();

    @ApiModelProperty(name = "querys", notes = "查询条件组", hidden = true)
    private List<Object> params = new LinkedList<>();

    @ApiModelProperty(name = "view", notes = "可作为查询参数的字段列表")
    private V view;

    /**
     * 各条件组之间以and链接 <br>
     * 同一组条件之间以or链接<br>
     * 如果query是一个空map则返回 1 and 1 相当于没有查询条件<br>
     */
    public String query(Class<? extends Model>... modelClass) {
        if(params == null ){
            params = new LinkedList<>();
        }else{
            params.clear();
        }
        StringBuffer where = new StringBuffer();
        where.append(querys.stream().map(e -> {
            return e.stream().map(field -> {
                if (field == null) {
                    return Keys.SQL_WHERE_DEFAULT;
                }
                boolean isEmpty = false;
                if (modelClass.length != 0) {
                    isEmpty = !ModelDelegate.attributeNames(modelClass[0]).contains(field.getName().toUpperCase());
                } else {
//					isEmpty = ReflectionUtils.findField(view.getClass(), field.getName()) == null;
                }
                ContextHelper.contextAssert(isEmpty, "不应包含该字段 ：" + field.getName());
                if (field.getValue() instanceof List) {
                    params.addAll((List) field.getValue());
                } else {
                    params.add(field.getValue());
                }
                return field.toExpression();
            }).collect(Collectors.joining(" or ", "and ( ", " )"));
        }).collect(Collectors.joining(" ", "1 ", " and 1 ")));
        params = params.stream().filter(e -> e != null).collect(Collectors.toList());
        return where.toString();
    }

    public String orderBy(Class<Model> modelClass) {
        return sorter.stream().filter(e -> {
            return ModelDelegate.attributeNames(modelClass).contains(e.getProperty());
        }).map(e -> e.toExpression()).collect(Collectors.joining(",", " ", ""));
    }

    public PageQuery<Map<String, Object>> search(Class<Model> modelClass) {
        return Paginator.instance()
                .modelClass(modelClass)
                .query(query(modelClass))
                .params(getParams().toArray())
                .orderBy(orderBy(modelClass))
                .columns(selectColumns(modelClass))
                .pageSize(getPageSize())
                .currentPageIndex(getPageNum(), true)
                .create().apply();
    }

    public List<String> selectColumns(Class<Model> modelClass) {
        return getColumns().stream().filter(e -> ModelDelegate.attributeNames(modelClass).contains(e)).collect(Collectors.toList());
    }


}
