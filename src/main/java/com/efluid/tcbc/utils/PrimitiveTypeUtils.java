package com.efluid.tcbc.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilitaire de manipulation des types primitifs.
 * <p>
 * Ces types sont : {@code boolean}, {@code byte}, {@code char}, {@code double}, {@code float}, {@code int}, {@code long}, {@code short}
 * <p>
 * Classe de support pour les opérations de boxing/unboxing, nécessaires à la réflection.
 */
public final class PrimitiveTypeUtils {

  static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER = new HashMap<>();
  static final Map<String, Class<?>> PRIMITIVE_CLASS_NAMES = new HashMap<>();

  static {
    PRIMITIVE_WRAPPER.put(Boolean.TYPE, Boolean.class);
    PRIMITIVE_WRAPPER.put(Byte.TYPE, Byte.class);
    PRIMITIVE_WRAPPER.put(Character.TYPE, Character.class);
    PRIMITIVE_WRAPPER.put(Short.TYPE, Short.class);
    PRIMITIVE_WRAPPER.put(Integer.TYPE, Integer.class);
    PRIMITIVE_WRAPPER.put(Long.TYPE, Long.class);
    PRIMITIVE_WRAPPER.put(Float.TYPE, Float.class);
    PRIMITIVE_WRAPPER.put(Double.TYPE, Double.class);
    PRIMITIVE_WRAPPER.keySet().forEach(c -> PRIMITIVE_CLASS_NAMES.put(c.getSimpleName(), c));
  }

  /**
   * La classe est-elle une classe d'encapsulation du type primitif?
   *
   * @param thisClass la classe a tester
   * @return <code>true</code> si la classe est une classe d'encapsulation du type primitif.
   */
  public static boolean isPrimitiveWrapper(Class<?> thisClass) {
    return PRIMITIVE_WRAPPER.containsValue(thisClass);
  }

  /**
   * Teste si le nom de classe spécifié est un type natif.
   *
   * @param name le nom de classe à évaluer
   * @return true s'il s'agit d'un type primitif.
   */
  public static boolean isPrimitiveClassName(String name) {
    return PRIMITIVE_CLASS_NAMES.containsKey(name);
  }

  /**
   * Teste si la classe désigne un type primitif
   *
   * @param cls la {@link Class}e à évaluer
   * @return <code>true</code> s'il s'agit d'un type primitif.
   */
  public static boolean isPrimitiveClass(Class<?> cls) {
    return cls != null && cls.isPrimitive();
  }

  /**
   * Retourne la classe primitive correspondant au nom passé en paramètre
   *
   * @param name nom dy type primitif: "int", "short", "long", "float", "double", "boolean", "char" ou "byte"
   * @return la classe primitive associée ( int.class, ...)
   */
  public static Class<?> getPrimitiveClass(String name) {
    return PRIMITIVE_CLASS_NAMES.get(name);
  }

  /**
   * La classe passée en paramètre est-elle une classe numérique?
   *
   * @param cls la classe à évaluer
   * @return <code>true</code> si cette classe ou type primitif peut être ramenée à un {@link Number}
   */
  public static boolean isNumberClass(Class<?> cls) {
    return cls != null && Number.class.isAssignableFrom(toWrapper(cls));
  }

  /**
   * Retourne la classe encapsulant le type primitif passé en paramètre
   *
   * @param primitive le type primitif (byte, int, ...)
   * @return La classe correspondant au type primitif, ou <code>null</code> sir le paramètres n'était pas un type primitif.
   */
  public static Class<?> getWrapper(Class<?> primitive) {
    return primitive != null && primitive.isPrimitive() ? PRIMITIVE_WRAPPER.get(primitive) : null;
  }

  /**
   * Encapsule les types primitifs.
   *
   * @param aClass une classe
   * @return la classe d'encapsulation du type primitif, le cas échéant. Sinon, la classe initiale <b>aClass</b> est retournée.
   */
  public static Class<?> toWrapper(Class<?> aClass) {
    Class<?> wrapper = getWrapper(aClass);
    return wrapper != null ? wrapper : aClass;
  }

  /**
   * @param thisClass the class
   * @return true if the given class is a primitive, a primitive wrapper or String.class
   */
  public static boolean isSimpleType(Class<?> thisClass) {
    return thisClass.isPrimitive() ||
      isPrimitiveWrapper(thisClass) ||
      String.class.equals(thisClass);
  }

  /**
   * Vérifie si la classe "fromThisClass" est du type de "theClass", en prenant en compte les types primitifs (boxing/unboxing)
   *
   * @param theClass      type de référence
   * @param fromThisClass type à tester
   * @return <code>true</code> si la classe à tester est compatible avec la classe de référence
   */
  public static boolean isAssignableWithAutoboxing(Class<?> theClass, Class<?> fromThisClass) {
    if (theClass.isAssignableFrom(fromThisClass)) {
      return true;
    }

    if (theClass.isPrimitive()) {
      if (fromThisClass.equals(getWrapper(theClass))) {
        return true;
      }
    } else if (fromThisClass.isPrimitive()) {
      if (theClass.equals(getWrapper(fromThisClass))) {
        return true;
      }
    }

    return false;
  }

  /**
   * Vérifie si les paramètres de "fromThisClasses" sont du type de ceux de "theClasses" de même rang, en prenant en compte les types primitifs (boxing/unboxing)
   * <p>
   * Cette méthode a pour objectif de vérifier si un ensemble de paramètres peuvent être appliqués à ceux d'une méthode
   *
   * @param theClasses      types de référence (arguments d'une méthode)
   * @param fromThisClasses liste de types de paramètres à valider
   * @return <code>true</code> si l'ensemble de types sont compatibles
   */
  public static boolean isAssignableWithAutoboxing(Class<?>[] theClasses, Class<?>[] fromThisClasses) {
    int fromThisClassesLength = null == fromThisClasses ? 0 : fromThisClasses.length;
    if (theClasses.length != fromThisClassesLength) {
      return false;
    }
    for (int i = 0; i < theClasses.length; ++i) {
      if (!PrimitiveTypeUtils.isAssignableWithAutoboxing(theClasses[i], fromThisClasses[i])) {
        return false;
      }
    }
    return true;
  }

}
