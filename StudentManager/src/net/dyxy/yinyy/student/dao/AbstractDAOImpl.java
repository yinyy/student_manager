package net.dyxy.yinyy.student.dao;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

public abstract class AbstractDAOImpl<T> implements AbstractDAO<T> {
	private static final String driver;
	protected static final String url;
	protected static final String uid;
	protected static final String pwd;

	/**
	 * 静态初始化块，初始化数据库连接
	 */
	static {
		// 获取WEB_INF路径
		File root = new File(Thread.currentThread().getContextClassLoader()
				.getResource("/").getPath());
		root = root.getParentFile();

		File file = new File(root, "jdbc.properties");

		// 默认的属性
		Properties defaults = new Properties();
		defaults.put("driver", "");
		defaults.put("url", "");
		defaults.put("uid", "");
		defaults.put("pwd", "");

		// 加载properties文件
		Properties prop = new Properties(defaults);
		try {
			prop.load(new FileInputStream(file));
		} catch (Exception e) {
			e.printStackTrace();
		}

		driver = prop.getProperty("driver");
		url = prop.getProperty("url");
		uid = prop.getProperty("uid");
		pwd = prop.getProperty("pwd");

		try {
			Class.forName(driver);
			System.out.println("加载驱动：" + driver);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/***
	 * 添加对象
	 */
	@Override
	public long add(T t) {
		long ans = -1;

		if (t.getClass().isAnnotationPresent(Table.class)) {
			String sql = createInsertSentence(t);
			// System.out.println(sql);

			try (Connection conn = DriverManager.getConnection(url, uid, pwd);
					PreparedStatement stmt = conn.prepareStatement(sql)) {
				// 把参数设置到Statement中
				injectFrom(stmt, t);

				ans = stmt.executeUpdate();

				try (Statement stmt2 = conn.createStatement();
						ResultSet rs = stmt2.executeQuery("select @@IDENTITY")) {
					rs.next();

					ans = rs.getLong(1);

					setId(t, ans);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// System.out.println(ans);

		return ans;
	}

	/***
	 * 删除对象
	 */
	@Override
	public boolean delete(T t) {
		boolean ans = false;

		// ParameterizedType paramType = (ParameterizedType) this.getClass()
		// .getGenericSuperclass();
		// Type type = paramType.getActualTypeArguments()[0];
		// Class<T> clz = (Class<T>) type;

		if (t.getClass().isAnnotationPresent(Table.class)) {
			String sql = createDeleteSentence(t);

			try (Connection conn = DriverManager.getConnection(url, uid, pwd);
					PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, getId(t));
				ans = stmt.executeUpdate() > 0;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return ans;
	}

	/***
	 * 根据ID删除对象
	 * 
	 * @param id
	 * @return
	 */
	@Override
	public boolean delete(long id) {
		return delete(get(id));
	}

	/**
	 * 更新对象
	 */
	@Override
	public boolean update(T t) {
		boolean ans = false;

		if (t.getClass().isAnnotationPresent(Table.class)) {
			String sql = createUpdateSentence(t);

			try (Connection conn = DriverManager.getConnection(url, uid, pwd);
					PreparedStatement stmt = conn.prepareStatement(sql)) {

				injectFrom(stmt, t);

				stmt.setLong(
						stmt.getParameterMetaData().getParameterCount() - 1,
						getId(t));

				ans = stmt.executeUpdate() > 0;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return ans;
	}

	/**
	 * 根据id获取对象
	 */
	@Override
	public T get(long id) {
		T t = null;

		Class<T> clz = getGenericsClass();
		if (clz.isAnnotationPresent(Table.class)) {
			String sql = createSelectSingleSentence(clz, id);

			try (Connection conn = DriverManager.getConnection(url, uid, pwd);
					PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, id);

				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						ResultSetMetaData data = rs.getMetaData();
						Map<String, Object> map = new HashMap<String, Object>();

						for (int i = 1; i <= data.getColumnCount(); i++) {
							map.put(data.getColumnLabel(i), rs.getObject(i));
						}

						t = clz.newInstance();
						injectFrom(t, map);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return t;
	}

	/**
	 * 取出全部对象
	 */
	@Override
	public List<T> list() {
		return list(0, 0);
	}

	/**
	 * 根据条件查询
	 */
	public List<T> list(String where) {
		return list(0, 0, where);
	}

	/**
	 * 分页获取数据
	 * 
	 * @param page
	 *            要获取的页码，若小于等于0表示不分页。
	 */
	@Override
	public List<T> list(int page, int size) {
		return list(page, size, null);
	}

	/**
	 * 待条件的分页查询
	 */
	@Override
	public List<T> list(int page, int size, String where) {
		return list(page, size, where, null);
	}

	/**
	 * 分页
	 */
	@Override
	public List<T> list(int page, int size, String where, String order) {
		List<T> list = new LinkedList<T>();

		Class<T> clz = getGenericsClass();
		if (clz.isAnnotationPresent(Table.class)) {
			String sql = createSelectSentence(clz, where, order);

			list = list(sql, page, size);
		}

		return list;
	}

	/**
	 * 
	 */
	@Override
	public List<T> list(String sql, int page, int size) {
		List<T> list = new LinkedList<T>();

		try (Connection conn = DriverManager.getConnection(url, uid, pwd);
				Statement stmt = conn.createStatement(
						ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY);
				ResultSet rs = stmt.executeQuery(sql)) {
			ResultSetMetaData meta = rs.getMetaData();

			if (page >= 1) {
				// 计算先跳过多少记录
				int row = (page - 1) * size;
				rs.absolute(row);
			}

			int count = size;
			Class<T> clz = getGenericsClass();
			
			while (rs.next() && (count-- > 0)) {
				Map<String, Object> map = new HashMap<String, Object>();
				for (int i = 1; i <= meta.getColumnCount(); i++) {
					map.put(meta.getColumnLabel(i), rs.getObject(i));
				}

				T obj = clz.newInstance();

				injectFrom(obj, map);

				list.add(obj);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	/**
	 * 创建对象的Insert语句
	 * 
	 * @param t
	 * @return
	 */
	private String createInsertSentence(T t) {
		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		Column column;
		String tmp;

		sb.append("insert into ");
		sb.append(t.getClass().getAnnotation(Table.class).name());
		sb.append(" (");

		// columns
		for (Field field : t.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Column.class)
					&& (column = field.getAnnotation(Column.class))
							.insertable()) {
				sb2.append(column.name());
				sb2.append(", ");
			}
		}

		tmp = sb2.toString();
		if (!"".equals(tmp)) {
			tmp = tmp.substring(0, tmp.length() - 2).trim();
		}

		sb.append(tmp);
		// columns

		sb.append(")");
		sb.append(" values (");

		// values
		sb2 = new StringBuilder();

		for (Field field : t.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Column.class)
					&& field.getAnnotation(Column.class).insertable()) {
				sb.append("?, ");
			}
		}

		tmp = sb2.toString();
		if (!"".equals(tmp)) {
			tmp = tmp.substring(0, tmp.length() - 2).trim();
		}

		sb.append(tmp);
		// values

		sb.append(")");

		return sb.toString();
	}

	/**
	 * 创建对象的Delete语句
	 * 
	 * @param t
	 * @return
	 */
	private String createDeleteSentence(T t) {
		StringBuilder sb = new StringBuilder();

		sb.append("delete from ");
		sb.append(t.getClass().getAnnotation(Table.class).name());
		sb.append(" where ");

		// where
		String where = null;

		for (Field field : t.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Column.class)
					&& field.isAnnotationPresent(Id.class)) {
				Column column = field.getAnnotation(Column.class);

				where = column.name() + " = ?";
				break;
			}
		}

		sb.append(where);
		// where

		return sb.toString();
	}

	/**
	 * 创建对象的Update语句
	 * 
	 * @param t
	 * @return
	 */
	private String createUpdateSentence(T t) {
		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		String tmp;

		sb.append("update ");
		sb.append(t.getClass().getAnnotation(Table.class).name());
		sb.append(" set ");

		// columns
		for (Field field : t.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Column.class)
					&& field.getAnnotation(Column.class).updatable()) {
				sb2.append(field.getName());
				sb2.append(" = ?");
				sb2.append(", ");
			}
		}

		tmp = sb2.toString();
		if (!"".equals(tmp)) {
			tmp = tmp.substring(0, tmp.length() - 2).trim();
		}

		sb.append(tmp);
		// columns

		sb.append(" where ");

		// where
		for (Field field : t.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Column.class)
					&& field.isAnnotationPresent(Id.class)) {
				Column column = field.getAnnotation(Column.class);

				tmp = column.name() + " = ?";
				break;
			}
		}

		sb.append(tmp);
		// where

		return sb.toString();
	}

	/**
	 * 创建带条件的select语句
	 * 
	 * @param clz
	 * @param where
	 * @return
	 */
	private String createSelectSentence(Class<T> clz, String where, String order) {
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ");
		sb.append(clz.getAnnotation(Table.class).name());

		// where
		if (where != null) {
			sb.append(" ");
			sb.append(where);
		}
		// where

		sb.append(" order by ");

		// order
		if (order == null) {
			// 按主键排序
			for (Field field : clz.getDeclaredFields()) {
				if (field.isAnnotationPresent(Column.class)
						&& field.isAnnotationPresent(Id.class)) {
					Column column = field.getAnnotation(Column.class);

					sb.append(column.name());
					sb.append(" desc");
					break;
				}
			}
			// 按主键排序
		} else {
			sb.append(order);
		}

		return sb.toString();
	}

	/**
	 * 创建获取一个对象的Select语句
	 * 
	 * @param clz
	 * @param id
	 * @return
	 */
	private String createSelectSingleSentence(Class<T> clz, long id) {
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ");
		sb.append(clz.getAnnotation(Table.class).name());
		sb.append(" where ");

		// where
		String tmp = null;
		for (Field field : clz.getDeclaredFields()) {
			if (field.isAnnotationPresent(Column.class)
					&& field.isAnnotationPresent(Id.class)) {
				Column column = field.getAnnotation(Column.class);

				tmp = column.name() + " = ?";
				break;
			}
		}

		sb.append(tmp);
		// where

		return sb.toString();
	}

	/**
	 * 得到泛型类型
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Class<T> getGenericsClass() {
		ParameterizedType paramType = (ParameterizedType) this.getClass()
				.getGenericSuperclass();
		Type type = paramType.getActualTypeArguments()[0];
		Class<T> clz = (Class<T>) type;

		return clz;
	}

	// /**
	// * 得到字段的getter方法
	// *
	// * @param name
	// * @return
	// */
	// private static String getter(String name) {
	// char first = name.charAt(0);
	// if (first >= 'a' && first <= 'z') {
	// first = (char) (first - 32);
	// }
	//
	// return "get" + first + name.substring(1);
	// }
	//
	// /**
	// * 得到字段的setter方法
	// *
	// * @param name
	// * @return
	// */
	// private static String setter(String name) {
	// char first = name.charAt(0);
	// if (first >= 'a' && first <= 'z') {
	// first = (char) (first - 32);
	// }
	//
	// return "set" + first + name.substring(1);
	// }
	//
	/**
	 * 给对象设置id
	 * 
	 * @param t
	 * @param id
	 */
	private void setId(T t, long id) {
		// 把ID设置回去
		for (Field field : t.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Id.class)) {
				field.setAccessible(true);
				try {
					field.setLong(t, id);
				} catch (Exception e) {
					e.printStackTrace();
				}

				break;
			}
		}
	}

