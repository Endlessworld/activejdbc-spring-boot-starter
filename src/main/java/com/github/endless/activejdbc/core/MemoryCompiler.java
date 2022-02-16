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

package com.github.endless.activejdbc.core;

import com.github.endless.activejdbc.constant.Keys;
import com.github.endless.activejdbc.model.BaseModel;
import com.github.endless.activejdbc.query.Utils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;
import lombok.extern.log4j.Log4j2;
import org.javalite.activejdbc.*;
import org.javalite.activejdbc.annotations.DbName;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;
import org.javalite.common.Convert;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ClassUtils;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/***
 * @author Endless
 * @date 2020 年10月2日
 */
@SuppressWarnings("all")
@Log4j2
public class MemoryCompiler {

    private static final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    private static final String TABLE_CAT = "TABLE_CAT", TABLE_NAME = "TABLE_NAME", TABLE_SCHEM = "TABLE_SCHEM", COLUMN_NAME = "COLUMN_NAME";

    static {
        taskExecutor.setCorePoolSize(10);
        taskExecutor.setMaxPoolSize(10);
        taskExecutor.setQueueCapacity(200);
        taskExecutor.setKeepAliveSeconds(10);
        taskExecutor.setThreadNamePrefix("taskExecutor-");
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(60);
        taskExecutor.initialize();
    }

    public static void main(String[] args) {
        compiler();
    }

    public static ThreadPoolTaskExecutor taskExecutor() {
        return taskExecutor;
    }

    public static <T extends Model> void invoke(List<Class<? extends Model>> modelClass) {
        Configuration configuration = Registry.instance().getConfiguration();
        Map<String, List<String>> modelsMap = ContextHelper.getField(Registry.instance().getConfiguration(), "modelsMap", Map.class);
        modelClass.stream().collect(Collectors.groupingBy(e -> e.getAnnotation(DbName.class).value())).forEach((k, v) -> {
            modelsMap.put(k, v.stream().map(c -> c.getName()).collect(Collectors.toList()));
        });
        ContextHelper.setField(configuration, configuration.getClass(), "modelsMap", modelsMap);
        Map<String, List<Class<? extends Model>>> modelClasses = new HashMap();
        modelsMap.forEach((k, v) -> modelClasses.put(k, modelClass.stream().filter(e -> k.equals(e.getAnnotation(DbName.class).value())).collect(Collectors.toList())));
        modelClasses.forEach((k, v) -> v.forEach(m -> log.info("invoke {} : {}", k, m)));
        ContextHelper.setField(null, ModelFinder.class, "modelClasses", modelClasses);
    }

    public static void compiler() {
        ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
        Map<String, Class<Model>> contextModels = ApplicationContextHelper.getContextModels();
        getPrimaryKeys().parallelStream().map(e -> {
            return assistCompiler(fullClassName(e.get(TABLE_NAME)), e, classLoader);
        }).filter(clazz -> clazz != null).collect(Collectors.toList()).forEach(clazz -> {
            if (!contextModels.containsKey(clazz.getAnnotation(Table.class).value())) {
                contextModels.put(clazz.getAnnotation(Table.class).value(), (Class<Model>) clazz);
                log.info("init model class {} {}", clazz.getAnnotation(DbName.class).value(), clazz);
            }
        });
        invoke(new ArrayList<>(contextModels.values()));
    }

