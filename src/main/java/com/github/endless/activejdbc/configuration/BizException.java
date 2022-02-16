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

import java.io.Serializable;

/**
 * Service层异常统一封装的业务异常，一般无法直接处理 BizException
 * @author Endless
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 5948018638602481391L;
    private Serializable exceptionData;

    public BizException(Exception e) {
        super(e);
    }

    public BizException(String msg) {
        super(msg);
    }

    public BizException(String msg, Serializable exceptionData) {
        super(msg);
        this.exceptionData = exceptionData;
    }

    public BizException(String msg, Exception e) {
        super(msg, e);
    }

    public Serializable getExceptionData() {
        return exceptionData;
    }

    public void setExceptionData(Serializable exceptionData) {
        this.exceptionData = exceptionData;
    }

}
