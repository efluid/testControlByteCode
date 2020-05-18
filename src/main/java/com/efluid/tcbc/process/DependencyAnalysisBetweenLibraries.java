package com.efluid.tcbc.process;

import java.util.concurrent.atomic.AtomicLong;

import com.efluid.tcbc.TestDependenceJar;
import com.efluid.tcbc.object.Fichier;
import com.efluid.tcbc.utils.ScanneClasspathUtils;
import javassist.NotFoundException;

public class DependencyAnalysisBetweenLibraries extends ReadByteCodeClass<TestDependenceJar> {

  public DependencyAnalysisBetweenLibraries(TestDependenceJar control, Fichier currentReadingClass) {
    super(control, currentReadingClass);
  }

  /**
   * On scanne toutes les classes référencées par la classe en cours
   */
  @Override
  protected void analyserMethode(String nomClasse, String nomMethode, String signature) throws NotFoundException {
    analyseDependence(toClass(nomClasse));
  }

  /**
   * Analyse la dépendance, en récupérant la source de la classe et en stockant le chemin.
   */
  private void analyseDependence(Class<?> aClass) {
    if (aClass == null) {
      return;
    }
    String path = ScanneClasspathUtils.getCheminDeLaClasse(aClass);
    // On ne comptabilise pas les dépendances récursives
    if (path != null && !path.equals(getControl().getJarEnCours().getNom())) {
      getControl().getJarEnCours().addDependence(path);
      addDependance(path);
    }
  }

  private void addDependance(String dependance) {
    AtomicLong compteur = getControl().getDependances().get(dependance);
    if (compteur == null) {
      compteur = new AtomicLong(1);
      getControl().getDependances().put(dependance, compteur);
    } else {
      compteur.incrementAndGet();
    }
  }
}
