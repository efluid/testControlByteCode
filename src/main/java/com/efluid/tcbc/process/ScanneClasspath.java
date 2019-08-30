package com.efluid.tcbc.process;

import static com.efluid.tcbc.process.ScanneClasspath.Exclusion.*;
import static java.io.File.separatorChar;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.*;

import org.junit.*;
import org.slf4j.*;
import org.yaml.snakeyaml.Yaml;

import com.efluid.tcbc.TestControleByteCode;
import com.efluid.tcbc.object.*;

/**
 * Canevas permettant de parcourir le classpath classe par classe <br>
 * Gestion des exclusions / inclusion par fichier de configuration (fichier properties) <br>
 * Journal des informations et du temps d'exécution<br>
 * <br>
 * Pour définir un autre classpath que celui par défaut, utiliser la variable d'environnement -Dclasspath=XXX<br>
 */
public abstract class ScanneClasspath {

  private static final Logger LOG = LoggerFactory.getLogger(ScanneClasspath.class);

  private static final String ENV_CLASSEPATH = "classpath";
  private static final String CLASSES_EXTENSION = ".class";
  private String classpath = System.getProperty(ENV_CLASSEPATH);

  public enum Exclusion {
    CLASSE,
    ERREUR
  }

  /**
   * Curseurs du jar et classe lus en cours
   */
  Classe classeEnCours;
  Jar jarEnCours;

  protected Classe getClasseEnCours() {
    return classeEnCours;
  }

  public Jar getJarEnCours() {
    return jarEnCours;
  }

  /**
   * Filtre indiquant les jars contrôlés
   */
  private Set<String> jarsInclus = new HashSet<>();
  private Set<String> jarsExcluded = new HashSet<>();
  private Set<String> filtreClassesExclues = new HashSet<>();
  private Set<String> filtreErreursExclues = new HashSet<>();

  /**
   * Utilisés pour effectuer le bilan global
   */
  Set<Jar> jarsTraites = new HashSet<>();

  Map<Exclusion, Set<String>> exclusions = new HashMap<>();

  protected ScanneClasspath() {
    exclusions.put(ERREUR, new HashSet<>());
    exclusions.put(CLASSE, new HashSet<>());
  }

  public Map<Exclusion, Set<String>> getExclusions() {
    return exclusions;
  }

  public Set<Jar> getJarsTraites() {
    return jarsTraites;
  }

  private void addToExclusions(Exclusion typeExclusion, String exclusion) {
    exclusions.get(typeExclusion).add(exclusion);
  }

  /**
   * @return nom du fichier de propriété permettant de configurer le scan
   */
  protected abstract String getFichierConfiguration();

  /**
   * traitement à définir pour chaque classe scannée
   */
  protected abstract void traitementClasseEnCours();

  /**
   * Affiche l'analyse et les erreurs rencontrées
   *
   * @return nombre d'erreur
   */
  protected int logBilan() {
    logExclusion();
    return 0;
  }

  private void logExclusion() {
    doLogList(exclusions.get(CLASSE), "Exclusions des classes");
    doLogList(exclusions.get(ERREUR), "Exclusions des erreurs");
  }

  @Before
  public void init() {
    chargerConfiguration();
  }

  @Test
  public void execute() {
    execute(classpath != null ? new String[] { classpath } : null);
  }

  /**
   * Lance l'exécution du contrôle du byteCode
   */
  private void execute(String... classpath) {
    scannerClasspaths(classpath);
    int erreurs = logBilan();
    terminate();
    isValid(erreurs);
  }

  /**
   * Aucune erreur ne doit être remontée
   *
   * @param erreurs Nombre d’erreur
   */
  protected void isValid(int erreurs) {
    assertThat(0).isEqualTo(erreurs);
  }

  protected void terminate() {
    /* Permet d'effectuer un traitement à la fin du scan */
  }

  /**
   * Charge la configuration de la classe de test, en l'occurrence : Liste des classes à ne pas contrôler
   */
  private void chargerConfiguration() {
    try {
      InputStream is = TestControleByteCode.class.getClassLoader().getResourceAsStream(getFichierConfiguration());
      if (is == null) {
        LOG.error("Configuration file not found : {}", getFichierConfiguration());
        return;
      }
      Map<String, ArrayList<String>> configuration = new Yaml().load(is);
      if (configuration != null) {
        chargerListeConfiguration(configuration, jarsInclus, "jarsInclus");
        chargerListeConfiguration(configuration, jarsExcluded, "jarsExcluded");
        chargerListeConfiguration(configuration, filtreClassesExclues, "filtreClassesExclues");
        chargerListeConfiguration(configuration, filtreErreursExclues, "filtreErreursExclues");
      }
    } catch (Throwable ex) {
      LOG.error("Erreur lors de la récupération du fichier de configuration {}", getFichierConfiguration());
      LOG.error("STACKTRACE", ex);
    }
    if (scanByJarExclusion()) {
      LOG.info("Jars excluded : {}", jarsExcluded);
    } else {
      LOG.info("Jars inclus : {}", jarsInclus);
    }

    LOG.info("Exclusion des classes a ne pas traiter : {}", filtreClassesExclues);
    LOG.info("Erreurs a ne pas traiter: {}", filtreErreursExclues);
  }

