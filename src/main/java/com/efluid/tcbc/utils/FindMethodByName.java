package com.efluid.tcbc.utils;

import java.lang.reflect.Method;
import java.util.function.Predicate;

/**
 * Recherche de la première méthode trouvée, à partir de son nom
 */
class FindMethodByName implements Predicate<Method> {

  final String name;
  Method found;

  public FindMethodByName(String name) {
    this.name = name;
  }

  @Override
  public boolean test(Method method) {
    if (method.getName().equals(name)) {
      found = method;
    }
    return null != found;
  }

  public Method get() {
    return found;
  }
}
