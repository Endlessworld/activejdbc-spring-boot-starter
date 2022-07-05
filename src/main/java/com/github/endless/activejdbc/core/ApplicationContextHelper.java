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

import com.github.endless.activejdbc.annotation.CallbackListeners;
import com.github.endless.activejdbc.annotation.ValidatorListener;
import com.github.endless.activejdbc.configuration.BizException;
import com.github.endless.activejdbc.constant.Keys;
import com.github.endless.activejdbc.constant.ModelType;
import org.javalite.activejdbc.CallbackListener;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.ModelDelegate;
import org.javalite.activejdbc.annotations.Table;
import org.javalite.activejdbc.logging.LogFilter;
import org.javalite.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.function.Function;

/**
 * ApplicationContext
 *
 * @author Endless
 */
@Component("ActiveJdbcContextHelper")
@Order
public class ApplicationContextHelper implements ApplicationContextAware, ApplicationRunner {
	private final Logger log = LoggerFactory.getLogger(ApplicationContextHelper.class);
	private static final ThreadLocal<Object> contextDataSource = ThreadLocal.withInitial(() -> ModelType.MASTER);
	private static final Map<String, Class<Model>> contextModels = new HashMap<>();
	public static List<Object> dataSourceKeys = new ArrayList<>();
	private static ApplicationContext applicationContext = null;

	/**
	 * 当前所使用的数据源
	 */
	public static Object getDataSourceKey() {
		return contextDataSource.get();
	}

	/**
	 * 切换数据源 master/slave
	 */
	public static void setDataSourceKey(String key) {
		if (dataSourceKeys.contains(key)) {
			contextDataSource.set(key);
		} else {
			throw new BizException("指定的数据源 " + key + " 不存在！当前可选：" + dataSourceKeys);
		}
	}

	public static void clearDataSourceKey() {
		contextDataSource.remove();
	}

	/**
	 * 数据源是否存在
	 */
	public static Boolean containDataSourceKey(String key) {
		return dataSourceKeys.contains(key);
	}

	/**
	 * 发布一个事件
	 */
	public static void publishEvent(ApplicationEvent event) {
		applicationContext.publishEvent(event);
	}

	/**
	 * 获取HttpServletRequest
	 */
	public static HttpServletRequest getRequest() {
		return attributes(ServletRequestAttributes::getRequest);
	}

	/**
	 * 获取HttpServletResponse
	 */
	public static HttpServletResponse getResponse() {
		return attributes(ServletRequestAttributes::getResponse);
	}

	/**
	 *
	 */
	private static <T> T attributes(Function<ServletRequestAttributes, T> apply) {
		return getRequestAttributes().map(apply).orElse(null);
	}

	/**
	 * 获取ServletRequestAttributes
	 */
	public static Optional<ServletRequestAttributes> getRequestAttributes() {
		return Optional.ofNullable((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());
	}

	/**
	 * 获取 ApplicationContext
	 */
	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		ApplicationContextHelper.applicationContext = applicationContext;
	}

	/**
	 * 获取对象
	 */
	public static Object getBeanByName(String name) {
		return applicationContext.getBean(name);
	}

	/**
	 * 获取对象
	 */
	public static <T> T getBeanByType(Class<T> beanType) {
		return Optional.ofNullable(applicationContext).map(e -> e.getBean(beanType)).orElse(null);
	}

	/**
	 * 获取类型为requiredType的对象 如果bean不能被类型转换，相应的异常将会被抛出（BeanNotOfRequiredTypeException）
	 */
	public static <T> Object getBeanByName(String name, Class<T> beanType) {
		return applicationContext.getBean(name, beanType);
	}

	public static <T> String loginUser() {
		return Keys.EMPTY;
	}

	/**
	 * 获取Class
	 */
	public static Class<?> getType(String name) {
		return applicationContext.getType(name);
	}

	public static <T> Map<String, T> getBeansOfType(Class<T> beanType) {
		return applicationContext.getBeansOfType(beanType);
	}

	public static Map<String, Class<Model>> getContextModels() {
		return contextModels;
	}

	@Override
	public void run(ApplicationArguments args) {
		LogFilter.setLogExpression(".*");
		log.info("activejdbc initContextModels");
		initContextModels();
		log.info("activejdbc compiler");
		MemoryCompiler.invokeActive();
		Map<String, Object> callbackListeners = applicationContext.getBeansWithAnnotation(CallbackListeners.class);
		for (Map.Entry<String, Object> callbackListener : callbackListeners.entrySet()) {
			if (callbackListener.getValue() instanceof CallbackListener) {
				CallbackListener callback = (CallbackListener) callbackListener.getValue();
				CallbackListeners callbackAnnotation = AnnotationUtils.findAnnotation(callback.getClass(), CallbackListeners.class);
				if (callbackAnnotation.tableNames().length > 0 || callbackAnnotation.value().length > 0) {
					contextModels.values().stream().filter(e -> {
						return Arrays.asList(callbackAnnotation.tableNames()).contains(ModelDelegate.tableNameOf(e)) || Arrays.asList(callbackAnnotation.value()).contains(e);
					}).forEach(e -> {
						ModelDelegate.callbackWith(e, callback);
					});
				}
			}
		}
		Map<String, Object> validatorListeners = applicationContext.getBeansWithAnnotation(ValidatorListener.class);
		for (Map.Entry<String, Object> validatorListener : validatorListeners.entrySet()) {
			if (validatorListener.getValue() instanceof Validator) {
				Validator validator = (Validator) validatorListener.getValue();
				ValidatorListener validatorAnnotation = AnnotationUtils.findAnnotation(validator.getClass(), ValidatorListener.class);
				if (validatorAnnotation.tableNames().length > 0 || validatorAnnotation.value().length > 0) {
					contextModels.values().stream().filter(e -> {
						return Arrays.asList(validatorAnnotation.tableNames()).contains(ModelDelegate.tableNameOf(e)) || Arrays.asList(validatorAnnotation.value()).contains(e);
					}).forEach(e -> {
						ModelDelegate.validateWith(e, validator);
					});
				}
			}
		}
	}


	public void initContextModels() {
		try {
			Properties modelNames = PropertiesLoaderUtils.loadAllProperties("activejdbc_models.properties");
			for (final String className : modelNames.stringPropertyNames()) {
				try {
					Class<Model> modelClass = (Class<Model>) Class.forName(className);
					contextModels.put(modelClass.getAnnotation(Table.class).value(), modelClass);
				} catch (Exception e) {
					throw new BizException("Failed to loader modelClass " + className, e);
				}
			}
		} catch (Exception e) {
			throw new BizException("Failed to loader activejdbc_models.properties,Please execute maven install and restart", e);
		}
	}
}
