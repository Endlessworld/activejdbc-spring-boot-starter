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
import com.github.endless.activejdbc.query.Response;
import org.javalite.activejdbc.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

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
public abstract class AbstractManager<T extends Model, V extends BaseModelVO> {

    /**
     * 获取第一个泛型参数的实际类型
     */
    public Class<T> modelClass() {
        return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public Response<V> createIt(@RequestBody V input) {
        return Response.respone(ContextHelper.createIt(modelClass(), input));
    }

    public Response<V> deleteTagById(@PathVariable(name = "id") Long id) {
        return Response.respone(ContextHelper.deleteTagById(modelClass(), id));
    }

    public Response<List<T>> createOrUpdate(@RequestBody List<V> rows) {
        return Response.respone(ContextHelper.batchCreateOrUpdateForVO(modelClass(), rows));
    }

    public Response<List<Model>> deleteTagByArray(@RequestParam(name = "id") Long[] id) {
        return Response.respone(ContextHelper.deleteTagById(modelClass(), id));
    }

    public Response<V> saveItById(@RequestBody V input) {
        return Response.respone(ContextHelper.saveItById(modelClass(), input));
    }

    public Response<PageQuery<V>> pageQuery(@RequestParam Map<String, Object> input, boolean isEqual) {
        return Response.respone(ContextHelper.pageQuery(modelClass(), input, isEqual));
    }

    public Response<PageQuery> queryTree(@RequestParam Map<String, Object> input, boolean isEqual) {
        return Response.respone(ContextHelper.includePageQuery(modelClass(), input, isEqual,
                ContextHelper.getChildrenClass(modelClass())));
    }

    public Response<V> findById(@PathVariable(name = "id") Long id) {
        return Response.respone(ContextHelper.findById(modelClass(), id));
    }

    public Response<Map<String, Object>> includeAll(@PathVariable(name = "id") Long id) {
        return Response.respone(ContextHelper.includeAllChildrens(ContextHelper.findById(modelClass(), id)).toMap());
    }

}
