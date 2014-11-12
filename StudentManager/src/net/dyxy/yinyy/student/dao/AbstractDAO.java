package net.dyxy.yinyy.student.dao;

import java.util.List;

public interface AbstractDAO<T> {
	/**
	 * 新建对象
	 * 
	 * @param t
	 * @return
	 */
	public long add(T t);

	/**
	 * 删除对象
	 * 
	 * @param t
	 * @return
	 */
	public boolean delete(T t);

	/**
	 * 根据主键删除对象
	 * 
	 * @param id
	 * @return
	 */
	public boolean delete(long id);

	/**
	 * 更新对象
	 * 
	 * @param t
	 * @return
	 */
	public boolean update(T t);

	/**
	 * 得到对象
	 * 
	 * @param id
	 * @return
	 */
	public T get(long id);

	/**
	 * 取出全部对象
	 * 
	 * @return
	 */
	public List<T> list();

	/**
	 * 取出符合条件的对象
	 * 
	 * @param conditions
	 *            查询条件，多个条件之间用空格分开
	 * @return
	 */
	public List<T> list(String where);

	/**
	 * 取出某页的对象
	 * 
	 * @param page
	 *            要取出的页，页从1开始
	 * @param size
	 *            分页的大小
	 * @return
	 */
	public List<T> list(int page, int size);

	/**
	 * 取出符合条件的某页的对象
	 * 
	 * @param conditions
	 *            查询条件，多个条件之间用空格分开
	 * @param page
	 *            要取出的页，页从1开始
	 * @param size
	 *            分页的大小
	 * @return
	 */
	public List<T> list(int page, int size, String where);
	
	/**
	 * 分页取出符合条件的对象
	 * @param page
	 * @param size
	 * @param where
	 * @param order
	 * @return
	 */
	public List<T> list(int page,int size, String where,String order);
	
	/**
	 * 执行指定的SQL语句
	 * @param sql
	 * @param page
	 * @param size
	 * @return
	 */
	public List<T> list(String sql, int page, int size);
}
