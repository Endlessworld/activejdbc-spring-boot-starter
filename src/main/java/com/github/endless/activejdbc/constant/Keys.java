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

package com.github.endless.activejdbc.constant;

/**
 * 常量
 *
 * @author Endless
 */
public class Keys {

    /**
     * 数据库类型-默认数据库-mysql
     */
    public final static String MASTER = "master";
    /**
     * 数据库类型-公共数据库-oracle
     */
    public final static String SLAVE = "slave";
    /**
     * 逻辑删除 是 代表数据无效
     */
    public final static String SQL_IS_DELETE = "1";
    /**
     * 逻辑删除 否 代表数据有效
     */
    public final static String SQL_IS_NOT_DEL = "0";
    /**
     * 系统中统一的逻辑删除字段名
     */
    public final static String SQL_DELETE_FILED = "IS_DELETE";
    /**
     * 条件生成-默认的条件语句
     */
    public final static String SQL_WHERE_DEFAULT = "1";
    /**
     * 条件生成-分割符号
     */
    public final static String SQL_WHERE_DELIMITER_AND_LIKE = " like ? \n AND ";
    /**
     * 条件生成-分割符号
     */
    public final static String SQL_WHERE_DELIMITER_OR_LIKE = " like ? \n OR ";
    /**
     * 条件生成-分割符号
     */
    public final static String SQL_WHERE_DELIMITER_AND = " = ? \n AND ";
    /**
     * 条件生成-分割符号
     */
    public final static String SQL_WHERE_DELIMITER_OR = " = ? \n OR ";
    /**
     * 条件生成-前缀
     */
    public final static String SQL_WHERE_PREFIX = " 1 \n AND( ";
    /**
     * 条件生成-后缀
     */
//    public final static String SQL_WHERE_SUFFIX = " = ? )\n AND IS_DELETE = 0 ";
    public final static String SQL_WHERE_SUFFIX = " = ? )\n ";
    /**
     * 条件生成-后缀
     */
//    public final static String SQL_WHERE_SUFFIX_LIKE = " like ? )\n AND IS_DELETE = 0 ";
    public final static String SQL_WHERE_SUFFIX_LIKE = " like ? )\n ";
    /**
     * 条件生成-未删除
     */
    public final static String SQL_WHERE_IS_NOT_DELETE = " AND IS_DELETE = 0 ";
    /**
     * 条件生成-未删除+一个占位符
     */
//    public final static String SQL_WHERE_SINGLE_PLACEHOLDER = " = ? AND IS_DELETE = 0 ";
    public final static String SQL_WHERE_SINGLE_PLACEHOLDER = " = ? ";
    /**
     * 参数解析-模糊查询
     */
    public final static String SQL_FIELD_LIKE = "%";
    /**
     * 分页查询
     */
    public final static String SQL_PAGE_SIZE = "pageSize";
    /**
     * 分页查询
     */
    public final static String SQL_PAGE_NUM = "pageNum";
    /**
     * 分页查询-排序字段多个以逗号隔开
     */
    public final static String SQL_PAGE_SORT = "sort";
    /**
     * 分页查询-排序方式多个以逗号隔开 与sort对应
     */
    public final static String SQL_PAGE_ODER = "order";
    /**
     * 条件生成-版本号
     */
    public final static String SQL_RECORD_VERSION_WHERE = "RECORD_VERSION = ?";
    /**
     * 条件生成-版本号为空
     */
    public final static String SQL_RECORD_VERSION_IS_NULL = "RECORD_VERSION IS NULL";
    /**
     * 默认日期时间格式
     */
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /**
     * 默认的创建人字段名
     */
    public static final String CREATED_BY = "CREATED_BY";
    /**
     * 默认的更新人字段名
     */
    public static final String UPDATED_BY = "UPDATED_BY";
    /**
     * 默认的乐观锁控制字段名
     */
    public static final String RECORD_VERSION = "RECORD_VERSION";
    /**
     * SQL防注入正则
     */
    public static final String INJECTION_REGEX = "[A-Za-z0-9\\_\\-\\+\\.]+";
    /**
     * 是
     */
    public static final String IS = "是";
    /**
     * 否
     */
    public static final String NOT = "否";
    /**
     * 空字符串
     */
    public static final String EMPTY = "";
    /**
     * 默认的用户名
     */
    public static final String DEFAULT = "Not login";
    /**
     * true
     */
    public static final String TRUE = "true";
    /**
     * json空对象
     */
    public static final String JSON_EMPTY = "{}";
    /**
     * isOr
     */
    public static final String IS_OR = "isOr";
    /**
     * 默认的操作符集合使用这个模板生成Expression
     */
    public static final String FILED_DEFAULT = "%s %s ?";
    /**
     * 空值判断操作符使用这个模板生成Expression
     */
    public static final String FILED_EMPTY = "%s %s";
    /**
     * 空值判断操作符使用这个模板生成Expression
     */
    public static final String FILED_IN = "%s %s (%s)";
    /**
     * 忽略大小写匹配操作符使用这个模板生成Expression
     */
    public static final String FILED_IGNORE_CASE = "upper(%s) %s upper(?)";
    /**
     * 区间判断匹配操作符使用这个模板生成Expression
     */
    public static final String FILED_BETWEEN = "%s between ? and ?";
    /**
     * 区间判断匹配操作符使用这个模板生成Expression
     */
    public static final String FILED_NOT_BETWEEN = "%s not between ? and ?";
    public static final String MSG_CLASS_NAME = "com.github.model.{0}";
    /**
     * 打开事务
     */
    public static final String LOG_MSG_OPEN_TRANSACTION = "activejdbc openTransaction : {}";
    /**
     * 提交事务
     */
    public static final String LOG_MSG_COMMIT_TRANSACTION = "activejdbc commitTransaction : {}";
    /**
     * 回滚事务
     */
    public static final String LOG_MSG_ROLLBACK_TRANSACTION = "activejdbc rollbackTransaction : {}";
    /**
     * 关闭事务
     */
    public static final String LOG_MSG_CLOSE_TRANSACTION = "activejdbc releaseConnection : {}";
    /**
     * 异步任务结果获取失败
     */
    public static final String LOG_MSG_FAILED_TASK_RESULTS = "failed to get async task results ：";
    /**
     * 保存失败
     */
    public static final String LOG_MSG_FAILED_TO_SAVEIT = "failed to saveIt ：";
    /**
     * 该数据无效或不存在
     */
    public static final String LOG_MSG_OBJECT_NOT_EXISTS = "该数据无效或不存在";
    /**
     * ID不可为空
     */
    public static final String LOG_MSG_ID_CANNOT_BE_NULL_A = "缺少参数,id不可为空 {}";
    /**
     * ID不可为空
     */
    public static final String LOG_MSG_ID_CANNOT_BE_NULL_B = "缺少参数,id不可为空";
    /**
     * 保存失败
     */
    public static final String LOG_MSG_FAILED_TO_DELETE = "failed to delete : ";
    /**
     * 参数解析失败
     */
    public static final String LOG_MSG_FAILED_TO_PARAMETER_RESOLVE = "参数解析异常";

