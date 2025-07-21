package org.komamitsu.chaosdukey.utils;

import java.lang.reflect.Field;
import java.util.Map;

public final class QueryOptionsUtils {
  private QueryOptionsUtils() {}

  @SuppressWarnings("unchecked")
  public static Map<String, String> getQueryOptionsFromServerQueryRequest(Object queryRequest)
      throws Exception {
    Object queryContext = getNestedFieldValue(queryRequest, "_queryContext");
    if (queryContext == null) {
      return null;
    }

    return (Map<String, String>) getNestedFieldValue(queryContext, "_queryOptions");
  }

  private static Object getNestedFieldValue(Object obj, String fieldName) throws Exception {
    if (obj == null) {
      return null;
    }

    Field field = findField(obj.getClass(), fieldName);
    if (field == null) {
      return null;
    }

    field.setAccessible(true);
    return field.get(obj);
  }

  private static Field findField(Class<?> clazz, String fieldName) {
    Class<?> currentClass = clazz;

    while (currentClass != null) {
      try {
        return currentClass.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        currentClass = currentClass.getSuperclass();
      }
    }

    return null;
  }
}
