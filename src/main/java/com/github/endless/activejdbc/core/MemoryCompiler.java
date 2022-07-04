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
import org.javalite.common.JsonHelper;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.github.endless.activejdbc.query.Utils.convertList;

/***
 * @author Endless
 * @date 2020 年10月2日
 */
@SuppressWarnings("all")
@Log4j2
public class MemoryCompiler {

	private static final String TABLE_CAT = "TABLE_CAT", TABLE_NAME = "TABLE_NAME", TABLE_SCHEM = "TABLE_SCHEM", COLUMN_NAME = "COLUMN_NAME";


	public static void main(String[] args) {
		invokeActive();
	}


	private static Map<String, ColumnMetadata> fetchMetaParams(DatabaseMetaData databaseMetaData, String dbType, String table) {
		Method fetchMetaParams = ReflectionUtils.findMethod(Registry.class, "fetchMetaParams", DatabaseMetaData.class, String.class, String.class);
		ReflectionUtils.makeAccessible(fetchMetaParams);
		return (Map<String, ColumnMetadata>) ReflectionUtils.invokeMethod(fetchMetaParams, Registry.instance(), databaseMetaData, dbType, table);
	}

	private static void registerColumnMetadata(String table, Map<String, ColumnMetadata> metaParams) {
		Method registerColumnMetadata = ReflectionUtils.findMethod(Registry.class, "registerColumnMetadata", String.class, Map.class);
		ReflectionUtils.makeAccessible(registerColumnMetadata);
		ReflectionUtils.invokeMethod(registerColumnMetadata, Registry.instance(), table, metaParams);
	}

	private static void registerModels(String dbName, Set<Class<? extends Model>> modelClasses, String dbType) {
		Method registerModels = ReflectionUtils.findMethod(Registry.class, "registerModels", String.class, Set.class, String.class);
		ReflectionUtils.makeAccessible(registerModels);
		ReflectionUtils.invokeMethod(registerModels, Registry.instance(), dbName, modelClasses, dbType);
	}

	public static void invokeActive() {
		Set<MetaModel> metaModels = getModelsForDb();
		Collector<MetaModel, ?, Set<String>> mapping = Collectors.mapping(e -> e.getModelClass().getSimpleName(), Collectors.toSet());
		Map<String, Set<String>> modelMap = metaModels.stream().collect(Collectors.groupingBy(MetaModel::getDbName, mapping));
		ContextHelper.setField(null, ModelFinder.class, "modelMap", modelMap);
	}

	static Set<MetaModel> getModelsForDb() {
		Set<MetaModel> metaModels = new HashSet<>();
		ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
		Map<String, Class<Model>> contextModels = ApplicationContextHelper.getContextModels();
		Map<String, DataSource> dataSources = ApplicationContextHelper.getBeansOfType(DataSource.class);
		Set<Map<String, String>> primaryKeys = Sets.newHashSet();
		for (Map.Entry<String, DataSource> dataSourceEntry : dataSources.entrySet()) {
			try {
				log.info("lookup dataSource :{} ", dataSourceEntry);
				Set<MetaTable> metaTables = getTablesForDb(dataSourceEntry);
				Connection connection = DataSourceUtils.doGetConnection(dataSourceEntry.getValue());
				DatabaseMetaData databaseMetaData = connection.getMetaData();
				String dbType = connection.getMetaData().getDatabaseProductName();
				String dbName = connection.getCatalog();
				ApplicationContextHelper.dataSourceKeys.add(connection.getCatalog());
				Set<String> initedDbs = ContextHelper.getField(Registry.instance(), "initedDbs", Set.class);
				initedDbs.add(dbName);
				for (MetaTable metaTable : metaTables) {
					Class<? extends Model> modelClass = getDynamicModelClass(metaTable, classLoader);
					if (modelClass != null && !modelClass.equals(Model.class) && Model.class.isAssignableFrom(modelClass)) {
						if (!contextModels.containsKey(modelClass.getAnnotation(Table.class).value())) {
							contextModels.put(modelClass.getAnnotation(Table.class).value(), (Class<Model>) modelClass);
							MetaModel metaModel = metaModelOf(metaTable.dbName, modelClass, dbType);
							metaModels.add(metaModel);
							registerModels(metaTable.dbName, Set.of(modelClass), dbType);
							registerColumnMetadata(metaTable.tableName, fetchMetaParams(databaseMetaData, dbType, metaTable.tableName));
							log.info("initialized table {} > model {}", metaTable.tableName, modelClass);
						}
					} else {
						throw new InitException("invalid class in the models list: " + modelClass.getName());
					}
				}
				DataSourceUtils.releaseConnection(connection, dataSourceEntry.getValue());
			} catch (Exception e) {
				throw new InitException(e);
			}
		}

		return metaModels;
	}

