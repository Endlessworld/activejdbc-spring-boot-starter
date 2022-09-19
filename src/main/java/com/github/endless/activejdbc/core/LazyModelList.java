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

package com.github.endless.activejdbc.core;

import org.javalite.activejdbc.MetaModel;
import org.javalite.activejdbc.Model;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author Endless
 */
public class LazyModelList<T extends Model> extends LazyList<T> {

	protected LazyModelList(String subQuery, MetaModel metaModel, Collection<String> columns, Object... params) {
		super(subQuery, metaModel, columns, params);
	}

	protected LazyModelList(boolean forPaginator, MetaModel metaModel, String fullQuery, Collection<String> columns, Object... params) {
		super(forPaginator, metaModel, fullQuery, columns, params);
	}

	public LazyModelList() {
		super();
	}

	protected String toInsert() {
		return this.stream().map(e -> e.toInsert().replace("INSERT INTO", "REPLACE INTO")).collect(Collectors.joining(";"));
	}
}
