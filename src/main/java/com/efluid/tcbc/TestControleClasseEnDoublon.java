package com.efluid.tcbc;

import java.util.*;

/**
 * Contrôle qu'il n'y ait pas de classe en doublon dans le classpath
 */
public class TestControleClasseEnDoublon extends ScanneClasspath {

  private static final String FICHIER_CONFIGURATION = "controleClasseEnDoublon.yaml";
  private Map<String, Classe> classesParcourues = new HashMap<>();
  protected Map<String, Set<Classe>> classesEnDoublon = new HashMap<>();

  @Override
  protected String getFichierConfiguration() {
    return FICHIER_CONFIGURATION;
  }

  @Override
  protected boolean isScanneRepertoireClasses() {
    return true;
  }

  /**
   * Indique de scanner tous les jars sans exclusion
   */
  @Override
  protected boolean isJarInclu(String pathJar) {
    return true;
  }

  @Override
  protected void traitementClasseEnCours() {
    if (isExclu(Exclusion.ERREUR, classeEnCours.getNom())) {
      return;
    }
    Classe classeExistante = classesParcourues.get(classeEnCours.getNom());
    if (classeExistante != null) {
      classesEnDoublon.computeIfAbsent(classeEnCours.getNom(), cle -> new HashSet<>()).addAll(Arrays.asList(classeEnCours, classeExistante));
    }
    classesParcourues.put(classeEnCours.getNom(), classeEnCours);
  }

  @Override
  protected int logBilan() {
    super.logBilan();
    doLog("Classes en doublon : ");
    classesEnDoublon.forEach((nom, classes) -> {
      doLog(" - " + nom + " : ");
      classes.forEach(Classe::afficherJar);
    });
    doLog("Nombre de classes trouvees : " + classesParcourues.size());
    doLog("Nombre de classes en doublon : " + classesEnDoublon.size());
    doLog("Nombre de classes en doublon exclues : " + exclusions.get(Exclusion.ERREUR).size());
    return classesEnDoublon.size();
  }
}
