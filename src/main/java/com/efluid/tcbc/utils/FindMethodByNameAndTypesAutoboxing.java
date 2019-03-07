package com.efluid.tcbc.utils;

import java.lang.reflect.Method;
import java.util.function.Predicate;

/**
 * Recherche de méthodes de même nom et ayant ds types de paramètres compatibles avec ceux spécifiés
 */
class FindMethodByNameAndTypesAutoboxing implements Predicate<Method> {

  final String name;
  final Class<?>[] parameterTypes;
  Method found;

  public FindMethodByNameAndTypesAutoboxing(String name, Class<?>... parameterTypes) {
    this.name = name;
    this.parameterTypes = parameterTypes;
  }

  @Override
  public boolean test(Method method) {
    if (method.getName().equals(name) && MethodPredicates.matchesWithAutoboxing(method, parameterTypes)) {
      found = method;
    }
    return null != found;
  }

  public Method get() {
    return found;
  }
}
