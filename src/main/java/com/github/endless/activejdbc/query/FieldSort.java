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

import com.github.endless.activejdbc.constant.Keys;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * @author Endless
 */
@ApiModel(description = "排序对象")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Log4j2
public class FieldSort implements Serializable {

    private static final long serialVersionUID = 6077064586212027820L;

    @ApiModelProperty(name = "property", notes = "排序字段", example = "id")
    private String property;

    @ApiModelProperty(name = "direction", notes = "排序方向",allowableValues = "ASC,DESC")
    private Direction direction = Direction.ASC;

    public static FieldSort newInstance(String property, Direction direction) {
        return new FieldSort(property, direction);
    }

    public String toExpression() {
        if (!Pattern.matches(Keys.INJECTION_REGEX, property)) {
            log.warn("SQLInjection property: " + property);
            throw new IllegalArgumentException("SQLInjection property: " + property);
        }
        return property + " " + direction.name();
    }

}
