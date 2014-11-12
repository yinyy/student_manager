package net.dyxy.yinyy.student.dao;

import java.sql.ResultSet;

public interface ObjectWrapper<T> {
	public T wrap(ResultSet rs);
}
