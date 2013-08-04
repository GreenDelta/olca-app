package org.openlca.app.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import org.apache.commons.beanutils.PropertyUtils;

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

	public static Class<?> getType(Object bean, String property)
			throws Exception {
		PropertyDescriptor descriptor = PropertyUtils.getPropertyDescriptor(
				bean, property);
		if (descriptor != null)
			return descriptor.getPropertyType();
		return null;
	}

	public static void setValue(Object bean, String property, Object value)
			throws Exception {
		Method method = findSetter(bean, property);
		if (method != null) {
			method.setAccessible(true);
			if (isNested(property)) {
				Object fieldValue = getValue(bean, getNestedHead(property));
				setValue(fieldValue, getNestedTail(property), value);
			} else
				method.invoke(bean, value);
		}
	}

	public static Object getValue(Object bean, String property)
			throws Exception {
		Method method = findGetter(bean, property);
		if (method != null) {
			method.setAccessible(true);
			if (isNested(property)) {
				Object fieldValue = getValue(bean, getNestedHead(property));
				return getValue(fieldValue, getNestedTail(property));
			} else
				return method.invoke(bean);
		}
		return null;
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