    /**
     * 参数类型错误-必须是1<元素个数<=1000 的集合
     */
    public static final String LOG_MSG_FAILED_TO_QUERY_IN = "操作符为 IN/NOT_IN时 value必须是1<元素个数<=1000 的集合 :";

    /**
     * 参数类型错误-必须是只有两个元素数的集合
     */
    public static final String LOG_MSG_FAILED_TO_QUERY_BETWEEN = "操作符为 BETWEEN/NOT_BETWEEN时 value必须是只有两个元素数的集合:";

    /**
     * 参数类型错误-必须是集合
     */
    public static final String LOG_MSG_FAILED_TO_QUERY_NOT_SET = "当前操作符value值必须是集合:";
    /**
     * 参数类型错误-不能是集合
     */
    public static final String LOG_MSG_FAILED_TO_QUERY_NOT_MUST_SET = "当前操作符value值不能是集合:";
    /**
     * 参数类型错误-swagger注释
     */
    public static final String SWAGGER_MSG_NOTES_OPERATION = "比较符 \r\n [EQUAL,等于,=] \r\n"
            + "[NOT_EQUAL: 不等于,!=] \r\n[LESS: 小于,<] \r\n[LESS_EQUAL: 小于等于,<=] \r\n[GREAT: 大于,>] \r\n"
            + "[GREAT_EQUAL: 大于等于,>=] \r\n[LIKE: 包含,like] \r\n[NOT_LIKE: 不包含,not like] \r\n"
            + "[LEFT_LIKE: 以...开始,like] \r\n[NOT_LEFT_LIKE: 不以...开始,not like] \r\n"
            + "[RIGHT_LIKE: 以...结束,like] \r\n[NOT_RIGHT_LIKE: 不以...结束,not like] \r\n"
            + "[IS_NULL: 为null,is null] \r\n[NOT_NULL: 不为null,is not null] \r\n[IS_EMPTY: 空字符串, == ''] \r\n"
            + "[NOT_EMPTY: 非空字符串,!= ''] \r\n[IN: 在...中,in] \r\n[NOT_IN: 不在...中,not in] \r\n"
            + "[BETWEEN: 在...之间,between] \r\n[NOT_BETWEEN: 不在...之间,not between] \r\n"
            + "[EQUAL_IGNORE_CASE: 等于忽略大小写,=] \r\n[REGEXP: 正则表达式匹配,REGEXP] ";

    public static final String SWAGGER_MSG_ALLOW_ABLE_VALUES_OPERATION = "EQUAL,NOT_EQUAL,LESS,LESS_EQUAL,GREAT,GREAT_EQUAL,LIKE,NOT_LIKE," +
            "LEFT_LIKE," +
            "NOT_LEFT_LIKE ,RIGHT_LIKE,NOT_RIGHT_LIKE,IS_NULL,NOT_NULL,IS_EMPTY,NOT_EMPTY,IN,NOT_IN,BETWEEN,NOT_BETWEEN,EQUAL_IGNORE_CASE,REGEXP";
}
