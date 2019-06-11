package com.efluid.tcbc;

import static java.lang.System.lineSeparator;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.*;

import org.cojen.classfile.*;
import org.cojen.classfile.constant.*;
import org.slf4j.*;

import com.efluid.tcbc.utils.MethodLookup;

/**
 * Classe de test JUNIT permettant de contrôler le byteCode des classes JAVA du classpath de la JVM en cours. <br>
 * Cela permet de s'assurer que toutes les dépendances appelées existent et sont cohérentes. <br>
 * <br>
 * Utiliser cette d'option de JVM -XX:MaxPermSize=256m (Chargement de toutes les classes)<br>
 * <br>
 * Pour définir le nombre minimum de jar scanné il faut définir la variable d'environnement suivante "-DnbJarMinimum=2". 6 par défaut.<br>
 * <br>
 * Element Type Encoding :
 * <ul>
 * <li>boolean Z</li>
 * <li>byte B</li>
 * <li>char C</li>
 * <li>double D</li>
 * <li>float F</li>
 * <li>int I</li>
 * <li>long J</li>
 * <li>short S</li>
 * <li>class or interface Lclassname;</li>
 * </ul>
 *
 * @author Vincent BOUTHINON
 */
public class TestControleByteCode extends ScanneClasspath {

  private static final String FICHIER_CONFIGURATION = "controleByteCode.yaml";

  private static final String ENV_NOMBRE_JAR_MINIMUM = "nbJarMinimum";
  private static int nbJarMinimum = System.getProperty(ENV_NOMBRE_JAR_MINIMUM) != null ? Integer.parseInt(System.getProperty(ENV_NOMBRE_JAR_MINIMUM)) : 0;

  /**
   * Utilisés pour effectuer le bilan global du contrôle du byteCode
   */
  private Set<String> classesReferenceesNonTrouveesOuChargees = new HashSet<String>();

  private static final Logger LOG = LoggerFactory.getLogger(TestControleByteCode.class);

  public TestControleByteCode() {
    super();
  }

  @Override
  public void init() {
    super.init();
  }

  @Override
  protected void traitementClasseEnCours() {
    chargerByteCodeClasse();
  }

  /**
   * Charge le byteCode de la classe en cours
   */
  private void chargerByteCodeClasse() {
    try {
      ClassFile classfile = ClassFile.readFrom(fluxEnCours);
      // On teste le chargement de la classe avant de contrôler le byteCode
      Class<?> classe = chargerClasseDansClassLoader(classfile.getType());
      if (classe != null) {
        lireByteCodeClasse(classfile.getConstantPool());
      }
    } catch (Throwable ex) {
      addErreur("Classe en erreur de lecture du byte code : " + classeEnCours);
      LOG.error("Classe en erreur de lecture du byte code : {}", classeEnCours, ex);
    }
  }

  /**
   * Parcours le byteCode de la classe en cours de lecture
   */
  protected void lireByteCodeClasse(ConstantPool constantPool) {
    scannerMethodes(constantPool);
  }

  /**
   * Parcours les méthodes référencées par la classe en cours
   */
  protected void scannerMethodes(ConstantPool constantPool) {
    for (Object obj : constantPool.getAllConstants()) {
      if (obj instanceof ConstantMethodInfo) {
        ConstantMethodInfo constantMethod = (ConstantMethodInfo) obj;
        ConstantNameAndTypeInfo constantNameAndTypeInfo = constantMethod.getNameAndType();
        String nomMethode = constantNameAndTypeInfo.getName();
        if (!"<init>".equals(nomMethode) && !"<clinit>".equals(nomMethode)) {
          Descriptor descriptor = constantNameAndTypeInfo.getType();
          if (descriptor instanceof MethodDesc) {
            analyserMethode(constantMethod, nomMethode, (MethodDesc) descriptor);
          }
        }
      }
    }
  }

