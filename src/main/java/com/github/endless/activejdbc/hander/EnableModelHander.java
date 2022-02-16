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

package com.github.endless.activejdbc.hander;

import com.github.endless.activejdbc.annotation.EnableModel;
import com.github.endless.activejdbc.core.ApplicationContextHelper;
import com.github.endless.activejdbc.core.ContextHelper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.DbName;
import org.javalite.common.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.TreeSet;

/***
 * @author Endless
 * @date 2020 年10月2日
 */
@Aspect
@Component
public class EnableModelHander {

    private static final String URI_TEMPLATE_VARIABLES = "org.springframework.web.servlet.HandlerMapping.uriTemplateVariables";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Pointcut("@within(com.github.endless.activejdbc.annotation.EnableModel) || @annotation(com.github.endless.activejdbc.annotation.EnableModel)")
    public void switchDataSource() {
    }

    @AfterThrowing(pointcut = "switchDataSource()", throwing = "e")
    public void afterThrowing(JoinPoint point, Throwable e) throws Throwable {

    }

    @Around(value = "switchDataSource()")
    public Object switchDataSource(ProceedingJoinPoint point) throws Throwable {
        long before = System.currentTimeMillis();
        Object result = null;
        try {
            String[] modelType = modelType(point);
            ContextHelper.initConnections(modelType);
            if (modelType.length == 0) {
                return point.proceed();
            }
            ApplicationContextHelper.setDataSourceKey(modelType[0]);
            logger.info("{} | {} |mybatis data source switch to {}", getSimpleName(point),
                    point.getSignature().getName(), modelType[0]);
            try {
                ContextHelper.openTransaction();
                result = point.proceed();
                ContextHelper.commitTransaction();
            } catch (Exception e) {
                logger.error("Exec db operation failed.");
                ContextHelper.rollbackTransaction();
                throw e;
            } finally {
                ContextHelper.close();
            }
        } catch (Throwable e) {
            logger.error("SwitchDataSource failed.");
            throw e;
        } finally {
            logger.info("Success ! Processing took: {} milliseconds", System.currentTimeMillis() - before);
        }
        return result;
    }

    private String getSimpleName(ProceedingJoinPoint point) throws SecurityException {
        return point.getTarget().getClass().getSimpleName();
    }

    /**
     * 获取类和方法上的注解值
     */
    private <T extends Model> String[] modelType(ProceedingJoinPoint point) throws NoSuchMethodException, SecurityException,
            IllegalArgumentException {
        MethodSignature pointMethod = (MethodSignature) point.getSignature();
        Class<? extends Object> pointClass = point.getTarget().getClass();
        Method currentMethod = pointClass.getMethod(pointMethod.getName(), pointMethod.getParameterTypes());
        EnableModel methodModel = pointClass.getAnnotation(EnableModel.class);
        EnableModel classModel = currentMethod.getAnnotation(EnableModel.class);
        TreeSet<String> result = new TreeSet<String>();
        if (methodModel != null) {
            result.addAll(Collections.li(methodModel.value()));
        }
        if (classModel != null) {
            result.addAll(Collections.li(classModel.value()));
        }
        Map<String, String> a = (Map<String, String>) ApplicationContextHelper.getRequestAttributes().get().getAttribute(URI_TEMPLATE_VARIABLES, 0);
        if (a.get("model-name") != null) {
            result.add(ApplicationContextHelper.modelClass(a.get("model-name")).getAnnotation(DbName.class).value());
        }
        if (!(pointClass.getGenericSuperclass() instanceof Class)) {
            Class<T> clazz = (Class<T>) ((ParameterizedType) pointClass.getGenericSuperclass()).getActualTypeArguments()[0];
            result.add(clazz.getAnnotation(DbName.class).value());
        }
        return result.toArray(new String[]{});

    }
}
