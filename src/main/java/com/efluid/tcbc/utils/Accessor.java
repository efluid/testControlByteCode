package com.efluid.tcbc.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Représentation d'une propriété annotée d'un objet
 */
public interface Accessor {

  /**
   * @param value valeur à affectuer à la propriété (sur le champ ou par appel du setter)
   * @throws AccessorException en cas d'erreur.
   */
  void setValue(Object value);

  /**
   * @return le nom de la propriété ( selon la norme JavaBeans )
   */
  String getName();


  /**
   * @return le type générique de cette propriété
   */
  Type getGenericType();


  /**
   * @return le type de cette propriété
   */
  Class<?> getType();

  /**
   * @return la valeur actuelle de la propriété
   */
  Object getValue();

  /**
   * @return la valeur actuelle de la propriété, castée
   */
  default <T> T getValue(Class<T> expectedType) {
    return expectedType.cast(getValue());
  }

  /**
   * Création d'un accesseur pour un champ
   */
  static Accessor of(Field field, Object target) {
    return new FieldAccessor(field, target);
  }

  /**
   * Création d'un accesseur pour une méthode.
   * <p>
   * Si la méthode est un setter, on cherchera le getter, et inversement.
   *
   * @throws AccessorException en cas d'erreur d'identification de getter ou setter
   */
  static Accessor of(Method method, Object target) {
    return new MethodAccessor(method, target);
  }

  /**
   * @return l'objet cible portant la méthode ou le champ concerné
   */
  Object getTarget();

}
