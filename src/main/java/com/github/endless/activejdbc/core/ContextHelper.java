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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.endless.activejdbc.annotation.ChildrenClass;
import com.github.endless.activejdbc.annotation.TransferClass;
import com.github.endless.activejdbc.configuration.BizException;
import com.github.endless.activejdbc.constant.Keys;
import com.github.endless.activejdbc.constant.ModelType;
import com.github.endless.activejdbc.core.Paginator.PaginatorBuilder;
import com.github.endless.activejdbc.domains.BaseModelVO;
import com.github.endless.activejdbc.model.BaseModel;
import com.github.endless.activejdbc.query.Helper;
import com.github.endless.activejdbc.query.PageQuery;
import com.github.endless.activejdbc.query.PaginatorQuery;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.*;
import org.javalite.activejdbc.associations.Association;
import org.javalite.common.Convert;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Endless
 */
@Slf4j
public abstract class ContextHelper {

	private static final ThreadLocal<List<DB>> connections = new ThreadLocal<>();
	private static final ThreadLocal<Map<Connection, DataSource>> connectionMap = new ThreadLocal<>();
	private static final Map<Class<? extends Model>, ColumnMetadata> VERSION = new HashMap<>();


	/**
	 * 获取Association
	 */
	public static <T extends Model> List<Association> association(Class<T> modelClass) {
		return ModelDelegate.associations(modelClass);
	}

	/**
	 * 根据List<Map>批量创建或更新
	 * 同步执行-所有数据使用相同事务
	 */
	public static <T extends Model> List<T> batchCreateOrUpdateForMap(Class<T> modelClass, Collection<Map> rows) {
		return rows.stream().map(row -> createItOrUpdate(modelClass, row)).collect(Collectors.toList());
	}

	/**
	 * 根据List<Map>批量创建或更新
	 * 异步并行-每条数据使用独立事物
	 */
	public static <T extends Model> List<T> batchAsyncCreateOrUpdateForMap(Class<T> modelClass, Collection<Map> rows) {
		return join(rows.stream().map((row) -> transaction(() -> createItOrUpdate(modelClass, row))).collect(Collectors.toList()));
	}

	/**
	 * 根据List<VO>批量创建或更新
	 * 同步执行-所有数据使用相同事务
	 */
	public static <T extends Model, V extends BaseModelVO> List<T> batchCreateOrUpdateForVO(Class<T> modelClass, Collection<V> rows) {
		return batchCreateOrUpdateForMap(modelClass, toListMap(metaModelOf(modelClass), rows));
	}

	/**
	 * 根据List<VO>批量创建或更新
	 * 异步并行-每条数据使用独立事物
	 */
	public static <T extends Model, V extends BaseModelVO> List<T> batchAsyncCreateOrUpdateForVO(Class<T> modelClass, Collection<V> rows) {
		return batchAsyncCreateOrUpdateForMap(modelClass, toListMap(metaModelOf(modelClass), rows));
	}

	/**
	 * 批量保存 忽略乐观锁限制
	 */
	public static <T extends Model> List<? extends Model> batchAsyncCreateOrUpdateIgnoreVersion(List<T> models) {
		if (models == null || models.isEmpty()) {
			return null;
		}
		Class<? extends Model> modelClass = modelClass(models.stream().findFirst().get());
		List<Map> list = models.stream().filter(Objects::nonNull).map(e -> {
			return e.set(Keys.RECORD_VERSION, null).toMap();
		}).collect(Collectors.toList());
		return batchAsyncCreateOrUpdateForMap(modelClass, list);
	}

	/**
	 * 根据List<Model>批量创建或更新
	 */
	public static <T extends Model> Integer batchCreateOrUpdate(List<T> rows, ModelType modelType) {
		if (rows == null) {
			return 0;
		}
		DB db = ContextHelper.openConnection(modelType.getName());
		Integer rowsSize = new Partition<T>(rows, 500).stream()
		                                              .map(list -> list.stream().map(e -> e.toInsert().replace("INSERT INTO", "REPLACE INTO"))
		                                                               .collect(Collectors.joining(";"))).filter(e -> e.length() > 0).map(db::exec)
		                                              .mapToInt(e -> e).sum();
		log.info("replace into total {} ", rowsSize);
		return rowsSize;
	}

