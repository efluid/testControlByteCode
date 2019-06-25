package com.efluid.tcbc;

import com.efluid.tcbc.object.Jar;
import com.efluid.tcbc.object.MethodeCall;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import jdk.dynalink.linker.support.TypeUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.*;

import static javassist.bytecode.ConstPool.*;
import static javassist.bytecode.Descriptor.getReturnType;
import static org.assertj.core.api.Assertions.assertThat;

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

  private static final Logger LOG = LoggerFactory.getLogger(TestControleByteCode.class);
  private static final String FICHIER_CONFIGURATION = "controleByteCode.yaml";
  private static final String ENV_NOMBRE_JAR_MINIMUM = "nbJarMinimum";
  private static int nbJarMinimum = System.getProperty(ENV_NOMBRE_JAR_MINIMUM) != null ? Integer.parseInt(System.getProperty(ENV_NOMBRE_JAR_MINIMUM)) : 0;
  /* Utilisés pour effectuer le bilan global du contrôle du byteCode */
  private Map<String, String> classesReferenceesNonTrouveesOuChargees = new HashMap<>();

  private ClassPool classPool;

  public TestControleByteCode() {
    super();
    classPool = ClassPool.getDefault();
  }

  @Override
  protected void traitementClasseEnCours() {
    if (toClass(classeEnCours.getNom()) != null) {
      chargerByteCodeClasse();
    }
  }

  /**
   * Charge le byteCode de la classe en cours
   */
  private void chargerByteCodeClasse() {
    try {
      lireByteCodeClasse(classPool.get(classeEnCours.getNom()).getClassFile().getConstPool());
    } catch (Throwable ex) {
      addErreur("Classe en erreur de lecture du byte code : " + classeEnCours + " Erreur : " + ex.getMessage());
    }
  }

  /**
   * Parcours le byteCode de la classe en cours de lecture
   */
  private void lireByteCodeClasse(ConstPool constantPool) throws NotFoundException {
    scannerMethodes(constantPool);
  }

  /**
   * Parcours les méthodes référencées par la classe en cours
   */
  private void scannerMethodes(ConstPool constantPool) throws NotFoundException {
    for (int index = 1; index < constantPool.getSize(); index++) {
      switch (constantPool.getTag(index)) {
        case (CONST_Methodref):
          analyserMethode(constantPool.getMethodrefClassName(index), constantPool.getMethodrefName(index), constantPool.getMethodrefType(index));
          break;
        case (CONST_InterfaceMethodref):
          analyserMethode(constantPool.getInterfaceMethodrefClassName(index), constantPool.getInterfaceMethodrefName(index), constantPool.getInterfaceMethodrefType(index));
          break;
        case (CONST_InvokeDynamic):
          String signature = constantPool.getInvokeDynamicType(index);
          toClass(getReturnType(signature, classPool));
          getClassParametresTypes(signature, classPool);
          break;
      }
    }
  }

  /**
   * Charge la classe référencée et appelle la méthode
   */
  private void analyserMethode(String nomClasse, String nomMethode, String signature) throws NotFoundException {
    if (!List.of("<init>", "<clinit>").contains(nomMethode)) {
      Class<?> aClass = chargerClasse(nomClasse, nomMethode);
      if (aClass != null) {
        MethodeCall methodeCall = new MethodeCall(classeEnCours, aClass, nomMethode, getClassParametresTypes(signature, classPool), toClass(getReturnType(signature, classPool)));
        new ControlePresenceMethode(this, methodeCall).executer();
      }
    }
  }

  private Class<?>[] getClassParametresTypes(String signature, ClassPool classPool) throws NotFoundException {
    return Arrays.stream(Descriptor.getParameterTypes(signature, classPool)).map(this::toClass).toArray(Class[]::new);
  }

  private Class<?> toClass(CtClass ctClasse) {
    if (ctClasse.isPrimitive()) {
      return TypeUtilities.getPrimitiveType(toClass(((CtPrimitiveType) ctClasse).getWrapperName()));
    } else if (ctClasse.isArray()) {
      try {
        return Array.newInstance(toClass(ctClasse.getComponentType()), 0).getClass();
      } catch (NotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return toClass(ctClasse.getName());
  }

  private Class<?> toClass(String nomClasse) {
    try {
      return chargerClasse(nomClasse, "");
    } catch (Throwable ex) {
      addErreur("Classe en erreur de chargement : " + classeEnCours + "" + ex.getMessage());
      return null;
    }
  }

  /**
   * Capture et traite l'ensemble des exceptions lors du chargement de la classe dans le classLoader
   */
  private Class<?> chargerClasse(String nomClasse, String nomMethode) {
    String libelle = nomClasse + (nomMethode != null ? "#" + nomMethode : "");
    try {
      return Class.forName(nomClasse);
    } catch (VerifyError ex) {
      String message = ex.getMessage();
      message = message.substring(ex.getMessage().indexOf("Exception Details:"), ex.getMessage().indexOf("Current Frame:")).replace('\n', ' ');
      libelle += " - " + (ex.getMessage().contains("Exception Details:") ? message : ex.getMessage());
    } catch (Throwable errClassDefFound) {
      libelle += " - " + errClassDefFound;
    }

    boolean erreurAjoutee = addErreur(libelle);
    if (erreurAjoutee && !isNullOrEmpty(nomMethode)) {
      classesReferenceesNonTrouveesOuChargees.putIfAbsent(nomClasse, libelle + (" - Classe appelante : " + classeEnCours));
    }
    return null;
  }

  /**
   * Ajout d'une erreur si non exclue
   */
  public boolean addErreur(final String erreur) {
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

  public Map<String, String> getClassesReferenceesNonTrouveesOuChargees() {
    return classesReferenceesNonTrouveesOuChargees;
  }
}
