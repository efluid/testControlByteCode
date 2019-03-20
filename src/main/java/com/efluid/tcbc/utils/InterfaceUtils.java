package com.efluid.tcbc.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Utilitaire de navigation sur les interfaces.
 */
public final class InterfaceUtils {

  /**
   * Class utilitaire
   */
  private InterfaceUtils() {
  }

  /**
   * Recherche récursive de toutes les interfaces déclarées sur l'arborescence de classes.
   * <p>
   * Toutes les interfaces définies sur la classe et ses classes mères sont listées ici (sans doublons).<br>
   * <b>Attention : </b>Les interfaces que l'on hérite d'autres interfaces ne seront pas listées ici.
   *
   * @return la liste de toutes les interfaces implémentées par une classe et ses classe mères.
   */
  public static List<Class<?>> listInterfaces(Class<?> cls) {
    List<Class<?>> interfaces = new ArrayList<>();
    Set<Class<?>> alreadySeen = new HashSet<>();
    for (Class<?> clazz = cls; null != clazz; clazz = clazz.getSuperclass()) {
      for (Class<?> iface : clazz.getInterfaces()) {
        if (alreadySeen.add(iface)) {
          interfaces.add(iface);
        }
      }
    }
    return interfaces;
  }

  /**
   * @param cls       classe dont on souhaite récupérer les interfaces
   * @param stopClass si non nulle, classe mère qui stoppe la récupération des interfaces: Aucune des interfaces déclarées par cette classe ou ses classes parentes ne sera prises en compte
   * @return la liste des interfaces implémentées par une classe et celles implémentées par ses classe mères jusqu'à la <code>stopClass</code> si spécifiée
   */
  public static List<Class<?>> listInterfaces(Class<?> cls, Class<?> stopClass) throws Exception {
    if (null == stopClass) {
      return listInterfaces(cls);
    }
    if (!isSuperClass(cls, stopClass)) {
      throw new Exception("La classe d'arrêt  " + stopClass.getName() + " n'est pas une classe parente de " + cls.getName());
    }

    List<Class<?>> interfaces = listInterfaces(cls);
    List<Class<?>> parentInterfaces = listInterfaces(stopClass);
    interfaces.removeAll(parentInterfaces);
    return interfaces;
  }

  /**
   * @return <code>true</code> si la <code>superClass</code> fait partie des classes parentes de cls, <code>false</code> sinon
   */
  public static boolean isSuperClass(Class<?> cls, Class<?> superClass) {
    return superClass.isAssignableFrom(cls.getSuperclass());
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
   * @param cls             la classe dont on veut inspecter les interfaces
   * @param methodExtractor routine d'identification d'une méthode sur une interface; retourne <code>null</code> si aucune méthode n'a été trouvée sur l'interface
   * @return la méthode correspondante, ou bien <code>null</code> si elle n'a pas pu être trouvée
   */
  public static Method findConcreteMethodOnInterfaces(Class<?> cls, Function<Class<?>, Method> methodExtractor) {
    for (Class<?> iface : listInterfaces(cls)) {
      Method method = methodExtractor.apply(iface);
      if (null != method && MethodPredicates.isConcreteInterfaceMethod(method)) {
        return method;
      }
    }
    return null;
  }

  /**
   * Parcours des méthodes "concrètes" sur les interfaces
   * <p>
   * Le parcours s'arrête dès que le prédicat est vrai.
   */
  public static void browseConcreteMethodsOnInterfaces(Class<?> cls, Predicate<Method> methodFinder) {
    for (Class<?> iface : listInterfaces(cls)) {
      for (Method method : iface.getDeclaredMethods()) {
        if (MethodPredicates.isConcreteInterfaceMethod(method) && methodFinder.test(method)) {
          return;
        }
      }
    }
  }
}