	/**
	 * 根据Map创建model但不保存到数据库
	 */
	public static <T extends Model> T create(Class<T> modelClass, Map input) {
		return ModelDelegate.create(modelClass).fromMap(input);
	}

	/**
	 * 根据VO创建model但不保存到数据库
	 */
	public static <T extends Model, V extends BaseModelVO> T create(Class<T> modelClass, V input) {
		return create(modelClass, Helper.trans2Map(metaModelOf(modelClass), input));
	}

	/**
	 * 根据Map创建model并保存到数据库
	 */
	public static <T extends Model> T createIt(Class<T> modelClass, Map input) {
		return saveIt(ModelDelegate.create(modelClass).fromMap(input));
	}

	/**
	 * 根据VO创建model并保存到数据库
	 */
	public static <T extends Model, V extends BaseModelVO> T createIt(Class<T> modelClass, V input) {
		return createIt(modelClass, Helper.trans2Map(metaModelOf(modelClass), input));
	}

	/**
	 * 根据Map创建或更新
	 */
	public static <T extends Model> T createItOrUpdate(Class<T> modelClass, Map input) {
		return StringUtils.isEmpty(input.get(idNameOf(modelClass))) ? createIt(modelClass, input) : saveItById(modelClass, input);
	}

	/**
	 * 如果数据库存在匹配的数据，则返回第一条， 不存在则创建并返回
	 */
	public static <T extends Model> T findOrInit(Class<T> modelClass, Map input) {
		return ModelDelegate.findOrInit(modelClass, Helper.trans2Array(input));
	}

	/**
	 * 如果数据库存在匹配的数据，则返回第一条， 不存在则创建并返回
	 */
	public static <T extends Model, V extends BaseModelVO> T findOrInit(Class<T> modelClass, V input) {
		return findOrInit(modelClass, Helper.trans2Map(metaModelOf(modelClass), input));
	}

	/**
	 * 根据VO创建或更新
	 */
	public static <T extends Model, V extends BaseModelVO> T createItOrUpdate(Class<T> modelClass, V input) {
		return createItOrUpdate(modelClass, Helper.trans2Map(metaModelOf(modelClass), input));
	}

	/**
	 * 展开Map中的Map input
	 */
	public static <K, V> Map<K, V> deLayer(Map<K, V> input) {
		if (input.containsKey("input")) {
			input.putAll(toVO(Convert.toString(input.get("input")), HashMap.class));
		}
		return input;
	}

	/**
	 * 逻辑删除 0 ：未删除 1 ： 已删除
	 */
	public static <T extends Model> T deleteTag(T model) {
		try {
			assertNotNull(model);
			if (ModelDelegate.attributeNames(model.getClass()).contains(Keys.SQL_DELETE_FILED)) {
				model.set(Keys.SQL_DELETE_FILED, Keys.SQL_IS_DELETE).saveIt();
			}
			return model;
		} catch (Exception e) {
			log.error(Keys.LOG_MSG_FAILED_TO_DELETE, e);
			throw new BizException(Keys.LOG_MSG_FAILED_TO_DELETE + e.getMessage());
		}
	}

	/**
	 * 根据ID删除
	 */
	public static <T extends Model> T deleteTagById(Class<T> modelClass, Long input) {
		return deleteTag(findById(modelClass, input));
	}

	/**
	 * 根据ID数组删除
	 */
	public static <T extends Model> List deleteTagById(Class<T> modelClass, Long... input) {
		return Arrays.stream(input).map(i -> deleteTagById(modelClass, i)).collect(Collectors.toList());
	}

	/**
	 * 根据多个条件删除
	 */
	public static <T extends Model> T deleteTagById(Class<T> modelClass, Map input) {
		return deleteTag(findById(modelClass, input));
	}

	/**
	 * 获取提交参数的stream对象
	 */
	public static Stream<Map.Entry<String, Object>> entryStream(Map input) {
		return input.entrySet().stream();
	}

	/**
	 * 如果表的元数据中包含该键值对的key值 并且其value值不为空 则使用此键值对作为筛选条件
	 */
	public static boolean existKey(ColumnMetadata metaData, Map input) {
		return (input.containsKey(metaData.getColumnName()) || input.containsKey(metaData.getColumnName()
		                                                                                 .toLowerCase())) && (((input.get(metaData.getColumnName()) != null && input.get(metaData.getColumnName()) != "")) || ((input.get(metaData.getColumnName()
		                                                                                                                                                                                                                          .toLowerCase()) != "")));
	}

