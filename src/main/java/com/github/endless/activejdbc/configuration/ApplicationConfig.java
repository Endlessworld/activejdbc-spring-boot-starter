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

package com.github.endless.activejdbc.configuration;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.github.endless.activejdbc.constant.Keys;
import com.github.endless.activejdbc.core.ApplicationContextHelper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * @author Endless
 */
@EnableAsync
@Configuration
@EnableAspectJAutoProxy
@ConditionalOnWebApplication
@ComponentScan("com.github.endless.**")
public class ApplicationConfig implements WebMvcConfigurer {

    /**
     * dynamic-data-source
     */
    @ConditionalOnBean({ApplicationContextHelper.class, DataSource.class})
    @Bean(name = "dynamicDataSource")
    public DataSource dynamicDataSource() {
        DynamicRoutingDataSource dynamicRoutingDataSource = new DynamicRoutingDataSource();
        Map<String, DataSource> types = ApplicationContextHelper.getApplicationContext().getBeansOfType(DataSource.class);
        Map<Object, Object> dataSourceMap = new HashMap<Object, Object>(types.size());
        types.forEach((k, v) -> {
            try {
                DatabaseMetaData dbMetaData = v.getConnection().getMetaData();
                if ("Oracle".equals(dbMetaData.getDatabaseProductName())) {
                    dataSourceMap.put(Arrays.stream(dbMetaData.getURL().split(":")).skip(5).findAny().get(), v);
                } else if ("MySQL".equals(dbMetaData.getDatabaseProductName())) {
                    dataSourceMap.put(v.getConnection().getCatalog(), v);
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });
        dynamicRoutingDataSource.setDefaultTargetDataSource(types.get("master"));
        dynamicRoutingDataSource.setTargetDataSources(dataSourceMap);
        ApplicationContextHelper.dataSourceKeys.addAll(dataSourceMap.keySet());
        return dynamicRoutingDataSource;
    }

    /**
     * 覆盖方法configureMessageConverters，使用fastJson
     */
    @Bean
    public HttpMessageConverters fastJsonHttpMessageConverters() {
        FastJsonHttpMessageConverter fastConverter = new FastJsonHttpMessageConverter();
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        fastJsonConfig.setDateFormat(Keys.DEFAULT_DATE_TIME_FORMAT);
        fastJsonConfig.setSerializerFeatures(SerializerFeature.PrettyFormat, SerializerFeature.WriteDateUseDateFormat, SerializerFeature.WriteMapNullValue);
        fastConverter.setFastJsonConfig(fastJsonConfig);
        return new HttpMessageConverters(fastConverter);
    }

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(10);
        executor.setThreadNamePrefix("taskExecutor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        return executor;
    }

}
