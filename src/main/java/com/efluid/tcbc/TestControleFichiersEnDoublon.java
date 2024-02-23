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
    if (isExclu(ERREUR, fichierEnCours.getNomEtExtension())) {
      return;
    }
    Fichier fichierExistant = fichiersParcourus.get(fichierEnCours.getNomEtExtension());
    if (fichierExistant != null) {
      fichiersEnDoublon.computeIfAbsent(fichierEnCours.getNomEtExtension(), cle -> new HashSet<>()).addAll(Arrays.asList(fichierEnCours, fichierExistant));
    }
    fichiersParcourus.put(fichierEnCours.getNomEtExtension(), fichierEnCours);
  }

  @Override
  protected int logBilan() {
    super.logBilan();
    LOG.debug("|=== Fichiers en doublon ===|");
    fichiersEnDoublon.forEach((nom, fichiers) -> {
      LOG.debug("[{}]", nom);
      fichiers.forEach(fichier -> LOG.debug("\t{}", fichier.getNomJar()));
    });
    LOG.debug("Fichier trouves : {}", fichiersParcourus.size());
    LOG.debug("Fichiers en doublon : {}", fichiersEnDoublon.size());
    LOG.debug("Fichiers en doublon exclus : {}", getExclusions().get(ERREUR).size());
    return fichiersEnDoublon.size();
  }
}
