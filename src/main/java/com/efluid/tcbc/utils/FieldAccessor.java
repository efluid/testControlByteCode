package com.efluid.tcbc.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Accesseur pour un champ
 */
class FieldAccessor implements Accessor {

  final Field field;
  final Object target;

  public FieldAccessor(Field field, Object target) {
    this.field = Objects.requireNonNull(field);
    this.target = Objects.requireNonNull(target);

    if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
      throw new AccessorException("Le champ " + field + " n'est pas injectable (est statique ou final)");
    }
  }

  @Override
  public String getName() {
    return field.getName();
  }

  @Override
  public Object getValue() {
    try {
      if (!field.isAccessible()) {
        field.setAccessible(true);
      }
      return field.get(target);
    } catch (Exception ex) {
      throw new AccessorException("Impossible de lire le champ " + getName(), ex);
    }
  }

  @Override
  public void setValue(Object value) {
    try {
      if (!field.isAccessible()) {
        field.setAccessible(true);
      }
      field.set(target, value);
    } catch (Exception ex) {
      throw new AccessorException("Impossible d'écrire le champ " + getName(), ex);
    }
  }

  @Override
  public Class<?> getType() {
    return field.getType();
  }

  @Override
  public Type getGenericType() {
    return field.getGenericType();
  }


  @Override
  public String toString() {
    return getName();
  }

  @Override
  public Object getTarget() {
    return target;
  }

}
