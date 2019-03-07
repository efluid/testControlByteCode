package com.efluid.tcbc.utils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;

/**
 * Classe utilitaire de recherche récursive de méthodes sur une classe ou une arborescence de classes.
 */
public final class MethodLookup {

  /**
   * Classe utilitaire
   */
  private MethodLookup() {
  }

  /**
   * Recherche de méthode sur une classe particulière
   * <p>
   * La méthode pourra avoir n'importe quelle visibilité (private, public, protected, packaged) sur la classe passée en paramètres. Les méthodes héritées sont ignorées.
   * <p>
   * <b>Attention - Usage à éviter tant que possible : </b> Si plusieurs méthodes de même nom existent, avec divers jeux de paramètres, l'une d'elle est choisie <u>arbitrairement</u>.<br>
   * Privilégier la recherche d'une signature explicite avec paramètres : {@link #getDeclaredMethod(Class, String, Class...)}
   *
   * @param theClass   classe à inspecter
   * @param methodName nom de la méthode
   * @return la méthode recherchée, ou bien <code>null</code> si aucune méthode de ce nom n'existe
   */
  public static Method getAnyDeclaredMethodByName(Class<?> theClass, String methodName) {
    for (Method method : theClass.getDeclaredMethods()) {
      if (method.getName().equals(methodName)) {
        return method;
      }
    }
    return null;
  }

  /**
   * Recherche récursive de méthode sur une arborescence complète
   * <p>
   * La méthode pourra avoir n'importe quelle visibilité (private, public, protected, packaged) sur la classe passée en paramètres. La recherche est poursuivie sur les classes parentes, et sur toutes
   * les interfaces.
   * <p>
   * <b>Attention - Usage à éviter tant que possible : </b> Si plusieurs méthodes de même nom existent, avec divers jeux de paramètres, l'une d'elle est choisie <u>arbitrairement</u>.<br>
   * Privilégier la recherche d'une signature explicite avec paramètres : {@link #findMethodInHierarchy(Class, String, Class...)}
   *
   * @param aClass     classe à inspecter
   * @param methodName nom de la méthode
   * @return une méthode ayant le nom recherché, ou bien <code>null</code> si aucune méthode de ce nom n'existe
   */
  public static Method findAnyMethodByNameInHierarchy(Class<?> aClass, String methodName) {
    FindMethodByName byName = new FindMethodByName(methodName);
    browseMethodsInHierarchy(aClass, byName);
    return byName.get();
  }

  /**
   * Recherche récursive des méthodes sur une arborescence complète
   * <p>
   * La méthode pourra avoir n'importe quelle visibilité (private, public, protected, packaged) sur la classe passée en paramètres. La recherche est poursuivie sur les classes parentes, et sur toutes
   * les interfaces.
   *
   * @param aClass     classe à inspecter
   * @param methodName nom de méthode à rechercher
   * @return la liste de toutes les méthodes ayant le nom demandé, ou une liste vide si aucne méthode ne correspond
   */
  public static List<Method> findMethodsByNameInHierarchy(Class<?> aClass, String methodName) {
    FindMethodsByName byName = new FindMethodsByName(methodName);
    browseMethodsInHierarchy(aClass, byName);
    return byName.get();
  }

  /**
   * Parcours récursif de méthodes sur une arborescence complète
   * <p>
   * La méthode pourra avoir n'importe quelle visibilité (private, public, protected, packaged) sur la classe passée en paramètres. La recherche est poursuivie sur les classes parentes, et sur toutes
   * les interfaces.
   * <p>
   * Le parcours s'arrête dès que le prédicat est vrai
   *
   * @param aClass classe à inspecter
   * @param finder call-back appelée à chaque nouvelle méthode rencontrée durant le parcours.
   */
  public static void browseMethodsInHierarchy(Class<?> aClass, Predicate<Method> finder) {
    Class<?> clazz = aClass;
    while (null != clazz) {
      for (Method method : clazz.getDeclaredMethods()) {
        if (finder.test(method)) {
          return;
        }
      }
      clazz = clazz.getSuperclass();
    }
    InterfaceUtils.browseConcreteMethodsOnInterfaces(aClass, finder);
  }

  /**
   * Recherche récursive d'une signature de méthode sur une arborescence complète
   * <p>
   * La méthode pourra avoir n'importe quelle visibilité (private, public, protected, packaged) sur la classe passée en paramètres. La recherche est poursuivie sur les classes parentes, et sur toutes
   * les interfaces.
   *
   * @param aClass         classe à inspecter
   * @param methodName     nom de la méthode
   * @param parameterTypes types des arguments de la méthode
   * @return une méthode ayant le nom recherché, ou bien <code>null</code> si aucune méthode de ce nom n'existe
   */
  public static Method findMethodInHierarchy(Class<?> aClass, String methodName, Class<?>... parameterTypes) {
    Class<?> clazz = aClass;
    while (null != clazz) {
      Method method = getDeclaredMethod(clazz, methodName, parameterTypes);
      if (null != method) {
        return method;
      }
      clazz = clazz.getSuperclass();
    }

    return InterfaceUtils.findConcreteMethodOnInterfaces(aClass, cls -> getDeclaredMethod(cls, methodName, parameterTypes));
  }

  /**
   * Recherche récursive d'une méthode sur une arborescence complète, avec une tolérance sur l'auto-boxing.
   * <p>
   * La méthode pourra avoir n'importe quelle visibilité (private, public, protected, packaged) sur la classe passée en paramètres. La recherche est poursuivie sur les classes parentes, et sur toutes
   * les interfaces.
   *
   * @param aClass         classe à inspecter
   * @param methodName     nom de la méthode
   * @param parameterTypes types des arguments de la méthode. Une recherche tolérante sera faite sur les types primitifs (int &lt;-&gt; Integer par exemple)
   * @return une méthode ayant le nom recherché, ou bien <code>null</code> si aucune méthode de ce nom n'existe
   */
  public static Method findCompatibleMethodInHierarchy(Class<?> aClass, String methodName, Class<?>... parameterTypes) {
    FindMethodByNameAndTypesAutoboxing byNameAndTypes = new FindMethodByNameAndTypesAutoboxing(methodName, parameterTypes);
    browseMethodsInHierarchy(aClass, byNameAndTypes);
    return byNameAndTypes.get();
  }

  /**
   * Recherche d'une méthode spécifique déclarée sur une classe.
   *
   * @return la méthode recherchée, ou <code>null</code> (plutôt qu'une exception) si la méthode n'existe pas.
   */
  public static Method getDeclaredMethod(Class<?> theClass, String methodName, Class<?>... parameterTypes) {
    try {
      return theClass.getDeclaredMethod(methodName, parameterTypes);
    } catch (Exception ex) {
      return null;
    }
  }
}