	/**
	 * 根据表的元数据与提交的参数过滤出匹配的key
	 */
	public static <T extends Model> Stream<String> filterKeys(Class<T> modelClass, Map input) {
		return keysStream(modelClass).filter(metaData -> existKey(metaData, deLayer(input))).map(mapper -> mapper.getColumnName());
	}

	/**
	 * 根据ID查询
	 */
	public static <T extends Model> T findById(Class<T> modelClass, Map input) {
		Object id = input.get(idNameOf(modelClass));
		if (id == null) {
			log.error(Keys.LOG_MSG_ID_CANNOT_BE_NULL_A, idNameOf(modelClass));
			throw new BizException(Keys.LOG_MSG_ID_CANNOT_BE_NULL_B + idNameOf(modelClass));
		}
		return findById(modelClass, id);
	}

	/**
	 * 根据ID查询
	 */
	public static <T extends Model> T findById(Class<T> modelClass, final Object id) {
		return assertNotNull(ModelDelegate.findFirst(modelClass, idNameOf(modelClass) + Keys.SQL_WHERE_SINGLE_PLACEHOLDER, id));
	}

	/**
	 * 加载第一条数据
	 */
	public static <T extends Model> T findFirst(Class<T> modelClass, Map input) {
		return ModelDelegate.findFirst(modelClass, query(modelClass, input, true), getParams(modelClass, input, true).toArray());
	}

	/**
	 * VO转model
	 */
	public static <T extends Model> T fromVO(Class<T> modelClass, BaseModelVO input) {
		return ModelDelegate.create(modelClass).fromMap(Helper.trans2Map(ModelDelegate.metaModelOf(modelClass), input));
	}

	/**
	 * 根据model获取一个或多个表的字段名（大写）
	 */
	public static Set getAttributeNames(Class<? extends Model>... modelClass) {
		return Stream.of(modelClass).map(ModelDelegate::metaModelOf).map(MetaModel::getAttributeNamesSkipGenerated)
		             .reduce(new HashSet<>(), Helper::merge);
	}


	/**
	 * 根据主键获取一个关联子表的数据, 并加载这个子表的关联子表数据数据（如果有）
	 */
	public static <T extends Model> LazyList<Model> getChildren(T parent, Class<? extends Model> childrenClass) {
		String subQuery = idNameOf(modelClass(parent)) + Keys.SQL_WHERE_SINGLE_PLACEHOLDER;
		log.info("{} | {} | {} | {}", childrenClass, modelClass(parent), subQuery, parent.getLongId());
		return ModelDelegate.where(childrenClass, subQuery, parent.getLongId()).include(getChildrenClass(childrenClass));
	}

	/**
	 * 获取model的关联子类
	 */
	public static <T extends Model> Class<T>[] getChildrenClass(Class<T> modelClass) {
		if (modelClass.getAnnotation(ChildrenClass.class) == null) {
			return new Class[]{};
		}
		return (Class<T>[]) modelClass.getAnnotation(ChildrenClass.class).value();
	}

	/**
	 * 获取model的关联子类
	 */
	public static <T extends Model> Class<T>[] getChildrenClass(T model) {
		return (Class<T>[]) getChildrenClass(modelClass(model));
	}

	/**
	 * 根据表的元数据过滤提交的参数
	 */
	public static <T extends Model> List<String> getParams(Class<T> modelClass, Map input, boolean isEqual) {
		return filterKeys(modelClass, input).map(key -> input.get(key.toLowerCase()) + (isEqual ? Keys.EMPTY : Keys.SQL_FIELD_LIKE))
		                                    .collect(Collectors.toList());
	}

	/**
	 * 获取自增长主键id列名
	 */
	public static <T extends Model> String idNameOf(Class<T> modelClass) {
		return metaModelOf(modelClass).getIdName();
	}

