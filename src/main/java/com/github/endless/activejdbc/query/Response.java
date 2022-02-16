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

package com.github.endless.activejdbc.query;

import com.github.endless.activejdbc.constant.ErrorCode;
import com.github.endless.activejdbc.core.ContextHelper;
import com.github.endless.activejdbc.domains.BaseModelVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.javalite.activejdbc.Model;
import org.springframework.util.ObjectUtils;

/**
 * @author Endless
 */
@Data
@AllArgsConstructor
public class Response<T> {

    public int code;
    public String message;
    public T data;


    /**
     * model转VO并用响应对象包装
     */
    public static <T extends Model, V extends BaseModelVO> Response<V> respone(T result) {
        return (Response<V>) respone(ContextHelper.toVO(result));
    }

    /**
     * 构造响应对象
     */
    public static <T> Response<T> respone(T result) {
        return ObjectUtils.isEmpty(result) ? responeObjectNotExists() : responeSucess(result);
    }

    /**
     * 构造响应空对象
     */
    public static <T> Response<T> responeObjectNotExists() {
        return new Response<T>(ErrorCode.OBECT_NOT_EXISTS.getResponeCode(), ErrorCode.OBECT_NOT_EXISTS.getResponeMsg(), null);
    }

    /**
     * 构造响应对象
     */
    public static <T> Response<T> responeSucess(T result) {
        return new Response<T>(ErrorCode.SUCESS.getResponeCode(), ErrorCode.SUCESS.getResponeMsg(), result);
    }

}