  /**
   * Charge la classe référencée et appelle la méthode
   */
  private void analyserMethode(ConstantMethodInfo constantMethod, String nomMethode, MethodDesc methodDesc) {
    Class<?> aClass = chargeClasseDansClassLoader(constantMethod.getParentClass().getType(), nomMethode);
    if (aClass != null) {
      appelMethod(aClass, nomMethode, getParametres(methodDesc), methodDesc.getReturnType());
    }
  }

  public Class<?> chargerClasseDansClassLoader(TypeDesc typeDesc) {
    return chargeClasseDansClassLoader(typeDesc, "");
  }

  /**
   * Capture et traite l'ensemble des exceptions lors du chargement de la classe dans le classLoader
   */
  private Class<?> chargeClasseDansClassLoader(TypeDesc typeDesc, String nomMethode) {
    String libelle = "Erreur de chargement de la classe dans le classLoader : " + typeDesc.getFullName();
    try {
      return typeDesc.toClass();
    } catch (java.lang.NoClassDefFoundError errClassDefFound) {
      libelle += " - " + errClassDefFound;
    } catch (java.lang.ExceptionInInitializerError errInInitializerError) {
      Exception exInInitializerError = (Exception) errInInitializerError.getException();
      throw errInInitializerError;
    } catch (Throwable ex) {
      LOG.error("Erreur de chargement", ex);
    }

    boolean erreurAjoutee = addErreur(libelle);

    if (erreurAjoutee && !nomMethode.isEmpty()) {
      classesReferenceesNonTrouveesOuChargees.add(libelle += " [Classe referencee] -  methode appelee : " + nomMethode + " - Classe en cours : " + classeEnCours);
    }

    return null;
  }

  /**
   * Obtenir les paramètres de la méthode (signature)
   */
  private static Class<?>[] getParametres(MethodDesc methodDesc) {
    Class<?>[] parametres = new Class[methodDesc.getParameterCount()];
    int i = 0;
    for (TypeDesc typeDesc : methodDesc.getParameterTypes()) {
      parametres[i++] = typeDesc.toClass();
    }
    return parametres;
  }

  /**
   * Test l'appel de la méthode
   */
  void appelMethod(Class<?> aClass, String methodName, Class<?>[] parameterTypes, TypeDesc typeDeRetour) {
    try {
      Method method = MethodLookup.findMethodInHierarchy(aClass, methodName, parameterTypes);
      if (method == null) {
        method = TestControleByteCode.getMethod(aClass, methodName, parameterTypes);
      }
      if (null == method && isPolymorphicSignature(aClass, methodName)) {
        return;
      }
      if (method == null) {
        addErreur("Methode referencee non trouvee : " + methodName + " - " + classeEnCours + " - parametres : " + getStringParameterTypes(parameterTypes));
      } else {
        testerTypeDeRetour(methodName, parameterTypes, typeDeRetour, method);
      }
    } catch (java.lang.NoClassDefFoundError errNoClassDefFound) {
      addErreur("Classe non trouvee lors de la récuperation de la méthode " + classeEnCours + "#" + methodName + " : " + errNoClassDefFound);
    } catch (Throwable ex) {
      LOG.error("Erreur d'appel de methode", ex);
    }
  }

  /**
   * Test d'accès "polymorphique" : le MethodHandle.invoke ne peut être retrouvé par réflexion.
   * <p>
   * On se base sur le marqueur interne du compilo.
   */
  private static boolean isPolymorphicSignature(Class<?> aClass, String methodName) {
    try {
      Method method = aClass.getMethod(methodName, Object[].class);
      if (null == method) {
        return false;
      }
      // MethodHandle.PolymorphicSugnature est une annotation interne à la classe, non publique.
      return Arrays.asList(method.getAnnotations()).stream()
        .map(Object::toString)
        .anyMatch("@java.lang.invoke.MethodHandle$PolymorphicSignature()"::equals);

    } catch (Throwable ex) {
      LOG.debug("Methode (polymorphique) non trouvee", ex);
    }
    return false;
  }