	/**
	 * 树形加载子表数据 最多共2层 <br>
	 */
	public static <T extends Model> T includeAll(Class<T> modelClass, final Object id) {
		Long count = ModelDelegate.count(modelClass, idNameOf(modelClass) + Keys.SQL_WHERE_SINGLE_PLACEHOLDER, id);
		if (count <= 0) {
			throw new BizException(Keys.LOG_MSG_OBJECT_NOT_EXISTS);
		}
		LazyList<T> model = ModelDelegate.where(modelClass, idNameOf(modelClass) + Keys.SQL_WHERE_SINGLE_PLACEHOLDER, id);
		return (T) model.include(getChildrenClass(modelClass)).limit(1).get(0);
	}

	/**
	 * 树形加载子表数据 最多共3层 <br>
	 */
	public static <T extends Model> T includeAll(T model) {
		Stream.of(getChildrenClass(model)).filter(child -> ModelDelegate.belongsTo(child, modelClass(model))).forEach(childrenClass -> {
			LazyList<Model> all = getChildren(model, childrenClass);
			all.forEach(ContextHelper::includeAll);
			((BaseModel) model).include(childrenClass, all);
		});
		return model;
	}

	/**
	 * 树形加载子表数据 最多共3层 <br>
	 */
	public static <T extends Model> T includeAllChildren(T model) {
		Stream.of(getChildrenClass(model)).filter(child -> ModelDelegate.belongsTo(child, modelClass(model))).forEach(childrenClass -> {
			((BaseModel) model).include(childrenClass, getChildren(model, childrenClass));
		});
		return model;
	}

	/**
	 * 根据VO分页查询并并导入指定关联表数据
	 */
	public static <T extends Model, V extends BaseModelVO, K> PageQuery includePageQuery(Class<T> modelClass, BaseModelVO input, boolean islike, Class<T>... otherClass) {
		return includePageQuery(modelClass, Helper.trans2Map(metaModelOf(modelClass), input), islike, otherClass);
	}

	/**
	 * 根据Map分页查询 并导入指定关联表数据
	 *
	 * @param input      参数
	 * @param islike     是否模糊匹配
	 * @param otherClass 关联表
	 */
	public static <T extends Model> PageQuery includePageQuery(Class<T> modelClass, Map input, boolean islike, Class<? extends Model>... otherClass) {
		Paginator<T> paginator = queryBuilder(modelClass, input, islike).create();
		return paginator.apply(otherClass);
	}


	/**
	 * 分页查询生成器，根据参数和modelClass生成一个分页查询器
	 *
	 * @param input   参数
	 * @param isEqual 是否模糊匹配
	 */
	public static <T extends Model> PaginatorBuilder queryBuilder(Class<T> modelClass, Map input, boolean isEqual) {
		PaginatorQuery pagehelper = analysis(input);
		input = deLayer(input);
		PaginatorBuilder<Model> paginator = Paginator.instance().countQuery(Keys.SQL_WHERE_DEFAULT).modelClass((Class<Model>) modelClass)
		                                             .orderBy(pagehelper.getOrderBy()).pageSize(pagehelper.getPageSize())
		                                             .currentPageIndex(pagehelper.getPageNum(), true);
		return paginator.params(getParams(modelClass, input, isEqual).toArray()).query(query(modelClass, input, isEqual));
	}

	/**
	 * @param input   参数
	 * @param isEqual 是否全等匹配
	 */
	public static <T extends Model> String query(Class<T> modelClass, Map input, boolean isEqual) {
		String query = !isEqual ? subQuery(modelClass, input) : subQueryNotLike(modelClass, input);
		return getParams(modelClass, input, isEqual).isEmpty() ? Keys.SQL_WHERE_DEFAULT : query;
	}

	/**
	 * 非空断言
	 */
	public static <T extends Model> T assertNotNull(T model) {
		contextAssert(ObjectUtils.isEmpty(model), Keys.LOG_MSG_OBJECT_NOT_EXISTS);
		return model;
	}

	/**
	 * 业务断言
	 */
	public static void contextAssert(boolean flag, String msg) {
		if (flag) {
			throw new BizException(msg);
		}
	}

	/**
	 * 获取表的元数据的stream对象
	 */
	public static <T extends Model> Stream<ColumnMetadata> keysStream(Class<T> modelClass) {
		return ModelDelegate.metaModelOf(modelClass).getColumnMetadata().values().stream();
	}

	/**
	 * 获取model对应表的元数据
	 */
	public static <T extends Model> MetaModel metaModelOf(Class<T> modelClass) {
		return ModelDelegate.metaModelOf(modelClass);
	}

