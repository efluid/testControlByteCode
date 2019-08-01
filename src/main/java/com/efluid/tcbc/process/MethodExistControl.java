package com.efluid.tcbc.process;

import java.lang.reflect.Method;
import java.util.Arrays;

import com.efluid.tcbc.TestControleByteCode;
import com.efluid.tcbc.object.MethodeCall;
import com.efluid.tcbc.utils.MethodLookup;

public class MethodExistControl {

  private TestControleByteCode controle;
  private MethodeCall methodeCall;

  MethodExistControl(TestControleByteCode controle, MethodeCall methodeCall) {
    this.controle = controle;
    this.methodeCall = methodeCall;
  }

  /**
   * Test l'appel de la méthode
   */
  void execute() {
    try {
      Method method = new MethodLookup(methodeCall.getClasseReferencee(), methodeCall.getNom(), methodeCall.getTypesEntree()).findMethodInHierarchy();
      if (method == null) {
        method = getMethod(methodeCall.getClasseReferencee());
      }
      if (null == method && isPolymorphicSignature()) {
        return;
      }
      if (method == null) {
        controle.addErreur("Methode referencee non trouvee : " + methodeCall);
      } else {
        testerTypeDeRetour(method);
      }
    } catch (NoClassDefFoundError errNoClassDefFound) {
      controle.addErreur("Classe non trouvee lors de la récuperation de la méthode " + errNoClassDefFound + methodeCall);
    } catch (Throwable ex) {
      controle.addErreur("Erreur d'appel de methode : " + ex + methodeCall);
    }
  }

  /**
   * Test d'accès "polymorphique" : le MethodHandle.invoke ne peut être retrouvé par réflexion.
   * <p>
   * On se base sur le marqueur interne du compilo.
   * MethodHandle.PolymorphicSugnature est une annotation interne à la classe, non publique.
   */
  private boolean isPolymorphicSignature() {
    try {
      Method method = methodeCall.getClasseReferencee().getMethod(methodeCall.getNom(), Object[].class);
      return method != null && Arrays.stream(method.getAnnotations()).map(Object::toString).anyMatch("@java.lang.invoke.MethodHandle$PolymorphicSignature()"::equals);
    } catch (Throwable ex) {
      controle.addErreur("Methode (polymorphique) non trouvee : " + ex);
      return false;
    }
  }

  /**
   * Contrôle si le type du retour est identique à celui attendu. La classe du type de retour peut être à l'origine d'une erreur lors du chargement dans le classLoader
   */
  private void testerTypeDeRetour(Method method) {
    try {
      if (methodeCall.getTypeRetour() != method.getReturnType()) {
        controle.addErreur("Type de retour [" + method.getReturnType().getSimpleName() + "] different de " + methodeCall);
      }
    } catch (Throwable ex) {
      controle.addErreur("Erreur lors du chargement de la classe du type de retour : " + methodeCall);
    }
  }

  /**
   * Récupère la méthode en appliquant la récursivité (sur les classes parents)
   */
  private Method getMethod(Class<?> classe) {
    try {
      return classe.getMethod(methodeCall.getNom(), methodeCall.getTypesEntree());
    } catch (NoSuchMethodException ex) {
      Class<?> supClass = classe.getSuperclass();
      return supClass != null ? getMethod(supClass) : null;
    }
  }
}
