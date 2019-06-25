package com.efluid.tcbc.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.reflect.Modifier.isStatic;

/**
 * Classe utilitaire de recherche récursive de méthodes sur une classe ou une arborescence de classes.
 */
public final class MethodLookup {

  private Class<?> aClass;
  private String methodName;
  private Class<?>[] parameterTypes;

  public MethodLookup(Class<?> aClass, String methodName, Class<?>[] parameterTypes) {
    this.aClass = aClass;
    this.methodName = methodName;
    this.parameterTypes = parameterTypes;
  }

  /**
   * Recherche récursive d'une signature de méthode sur une arborescence complète
   * <p>
   * La méthode pourra avoir n'importe quelle visibilité (private, public, protected, packaged) sur la classe passée en paramètres. La recherche est poursuivie sur les classes parentes, et sur toutes
   * les interfaces.
   *
   * @return une méthode ayant le nom recherché, ou bien <code>null</code> si aucune méthode de ce nom n'existe
   */
  public Method findMethodInHierarchy() {
    Class<?> clazz = aClass;
    while (null != clazz) {
      Method method = getDeclaredMethod(clazz);
      if (null != method) {
        return method;
      }
      clazz = clazz.getSuperclass();
    }

    return findConcreteMethodOnInterfaces();
  }

  /**
   * Recherche d'une méthode "concrète" sur les interfaces
   * <p>
   * On s'arrêtera à la première méthode trouvée respectant les critères :
   * <ul>
   * <li>une méthode non-nulle est proposée par <code>methodExtractor</code></li>
   * <li>cette méthode est concrète (default ou statique)</li>
   * </ul>
   *
   * @return la méthode correspondante, ou bien <code>null</code> si elle n'a pas pu être trouvée
   */
  private Method findConcreteMethodOnInterfaces() {
    for (Class<?> iface : getInterfaces()) {
      Method method = getDeclaredMethod(iface);
      if (null != method && (method.isDefault() || isStatic(method.getModifiers()))) {
        return method;
      }
    }
    return null;
  }

  /**
   * Recherche d'une méthode spécifique déclarée sur une classe.
   *
   * @return la méthode recherchée, ou <code>null</code> (plutôt qu'une exception) si la méthode n'existe pas.
   */
  private Method getDeclaredMethod(Class<?> theClass) {
    try {
      return theClass.getDeclaredMethod(methodName, parameterTypes);
    } catch (Exception ex) {
      return null;
    }
  }

  /**
   * Recherche récursive de toutes les interfaces déclarées sur l'arborescence de classes.
   * <p>
   * Toutes les interfaces définies sur la classe et ses classes mères sont listées ici (sans doublons).<br>
   * <b>Attention : </b>Les interfaces que l'on hérite d'autres interfaces ne seront pas listées ici.
   *
   * @return la liste de toutes les interfaces implémentées par une classe et ses classe mères.
   */
  private List<Class<?>> getInterfaces() {
    List<Class<?>> interfaces = new ArrayList<>();
    Set<Class<?>> alreadySeen = new HashSet<>();
    for (Class<?> clazz = aClass; null != clazz; clazz = clazz.getSuperclass()) {
      for (Class<?> iface : clazz.getInterfaces()) {
        if (alreadySeen.add(iface)) {
          interfaces.add(iface);
        }
      }
    }
    return interfaces;
  }
}