	/**
	 * 获取model对应表的元数据
	 */
	public static <T extends Model> MetaModel metaModelOf(T model) {
		return metaModelOf(modelClass(model));
	}

	/**
	 * 获取modelClass
	 */
	public static <T extends Model> Class<? extends Model> modelClass(T model) {
		return model.getClass();
	}

	/**
	 * 根据VO分页查询
	 */
	public static <T extends Model, V extends BaseModelVO> PageQuery<V> pageQuery(Class<T> modelClass, BaseModelVO input, boolean... isLike) {
		return pageQuery(modelClass, toUpperKey(Helper.trans2Map(metaModelOf(modelClass), input)), isLike);
	}

	/**
	 * 根据Map分页查询
	 *
	 * @param isLike 是否模糊匹配
	 */
	public static <T extends Model> PageQuery pageQuery(Class<T> modelClass, Map input, boolean... isLike) {
		Paginator<T> paginator = queryBuilder(modelClass, input, isLike.length == 0 || isLike[0]).create();
		return paginator.apply();
	}

	/**
	 * Map转model并保存
	 */
	public static <T extends Model> T saveIt(T model, Map input) {
		return saveIt(model.fromMap(input));
	}

	/**
	 * VO转model并保存
	 */
	public static <T extends Model, V extends BaseModelVO> T saveIt(T model, V input) {
		return saveIt(model.fromMap(Helper.trans2Map(metaModelOf(modelClass(model)), input)));
	}

	/**
	 * 根据Map更新到数据库 id不可为空
	 */
	public static <T extends Model> T saveItById(Class<T> modelClass, Map input) {
		return saveIt(findById(modelClass, input).fromMap(input));
	}

	/**
	 * 根据VO更新到数据库 id不可为空
	 */
	public static <T extends Model, V extends BaseModelVO> T saveItById(Class<T> modelClass, V input) {
		return saveItById(modelClass, Helper.trans2Map(metaModelOf(modelClass), input));
	}


	/**
	 * 保存
	 */
	public static <T extends Model> T saveIt(T model) {
		try {
			assertNotNull(model);
			Set<String> attribute = ModelDelegate.attributeNames(modelClass(model));
			if (attribute.contains(Keys.SQL_DELETE_FILED)) {
				if (!Keys.SQL_IS_NOT_DEL.equals(model.getInteger(Keys.SQL_DELETE_FILED))) {
					model.set(Keys.SQL_DELETE_FILED, Keys.SQL_IS_NOT_DEL);
				}
			}
			if (model.isNew() && attribute.contains(Keys.CREATED_BY)) {
				model.set(Keys.CREATED_BY, ApplicationContextHelper.loginUser());
			}
			if (!model.isNew() && attribute.contains(Keys.UPDATED_BY)) {
				model.set(Keys.UPDATED_BY, ApplicationContextHelper.loginUser());
			}
			model.saveIt();
		} catch (Exception e) {
			e.printStackTrace();
			throw new BizException(Keys.LOG_MSG_FAILED_TO_SAVEIT + e.getMessage(), e);
		}
		return model;
	}

	/**
	 * 根据表的元数据与提交的参数生成where条件<br>
	 * 此方法返回模糊匹配的 条件语句
	 */
	public static <T extends Model> String subQuery(Class<T> modelClass, Map input) {
		String delimiter = isOr(input) ? Keys.SQL_WHERE_DELIMITER_OR_LIKE : Keys.SQL_WHERE_DELIMITER_AND_LIKE;
		return filterKeys(modelClass, input).collect(Collectors.joining(delimiter, Keys.SQL_WHERE_PREFIX, Keys.SQL_WHERE_SUFFIX_LIKE));
	}

	/**
	 * 根据表的元数据与提交的参数生成where条件<br>
	 * 此方法返回精确匹配的 条件语句
	 */
	public static <T extends Model> String subQueryNotLike(Class<T> modelClass, Map input) {
		String delimiter = isOr(input) ? Keys.SQL_WHERE_DELIMITER_OR : Keys.SQL_WHERE_DELIMITER_AND;
		return filterKeys(modelClass, input).collect(Collectors.joining(delimiter, Keys.SQL_WHERE_PREFIX, Keys.SQL_WHERE_SUFFIX));
	}

