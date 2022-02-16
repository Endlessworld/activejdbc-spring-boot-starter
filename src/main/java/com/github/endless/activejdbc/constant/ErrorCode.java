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

package com.github.endless.activejdbc.constant;

import lombok.Getter;

/**
 * @author Endless
 */

@Getter
public enum ErrorCode {

    SUCESS(0, "success"),

    OBECT_EXISTS(-1, "object exists"),

    OBECT_NOT_EXISTS(-2, "object not exists"),

    OBECT_IN_USING(-3, "object in-using");

    private final int responeCode;

    private final String responeMsg;

    ErrorCode(int code, String msg) {
        this.responeCode = code;
        this.responeMsg = msg;
    }
}
