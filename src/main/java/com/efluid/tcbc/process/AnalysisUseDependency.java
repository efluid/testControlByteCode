package com.efluid.tcbc.process;

import com.efluid.tcbc.TestAPIsDependenceJar;
import com.efluid.tcbc.object.Fichier;
import com.efluid.tcbc.utils.ScanneClasspathUtils;
import javassist.NotFoundException;

public class AnalysisUseDependency extends ReadByteCodeClass<TestAPIsDependenceJar> {

  public AnalysisUseDependency(TestAPIsDependenceJar control, Fichier currentReadingClass) {
    super(control, currentReadingClass);
  }

  /**
   * Pour chaque méthode appelée, on vérifie si la classe référencée fait partie du jar que l'on est en train de controler
   * Si c'est le cas, alors on stocke l'API appelée.
   */
  @Override
  protected void analyserMethode(String nomClasse, String nomMethode, String signature) throws NotFoundException {
    final Class<?> aClass = toClass(nomClasse);
    if (aClass == null) {
      return;
    }
    String path = ScanneClasspathUtils.getCheminDeLaClasse(aClass);
    if (path != null && path.contains(getControl().getLibraryControl())) {
      getControl().getApis().add(getControl().getFichierEnCours().getNom() + " use : " +aClass + " " + nomMethode + " " + signature);
    }
  }
}