	/**
	 * 本次查询是否使用或运算，如果返回true则多个条件间以or连接 否则默认and连接
	 */
	public static boolean isOr(Map input) {
		return Keys.TRUE.equals(input.getOrDefault(Keys.IS_OR, false));
	}

	/**
	 * 获取model对应表名
	 */
	public static <T extends Model> String tableNameOf(Class<T> modelClass) {
		return ModelDelegate.tableNameOf(modelClass);
	}

	/**
	 * 集合转where条件 example : [a,b,c] >>> "'a','b','c'"
	 */
	public static <E extends CharSequence> String toJoinString(Collection<E> collection) {
		return collection.stream().collect(Collectors.joining("','", "'", "'"));
	}

	/**
	 * 集合转where条件 example : [a,b,c] >>> "'a','b','c'"
	 */
	public static <E extends CharSequence> String toJoinString(E... collection) {
		return Stream.of(collection).collect(Collectors.joining("','", "'", "'"));
	}

	/**
	 * 集合转where条件 example : [a,b,c] >>> "?,?,?"
	 */
	public static <E> String toHolderString(List<E> collection) {
		return collection.stream().map(e -> "?").collect(Collectors.joining(","));
	}

	/**
	 * 集合转where条件 example : [a,b,c] >>> "?,?,?"
	 */
	public static <E> String toHolderString(E... collection) {
		return Stream.of(collection).map(e -> "?").collect(Collectors.joining(","));
	}

	/**
	 * model 转 json
	 */
	public static <T extends Model> String toJson(T model) {
		if (model == null) {
			return Keys.JSON_EMPTY;
		}
		return model.toJson(true);
	}

	/**
	 * Collection<Model>转 List<Map>
	 */
	public static <T extends Model, V extends BaseModelVO> List<Map> toListMap(Collection<T> rows, String... keys) {
		return rows.stream().map(row -> ((BaseModel) row).toMap(keys)).collect(Collectors.toList());
	}

	/**
	 * Collection<VO>转 List<Map>
	 */
	public static <T extends Model, V extends BaseModelVO> List<Map> toListMap(MetaModel metaModel, Collection<V> rows) {
		return rows.stream().map(row -> Helper.trans2Map(metaModel, row)).collect(Collectors.toList());
	}

	/**
	 * List<Model>转 List<VO>
	 */
	public static <T extends Model, V extends BaseModelVO> List<V> toListVO(LazyList<T> rows) {
		return (List<V>) rows.stream().map(ContextHelper::toVO).collect(Collectors.toList());
	}

	/**
	 * 对象转jsonNode
	 */
	public static <T> JsonNode toJsonNode(T apply) {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.convertValue(apply, JsonNode.class);
	}

	/**
	 * VO转model
	 */
	public static <T extends Model> Model toPO(T model, BaseModelVO input) {
		if (model == null) {
			return null;
		}
		return model.fromMap(Helper.trans2Map(metaModelOf(model), input));
	}

	/**
	 * Map key值转大写
	 */
	public static Map toUpperKey(Map input) {
		return toKeyValue(input, String::toUpperCase);
	}

	/**
	 * Map key值转小写
	 */
	public static Map toLowerkey(Map input) {
		return toKeyValue(input, String::toLowerCase);
	}

	public static Map toKeyValue(Map input, UnaryOperator<String> apply) {
		return entryStream(input).collect(HashMap::new, (n, e) -> n.put(Optional.ofNullable(e.getKey()).map(apply)
		                                                                        .orElse(null), e.getValue()), HashMap::putAll);
	}

	/**
	 * model转VO
	 */
	public static <T extends Model> BaseModelVO toVO(T model) {
		return toVO(transferClass(model), model.toMap());
	}

	/**
	 * json转VO
	 */
	@SneakyThrows
	public static <T> T toVO(String json, Class<T> clazz) {
		ObjectMapper objectMapper = ApplicationContextHelper.getBeanByType(ObjectMapper.class);
		return objectMapper.readValue(json, clazz);
	}

	/**
	 * map转VO
	 */
	@SneakyThrows
	public static <T extends BaseModelVO> T toVO(Class<T> clazz, Map input) {
		return toVO(toJson(input), clazz);
	}

