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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Endless
 */
@Data
@AllArgsConstructor
@ApiModel(description = "查询条件")
public class QueryField {

    @ApiModelProperty(hidden = true)
    private static final List<QueryOP> QUERY_LIKE = new ArrayList<>();

    @ApiModelProperty(hidden = true)
    private static final List<QueryOP> QUERY_LEFT_LIKE = new ArrayList<>();

    @ApiModelProperty(hidden = true)
    private static final List<QueryOP> QUERY_RIGHT_LIKE = new ArrayList<>();

    @ApiModelProperty(hidden = true)
    private static final List<QueryOP> QUERY_IN = new ArrayList<>();

    @ApiModelProperty(hidden = true)
    private static final List<QueryOP> QUERY_BETWEEN = new ArrayList<>();

    @ApiModelProperty(hidden = true)
    private static final List<QueryOP> QUERY_DEFAULT = new ArrayList<>();

    @ApiModelProperty(hidden = true)
    private static final List<QueryOP> QUERY_NULL_OR_EMPTY = new ArrayList<>();

    static {
        QUERY_IN.add(QueryOP.IN);
        QUERY_IN.add(QueryOP.NOT_IN);
        QUERY_LIKE.add(QueryOP.LIKE);
        QUERY_LIKE.add(QueryOP.NOT_LIKE);
        QUERY_LEFT_LIKE.add(QueryOP.LEFT_LIKE);
        QUERY_LEFT_LIKE.add(QueryOP.NOT_LEFT_LIKE);
        QUERY_RIGHT_LIKE.add(QueryOP.RIGHT_LIKE);
        QUERY_RIGHT_LIKE.add(QueryOP.NOT_RIGHT_LIKE);
        QUERY_BETWEEN.add(QueryOP.BETWEEN);
        QUERY_BETWEEN.add(QueryOP.NOT_BETWEEN);
        QUERY_DEFAULT.add(QueryOP.LIKE);
        QUERY_DEFAULT.add(QueryOP.NOT_LIKE);
        QUERY_DEFAULT.add(QueryOP.LEFT_LIKE);
        QUERY_DEFAULT.add(QueryOP.RIGHT_LIKE);
        QUERY_DEFAULT.add(QueryOP.NOT_LEFT_LIKE);
        QUERY_DEFAULT.add(QueryOP.NOT_RIGHT_LIKE);
        QUERY_DEFAULT.add(QueryOP.REGEXP);
        QUERY_DEFAULT.add(QueryOP.EQUAL);
        QUERY_DEFAULT.add(QueryOP.LESS);
        QUERY_DEFAULT.add(QueryOP.GREAT);
        QUERY_DEFAULT.add(QueryOP.NOT_EQUAL);
        QUERY_DEFAULT.add(QueryOP.LESS_EQUAL);
        QUERY_DEFAULT.add(QueryOP.GREAT_EQUAL);
        QUERY_NULL_OR_EMPTY.add(QueryOP.IS_NULL);
        QUERY_NULL_OR_EMPTY.add(QueryOP.NOT_NULL);
        QUERY_NULL_OR_EMPTY.add(QueryOP.IS_EMPTY);
        QUERY_NULL_OR_EMPTY.add(QueryOP.NOT_EMPTY);
    }

    @ApiModelProperty(name = "name", notes = "当前表包含的字段名", example = "id")
    private String name;

    @ApiModelProperty(name = "value", notes = "字段值: 数字，日期，字符串，数组  [1,2,3]/'value'/123/'2020-10-01' ", example = "1")
    private Object value;

    @ApiModelProperty(name = "operation", notes = Keys.SWAGGER_MSG_NOTES_OPERATION)
    private QueryOP operation = QueryOP.EQUAL;

    public Object getValue() {
        if (QUERY_NULL_OR_EMPTY.contains(operation)) {
            return null;
        }
        boolean contains = QUERY_IN.contains(operation) || QUERY_BETWEEN.contains(operation);
        ContextHelper.contextAssert(contains && !(value instanceof List), Keys.LOG_MSG_FAILED_TO_QUERY_NOT_SET + operation);
        ContextHelper.contextAssert(!contains && value instanceof List, Keys.LOG_MSG_FAILED_TO_QUERY_NOT_MUST_SET + operation);
        if (value instanceof List) {
            List<Object> transform = (List<Object>) value;
            boolean in = QUERY_IN.contains(operation) && !(1 <= transform.size() && transform.size() <= 1000);
            ContextHelper.contextAssert(in, Keys.LOG_MSG_FAILED_TO_QUERY_IN + value);
            boolean between = QUERY_BETWEEN.contains(operation) && !(transform.size() == 2);
            ContextHelper.contextAssert(between, Keys.LOG_MSG_FAILED_TO_QUERY_BETWEEN + value);
            return transform;
        }
        if (!StringUtils.isEmpty(value)) {
            boolean startsWith = value.toString().startsWith(Keys.SQL_FIELD_LIKE);
            boolean endsWith = value.toString().endsWith(Keys.SQL_FIELD_LIKE);
            if (QUERY_LIKE.contains(operation) && !startsWith && !endsWith) {
                return Keys.SQL_FIELD_LIKE + value + Keys.SQL_FIELD_LIKE;
            } else if (QUERY_LEFT_LIKE.contains(operation) && !startsWith) {
                return Keys.SQL_FIELD_LIKE + value;
            } else if (QUERY_RIGHT_LIKE.contains(operation) && !endsWith) {
                return value + Keys.SQL_FIELD_LIKE;
            }
        }
        return value;
    }

    public String toExpression() {
        if (QUERY_DEFAULT.contains(operation)) {
            return String.format(Keys.FILED_DEFAULT, name, operation.op());
        }
        if (QUERY_NULL_OR_EMPTY.contains(operation)) {
            return String.format(Keys.FILED_EMPTY, name, operation.op());
        }
        if (QueryOP.IN.equals(operation) || QueryOP.NOT_IN.equals(operation)) {
            return String.format(Keys.FILED_IN, name, operation.op(), ContextHelper.toHolderString((List<Object>) value));
        }
        if (QueryOP.BETWEEN.equals(operation)) {
            return String.format(Keys.FILED_BETWEEN, name);
        }
        if (QueryOP.NOT_BETWEEN.equals(operation)) {
            return String.format(Keys.FILED_NOT_BETWEEN, name);
        }
        if (QueryOP.EQUAL_IGNORE_CASE.equals(operation)) {
            return String.format(Keys.FILED_IGNORE_CASE, name, operation.op());
        }
        return Keys.EMPTY;
    }
}
