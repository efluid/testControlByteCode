package com.efluid.tcbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;

import org.apache.commons.cli.*;
import org.slf4j.*;

import com.efluid.tcbc.process.AnalysisUseDependency;

/**
 * <pre>
 * Affiche toutes les API utilisés d'une dépendance (jar)
 *
 * Exemple "java TestAPIsDependanceJar -jar commons-lang"
 * ou via les variables d'environnements la jvm -djar=commons-lang
 * Penser à augmenter la taille de la JVM "-Xmx1024m"
 *
 * Quelques remarques :
 * - Cela ne donne pas la provenance de l’appel des API.
 * - Les appels peuvent être fait par des dépendances
 * </pre>
 */
public class TestAPIsDependenceJar extends TestControleByteCode {

  private static final Logger LOG = LoggerFactory.getLogger(TestAPIsDependenceJar.class);

  private static final String FICHIER_CONFIGURATION = "apisDependence.yaml";

  public static String ENV_JAR = "jar";
  public static String ENV_ERREUR = "erreur";

  /** Nom du jar qui est contrôlé */
  private String libraryControl;
  /** Si le système détecte une référence au jar contrôlé, on lève une lève une exception */
  private boolean erreur = Boolean.parseBoolean(System.getProperty(ENV_ERREUR));
  private Set<String> apis = new HashSet<>();

  public TestAPIsDependenceJar(String jar) {
    libraryControl = isNullOrEmpty(jar) ? System.getProperty(ENV_JAR) : jar;
  }

  @Override
  protected void traitementClasseEnCours() {
    new AnalysisUseDependency(this, getClasseEnCours()).execute();
  }

  @Override
  protected boolean isScanneRepertoireClasses() {
    return true;
  }

  @Override
  protected boolean isJarInclu(String pathJar) {
    return super.isJarInclu(pathJar) && doNotControlYourself(pathJar);
  }

  @Override
  protected boolean scanByJarExclusion() {
    return true;
  }

  private boolean doNotControlYourself(String pathJar) {
    return !pathJar.contains(libraryControl);
  }

  @Override
  protected String getFichierConfiguration() {
    return FICHIER_CONFIGURATION;
  }

  @Override
  protected int logBilan() {
    super.logBilan();
    doLogList(apis, "Apis used of library : " + libraryControl);
    if (apis.isEmpty()) {
      LOG.error("Aucun appel au jar {}. Verifier son nom et s'il est bien présent dans le classpath : ", libraryControl);
      LOG.error(System.getProperty("java.class.path"));
    } else if (erreur) {
      return apis.size();
    }
    return 0;
  }

  @Override
  public void execute() {
    assertThat(libraryControl).withFailMessage("Variable d'environnement 'jar' obligatoire").isNotBlank();
    super.execute();
  }

  public static void main(String args[]) {
    TestAPIsDependenceJar test = new TestAPIsDependenceJar(parserArgument(args));
    test.init();
    test.execute();
  }

  /**
   * Parse les arguments.
   * Argument -jar [NomDujar]
   */
  public static String parserArgument(String args[]) {
    Options options = getOptions();
    try {
      return new PosixParser().parse(options, args).getOptionValue("jar");
    } catch (ParseException e) {
      new HelpFormatter().printHelp("TestAPIsDependenceJar", options);
      return "";
    }
  }

  private static Options getOptions() {
    Options options = new Options();
    Option option = new Option("jar", true, "Jar name");
    option.setRequired(true);
    options.addOption(option);
    return options;
  }

  public String getLibraryControl() {
    return libraryControl;
  }

  public Set<String> getApis() {
    return apis;
  }
}