	/**
	 * 泛型对象转json
	 */
	@SneakyThrows
	public static <T> String toJson(T input) {
		ObjectMapper objectMapper = ApplicationContextHelper.getBeanByType(ObjectMapper.class);
		return objectMapper.writeValueAsString(input);
	}

	/**
	 * 获取model对应VO类型
	 */
	public static <T extends Model> Class<? extends BaseModelVO> transferClass(Class<T> modelClass) {
		return AnnotationUtils.findAnnotation(modelClass, TransferClass.class).value();
	}

	/**
	 * 获取model对应VO类型
	 */
	public static <T extends Model> Class<? extends BaseModelVO> transferClass(T model) {
		return transferClass(modelClass(model));
	}


	public static DB initDB(String name) {
		DB db = new DB(name);
		return db.hasConnection() ? db : db.open();
	}

	/**
	 * 根据注解打开数据库链接
	 */
	public static DataSource getDataSource(String modelType) {
		Map<String, DataSource> dataSources = ApplicationContextHelper.getBeansOfType(DataSource.class);
		return dataSources.getOrDefault(modelType, ApplicationContextHelper.getBeanByType(DataSource.class));
	}

	/**
	 * 当前所有链接开启事务
	 */
	public static void openTransaction() {
		connections.get().forEach(DB::openTransaction);
		log.info(Keys.LOG_MSG_OPEN_TRANSACTION, DB.getCurrrentConnectionNames());
	}

	/**
	 * 当前所有链接提交事务
	 */
	public static void commitTransaction() {
		connections.get().forEach(DB::commitTransaction);
		log.info(Keys.LOG_MSG_COMMIT_TRANSACTION, DB.getCurrrentConnectionNames());
	}

