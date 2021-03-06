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
import com.github.endless.activejdbc.annotation.ChildrensClass;
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
import com.google.common.collect.Lists;
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

	private static final Map<Class<? extends Model>, ColumnMetadata> VERSION = new HashMap<>();


	/**
	 * ??????Association
	 */
	public static <T extends Model> List<Association> association(Class<T> modelClass) {
		return ModelDelegate.associations(modelClass);
	}

	/**
	 * ??????List<Map>?????????????????????
	 * ????????????-??????????????????????????????
	 */
	public static <T extends Model> List<T> batchCreateOrUpdateForMap(Class<T> modelClass, Collection<Map> rows) {
		return rows.stream().map(row -> createItOrUpdate(modelClass, row)).collect(Collectors.toList());
	}

	/**
	 * ??????List<Map>?????????????????????
	 * ????????????-??????????????????????????????
	 */
	public static <T extends Model> List<T> batchAsyncCreateOrUpdateForMap(Class<T> modelClass, Collection<Map> rows) {
		return join(rows.stream().map((row) -> transaction(() -> createItOrUpdate(modelClass, row))).collect(Collectors.toList()));
	}

	/**
	 * ??????List<VO>?????????????????????
	 * ????????????-??????????????????????????????
	 */
	public static <T extends Model, V extends BaseModelVO> List<T> batchCreateOrUpdateForVO(Class<T> modelClass, Collection<V> rows) {
		return batchCreateOrUpdateForMap(modelClass, toListMap(metaModelOf(modelClass), rows));
	}

	/**
	 * ??????List<VO>?????????????????????
	 * ????????????-??????????????????????????????
	 */
	public static <T extends Model, V extends BaseModelVO> List<T> batchAsyncCreateOrUpdateForVO(Class<T> modelClass, Collection<V> rows) {
		return batchAsyncCreateOrUpdateForMap(modelClass, toListMap(metaModelOf(modelClass), rows));
	}

	/**
	 * ???????????? ?????????????????????
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
	 * ??????List<Model>?????????????????????
	 */
	public static <T extends Model> Integer batchCreateOrUpdate(List<T> rows, ModelType modelType) {
		if (rows == null) {
			return 0;
		}
		DB db = ContextHelper.openConnection(modelType.getName());
		Integer rowsSize = Lists.partition(Lists.newArrayList(rows), 500).stream().map(list -> list.stream().map(e -> e.toInsert().replace("INSERT INTO", "REPLACE INTO")).collect(Collectors.joining(";"))).filter(e -> e.length() > 0).map(db::exec).mapToInt(e -> e).sum();
		log.info("replace into total {} ", rowsSize);
		return rowsSize;
	}

	/**
	 * ??????Map??????model????????????????????????
	 */
	public static <T extends Model> T create(Class<T> modelClass, Map input) {
		return ModelDelegate.create(modelClass).fromMap(input);
	}

	/**
	 * ??????VO??????model????????????????????????
	 */
	public static <T extends Model, V extends BaseModelVO> T create(Class<T> modelClass, V input) {
		return create(modelClass, Helper.trans2Map(metaModelOf(modelClass), input));
	}

	/**
	 * ??????Map??????model?????????????????????
	 */
	public static <T extends Model> T createIt(Class<T> modelClass, Map input) {
		return saveIt(ModelDelegate.create(modelClass).fromMap(input));
	}

	/**
	 * ??????VO??????model?????????????????????
	 */
	public static <T extends Model, V extends BaseModelVO> T createIt(Class<T> modelClass, V input) {
		return createIt(modelClass, Helper.trans2Map(metaModelOf(modelClass), input));
	}

	/**
	 * ??????Map???????????????
	 */
	public static <T extends Model> T createItOrUpdate(Class<T> modelClass, Map input) {
		return StringUtils.isEmpty(input.get(idNameOf(modelClass))) ? createIt(modelClass, input) : saveItById(modelClass, input);
	}

	/**
	 * ???????????????????????????????????????????????????????????? ???????????????????????????
	 */
	public static <T extends Model> T findOrInit(Class<T> modelClass, Map input) {
		return ModelDelegate.findOrInit(modelClass, Helper.trans2Array(input));
	}

	/**
	 * ???????????????????????????????????????????????????????????? ???????????????????????????
	 */
	public static <T extends Model, V extends BaseModelVO> T findOrInit(Class<T> modelClass, V input) {
		return findOrInit(modelClass, Helper.trans2Map(metaModelOf(modelClass), input));
	}

	/**
	 * ??????VO???????????????
	 */
	public static <T extends Model, V extends BaseModelVO> T createItOrUpdate(Class<T> modelClass, V input) {
		return createItOrUpdate(modelClass, Helper.trans2Map(metaModelOf(modelClass), input));
	}

	/**
	 * ??????Map??????Map input
	 */
	public static <K, V> Map<K, V> deLayer(Map<K, V> input) {
		if (input.containsKey("input")) {
			input.putAll(toVO(Convert.toString(input.get("input")), HashMap.class));
		}
		return input;
	}

	/**
	 * ???????????? 0 ???????????? 1 ??? ?????????
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
	 * ??????ID??????
	 */
	public static <T extends Model> T deleteTagById(Class<T> modelClass, Long input) {
		return deleteTag(findById(modelClass, input));
	}

	/**
	 * ??????ID????????????
	 */
	public static <T extends Model> List deleteTagById(Class<T> modelClass, Long... input) {
		return Arrays.stream(input).map(i -> deleteTagById(modelClass, i)).collect(Collectors.toList());
	}

	/**
	 * ????????????????????????
	 */
	public static <T extends Model> T deleteTagById(Class<T> modelClass, Map input) {
		return deleteTag(findById(modelClass, input));
	}

	/**
	 * ?????????????????????stream??????
	 */
	public static Stream<Map.Entry<String, Object>> entryStream(Map input) {
		return input.entrySet().stream();
	}

	/**
	 * ?????????????????????????????????????????????key??? ?????????value???????????? ???????????????????????????????????????
	 */
	public static boolean existKey(ColumnMetadata metaData, Map input) {
		return (input.containsKey(metaData.getColumnName()) || input.containsKey(metaData.getColumnName().toLowerCase())) && (((input.get(metaData.getColumnName()) != null && input.get(metaData.getColumnName()) != "")) || ((input.get(metaData.getColumnName().toLowerCase()) != null && input.get(metaData.getColumnName().toLowerCase()) != "")));
	}

	/**
	 * ?????????????????????????????????????????????????????????key
	 */
	public static <T extends Model> Stream<String> filterKeys(Class<T> modelClass, Map input) {
		return keysStream(modelClass).filter(metaData -> existKey(metaData, deLayer(input))).map(mapper -> ((ColumnMetadata) mapper).getColumnName());
	}

	/**
	 * ??????ID??????
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
	 * ??????ID??????
	 */
	public static <T extends Model> T findById(Class<T> modelClass, final Object id) {
		return assertNotNull(ModelDelegate.findFirst(modelClass, idNameOf(modelClass) + Keys.SQL_WHERE_SINGLE_PLACEHOLDER, id));
	}

	/**
	 * ?????????????????????
	 */
	public static <T extends Model> T findFirst(Class<T> modelClass, Map input) {
		return ModelDelegate.findFirst(modelClass, query(modelClass, input, true), getParams(modelClass, input, true).toArray());
	}

	/**
	 * VO???model
	 */
	public static <T extends Model> T fromVO(Class<T> modelClass, BaseModelVO input) {
		return ModelDelegate.create(modelClass).fromMap(Helper.trans2Map(ModelDelegate.metaModelOf(modelClass), input));
	}

	/**
	 * ??????model????????????????????????????????????????????????
	 */
	public static Set getAttributeNames(Class<? extends Model>... modelClass) {
		return Stream.of(modelClass).map(ModelDelegate::metaModelOf).map(MetaModel::getAttributeNamesSkipGenerated).reduce(new HashSet<>(), Helper::merge);
	}


	/**
	 * ?????????????????????????????????????????????, ???????????????????????????????????????????????????????????????
	 */
	public static <T extends Model> LazyList<Model> getChildren(T parent, Class<? extends Model> childrenClass) {
		String subQuery = idNameOf(modelClass(parent)) + Keys.SQL_WHERE_SINGLE_PLACEHOLDER;
		log.info("{} | {} | {} | {}", childrenClass, modelClass(parent), subQuery, parent.getLongId());
		return ModelDelegate.where(childrenClass, subQuery, parent.getLongId()).include(getChildrenClass(childrenClass));
	}

	/**
	 * ??????model???????????????
	 */
	public static <T extends Model> Class<T>[] getChildrenClass(Class<T> modelClass) {
		if (modelClass.getAnnotation(ChildrensClass.class) == null) {
			return new Class[]{};
		}
		return (Class<T>[]) modelClass.getAnnotation(ChildrensClass.class).value();
	}

	/**
	 * ??????model???????????????
	 */
	public static <T extends Model> Class<T>[] getChildrenClass(T model) {
		return (Class<T>[]) getChildrenClass(modelClass(model));
	}

	/**
	 * ??????????????????????????????????????????
	 */
	public static <T extends Model> List<String> getParams(Class<T> modelClass, Map input, boolean isEqual) {
		return filterKeys(modelClass, input).map(key -> input.get(key.toLowerCase()) + (isEqual ? Keys.EMPTY : Keys.SQL_FIELD_LIKE)).collect(Collectors.toList());
	}

	/**
	 * ?????????????????????id??????
	 */
	public static <T extends Model> String idNameOf(Class<T> modelClass) {
		return metaModelOf(modelClass).getIdName();
	}

	/**
	 * ???????????????????????? ?????????2??? <br>
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
	 * ???????????????????????? ?????????3??? <br>
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
	 * ???????????????????????? ?????????3??? <br>
	 */
	public static <T extends Model> T includeAllChildrens(T model) {
		Stream.of(getChildrenClass(model)).filter(child -> ModelDelegate.belongsTo(child, modelClass(model))).forEach(childrenClass -> {
			((BaseModel) model).include(childrenClass, getChildren(model, childrenClass));
		});
		return model;
	}

	/**
	 * ??????VO?????????????????????????????????????????????
	 */
	public static <T extends Model, V extends BaseModelVO, K> PageQuery includePageQuery(Class<T> modelClass, BaseModelVO input, boolean islike, Class<T>... otherClass) {
		return includePageQuery(modelClass, Helper.trans2Map(metaModelOf(modelClass), input), islike, otherClass);
	}

	/**
	 * ??????Map???????????? ??????????????????????????????
	 *
	 * @param input      ??????
	 * @param islike     ??????????????????
	 * @param otherClass ?????????
	 */
	public static <T extends Model> PageQuery includePageQuery(Class<T> modelClass, Map input, boolean islike, Class<? extends Model>... otherClass) {
		Paginator<T> paginator = queryBuilder(modelClass, input, islike).create();
		return paginator.apply(otherClass);
	}


	/**
	 * ???????????????????????????????????????modelClass???????????????????????????
	 *
	 * @param input   ??????
	 * @param isEqual ??????????????????
	 */
	public static <T extends Model> PaginatorBuilder queryBuilder(Class<T> modelClass, Map input, boolean isEqual) {
		PaginatorQuery pagehelper = analysis(input);
		input = deLayer(input);
		PaginatorBuilder<Model> paginator = Paginator.instance().countQuery(Keys.SQL_WHERE_DEFAULT).modelClass((Class<Model>) modelClass).orderBy(pagehelper.getOrderBy()).pageSize(pagehelper.getPageSize()).currentPageIndex(pagehelper.getPageNum(), true);
		return paginator.params(getParams(modelClass, input, isEqual).toArray()).query(query(modelClass, input, isEqual));
	}

	/**
	 * @param input   ??????
	 * @param isEqual ??????????????????
	 */
	public static <T extends Model> String query(Class<T> modelClass, Map input, boolean isEqual) {
		String query = !isEqual ? subQuery(modelClass, input) : subQueryNotLike(modelClass, input);
		return getParams(modelClass, input, isEqual).isEmpty() ? Keys.SQL_WHERE_DEFAULT : query;
	}

	/**
	 * ????????????
	 */
	public static <T extends Model> T assertNotNull(T model) {
		contextAssert(ObjectUtils.isEmpty(model), Keys.LOG_MSG_OBJECT_NOT_EXISTS);
		return model;
	}

	/**
	 * ????????????
	 */
	public static void contextAssert(boolean flag, String msg) {
		if (flag) {
			throw new BizException(msg);
		}
	}

	/**
	 * ????????????????????????stream??????
	 */
	public static <T extends Model> Stream<ColumnMetadata> keysStream(Class<T> modelClass) {
		return ModelDelegate.metaModelOf(modelClass).getColumnMetadata().values().stream();
	}

	/**
	 * ??????model?????????????????????
	 */
	public static <T extends Model> MetaModel metaModelOf(Class<T> modelClass) {
		return ModelDelegate.metaModelOf(modelClass);
	}

	/**
	 * ??????model?????????????????????
	 */
	public static <T extends Model> MetaModel metaModelOf(T model) {
		return metaModelOf(modelClass(model));
	}

	/**
	 * ??????modelClass
	 */
	public static <T extends Model> Class<? extends Model> modelClass(T model) {
		return model.getClass();
	}

	/**
	 * ??????VO????????????
	 */
	public static <T extends Model, V extends BaseModelVO> PageQuery<V> pageQuery(Class<T> modelClass, BaseModelVO input, boolean... isLike) {
		return pageQuery(modelClass, toUpperKey(Helper.trans2Map(metaModelOf(modelClass), input)), isLike);
	}

	/**
	 * ??????Map????????????
	 *
	 * @param isLike ??????????????????
	 */
	public static <T extends Model> PageQuery pageQuery(Class<T> modelClass, Map input, boolean... isLike) {
		Paginator<T> paginator = queryBuilder(modelClass, input, isLike.length == 0 || isLike[0]).create();
		return paginator.apply();
	}

	/**
	 * Map???model?????????
	 */
	public static <T extends Model> T saveIt(T model, Map input) {
		return saveIt(model.fromMap(input));
	}

	/**
	 * VO???model?????????
	 */
	public static <T extends Model, V extends BaseModelVO> T saveIt(T model, V input) {
		return saveIt(model.fromMap(Helper.trans2Map(metaModelOf(modelClass(model)), input)));
	}

	/**
	 * ??????Map?????????????????? id????????????
	 */
	public static <T extends Model> T saveItById(Class<T> modelClass, Map input) {
		return saveIt(findById(modelClass, input).fromMap(input));
	}

	/**
	 * ??????VO?????????????????? id????????????
	 */
	public static <T extends Model, V extends BaseModelVO> T saveItById(Class<T> modelClass, V input) {
		return saveItById(modelClass, Helper.trans2Map(metaModelOf(modelClass), input));
	}


	/**
	 * ??????
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
				System.err.println(ApplicationContextHelper.loginUser());
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
	 * ?????????????????????????????????????????????where??????<br>
	 * ?????????????????????????????? ????????????
	 */
	public static <T extends Model> String subQuery(Class<T> modelClass, Map input) {
		String delimiter = isOr(input) ? Keys.SQL_WHERE_DELIMITER_OR_LIKE : Keys.SQL_WHERE_DELIMITER_AND_LIKE;
		return filterKeys(modelClass, input).collect(Collectors.joining(delimiter, Keys.SQL_WHERE_PREFIX, Keys.SQL_WHERE_SUFFIX_LIKE));
	}

	/**
	 * ?????????????????????????????????????????????where??????<br>
	 * ?????????????????????????????? ????????????
	 */
	public static <T extends Model> String subQueryNotLike(Class<T> modelClass, Map input) {
		String delimiter = isOr(input) ? Keys.SQL_WHERE_DELIMITER_OR : Keys.SQL_WHERE_DELIMITER_AND;
		return filterKeys(modelClass, input).collect(Collectors.joining(delimiter, Keys.SQL_WHERE_PREFIX, Keys.SQL_WHERE_SUFFIX));
	}

	/**
	 * ????????????????????????????????????????????????true?????????????????????or?????? ????????????and??????
	 */
	public static boolean isOr(Map input) {
		return Keys.TRUE.equals(input.getOrDefault(Keys.IS_OR, false));
	}

	/**
	 * ??????model????????????
	 */
	public static <T extends Model> String tableNameOf(Class<T> modelClass) {
		return ModelDelegate.tableNameOf(modelClass);
	}

	/**
	 * ?????????where?????? example : [a,b,c] >>> "'a','b','c'"
	 */
	public static <E extends CharSequence> String toJoinString(Collection<E> collection) {
		return collection.stream().collect(Collectors.joining("','", "'", "'"));
	}

	/**
	 * ?????????where?????? example : [a,b,c] >>> "'a','b','c'"
	 */
	public static <E extends CharSequence> String toJoinString(E... collection) {
		return toJoinString(new ArrayList<E>(Arrays.asList(collection)));
	}

	/**
	 * ?????????where?????? example : [a,b,c] >>> "?,?,?"
	 */
	public static <E> String toHolderString(List<E> collection) {
		return String.join(",", Lists.transform(collection, e -> "?"));
	}

	/**
	 * ?????????where?????? example : [a,b,c] >>> "?,?,?"
	 */
	public static <E> String toHolderString(E... collection) {
		return toHolderString(new ArrayList<E>(Arrays.asList(collection)));
	}

	/**
	 * model ??? json
	 */
	public static <T extends Model> String toJson(T model) {
		if (model == null) {
			return Keys.JSON_EMPTY;
		}
		return model.toJson(true);
	}

	/**
	 * Collection<Model>??? List<Map>
	 */
	public static <T extends Model, V extends BaseModelVO> List<Map> toListMap(Collection<T> rows, String... keys) {
		return rows.stream().map(row -> ((BaseModel) row).toMap(keys)).collect(Collectors.toList());
	}

	/**
	 * Collection<VO>??? List<Map>
	 */
	public static <T extends Model, V extends BaseModelVO> List<Map> toListMap(MetaModel metaModel, Collection<V> rows) {
		return rows.stream().map(row -> Helper.trans2Map(metaModel, row)).collect(Collectors.toList());
	}

	/**
	 * List<Model>??? List<VO>
	 */
	public static <T extends Model, V extends BaseModelVO> List<V> toListVO(LazyList<T> rows) {
		return (List<V>) rows.stream().map(ContextHelper::toVO).collect(Collectors.toList());
	}

	/**
	 * ?????????jsonNode
	 */
	public static <T> JsonNode toJsonNode(T apply) {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.convertValue(apply, JsonNode.class);
	}

	/**
	 * VO???model
	 */
	public static <T extends Model> Model toPO(T model, BaseModelVO input) {
		if (model == null) {
			return null;
		}
		return model.fromMap(Helper.trans2Map(metaModelOf(model), input));
	}

	/**
	 * Map key????????????
	 */
	public static Map toUpperKey(Map input) {
		return toKeyValue(input, String::toUpperCase);
	}

	/**
	 * Map key????????????
	 */
	public static Map toLowerkey(Map input) {
		return toKeyValue(input, String::toLowerCase);
	}

	public static Map toKeyValue(Map input, UnaryOperator<String> apply) {
		return entryStream(input).collect(HashMap::new, (n, e) -> n.put(Optional.ofNullable(e.getKey()).map(apply).orElse(null), e.getValue()), HashMap::putAll);
	}

	/**
	 * model???VO
	 */
	public static <T extends Model> BaseModelVO toVO(T model) {
		return toVO(transferClass(model), model.toMap());
	}

	/**
	 * json???VO
	 */
	@SneakyThrows
	public static <T> T toVO(String json, Class<T> clazz) {
		ObjectMapper objectMapper = ApplicationContextHelper.getBeanByType(ObjectMapper.class);
		return objectMapper.readValue(json, clazz);
	}

	/**
	 * map???VO
	 */
	@SneakyThrows
	public static <T extends BaseModelVO> T toVO(Class<T> clazz, Map input) {
		return toVO(toJson(input), clazz);
	}

	/**
	 * ???????????????json
	 */
	@SneakyThrows
	public static <T> String toJson(T input) {
		ObjectMapper objectMapper = ApplicationContextHelper.getBeanByType(ObjectMapper.class);
		return objectMapper.writeValueAsString(input);
	}

	/**
	 * ??????model??????VO??????
	 */
	public static <T extends Model> Class<? extends BaseModelVO> transferClass(Class<T> modelClass) {
		return AnnotationUtils.findAnnotation(modelClass, TransferClass.class).value();
	}

	/**
	 * ??????model??????VO??????
	 */
	public static <T extends Model> Class<? extends BaseModelVO> transferClass(T model) {
		return transferClass(modelClass(model));
	}

	/**
	 * ???????????????????????????????????????
	 */
	public static List<DB> openConnections(ModelType... modelType) {
		return openConnections(Stream.of(modelType).map(ModelType::getName).toArray(String[]::new));
	}

	/**
	 * ???????????????????????????????????????
	 */
	public static List<DB> openConnections(String... modelType) {
		return Stream.of(modelType).map(ContextHelper::openConnection).collect(Collectors.toList());
	}

	/**
	 * ?????????????????????????????????
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
				return db;
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return new DB(modelType).open(dataSource);
		}
		return initDB(modelType);
	}

	public static DB initDB(String name) {
		DB db = new DB(name);
		return db.hasConnection() ? db : db.open();
	}


	/**
	 * ???????????????????????????
	 */
	public static DataSource getDataSource(ModelType modelType) {
		return getDataSource(modelType.getName());
	}

	/**
	 * ?????????????????????????????????
	 */
	public static DataSource getDataSource(String modelType) {
		Map<String, DataSource> dataSources = ApplicationContextHelper.getBeansOfType(DataSource.class);
		return dataSources.getOrDefault(modelType, ApplicationContextHelper.getBeanByType(DataSource.class));
	}

	/**
	 * ??????????????????????????????
	 */
	public static void openTransaction() {
		connections.get().forEach(DB::openTransaction);
		log.info(Keys.LOG_MSG_OPEN_TRANSACTION, DB.getCurrrentConnectionNames());
	}

	/**
	 * ??????????????????????????????
	 */
	public static void commitTransaction() {
		connections.get().forEach(DB::commitTransaction);
		log.info(Keys.LOG_MSG_COMMIT_TRANSACTION, DB.getCurrrentConnectionNames());
	}

	/**
	 * ??????????????????????????????
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
	 * ????????????????????????
	 */
	public static void close() {
		if (!DB.getCurrrentConnectionNames().isEmpty()) {
			log.info(Keys.LOG_MSG_CLOSE_TRANSACTION, DB.getCurrrentConnectionNames());
			connections.get().forEach(DB::close);
		}
	}

	public static void initConnections(String... modelType) {
		if (modelType.length == 0) {
			connections.set(openConnections(modelType));
		} else {
			connections.set(openConnections(modelType));
		}
		log.info("activejdbc openConnections : {}", DB.getCurrrentConnectionNames());
	}

	/**
	 * ???????????????????????????????????? ????????????master
	 */
	public static void initConnections(ModelType... modelType) {
		if (modelType.length == 0) {
			connections.set(openConnections(ModelType.MASTER));
		} else {
			connections.set(openConnections(modelType));
		}
		log.info("activejdbc openConnections : {}", DB.getCurrrentConnectionNames());
	}

	/**
	 * ???????????????????????? ??????????????? ???????????????????????????????????????????????????
	 */
	public static <T> Function<T> transaction(Function<T> apply, ModelType... ModelType) {
		return () -> {
			ContextHelper.initConnections(ModelType);
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
				ContextHelper.close();
			}
			return result;
		};
	}

	/**
	 * ???????????????????????????,?????????????????????
	 */
	public static <T> Future<T> asyncApplyTransaction(Function<T> apply) {
		return asyncApply(transaction(apply));
	}

	/**
	 * ???????????????????????????,????????????????????????
	 */
	public static <T> CompletableFuture<T> asyncApply(Function<T> apply) {
		ThreadPoolTaskExecutor contextPool = ApplicationContextHelper.getBeanByType(ThreadPoolTaskExecutor.class);
		if (contextPool == null) {
			return CompletableFuture.supplyAsync(apply::apply);
		}
		return CompletableFuture.supplyAsync(apply::apply, contextPool);
	}

	/**
	 * ?????????????????????????????????
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
	 * ?????????????????????
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
	 * ?????????????????????
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
	 * ??????????????????????????????????????????join??????????????????????????????????????????????????????????????????????????????
	 */
	public static <T> List<T> join(List<Function<T>> applys) {
		List<T> result = new ArrayList<>();
		CompletableFuture.allOf(applys.stream().map(apply -> {
			return asyncApply(apply).whenComplete((r, e) -> result.add(r));
		}).toArray(CompletableFuture[]::new)).join();
		return result;
	}

	/**
	 * ??????tableName??????modelClass
	 */
	public static Class<Model> modelClass(String tableName) {
		MetaModel metaModel = Registry.instance().getMetaModel(tableName);
		if (metaModel == null) {
			throw new BizException("tableName:" + tableName + "?????????,???????????????model");
		}
		return (Class<Model>) metaModel.getModelClass();
	}
}
