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

import com.github.endless.activejdbc.query.Response;
import com.github.endless.activejdbc.query.Utils;
import lombok.extern.log4j.Log4j2;
import org.javalite.activejdbc.InitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Endless
 */
@Log4j2
public class ExceptionControllerAdvice {
    @ExceptionHandler(InitException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bizExceptionAdvice(InitException error) {
        Map<String, Object> map = new HashMap<>(1);
        map.put("retMsg", "Please execute maven install and restart" + error.getLocalizedMessage());
        return new ResponseEntity<>(map, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 处理BizException
     */
    @ExceptionHandler(BizException.class)
    @ResponseBody
    public Response<Object> bizExceptionAdvice(BizException error) {
        StringBuilder msg = new StringBuilder("错误原因:");
        msg.append(error.getMessage()).append(" : ");
        if (!Objects.isNull(error.getCause())) {
            msg.append(error.getMessage());
        }
        log.error("业务错误：\n{}", Utils.getStackTraceString(error));
        return new Response<>(500, msg.toString(), null);
    }

}
