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

import lombok.extern.log4j.Log4j2;
import org.javalite.activejdbc.MetaModel;
import org.javalite.activejdbc.ModelDelegate;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Endless
 */
@Log4j2
public class Utils {

    private static final Set<String> FILTER = new HashSet<String>();
    private static final Pattern linePattern = Pattern.compile("_(\\w)");
    private static final Pattern humpPattern = Pattern.compile("[A-Z]");

    static {
        FILTER.add("com.");
    }

    public static boolean isNullOrEmpty(Object obj) {
        return (null == obj || "".equals(obj) || "null".equals(obj));
    }

    /**
     * 生成order by 子句
     * <hr>
     *
     * @param sort  排序的字段
     * @param order DESC/ASC
     * @return String
     */
    public static String orderBy(String sort, String order) {
        StringBuilder sb = new StringBuilder();
        if (sort == null) {
            return null;
        }
        if (order == null) {
            return sort;
        }
        String[] sortArray = sort.split(",");
        String[] orderArray = order.split(",");
        for (int i = 0; i < sortArray.length; i++) {
            if (sortArray[i] != null && !"".equals(sortArray[i])) {
                sb.append(sortArray[i]).append(" ");
                if (i < orderArray.length && orderArray[i] != null) {
                    sb.append(orderArray[i]);
                }
                sb.append(",");
            }
        }
        return sb.substring(0, sb.length() - 1);
    }

    public static <T extends Object> Object[] trans2Array(T obj) {
        if (obj == null) {
            return null;
        }
        List<Object> list = new LinkedList<>();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor property : propertyDescriptors) {
                String key = property.getName();
                if (!"class".equals(key) && !"pageNo".equals(key) && !"pageSize".equals(key)) {
                    Method getter = property.getReadMethod();
                    Object value = getter.invoke(obj);
                    list.add(key);
                    list.add(value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list.toArray();
    }

    public static Map<String, Object> trans2Map(MetaModel metaModel, Object obj) {
        return trans2Map(ModelDelegate.attributeNames(metaModel.getModelClass()), obj);
    }

    public static Map<String, Object> trans2Map(Set<String> set, Object obj) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (Objects.isNull(obj)) {
                return result;
            }
            BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor property : propertyDescriptors) {
                String key = property.getName();
                if (set.contains(key.toUpperCase())) {
                    Method getter = property.getReadMethod();
                    Object value = getter.invoke(obj);
                    if (value != null) {
                        result.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 首字母大写
     */
    public static String toUpperFirstCode(String str) {
        String[] strs = str.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String strTmp : strs) {
            char[] ch = strTmp.toCharArray();
            if (ch[0] >= 'a' && ch[0] <= 'z') {
                ch[0] = (char) (ch[0] - 32);
            }
            String strT = new String(ch);
            sb.append(strT).append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * 下划线转驼峰
     */
    public static String lineToHump(String str) {
        str = str.toLowerCase();
        Matcher matcher = linePattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 驼峰转下划线
     */
    public static String humpToLine(String str) {
        Matcher matcher = humpPattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 获取堆栈信息
     *
     * @param t - throwable to convert.
     * @return message and stack trace converted to string.
     */
    public static String getStackTraceString(Throwable t) {
        return Stream.concat(Stream.of(String.valueOf(t)), Arrays.stream(t.getStackTrace()).map(e -> String.valueOf(e)))
                .filter(e -> {
                    return FILTER.stream().anyMatch(prefix -> e.startsWith(prefix));
                }).limit(20).collect(Collectors.joining("\n\tat "));
    }

    public static List<Map<String, Object>> convertList(ResultSet rs, String... columns) {
        List<Map<String, Object>> array = new ArrayList<>();
        try {
            ResultSetMetaData md = rs.getMetaData();
            int columnCount = md.getColumnCount();
            while (rs.next()) {
                Map<String, Object> rowData = new HashMap<>();
                if (columns.length == 0) {
                    for (int i = 1; i <= columnCount; i++) {
                        rowData.put(md.getColumnName(i), rs.getObject(i));
                    }
                } else {
                    for (int i = 0; i < columns.length; i++) {
                        rowData.put(columns[i], rs.getObject(columns[i]));
                    }
                }
                array.add(rowData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return array;
    }
}
