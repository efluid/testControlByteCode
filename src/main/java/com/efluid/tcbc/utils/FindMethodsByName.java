package com.efluid.tcbc.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Recherche de l'ensemble des méthodes ayant un nom donné.
 * <p>
 * On parcourera l'intégralité des classes / interfaces / méthodes
 */
class FindMethodsByName implements Predicate<Method> {

  final String name;
  final List<Method> found = new ArrayList<>();

  public FindMethodsByName(String name) {
    this.name = name;
  }

  @Override
  public boolean test(Method method) {
    if (method.getName().equals(name)) {
      found.add(method);
    }
    return false;
  }

  public List<Method> get() {
    return found;
  }
}