  /**
   * Contrôle si le type du retour est identique à celui attendu. La classe du type de retour peut être à l'origine d'une erreur lors du chargement dans le classLoader
   */
  private void testerTypeDeRetour(String methodName, Class<?>[] parameterTypes, TypeDesc typeDeRetour, Method method) {
    try {
      if (typeDeRetour.toClass() != method.getReturnType()) {
        addErreur("Type de retour [" + method.getReturnType() + "] different [" + typeDeRetour + "] - " + classeEnCours + "#" + methodName + "(" + getStringParameterTypes(parameterTypes) + ")");
      }
    } catch (Throwable ex) {
      addErreur(
        "Erreur lors du chargement de la classe du type de retour : " + typeDeRetour.getFullName() + " - " + classeEnCours + "#" + methodName + "(" + getStringParameterTypes(parameterTypes) + ")");
    }
  }

  /**
   * Récupère la méthode en appliquant la récursivité (sur les classes parents)
   */
  private static Method getMethod(Class<?> aClass, String methodName, Class<?>[] parameterTypes) {
    Method method = null;
    try {
      method = aClass.getMethod(methodName, parameterTypes);
    } catch (java.lang.NoSuchMethodException ex) {
      Class<?> supClass = aClass.getSuperclass();
      if (supClass != null) {
        method = getMethod(supClass, methodName, parameterTypes);
      }
    }
    return method;
  }

  /**
   * Sert uniquement pour l'affichage dans le log
   */
  public static String getStringParameterTypes(Class<?>[] parameterTypes) {
    StringJoiner retour = new StringJoiner(",", "[", "]");
    for (Class<?> parametre : parameterTypes) {
      retour.add(parametre.toString());
    }
    return retour.toString();
  }

  /**
   * Ajout d'une erreur si non exclue
   */
  protected boolean addErreur(final String erreur) {
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
    int nbErreurTotal = 0;
    int nbErreurParJar = 0;
    int nbJarEnErreur = 0;
    int nbClassesEnErreur = 0;
    StringBuilder erreursParJar = new StringBuilder();

    for (Jar jar : jarsTraites) {
      nbErreurParJar = 0;
      if (!jar.getClassesEnErreur().isEmpty()) {
        LOG.info("|=== " + jar.getNom() + " ===|");
        LOG.info("\t|=== Classes en erreur ===|");
      }
      for (Classe classeEnErreur : jar.getClassesEnErreur()) {
        nbErreurTotal += classeEnErreur.getNbErreurs();
        nbErreurParJar += classeEnErreur.getNbErreurs();
        nbClassesEnErreur++;

        LOG.info("\t\t" + classeEnErreur.getNom() + " : " + classeEnErreur.getNbErreurs() + " erreur(s)");
      }
      erreursParJar.append("\t" + jar.getNom() + " : " + nbErreurParJar).append(lineSeparator());
      if (nbErreurParJar > 0) {
        nbJarEnErreur++;
      }
    }

    doLogList(classesReferenceesNonTrouveesOuChargees, "Classes referencees non trouvees :");
    LOG.info("Classes non trouvees ou non chargees (erreur de chargement) : " + classesReferenceesNonTrouveesOuChargees.size());
    LOG.info("Erreur par jar : " + lineSeparator() + erreursParJar.toString());
    LOG.info("Jar en erreur : " + nbJarEnErreur);
    LOG.info("Classe en erreur : " + nbClassesEnErreur);
    LOG.info("Nombre d'erreur totale : " + nbErreurTotal);

    return nbErreurTotal;
  }

  @Override
  protected void isValid(int erreurs) {
    super.isValid(erreurs);
    validerNombreJarMinimumTraite();
  }

  /**
   * Possibilité de configurer le nombre de jar minimum traité via la variable d'environnement -DnbJarMinimum=2
   */
  protected void validerNombreJarMinimumTraite() {
    assertThat(jarsTraites.size() > nbJarMinimum).isTrue();
  }

  @Override
  protected String getFichierConfiguration() {
    return FICHIER_CONFIGURATION;
  }

  @Override
  protected boolean isAvecFlux() {
    return true;
  }
}
