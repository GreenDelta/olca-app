package org.openlca.app.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bean {

	public static Method findSetter(Object bean, String property)
			throws Exception {
		PropertyDescriptor descriptor = PropertyUtils.getPropertyDescriptor(
				bean, property);
		if (descriptor != null)
			return PropertyUtils.getWriteMethod(descriptor);
		return null;
	}

	public static Method findGetter(Object bean, String property)
			throws Exception {
		PropertyDescriptor descriptor = PropertyUtils.getPropertyDescriptor(
				bean, property);
		if (descriptor != null)
			return PropertyUtils.getReadMethod(descriptor);
		return null;
	}

	public static Field findField(Object bean, String name) {
		if (bean == null)
			return null;
		return _findField(bean.getClass(), name);
	}

	private static Field _findField(Class<?> clazz, String name) {
		if (clazz == Object.class)
			return null;
		try {
			Field field = clazz.getDeclaredField(name);
			if (field != null)
				return field;
		} catch (Exception e) {
			// if field does not exist, move on
		}
		return _findField(clazz.getSuperclass(), name);
	}

	public static Class<?> getType(Object bean, String prop) {
		if (bean == null || prop == null)
			return null;
		try {
			Class<?> c = bean.getClass();
			String[] props = prop.split("\\.");
			Field field = null;
			for (String p : props) {
				field = c.getDeclaredField(p);
				if (field == null)
					return null;
				c = field.getType();
			}
			return c;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Bean.class);
			log.error("Failed to get type of field "
					+ prop + " in " + bean, e);
			return null;
		}
	}

	public static void setValue(Object bean, String property, Object value)
			throws Exception {
		while (isNested(property)) {
			bean = _getValue(bean, getNestedHead(property));
			property = getNestedTail(property);
		}
		_setValue(bean, property, value);
	}

	private static void _setValue(Object bean, String property, Object value)
			throws Exception {
		Method method = findSetter(bean, property);
		if (method != null && method.isAccessible()) {
			method.invoke(bean, value);
			return;
		}
		Field field = findField(bean, property);
		if (field == null)
			return;
		boolean wasAccessible = field.isAccessible();
		field.setAccessible(true);
		field.set(bean, value);
		field.setAccessible(wasAccessible);
	}

	public static Object getValue(Object bean, String property)
			throws Exception {
		while (isNested(property)) {
			bean = _getValue(bean, getNestedHead(property));
			property = getNestedTail(property);
		}
		return _getValue(bean, property);
	}

	private static Object _getValue(Object bean, String property)
			throws Exception {
		Method method = findGetter(bean, property);
		if (method != null && method.isAccessible())
			return method.invoke(bean);
		Field field = findField(bean, property);
		if (field == null)
			return null;
		boolean wasAccessible = field.isAccessible();
		field.setAccessible(true);
		Object value = field.get(bean);
		field.setAccessible(wasAccessible);
		return value;
	}

	private static boolean isNested(String property) {
		return property.contains(".");
	}

	private static String getNestedHead(String property) {
		return property.substring(0, property.indexOf("."));
	}

	private static String getNestedTail(String property) {
		return property.substring(property.indexOf(".") + 1);
	}

}