	/**
	 * 得到对象的id
	 * 
	 * @param t
	 * @return
	 */
	private long getId(T t) {
		long value = -1;

		for (Field field : t.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Id.class)) {
				field.setAccessible(true);
				try {
					value = field.getLong(t);
				} catch (Exception e) {
					e.printStackTrace();
				}

				break;
			}
		}

		return value;
	}

	/**
	 * 把T的值给stmt
	 * 
	 * @param stmt
	 * @param t
	 */
	private void injectFrom(PreparedStatement stmt, T t) {
		int iField = 1;

		for (Field field : t.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Column.class)
					&& (field.getAnnotation(Column.class).insertable() || field
							.getAnnotation(Column.class).updatable())) {
				Class<?> type = field.getType();
				String name = field.getType().getName();

				field.setAccessible(true);
				try {
					if ("java.lang.String".equals(name)) {
						stmt.setString(iField, field.get(t).toString());
					} else if ("java.util.Date".equals(name)) {
						SimpleDateFormat sdf = new SimpleDateFormat(
								"yyyy-MM-dd HH:mm:ss");
						stmt.setString(iField, sdf.format(field.get(t)));
					} else if ("java.lang.Integer".equals(name)
							|| type == Integer.TYPE) {
						stmt.setInt(iField, field.getInt(t));
					} else if ("java.lang.Long".equals(name)
							|| type == Long.TYPE) {
						stmt.setLong(iField, field.getLong(t));
					} else if ("java.lang.Character".equals(name)
							|| type == Character.TYPE) {
						stmt.setString(iField, "" + field.getChar(t));
					} else if ("java.lang.Boolean".equals(name)
							|| type == Boolean.TYPE) {
						stmt.setBoolean(iField, field.getBoolean(t));
					} else if ("java.lang.Float".equals(name)
							|| type == Float.TYPE) {
						stmt.setFloat(iField, field.getFloat(t));
					} else if ("java.lang.Double".equals(name)
							|| type == Double.TYPE) {
						stmt.setDouble(iField, field.getDouble(t));
					} else if ("java.lang.Short".equals(name)
							|| type == Short.TYPE) {
						stmt.setShort(iField, field.getShort(t));
					} else if ("java.lang.Byte".equals(name)
							|| type == Byte.TYPE) {
						stmt.setByte(iField, field.getByte(t));
					} else {
						stmt.setObject(iField, field.get(t));
					}
					// stmt.setObject(iField, field.get(t));
				} catch (Exception e) {
					e.printStackTrace();
				}

				iField++;
			}
		}
	}

	/**
	 * 把map的值给T
	 * 
	 * @param t
	 * @param map
	 */
	private void injectFrom(T t, Map<String, Object> map) {
		for (Field field : t.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Column.class)) {
				Column column = field.getAnnotation(Column.class);

				String columnLabel = column.name();
				// Class<?> type = field.getType();
				// String name = type.getName();

				field.setAccessible(true);
				try {
					field.set(t, map.get(columnLabel));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