  /**
   * Ajoute les éléments chargés dans le fichier de configuration pour les placer dans la liste correspondante (gère la nullité)
   */
  private static void chargerListeConfiguration(Map<String, ArrayList<String>> configuration, Collection<String> liste, String nomListe) {
    Collection<String> listeAAjouter = configuration.get(nomListe);
    if (listeAAjouter != null) {
      liste.addAll(listeAAjouter);
    }
  }

  /**
   * Parcourt toutes les jars du classpath. Celui passé en paramètre, sinon celui de la JVM en cours
   */
  private void scannerClasspaths(String... classpaths) {
    String[] chemins = classpaths;
    // Si aucun classpath spécifié on récupère celui du projet en cours
    if (chemins == null || chemins.length == 0) {
      chemins = System.getProperty("java.class.path").split(File.pathSeparator);
    }

    for (String path : chemins) {
      if (path.endsWith(".jar") && isJarInclu(path)) {
        initJarEnCours(path);
        controlerJar();
      } else if (path.endsWith("classes") && isScanneRepertoireClasses()) {
        initJarEnCours(path);
        scannerRepertoireClasses(path);
      }
    }
  }

  private void initJarEnCours(String path) {
    jarEnCours = new Jar(path);
    jarsTraites.add(jarEnCours);
  }

  protected boolean isScanneRepertoireClasses() {
    return false;
  }

  /**
   * Scanne le répertoire classes défini dans le classpath
   */
  private void scannerRepertoireClasses(String chemin) {
    try {
      for (Path classe : getFichiersClass(chemin)) {
        String nomClasseEnCours = classe.toString().substring(chemin.length() + 1);
        nomClasseEnCours = removeEnd(nomClasseEnCours.replaceAll(
          File.separator.equals("/") ? File.separator : "\\\\", "."));
        classeEnCours = new Classe(jarEnCours, nomClasseEnCours);
        if (isExclu(CLASSE, classeEnCours.getNom())) {
          continue;
        }
        traitementClasseEnCours();
      }
    } catch (Throwable ex) {
      LOG.error("STACKTRACE", ex);
    }
  }

  /**
   * Retourne une liste de tous les fichiers ".class" du répertoire repertoireClasses
   */
  private List<Path> getFichiersClass(String repertoireClasses) throws IOException {
    final List<Path> fichiersClass = new ArrayList<>();
    Path repertoireClassesPath = Paths.get(repertoireClasses);
    if (Files.notExists(repertoireClassesPath)) {
      return fichiersClass;
    }
    Files.walkFileTree(repertoireClassesPath, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
        if (file.getFileName().toString().endsWith(CLASSES_EXTENSION)) {
          fichiersClass.add(file);
        }
        return FileVisitResult.CONTINUE;
      }
    });

    return fichiersClass;
  }

  /**
   * Parcours toutes les classes du jar en cours
   */
  private void controlerJar() {
    LOG.info("Scans jar : " + jarEnCours);
    try (JarFile jar = new JarFile(jarEnCours.getNom())) {
      Enumeration<JarEntry> enumeration = jar.entries();
      JarEntry jarEntry;
      // Boucle sur tous les fichiers contenus dans le JAR
      while (enumeration.hasMoreElements()) {
        jarEntry = enumeration.nextElement();
        // Récupération du nom de chaque fichier
        if (jarEntry.getName().endsWith(CLASSES_EXTENSION)) {
          classeEnCours = new Classe(jarEnCours, removeEnd(jarEntry.getName().replaceAll("/", ".")));
          if (isExclu(CLASSE, classeEnCours.getNom())) {
            continue;
          }
          traitementClasseEnCours();
        }
      }
    } catch (Throwable ex) {
      LOG.error("STACKTRACE", ex);
    }
  }

  /**
   * Filtre permettant de ne parcourir que les jars souhaités
   *
   * @param pathJar Chemin du jar a tester
   * @return true si le jar est présent
   */
  protected boolean isJarInclu(String pathJar) {
    if (scanByJarExclusion()) {
      return !jarsExcluded.stream().anyMatch(jar -> pathJar.contains(separatorChar + jar));
    } else {
      return jarsInclus.stream().anyMatch(jar -> pathJar.contains(separatorChar + jar));
    }
  }

  protected boolean scanByJarExclusion() {
    return false;
  }

  /**
   * Indique si l'erreur ou la classe est exclue
   */
  public boolean isExclu(Exclusion typeExclusion, final String str) {
    for (String exclusion : (CLASSE.equals(typeExclusion) ? filtreClassesExclues : filtreErreursExclues)) {
      if (str.toLowerCase().contains(exclusion.toLowerCase())) {
        addToExclusions(typeExclusion, str);
        return true;
      }
    }
    return false;
  }

  public static void doLogList(Collection<String> col, String msgEntete) {
    if (col != null && !col.isEmpty()) {
      List<String> liste = (List<String>) ((col instanceof List) ? col : new ArrayList<>(col));
      LOG.info("|==== {} ====|", msgEntete);
      liste.stream().sorted().forEach(s -> LOG.info("\t{}", s));
    }
  }

  private static String removeEnd(String str) {
    if (isNullOrEmpty(str) || isNullOrEmpty(CLASSES_EXTENSION) || !str.endsWith(CLASSES_EXTENSION)) {
      return str;
    }
    return str.substring(0, str.length() - CLASSES_EXTENSION.length());
  }

  public static boolean isNullOrEmpty(String s) {
    return ((s == null) || (s.isEmpty()));
  }
}
