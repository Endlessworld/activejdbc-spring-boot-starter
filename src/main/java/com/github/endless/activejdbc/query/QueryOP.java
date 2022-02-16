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

import io.swagger.annotations.ApiModel;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 查询字段和值的操作类型枚举
 * @author Endless
 */
@ApiModel(description = "操作类型枚举")
public enum QueryOP {

    EQUAL("EQ", "=", "等于"),

    NOT_EQUAL("NE", "!=", "不等于"),

    LESS("LT", "<", "小于"),

    LESS_EQUAL("LE", "<=", "小于等于"),

    GREAT("GT", ">", "大于"),

    GREAT_EQUAL("GE", ">=", "大于等于"),

    LIKE("LK", "like", "包含"),

    NOT_LIKE("LK", "not like", "不包含"),

    LEFT_LIKE("LFK", "like", "以...开始"),

    NOT_LEFT_LIKE("LFK", "not like", "不以...开始"),

    RIGHT_LIKE("RHK", "like", "以...结束"),

    NOT_RIGHT_LIKE("RHK", "not like", "不以...结束"),

    IS_NULL("ISNULL", "is null", "为null"),

    NOT_NULL("NOTNULL", "is not null", "不为null"),

    IS_EMPTY("ISEMPTY", "= ''", "空字符串"),

    NOT_EMPTY("NOTEMPTY", "!= ''", "非空字符串"),

    IN("IN", "in", "在...中"),

    NOT_IN("NOT_IN", "not in", "不在...中"),

    BETWEEN("BETWEEN", "between", "在...之间"),

    NOT_BETWEEN("NOT_BETWEEN", "between", "不在...之间"),

    EQUAL_IGNORE_CASE("EIC", "=", "等于忽略大小写"),

    REGEXP("REGEXP", "REGEXP", "正则表达式匹配");

    private final String val;
    private final String op;
    private final String desc;

    QueryOP(String _val, String _op, String _desc) {
        val = _val;
        op = _op;
        desc = _desc;
    }

    /**
     * 根据运算符获取QueryOp
     *
     * @return QueryOP
     * @throws @since 1.0.0
     */
    public static QueryOP getByOP(String op) {
        return Arrays.stream(values()).filter(o -> o.equals(op)).findFirst().orElse(null);
    }

    public static QueryOP getByVal(String val) {
        return Arrays.stream(values()).filter(o -> o.val.equals(val)).findFirst().orElse(null);
    }

    public static String fullString() {
        return Arrays.stream(QueryOP.values()).map(e -> {
            return new StringBuffer("[").append(e.name()).append(": ").append(e.desc).append(",").append(e.op).append("]");
        }).collect(Collectors.joining("\r\n"));
    }

    public String value() {
        return val;
    }

    public String op() {
        return op;
    }

    public String desc() {
        return desc;
    }
}
