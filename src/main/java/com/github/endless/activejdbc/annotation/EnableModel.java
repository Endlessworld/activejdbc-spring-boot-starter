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

package com.github.endless.activejdbc.annotation;

import java.lang.annotation.*;

/**
 * <li>作用于Controller 为当前方法开启到指定数据库的链接
 * <li>@DbName参数指定model映射的数据库
 *
 * @author Endless
 */

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface EnableModel {

    String[] value() default {};

}
