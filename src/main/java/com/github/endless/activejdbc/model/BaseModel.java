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

package com.github.endless.activejdbc.model;

import com.github.endless.activejdbc.core.ContextHelper;
import org.javalite.activejdbc.Model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 对所有model类提供公共方法
 * @author Endless
 */
public abstract class BaseModel extends Model {

    public <T extends Model> Model include(Class<T> childrenClass, List<Model> childrens) {
        this.setChildren(childrenClass, childrens);
        return this;
    }

    public <T extends Model> String toJson() {
        return ContextHelper.toJson(this);
    }

    public Map<String, Object> toMap(String... keys) {
        if (keys.length == 0) {
            return toMap();
        }
        return Stream.of(keys).collect(Collectors.toMap(key -> key, key -> Optional.ofNullable(get(key)).orElse(""), (x, y) -> x, TreeMap::new));
    }

    @Override
    public void toJsonP(StringBuilder sb, boolean pretty, String indent, String... attributeNames) {
        super.toJsonP(sb, pretty, indent, attributeNames);
    }

    @Override
    public void toXmlP(StringBuilder sb, boolean pretty, String indent, String... attributeNames) {
        super.toXmlP(sb, pretty, indent, attributeNames);
    }

}
