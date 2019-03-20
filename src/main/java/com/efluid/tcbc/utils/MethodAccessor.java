package com.efluid.tcbc.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Objects;

class MethodAccessor implements Accessor {

  final Object target;
  final Method getter;
  final Method setter;
  String name;

  private static final Logger LOG = LoggerFactory.getLogger(MethodAccessor.class);

  public MethodAccessor(Method method, Object target) {
    this.target = Objects.requireNonNull(target);

    if (isSetter(Objects.requireNonNull(method))) {
      this.setter = method;
      this.getter = findGetter();
    } else if (isGetter(method)) {
      this.getter = method;
      this.setter = findSetter();
    } else {
      throw new AccessorException("Injection impossible : la méthode '" + method + "' n'est ni un setter un un getter");
    }
  }

  boolean isSetter(Method m) {
    if (m.getParameterCount() != 1 || Modifier.isStatic(m.getModifiers())) {
      return false;
    }
    String n = m.getName();
    if (n.length() < 4 || !n.startsWith("set")) {
      return false;
    }
    name = lowercaseFirstChar(n.substring(3));
    return true;
  }

  Method findGetter() {
    try {
      Method m = setter.getDeclaringClass().getMethod("get" + upcaseFirstChar(name));
      if (setter.getParameterTypes()[0].isAssignableFrom(m.getReturnType())) {
        return m;
      }
    } catch (Exception ex) {
      LOG.trace("Getter invalide", ex);
    }

    try {
      Method m = setter.getDeclaringClass().getMethod("is" + upcaseFirstChar(name));
      if (boolean.class.equals(m.getReturnType())) {
        return m;
      }
    } catch (Exception ex) {
      LOG.trace("Getter booleen invalide", ex);
    }

    throw new AccessorException("Impossible de trouver le getter pour le champ " + name);
  }

  boolean isGetter(Method m) {
    if (m.getParameterCount() != 0 || Modifier.isStatic(m.getModifiers())) {
      return false;
    }
    Class<?> type = m.getReturnType();
    if (void.class.equals(type) || Void.class.equals(type)) {
      return false;
    }
    String n = m.getName();
    if (n.length() >= 4 && n.startsWith("get")) {
      name = lowercaseFirstChar(n.substring(3));
      return true;
    }
    if (n.length() >= 3 && n.startsWith("is") && boolean.class.equals(type)) {
      name = lowercaseFirstChar(n.substring(2));
      return true;
    }
    return false;
  }

  Method findSetter() {
    try {
      return getter.getDeclaringClass().getMethod("set" + upcaseFirstChar(name), getter.getReturnType());
    } catch (Exception ex) {
      throw new AccessorException("Impossible de trouver le setter pour le champ " + name, ex);
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Object getValue() {
    try {
      return getter.invoke(target);
    } catch (Exception ex) {
      throw new AccessorException("Impossible de lire le champ " + name + " par son getter", ex);
    }
  }

  @Override
  public void setValue(Object value) {
    try {
      setter.invoke(target, value);
    } catch (Exception ex) {
      throw new AccessorException("Impossible d'écrire le champ " + name + " par son setter", ex);
    }
  }

  @Override
  public Class<?> getType() {
    return getter.getReturnType();
  }

  @Override
  public Type getGenericType() {
    return getter.getGenericReturnType();
  }

  @Override
  public String toString() {
    return getName();
  }

  @Override
  public Object getTarget() {
    return target;
  }

  /**
   * Converts the first character in the String to upper case using the rules of the default locale.
   *
   * @param theString the String
   * @return the new string with the first character to uppercase
   */
  public static String upcaseFirstChar(String theString) {
    if ((theString != null) && (!theString.isEmpty())) {
      StringBuilder buffer = new StringBuilder();
      String tmp = theString.substring(0, 1);
      tmp = tmp.toUpperCase();
      buffer.append(tmp);
      buffer.append(theString.substring(1));
      return (buffer.toString());
    }
    return theString;
  }

  /**
   * Converts the first character in the String to lower case using the rules of the default locale.
   *
   * @param theString the string
   * @return the String whith the first character in lowercase
   */
  public static String lowercaseFirstChar(String theString) {
    if ((theString != null) && (!theString.isEmpty())) {
      StringBuilder buffer = new StringBuilder();
      String tmp = theString.substring(0, 1);
      tmp = tmp.toLowerCase();
      buffer.append(tmp);
      buffer.append(theString.substring(1));
      return (buffer.toString());
    }
    return theString;
  }

}
