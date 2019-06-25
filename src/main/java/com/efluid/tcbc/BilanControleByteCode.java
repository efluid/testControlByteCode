package com.efluid.tcbc;

import static java.lang.System.lineSeparator;

import com.efluid.tcbc.object.Classe;
import com.efluid.tcbc.object.Jar;
import org.slf4j.*;

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
    LOG.info("Classes en erreur lors du chargement : " + controle.getClassesReferenceesNonTrouveesOuChargees().size());
    controle.getClassesReferenceesNonTrouveesOuChargees().values().forEach(classe -> LOG.info("\t" + classe));
    LOG.info("Jars en erreur : " + controle.getJarsTraites().stream().filter(Jar::isErreur).count());
    LOG.info("=== Synthèse classes en erreur (" + controle.getJarsTraites().stream().mapToLong(jar -> jar.getClassesEnErreur().size()).sum() + ") ===");
    controle.getJarsTraites().stream().filter(Jar::isErreur).forEach(jar -> LOG.info("\t" + jar.getNom() + " : " + jar.getClassesEnErreur().size() + lineSeparator()));
    LOG.info("Nombre d'erreurs totales : " + controle.getJarsTraites().stream().flatMap(jar -> jar.getClassesEnErreur().stream()).mapToInt(Classe::getNbErreurs).sum());
  }

  private void loggerJar(Jar jar) {
    if (!jar.getClassesEnErreur().isEmpty()) {
      LOG.info("|=== " + jar.getNom() + " ===|");
      LOG.info("\t|=== Classes en erreur ===|");
    }
    for (Classe classeEnErreur : jar.getClassesEnErreur()) {
      LOG.info("\t\t" + classeEnErreur.getNom() + " : " + classeEnErreur.getNbErreurs() + " erreur(s)");
      classeEnErreur.getErreurs().forEach(erreur -> LOG.info("\t\t\t" + erreur));
    }
  }
}
