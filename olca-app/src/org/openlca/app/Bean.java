package org.openlca.app;

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

	public static void setValue(Object bean, String property, Object value)
			throws Exception {
		Method method = findSetter(bean, property);
		if (method != null) {
			method.setAccessible(true);
			method.invoke(bean, value);
		}
	}

	public static Object getValue(Object bean, String property)
			throws Exception {
		Method method = findGetter(bean, property);
		if (method != null) {
			method.setAccessible(true);
			return method.invoke(bean);
		}
		return null;
	}

}
