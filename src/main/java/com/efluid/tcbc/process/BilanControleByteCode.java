package com.efluid.tcbc.process;

import static java.lang.System.lineSeparator;

import org.slf4j.*;

import com.efluid.tcbc.TestControleByteCode;
import com.efluid.tcbc.object.*;

public class BilanControleByteCode {

  private static final Logger LOG = LoggerFactory.getLogger(BilanControleByteCode.class);

  private final TestControleByteCode controle;

  public BilanControleByteCode(TestControleByteCode controle) {
    this.controle = controle;
  }

  /**
   * Affiche le bilan du contrôle du byteCode
   */
  public int execute() {
    controle.getJarsTraites().forEach(this::loggerJar);
    loggerSynthese();
    return 0;
  }

  private void loggerSynthese() {
    TestControleByteCode.doLogList(controle.getClassesReferenceesNonTrouveesOuChargees().values(), "Classes référencées non trouvées :");
    LOG.debug("Classes en erreur lors du chargement : {}", controle.getClassesReferenceesNonTrouveesOuChargees().size());
    controle.getClassesReferenceesNonTrouveesOuChargees().values().forEach(classe ->
      LOG.debug("\t{}", classe));
    LOG.debug("Jars en erreur : {}", controle.getJarsTraites().stream().filter(Jar::isErreur).count());
    LOG.debug("=== Synthèse classes en erreur ({}) ===", controle.getJarsTraites().stream().mapToLong(jar -> jar.getClassesEnErreur().size()).sum());
    controle.getJarsTraites().stream().filter(Jar::isErreur).forEach(jar ->
      LOG.debug("\t{} : {}", jar.getNom(), jar.getClassesEnErreur().size() + lineSeparator()));
    LOG.debug("Nombre d'erreurs totales : {}", controle.getJarsTraites().stream().flatMap(jar -> jar.getClassesEnErreur().stream()).mapToInt(Fichier::getNbErreurs).sum());
  }

  private void loggerJar(Jar jar) {
    if (!jar.getClassesEnErreur().isEmpty()) {
      LOG.debug("|=== {} ===|", jar.getNom());
      LOG.debug("\t|=== Classes en erreur ===|");
    }
    for (Fichier classeEnErreur : jar.getClassesEnErreur()) {
      LOG.debug("\t\t{} : {} erreur(s)", classeEnErreur.getNom(), classeEnErreur.getNbErreurs());
      classeEnErreur.getErreurs().forEach(erreur ->
        LOG.debug("\t\t\t{}", erreur));
    }
  }
}
