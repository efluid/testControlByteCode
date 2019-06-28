package com.efluid.tcbc;

import java.util.*;

import com.efluid.tcbc.object.Classe;
import org.slf4j.*;

/**
 * Contrôle qu'il n'y ait pas de classe en doublon dans le classpath
 */
public class TestControleClasseEnDoublon extends ScanneClasspath {

  private static final Logger LOG = LoggerFactory.getLogger(TestControleClasseEnDoublon.class);

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
    LOG.info("|=== Classes en doublon ===|");
    classesEnDoublon.forEach((nom, classes) -> {
      LOG.info("[" + nom + "]");
      classes.forEach(classe -> LOG.info("\t" + classe.getNomJar()));
    });
    LOG.info("Classes trouvees : " + classesParcourues.size());
    LOG.info("Classes en doublon : " + classesEnDoublon.size());
    LOG.info("Classes en doublon exclues : " + exclusions.get(Exclusion.ERREUR).size());
    return classesEnDoublon.size();
  }
}
