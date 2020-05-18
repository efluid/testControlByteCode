package com.efluid.tcbc;

import static com.efluid.tcbc.process.ScanneClasspath.Exclusion.ERREUR;

import java.util.*;

import org.slf4j.*;

import com.efluid.tcbc.object.Fichier;
import com.efluid.tcbc.process.ScanneClasspath;

/**
 * Contrôle qu'il n'y ait pas de fichier en doublon dans le classpath
 */
public class TestControleFichiersEnDoublon extends ScanneClasspath {

  private static final Logger LOG = LoggerFactory.getLogger(TestControleFichiersEnDoublon.class);

  private static final String FICHIER_CONFIGURATION = "controleClasseEnDoublon.yaml";
  private Map<String, Fichier> fichiersParcourus = new HashMap<>();
  protected Map<String, Set<Fichier>> fichiersEnDoublon = new HashMap<>();

  private static final String PROPERTIES_EXTENSION = "properties";

  public TestControleFichiersEnDoublon() {
    addToExtensions(PROPERTIES_EXTENSION);
  }

  @Override
  protected String getFichierConfiguration() {
    return FICHIER_CONFIGURATION;
  }

  @Override
  protected boolean isScanneRepertoireClasses() {
    return true;
  }

  @Override
  protected boolean scanByJarExclusion() {
    return true;
  }

  @Override
  protected void traitementFichierEnCours() {
    Fichier fichierEnCours = getFichierEnCours();
    if (isExclu(ERREUR, fichierEnCours.getNom())) {
      return;
    }
    Fichier fichierExistant = fichiersParcourus.get(fichierEnCours.getNom());
    if (fichierExistant != null) {
      fichiersEnDoublon.computeIfAbsent(fichierEnCours.getNomEtExtension(), cle -> new HashSet<>()).addAll(Arrays.asList(fichierEnCours, fichierExistant));
    }
    fichiersParcourus.put(fichierEnCours.getNom(), fichierEnCours);
  }

  @Override
  protected int logBilan() {
    super.logBilan();
    LOG.error("|=== Fichiers en doublon ===|");
    fichiersEnDoublon.forEach((nom, fichiers) -> {
      LOG.error("[{}]", nom);
      fichiers.forEach(fichier -> LOG.error("\t{}", fichier.getNomJar()));
    });
    LOG.error("Fichier trouves : {}", fichiersParcourus.size());
    LOG.error("Fichiers en doublon : {}", fichiersEnDoublon.size());
    LOG.error("Fichiers en doublon exclus : {}", getExclusions().get(ERREUR).size());
    return fichiersEnDoublon.size();
  }
}
