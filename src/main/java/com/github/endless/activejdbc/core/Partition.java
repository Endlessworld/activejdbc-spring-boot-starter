package com.github.endless.activejdbc.core;

import java.util.AbstractList;
import java.util.List;

public class Partition<T> extends AbstractList<List<T>> {
	final List<T> list;
	final int size;

	Partition(List<T> list, int size) {
		this.list = list;
		this.size = size;
	}

	@Override
	public List<T> get(int index) {
		int listSize = size();
		if (index < 0 || index >= size) {
			throw new IndexOutOfBoundsException();
		}
		int start = index * size;
		int end = Math.min(start + size, list.size());
		return list.subList(start, end);
	}

	@Override
	public int size() {
		return (list.size() + size - 1) / size;
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}
}