	/**
	 * 当前所有链接回滚事务
	 */
	public static void rollbackTransaction() {
		connections.get().forEach(db -> {
			try {
				db.rollbackTransaction();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		log.warn(Keys.LOG_MSG_ROLLBACK_TRANSACTION, DB.getCurrrentConnectionNames());
	}

	/**
	 * 释放当前所有链接
	 */
	public static void releaseConnection() {
		if (!DB.getCurrrentConnectionNames().isEmpty()) {
			log.info(Keys.LOG_MSG_CLOSE_TRANSACTION, DB.getCurrrentConnectionNames());
			connections.get().stream().map(DB::detach).forEach(connection -> {
				DataSourceUtils.releaseConnection(connection, connectionMap.get().get(connection));
			});
		}
	}

	public static void initConnections(String... dbName) {
		connections.set(openConnections(dbName));
	}

	/**
	 * 根据注解打开多个数据库链接
	 */
	public static List<DB> openConnections(String... dbName) {
		return Stream.of(dbName).map(ContextHelper::openConnection).collect(Collectors.toList());
	}

	public static void connectionCache(Connection connection, DataSource dataSource) {
		if (connectionMap.get() == null) {
			connectionMap.set(new HashMap<>());
		}
		connectionMap.get().put(connection, dataSource);
	}

	/**
	 * 根据注解打开数据库链接
	 */
	public static DB openConnection(String modelType) {
		if (DB.getCurrrentConnectionNames().contains(modelType)) {
			return initDB(modelType);
		}
		DataSource dataSource = getDataSource(modelType);
		if (dataSource != null) {
			try {
				Connection connection = DataSourceUtils.doGetConnection(dataSource);
				DB db = new DB(modelType);
				db.attach(connection);
				connectionCache(connection, dataSource);
				return db;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return initDB(modelType);
	}

	/**
	 * 传入一个匿名函数 并返回一个 对传入函数包装了事务管理的匿名函数
	 */
	public static <T> Function<T> transaction(Function<T> apply, String... dbName) {
		return () -> {
			ContextHelper.initConnections(dbName);
			T result;
			try {
				ContextHelper.openTransaction();
				result = apply.apply();
				ContextHelper.commitTransaction();
			} catch (Throwable e) {
				e.printStackTrace();
				ContextHelper.rollbackTransaction();
				throw e;
			} finally {
				ContextHelper.releaseConnection();
			}
			return result;
		};
	}

	/**
	 * 异步执行传入的函数,并进行事务管理
	 */
	public static <T> Future<T> asyncApplyTransaction(Function<T> apply) {
		return asyncApply(transaction(apply));
	}

	/**
	 * 异步执行传入的函数,但不进行事务管理
	 */
	public static <T> CompletableFuture<T> asyncApply(Function<T> apply) {
		ThreadPoolTaskExecutor contextPool = ApplicationContextHelper.getBeanByType(ThreadPoolTaskExecutor.class);
		if (contextPool == null) {
			return CompletableFuture.supplyAsync(apply::apply);
		}
		return CompletableFuture.supplyAsync(apply::apply, contextPool);
	}

	/**
	 * 解析当前请求的分页信息
	 */
	public static PaginatorQuery analysis(Map input) {
		HttpServletRequest request = ApplicationContextHelper.getRequest();
		Integer pageNum = Convert.toInteger(input.getOrDefault(Keys.SQL_PAGE_NUM, 1));
		Integer pageSize = Convert.toInteger(input.getOrDefault(Keys.SQL_PAGE_SIZE, 10));
		String order = request.getParameter(Keys.SQL_PAGE_ODER);
		String sort = request.getParameter(Keys.SQL_PAGE_SORT);
		return new PaginatorQuery().setPageNum(pageNum).setPageSize(pageSize).setOrderBy(Helper.orderBy(sort, order));
	}

	/**
	 * 关闭乐观锁限制
	 */
	@SneakyThrows
	public static <T extends Model> void closeLock(Class<T> modelClass) {
		Field field = MetaModel.class.getDeclaredField("columnMetadata");
		field.setAccessible(true);
		Map mate = (Map) field.get(metaModelOf(modelClass));
		if (mate.containsKey(Keys.RECORD_VERSION)) {
			VERSION.put(modelClass, (ColumnMetadata) mate.remove(Keys.RECORD_VERSION));
		}
		log.info("closeLock {}", modelClass);
	}

	/**
	 * 开启乐观锁限制
	 */
	@SneakyThrows
	public static <T extends Model> void openLock(Class<T> modelClass) {
		Field field = MetaModel.class.getDeclaredField("columnMetadata");
		field.setAccessible(true);
		Map mate = (Map) field.get(metaModelOf(modelClass));
		if (!mate.containsKey(Keys.RECORD_VERSION)) {
			mate.put(Keys.RECORD_VERSION, VERSION.get(modelClass));
			field.set(metaModelOf(modelClass), mate);
		}
		ModelDelegate.update(modelClass, Keys.SQL_RECORD_VERSION_WHERE, Keys.SQL_RECORD_VERSION_IS_NULL, 1);
		log.info("openLock {}", modelClass);
	}

	@SneakyThrows
	public static <T, R> R getField(T instance, String fieldName, Class<R> returnType) {
		Class<?> type = Class.forName(instance.getClass().getTypeName());
		Field field = ReflectionUtils.findField(type, fieldName);
		ReflectionUtils.makeAccessible(field);
		return (R) ReflectionUtils.getField(field, instance);
	}

	public static void refresh() {
		ContextHelper.getField(Registry.instance(), "initedDbs", Set.class).clear();
		MemoryCompiler.invokeActive();
	}

	@SneakyThrows
	public static <T> void setField(T instance, Class<?> clazz, String fieldName, Object value) {
		Field field = clazz.getDeclaredField(fieldName);
		int modifiers = field.getModifiers();
		if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
		}
		field.setAccessible(true);
		ReflectionUtils.setField(field, instance, value);
	}

	/**
	 * 异步并行执行匿名函数集合，并join各线程，待所有线程执行完之后返回各线程执行结果之集合
	 */
	public static <T> List<T> join(List<Function<T>> applys) {
		List<T> result = new ArrayList<>();
		CompletableFuture.allOf(applys.stream().map(apply -> {
			return asyncApply(apply).whenComplete((r, e) -> result.add(r));
		}).toArray(CompletableFuture[]::new)).join();
		return result;
	}

	/**
	 * 根据tableName获取modelClass
	 */
	public static Class<Model> modelClass(String tableName) {
		MetaModel metaModel = Registry.instance().getMetaModel(tableName);
		if (metaModel == null) {
			throw new BizException("tableName:" + tableName + "不存在,或尚未创建model");
		}
		return (Class<Model>) metaModel.getModelClass();
	}
}
