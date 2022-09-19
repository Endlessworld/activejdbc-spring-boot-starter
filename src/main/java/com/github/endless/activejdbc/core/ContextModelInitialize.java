package com.github.endless.activejdbc.core;

import com.github.endless.activejdbc.annotation.CallbackListeners;
import com.github.endless.activejdbc.annotation.ValidatorListener;
import com.github.endless.activejdbc.configuration.BizException;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.CallbackListener;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.ModelDelegate;
import org.javalite.activejdbc.annotations.Table;
import org.javalite.activejdbc.logging.LogFilter;
import org.javalite.validation.Validator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Component
public class ContextModelInitialize implements ApplicationRunner {
	private static final Map<String, Class<Model>> contextModels = new HashMap<>();

	public static Map<String, Class<Model>> getContextModels() {
		return contextModels;
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

	@Override
	public void run(ApplicationArguments args) {
		LogFilter.setLogExpression(".*");
		log.info("activejdbc initContextModels");
		initContextModels();
		log.info("activejdbc compiler");
		MemoryCompiler.invokeActive();
		Map<String, Object> callbackListeners = ApplicationContextHelper.getApplicationContext().getBeansWithAnnotation(CallbackListeners.class);
		for (Map.Entry<String, Object> callbackListener : callbackListeners.entrySet()) {
			if (callbackListener.getValue() instanceof CallbackListener) {
				CallbackListener callback = (CallbackListener) callbackListener.getValue();
				CallbackListeners callbackAnnotation = AnnotationUtils.findAnnotation(callback.getClass(), CallbackListeners.class);
				if (callbackAnnotation.tableNames().length > 0 || callbackAnnotation.value().length > 0) {
					contextModels.values().stream().filter(e -> {
						return Arrays.asList(callbackAnnotation.tableNames())
						             .contains(ModelDelegate.tableNameOf(e)) || Arrays.asList(callbackAnnotation.value()).contains(e);
					}).forEach(e -> {
						ModelDelegate.callbackWith(e, callback);
					});
				}
			}
		}
		Map<String, Object> validatorListeners = ApplicationContextHelper.getApplicationContext().getBeansWithAnnotation(ValidatorListener.class);
		for (Map.Entry<String, Object> validatorListener : validatorListeners.entrySet()) {
			if (validatorListener.getValue() instanceof Validator) {
				Validator validator = (Validator) validatorListener.getValue();
				ValidatorListener validatorAnnotation = AnnotationUtils.findAnnotation(validator.getClass(), ValidatorListener.class);
				if (validatorAnnotation.tableNames().length > 0 || validatorAnnotation.value().length > 0) {
					contextModels.values().stream().filter(e -> {
						return Arrays.asList(validatorAnnotation.tableNames())
						             .contains(ModelDelegate.tableNameOf(e)) || Arrays.asList(validatorAnnotation.value()).contains(e);
					}).forEach(e -> {
						ModelDelegate.validateWith(e, validator);
					});
				}
			}
		}
	}

}