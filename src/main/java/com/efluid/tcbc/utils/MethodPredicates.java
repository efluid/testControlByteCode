package com.efluid.tcbc.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Recensement de prédicats appliquables aux méthodes.
 */
public final class MethodPredicates {

  /**
   * classe utilitaire
   */
  private MethodPredicates() {
  }

  /**
   * La méthode d'une interface est-elle concrète (i.e. soit une méthode par défaut, soit statique)?
   *
   * @param method la méthode à tester
   * @return <code>true</code> si c'est une méthode concrète
   */
  public static boolean isConcreteInterfaceMethod(Method method) {
    return method.isDefault() || Modifier.isStatic(method.getModifiers());
  }

  /**
   * Vérifie que les types de paramètres sont compatibles avec ceux de la méthode, en tolérant le boxing/unboxing des types primitifs ( int &lt;-&gt; Integer )
   *
   * @param method         méthode de référence
   * @param parameterTypes liste de types de paramètres
   * @return <code>true</code> sir les paramètres sont compatibles avec la méthode
   */
  public static boolean matchesWithAutoboxing(Method method, Class<?>[] parameterTypes) {
    return PrimitiveTypeUtils.isAssignableWithAutoboxing(method.getParameterTypes(), parameterTypes);
  }

}
