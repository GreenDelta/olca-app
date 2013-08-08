package org.openlca.core.application.navigation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class for the comparison of string fields.
 */
public class StringFieldComparator {

	/**
	 * Compares the all string fields of two instances. Returns true if object 1
	 * and object 2 have the same string fields with equal values.
	 */
	public static <T> boolean areEqual(T object1, T object2) {
		if (!canCompare(object1, object2))
			return false;
		List<Field> fields = getFields(object1.getClass());
		for (Field field : fields) {
			if (field.getType().equals(String.class)) {
				if (!compareField(field, object1, object2))
					return false;
			}
		}
		return true;
	}

	private static List<Field> getFields(Class<? extends Object> clazz) {
		List<Field> fields = new ArrayList<>();
		Class<?> fetchClass = clazz;
		while (!fetchClass.equals(Object.class)) {
			List<Field> newFields = Arrays.asList(fetchClass
					.getDeclaredFields());
			fields.addAll(newFields);
			fetchClass = fetchClass.getSuperclass();
		}
		return fields;
	}

	private static boolean canCompare(Object object1, Object object2) {
		if (object1 == null || object2 == null)
			return false;
		if (!object1.getClass().equals(object2.getClass()))
			return false;
		return true;
	}

	private static boolean compareField(Field field, Object object1,
			Object object2) {
		try {
			field.setAccessible(true);
			Object val1 = field.get(object1);
			Object val2 = field.get(object2);
			return compareValues(val1, val2);
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean compareValues(Object val1, Object val2) {
		if (val1 == null && val2 == null)
			return true;
		if (val1 != null && val2 != null && val1.equals(val2))
			return true;
		return false;
	}
}