	public static Set<MetaTable> getTablesForDb(Map.Entry<String, DataSource> dataSourceEntry) throws SQLException {
		Connection connection = DataSourceUtils.doGetConnection(dataSourceEntry.getValue());
		if (connection == null) {
			throw new DBException("Failed to retrieve metadata from DB, connection: '" + dataSourceEntry.getKey() + "' is not available");
		}
		DatabaseMetaData databaseMetaData = connection.getMetaData();
		String schema = getConnectionSchema(databaseMetaData);
		String catalog = getConnectionCatalog(databaseMetaData);
		ResultSet result = databaseMetaData.getTables(catalog, schema, null, org.javalite.common.Collections.arr("TABLE"));
		List<Map<String, Object>> listMap = convertList(result);
		Set<MetaTable> metaTables = new HashSet<>();
		for (Map<String, Object> map : listMap) {
			String tableName = Convert.toString(map.get("TABLE_NAME"));
			List<Map<String, Object>> primaryKeys = convertList(databaseMetaData.getPrimaryKeys(catalog, schema, tableName));
			primaryKeys.stream().findFirst().ifPresent(primaryKey -> {
				MetaTable metaTable = new MetaTable();
				try {
					metaTable.setDbName(connection.getCatalog());
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
				metaTable.setTableName(tableName);
				metaTable.setIdName(Convert.toString(primaryKey.get("COLUMN_NAME")));
				metaTables.add(metaTable);
				log.info("find table: {}", JsonHelper.toJsonString(metaTable));
			});
		}
		DataSourceUtils.releaseConnection(connection, dataSourceEntry.getValue());
		return metaTables;
	}

	static <T extends Model> MetaModel metaModelOf(String dbName, Class<? extends Model> modelClass, String dbType) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		Constructor<?> constructor = MetaModel.class.getDeclaredConstructors()[0];
		ReflectionUtils.makeAccessible(constructor);
		return (MetaModel) constructor.newInstance(dbName, modelClass, dbType);
	}

	public static Class<? extends Model> getDynamicModelClass(MetaTable metaTable, ClassLoader classLoader) {
		try {
			String className = fullClassName(metaTable.tableName);
			ClassPool assist = new ClassPool(true);
			assist.appendClassPath(new LoaderClassPath(classLoader));
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
			ByteArrayClassLoader loader = new ByteArrayClassLoader(ClassUtils.getDefaultClassLoader());
			addAnnotation(ctClass, metaTable);
			ctClass.writeFile();
			return (Class<? extends Model>) ctClass.toClass(loader, ctClass.getClass().getProtectionDomain());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static CtClass addAnnotation(CtClass ctClass, MetaTable metaTable) throws Exception {
		String idName = metaTable.getIdName();
		String tableName = metaTable.getTableName();
		String dbName = metaTable.getDbName();
		ClassFile classFile = ctClass.getClassFile();
		ConstPool constpool = classFile.getConstPool();
		AnnotationsAttribute annotationsAttribute = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
		/**
		 * IdName注解
		 */
		Annotation annotationIdName = new Annotation(IdName.class.getName(), constpool);
		annotationIdName.addMemberValue("value", new StringMemberValue(idName.toLowerCase(), constpool));
		annotationsAttribute.addAnnotation(annotationIdName);
		/**
		 * Table注解
		 */
		Annotation annotationTable = new Annotation(Table.class.getName(), constpool);
		annotationTable.addMemberValue("value", new StringMemberValue(tableName.toLowerCase(), constpool));
		annotationsAttribute.addAnnotation(annotationTable);
		/**
		 * DbName注解
		 */
		Annotation annotationDbName = new Annotation(DbName.class.getName(), constpool);
		annotationDbName.addMemberValue("value", new StringMemberValue(dbName, constpool));
		annotationsAttribute.addAnnotation(annotationDbName);
//        Annotation annotationCached = new Annotation(Cached.class.getName(), constpool);
//        annotationsAttribute.addAnnotation(annotationCached);
		ctClass.getClassFile().addAttribute(annotationsAttribute);
		return ctClass;
	}

	/**
	 * 包名+类名
	 */
	private static String fullClassName(String tableName) {
		return MessageFormat.format(Keys.MSG_CLASS_NAME, Utils.toUpperFirstCode(Utils.lineToHump(tableName)));
	}

	private static String getConnectionSchema(DatabaseMetaData databaseMetaData) throws SQLException {
		try {
			return databaseMetaData.getConnection().getSchema();
		} catch (SQLException e) {
			throw e;
		} catch (Exception ignore) {
		}
		return null;
	}

	private static String getConnectionCatalog(DatabaseMetaData databaseMetaData) throws SQLException {
		try {
			return databaseMetaData.getConnection().getCatalog();
		} catch (SQLException e) {
			throw e;
		} catch (Exception ignore) {
		}
		return null;
	}

}
