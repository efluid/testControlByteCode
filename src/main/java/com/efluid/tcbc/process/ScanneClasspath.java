package com.efluid.tcbc.process;

import static java.io.File.*;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.*;
import static org.assertj.core.api.Assertions.assertThat;
import static com.efluid.tcbc.process.ScanneClasspath.Exclusion.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.assertj.core.util.VisibleForTesting;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.efluid.tcbc.TestControleByteCode;
import com.efluid.tcbc.object.Fichier;
import com.efluid.tcbc.object.Jar;

/**
 * Canevas permettant de parcourir le classpath fichier par fichier <br>
 * Gestion des exclusions / inclusion par fichier de configuration (fichier yaml) <br>
 * Journal des informations et du temps d'exécution<br>
 * <br>
 * Pour définir un classpath différent que celui par défaut, utiliser la variable d'environnement -Dclasspath=XXX<br>
 */
public abstract class ScanneClasspath {

  private static final Logger LOG = LoggerFactory.getLogger(ScanneClasspath.class);
  private static final String CLASSES_EXTENSION = "class";
  private static final Set<String> EXTENSIONS = new HashSet<>(Collections.singletonList(CLASSES_EXTENSION));

  private static final String ENV_CLASSEPATH = "classpath";

  private final String classpath = System.getProperty(ENV_CLASSEPATH);

  public enum Exclusion {
    FICHIER,
    ERREUR
  }

  /**
   * Curseurs du jar et classe lus en cours
   */
  Fichier fichierEnCours;
  Jar jarEnCours;

  protected void addToExtensions(String extension) {
    EXTENSIONS.add(extension);
  }

  protected Fichier getFichierEnCours() {
    return fichierEnCours;
  }

  public Jar getJarEnCours() {
    return jarEnCours;
  }

  /**
   * Filtre indiquant les jars contrôlés
   */
  private Set<String> jarsInclus = new HashSet<>();
  private Set<String> jarsExcluded = new HashSet<>();
  private Set<String> filtreFichiersExclus = new HashSet<>();
  private Set<String> filtreErreursExclues = new HashSet<>();

  /**
   * Utilisés pour effectuer le bilan global
   */
  Set<Jar> jarsTraites = new HashSet<>();

  Map<Exclusion, Set<String>> exclusions = new EnumMap<>(Exclusion.class);

  protected ScanneClasspath() {
    exclusions.put(ERREUR, new HashSet<>());
    exclusions.put(FICHIER, new HashSet<>());
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
   * traitement à définir pour chaque fichier identifié
   */
  protected abstract void traitementFichierEnCours();

  /**
   * Affiche l'analyse et les erreurs rencontrées
   * @return nombre d'erreur
   */
  protected int logBilan() {
    logExclusion();
    return 0;
  }

  private void logExclusion() {
    doLogList(exclusions.get(FICHIER), "Exclusions des fichiers");
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
        chargerListeConfiguration(configuration, filtreFichiersExclus, "filtreClassesExclues");
        chargerListeConfiguration(configuration, filtreFichiersExclus, "filtreFichiersExclus");
        chargerListeConfiguration(configuration, filtreErreursExclues, "filtreErreursExclues");
      }
    } catch (Exception ex) {
      LOG.error("Erreur lors de la récupération du fichier de configuration {}", getFichierConfiguration());
      LOG.error("", ex);
    }
    if (scanByJarExclusion()) {
      LOG.error("Jars excluded : {}", jarsExcluded);
    } else {
      LOG.error("Jars inclus : {}", jarsInclus);
    }

    LOG.error("Exclusion des fichiers a ne pas traiter : {}", filtreFichiersExclus);
    LOG.error("Erreurs a ne pas traiter: {}", filtreErreursExclues);
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
        scannerJar();
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
      for (Path classe : getFichiers(chemin)) {
        String nomFichierEnCours = classe.toString().substring(chemin.length() + 1);
        String nomFichierEnCoursSansExtension = removeExtension(nomFichierEnCours.replaceAll(separator.equals("/") ? separator : "\\\\", "."));
        fichierEnCours = new Fichier(jarEnCours, nomFichierEnCoursSansExtension, getExtension(nomFichierEnCours));
        if (isExclu(FICHIER, fichierEnCours.getNom())) {
          continue;
        }
        traitementFichierEnCours();
      }
    } catch (Throwable ex) {
      LOG.error("", ex);
    }
  }

  /**
   * Retourne une liste de tous les fichiers du type d'extension défini du répertoire classes
   */
  @VisibleForTesting
  List<Path> getFichiers(String repertoireClasses) throws IOException {
    final List<Path> fichiers = new ArrayList<>();
    Path repertoireClassesPath = Paths.get(repertoireClasses);
    if (notExists(repertoireClassesPath)) {
      return fichiers;
    }
    walkFileTree(repertoireClassesPath, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
        if (EXTENSIONS.contains(getExtension(file.getFileName().toString()))) {
          fichiers.add(file);
        }
        return CONTINUE;
      }
    });

    return fichiers;
  }

  /**
   * Parcours toutes les classes du jar en cours
   */
  private void scannerJar() {
    LOG.error("Scans jar : " + jarEnCours);
    try (JarFile jar = new JarFile(jarEnCours.getNom())) {
      Enumeration<JarEntry> enumeration = jar.entries();
      JarEntry jarEntry;
      // Boucle sur tous les fichiers contenus dans le JAR
      while (enumeration.hasMoreElements()) {
        jarEntry = enumeration.nextElement();
        // Récupération du nom de chaque fichier
        if (EXTENSIONS.contains(getExtension(jarEntry.getName()))) {
          fichierEnCours = new Fichier(jarEnCours, removeExtension(jarEntry.getName().replace("/", ".")), getExtension(jarEntry.getName()));
          if (isExclu(FICHIER, fichierEnCours.getNom())) {
            continue;
          }
          traitementFichierEnCours();
        }
      }
    } catch (Throwable ex) {
      LOG.error("", ex);
    }
  }

  /**
   * Filtre permettant de ne parcourir que les jars souhaités
   * @param pathJar Chemin du jar a tester
   * @return true si le jar est présent
   */
  protected boolean isJarInclu(String pathJar) {
    if (scanByJarExclusion()) {
      return jarsExcluded.stream().noneMatch(jar -> Pattern.compile(jar).matcher(pathJar).find());
    } else {
      return jarsInclus.stream().anyMatch(jar -> Pattern.compile(jar).matcher(pathJar).find());
    }
  }

  protected boolean scanByJarExclusion() {
    return false;
  }

  /**
   * Indique si l'erreur ou la classe est exclue
   */
  public boolean isExclu(Exclusion typeExclusion, final String str) {
    for (String exclusion : (FICHIER.equals(typeExclusion) ? filtreFichiersExclus : filtreErreursExclues)) {
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
      LOG.error("|==== {} ====|", msgEntete);
      liste.stream().sorted().forEach(s -> LOG.error("\t{}", s));
    }
  }

  private String removeExtension(String str) {
    if (isNullOrEmpty(str) || !str.contains(".")) {
      return str;
    }
    return str.substring(0, str.lastIndexOf("."));
  }

  private String getExtension(String nomFichier) {
    return !isNullOrEmpty(nomFichier) && nomFichier.contains(".") ? nomFichier.substring(nomFichier.indexOf(".") + 1) : "";
  }

  public static boolean isNullOrEmpty(String s) {
    return ((s == null) || (s.isEmpty()));
  }
}