    public static Class<? extends Model> assistCompiler(String className, Map<String, String> javaScript, ClassLoader springClassLoader) {
        try {
            log.info("{} compiler >>>{}", className, javaScript);
            ClassPool assist = new ClassPool(true);
            assist.appendClassPath(new LoaderClassPath(springClassLoader));
            CtClass superClass = assist.getCtClass(BaseModel.class.getName());
            CtClass ctClass = assist.makeClass(className);
            ctClass.setSuperclass(superClass);
            CtConstructor constructor = new CtConstructor(new CtClass[]{}, ctClass);
            constructor.setBody("{}");
            ctClass.addConstructor(constructor);
            CtMethod modelGetClass = assist.getCtClass(Model.class.getName()).getDeclaredMethod("modelClass");
            CtMethod newGetClass = CtNewMethod.copy(modelGetClass, ctClass, null);
            newGetClass.setBody("{return " + className + ".class;}");
            ctClass.addMethod(newGetClass);
            ctClass.defrost();
            ByteArrayClassLoader loader = AccessController.doPrivileged((PrivilegedAction<ByteArrayClassLoader>) () -> new ByteArrayClassLoader(ClassUtils.getDefaultClassLoader()));
            addAnnotation(ctClass, javaScript);
//            ctClass.writeFile();
            return ctClass.toClass(loader, ctClass.getClass().getProtectionDomain());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static CtClass addAnnotation(CtClass ctClass, Map<String, String> javaScript) throws Exception {
        String idName = javaScript.get(COLUMN_NAME), tableName = javaScript.get(TABLE_NAME), tableSchema = javaScript.get(TABLE_CAT);
        ClassFile classFile = ctClass.getClassFile();
        ConstPool constpool = classFile.getConstPool();
        AnnotationsAttribute annotationsAttribute = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
        Annotation annotationIdName = new Annotation(IdName.class.getName(), constpool);
        annotationIdName.addMemberValue("value", new StringMemberValue(idName.toLowerCase(), constpool));
        Annotation annotationTable = new Annotation(Table.class.getName(), constpool);
        annotationTable.addMemberValue("value", new StringMemberValue(tableName.toLowerCase(), constpool));
        Annotation annotationDbName = new Annotation(DbName.class.getName(), constpool);
//        Annotation annotationCached = new Annotation(Cached.class.getName(), constpool);
        annotationsAttribute.addAnnotation(annotationIdName);
        annotationsAttribute.addAnnotation(annotationTable);
//        annotationsAttribute.addAnnotation(annotationCached);
        if (tableSchema != null) {
            annotationDbName.addMemberValue("value", new StringMemberValue(tableSchema, constpool));
            annotationsAttribute.addAnnotation(annotationDbName);
        }
        ctClass.getClassFile().addAttribute(annotationsAttribute);
        return ctClass;
    }

    /**
     * 类名
     */
    private static String modelClassName(String tableName) {
        return Utils.toUpperFirstCode(Utils.lineToHump(tableName)) + "PO";
    }

    /**
     * 包名+类名
     */
    private static String fullClassName(String tableName) {
        return MessageFormat.format(Keys.MSG_CLASS_NAME, modelClassName(tableName));
    }

    public static Set<Map<String, String>> getPrimaryKeys() {
        log.info("dataSourceKeys {} ", ApplicationContextHelper.dataSourceKeys);
        Set<Map<String, String>> primaryKeys = ApplicationContextHelper.dataSourceKeys.stream()
                .map(Convert::toString)
                .filter(Objects::nonNull)
                .map(dbName -> getTables(dbName).parallelStream()
                        .map(MemoryCompiler::getAllPrimaryKeys)
                        .reduce(Sets.newHashSet(), Sets::union)
                ).reduce(Sets.newHashSet(), Sets::union);
        primaryKeys.forEach(e -> log.info("table primaryKey  {} ", e));
        return primaryKeys;
    }

    /**
     * 获得一个表的主键信息
     */
    public static Set<Map<String, String>> getAllPrimaryKeys(Map<String, String> tableInfo) {
        try {
            if (tableInfo == null) {
                return Collections.emptySet();
            }
            String schemaName = tableInfo.get(TABLE_CAT);
            schemaName = schemaName == null ? tableInfo.get(TABLE_SCHEM) : schemaName;
            String tableName = tableInfo.get(TABLE_NAME);
            DB db = ContextHelper.openConnection(schemaName);
            DatabaseMetaData dbMetaData = db.connection().getMetaData();
            List<Map<String, Object>> primaryKeys = Utils.convertList(dbMetaData.getPrimaryKeys(null, null, tableName));
            primaryKeys = primaryKeys.stream().filter(Objects::nonNull).collect(Collectors.toList());
            HashSet<Map<String, String>> hashSet = Sets.newHashSet(Lists.transform(primaryKeys, e -> Maps.transformValues(e, v -> Convert.toString(v))));
            hashSet.forEach(e -> log.info("transform table {} ", e));
            return hashSet;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return Collections.emptySet();
    }

    private static List<Map<String, String>> getTables(String schemaName) {
        try {
            log.info("getTables {} ", schemaName);
            DB db = ContextHelper.openConnection(schemaName);
            DatabaseMetaData dbMetaData = db.connection().getMetaData();
            ResultSet tableSet = dbMetaData.getTables(schemaName, null, null, new String[]{"TABLE"});
            List<Map<String, Object>> tables = Utils.convertList(tableSet, TABLE_SCHEM, TABLE_CAT, TABLE_NAME);
            tables.forEach(e -> log.info("find table {} ", e));
            List<Map<String, String>> transform = Lists.transform(tables, e -> Maps.transformValues(e, v -> {
                return v == null ? schemaName : Convert.toString(v);
            }));
            transform.forEach(e -> log.info("transform table {} ", e));
            return transform;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        log.info("getTables emptyList");
        return Collections.emptyList();
    }

    public static List<String> getAllSchemas(Map.Entry<String, DB> db) throws SQLException {
        DatabaseMetaData dbMetaData = db.getValue().connection().getMetaData();
        List<Map<String, Object>> catalogs = Utils.convertList(dbMetaData.getCatalogs(), TABLE_CAT);
        if (!catalogs.isEmpty()) {
            return catalogs.stream().map(e -> Convert.toString(e.get(TABLE_CAT))).collect(Collectors.toList());
        }
        return Utils.convertList(dbMetaData.getSchemas()).stream().map(e -> Convert.toString(e.get(TABLE_SCHEM))).collect(Collectors.toList());
    }
}
