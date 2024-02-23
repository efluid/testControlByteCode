package com.efluid.tcbc;

import static java.lang.System.lineSeparator;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.*;

import com.efluid.tcbc.object.Jar;
import com.efluid.tcbc.process.DependencyAnalysisBetweenLibraries;

/**
 * <pre>
 * Détermine les dépendances entre les jars (classes inclus).
 * Génère un fichier graphviz.
 *
 * Quelques points :
 * - N’est comptabilisé qu’une fois l’appel d’une classe par classe lue (par type de classe).
 * Si une classe appelle plusieurs fois « String », on ne comptera qu’une fois la dépendance vers la jar contenant « String ».
 * - Ne sont pas comptabilisé les dépendances internes
 * Si une classe appelle une classe contenue dans le même jar, cette dépendance n’est pas comptabilisée
 * - Le répertoire « classes » est considéré comme un jar
 * </pre>
 */
public class TestDependenceJar extends TestControleByteCode {

  private static final Logger LOG = LoggerFactory.getLogger(TestDependenceJar.class);

  private static final String FICHIER_CONFIGURATION = "dependenceJar.yaml";
  public static String NOM_FICHIER_GRAPHVIZ = "dependenceJar.dot";

  /** Nombre de dépendance total */
  private Map<String, AtomicLong> dependances = new HashMap<String, AtomicLong>();
  private StringBuffer fichierGraphViz = new StringBuffer("digraph dependence {" + lineSeparator() + "\tlayout=dot;concentrate=true;node [shape=box];edge [color=blue];classes [color=red];" + lineSeparator());

  @Override
  protected void traitementFichierEnCours() {
    new DependencyAnalysisBetweenLibraries(this, getFichierEnCours()).execute();
  }

  public Map<String, AtomicLong> getDependances() {
    return dependances;
  }

  @Override
  protected String getFichierConfiguration() {
    return FICHIER_CONFIGURATION;
  }

  @Override
  protected boolean isScanneRepertoireClasses() {
    return true;
  }

  /** On ne souhaite gérer les erreurs de chargement */
  @Override
  public boolean addErreur(final String erreur) {
    return false;
  }

  @Override
  protected int logBilan() {
    for (Jar jar : getJarsTraites()) {
      logDependances("Dependences of " + jar.getNom(), jar.getDependences(), true);
    }
    logDependances("Total reference", dependances, false);
    logJarNonReference();
    return 0;
  }

  @Override
  protected void terminate() {
    creerFichierGraphViz();
  }

  /**
   * Log les dépendances entre triant par le compteur (valeur desc)
   */
  private void logDependances(String nom, Map<String, AtomicLong> dependances, boolean graphViz) {
    TreeMap<String, AtomicLong> mapTrie;
    if (!dependances.isEmpty()) {
      LOG.error(nom + " :");
      mapTrie = new TreeMap<String, AtomicLong>(new ValueComparator(dependances));
      mapTrie.putAll(dependances);
      for (String cle : mapTrie.keySet()) {
        LOG.debug("\treference {} type class of {}", mapTrie.get(cle), cle);
        if (graphViz) {
          addToFichierGraphViz(nom, cle, mapTrie.get(cle).toString());
        }
      }
    }
  }

  private void addToFichierGraphViz(String jar, String dependance, String nombre) {
    String nomJar = jar.substring(jar.lastIndexOf(File.separator) + 1);
    String nomDependance = dependance.substring(dependance.lastIndexOf(File.separator) + 1);

    fichierGraphViz.append("\t" + "\"" + nomJar + "\" -> \"" + nomDependance + "\" [label=\"" + nombre + "\"];" + lineSeparator());
  }

  private void logJarNonReference() {
    LOG.error("");
    getJarsTraites().stream().sorted().filter(jar -> !dependances.containsKey(jar.getNom())).forEach(jar -> LOG.error("jar not referenced : {}", jar.getNom()));
  }

  @Override
  protected boolean scanByJarExclusion() {
    return true;
  }

  /**
   * Permet de trier une Map par ses valeurs en l'occurence un compteur
   * On concatène la clé dans le cas où plusieurs valeurs seraient identique
   * et qui aurait comme effet de considérer cette entrée (Map.Entry) comme déjà existante
   */
  class ValueComparator implements Comparator {

    Map<String, AtomicLong> map;

    public ValueComparator(Map<String, AtomicLong> base) {
      this.map = base;
    }

    public int compare(Object a, Object b) {
      String valA = "" + map.get(a) + a;
      String valB = "" + map.get(b) + b;
      return Comparator.<String> naturalOrder().compare(valB, valA);
    }
  }

  private void creerFichierGraphViz() {
    fichierGraphViz.append("}");
    try {
      new BufferedWriter(new FileWriter(NOM_FICHIER_GRAPHVIZ, false)).append(fichierGraphViz.toString()).close();
    } catch (IOException ex) {
      LOG.error("", ex);
    }
  }
}
