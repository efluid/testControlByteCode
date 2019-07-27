package com.efluid.tcbc;

import com.efluid.tcbc.object.Jar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Classe de test JUNIT permettant de contrôler le byteCode des classes JAVA du classpath de la JVM en cours. <br>
 * Cela permet de s'assurer que toutes les dépendances appelées existent et sont cohérentes. <br>
 * <br>
 * Utiliser cette d'option de JVM -XX:MaxPermSize=256m (Chargement de toutes les classes)<br>
 * <br>
 * Pour définir le nombre minimum de jar scanné il faut définir la variable d'environnement suivante "-DnbJarMinimum=2". 6 par défaut.<br>
 *
 * @author Vincent BOUTHINON
 */
public class TestControleByteCode extends ScanneClasspath {

  private static final Logger LOG = LoggerFactory.getLogger(TestControleByteCode.class);
  private static final String FICHIER_CONFIGURATION = "controleByteCode.yaml";
  private static final String ENV_NOMBRE_JAR_MINIMUM = "nbJarMinimum";
  private static int nbJarMinimum = System.getProperty(ENV_NOMBRE_JAR_MINIMUM) != null ? Integer.parseInt(System.getProperty(ENV_NOMBRE_JAR_MINIMUM)) : 0;
  /* Utilisés pour effectuer le bilan global du contrôle du byteCode */
  private Map<String, String> classesReferenceesNonTrouveesOuChargees = new HashMap<>();

  @Override
  protected void traitementClasseEnCours() {
    new ReadByteCodeClass(this, classeEnCours).execute();
  }

  /**
   * Ajout d'une erreur si non exclue
   */
  boolean addErreur(String erreur) {
    if (!isExclu(Exclusion.ERREUR, erreur)) {
      jarEnCours.addToClassesEnErreur(classeEnCours).addErreur(erreur);
      LOG.error(erreur);
      return true;
    }
    return false;
  }

  /**
   * Affiche le bilan du contrôle du byteCode
   */
  @Override
  protected int logBilan() {
    super.logBilan();
    return new BilanControleByteCode(this).execute();
  }

  @Override
  protected void isValid(int erreurs) {
    super.isValid(erreurs);
    validerNombreJarMinimumTraite();
  }

  /**
   * Possibilité de configurer le nombre de jar minimum traité via la variable d'environnement -DnbJarMinimum=2
   */
  private void validerNombreJarMinimumTraite() {
    assertThat(jarsTraites.size() > nbJarMinimum).isTrue();
  }

  @Override
  protected String getFichierConfiguration() {
    return FICHIER_CONFIGURATION;
  }

  protected Set<Jar> getJarsTraites() {
    return jarsTraites;
  }

  Map<String, String> getClassesReferenceesNonTrouveesOuChargees() {
    return classesReferenceesNonTrouveesOuChargees;
  }
}
