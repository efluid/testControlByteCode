package com.efluid.tcbc.process;

import static javassist.bytecode.ConstPool.*;
import static javassist.bytecode.Descriptor.getReturnType;

import java.lang.reflect.Array;
import java.util.*;

import com.efluid.tcbc.TestControleByteCode;
import com.efluid.tcbc.object.*;
import javassist.*;
import javassist.bytecode.*;

public class ReadByteCodeClass<T extends TestControleByteCode> {

  private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE;

  static {
    Map<Class<?>, Class<?>> wrapperTypes = new HashMap<>();
    wrapperTypes.put(Void.class, void.class);
    wrapperTypes.put(Boolean.class, boolean.class);
    wrapperTypes.put(Byte.class, byte.class);
    wrapperTypes.put(Character.class, char.class);
    wrapperTypes.put(Short.class, short.class);
    wrapperTypes.put(Integer.class, int.class);
    wrapperTypes.put(Long.class, long.class);
    wrapperTypes.put(Float.class, float.class);
    wrapperTypes.put(Double.class, double.class);

    WRAPPER_TO_PRIMITIVE = Collections.unmodifiableMap(wrapperTypes);
  }

  private T control;
  private Classe currentReadingClass;

  public ReadByteCodeClass(T control, Classe currentReadingClass) {
    this.control = control;
    this.currentReadingClass = currentReadingClass;
  }

  public void execute() {
    if (toClass(currentReadingClass.getNom()) != null) {
      try {
        lireByteCodeClasse(ClassPool.getDefault().get(currentReadingClass.getNom()).getClassFile().getConstPool());
      } catch (Throwable ex) {
        control.addErreur("Classe en erreur de lecture du byte code : " + currentReadingClass + " Erreur : " + ex.getMessage());
      }
    }
  }

  /**
   * Parcours le byteCode de la classe en cours de lecture
   */
  private void lireByteCodeClasse(ConstPool constantPool) throws NotFoundException {
    analyserClasse(constantPool);
  }

  /**
   * Parcours les méthodes référencées par la classe en cours
   */
  private void analyserClasse(ConstPool constantPool) throws NotFoundException {
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
          toClass(getReturnType(signature, ClassPool.getDefault()));
          getClassParametresTypes(signature);
          break;
      }
    }
  }

  /**
   * Charge la classe référencée et appelle la méthode
   */
  protected void analyserMethode(String nomClasse, String nomMethode, String signature) throws NotFoundException {
    if (!Arrays.asList("<init>", "<clinit>").contains(nomMethode)) {
      Class<?> aClass = chargerClasse(nomClasse, nomMethode);
      if (aClass != null) {
        MethodeCall methodeCall = new MethodeCall(currentReadingClass, aClass, nomMethode, getClassParametresTypes(signature), toClass(getReturnType(signature, ClassPool.getDefault())));
        new MethodExistControl(control, methodeCall).execute();
      }
    }
  }

  private Class<?>[] getClassParametresTypes(String signature) throws NotFoundException {
    return Arrays.stream(Descriptor.getParameterTypes(signature, ClassPool.getDefault())).map(this::toClass).toArray(Class[]::new);
  }

  private Class<?> toClass(CtClass ctClasse) {
    if (ctClasse.isPrimitive()) {
      return WRAPPER_TO_PRIMITIVE.get(toClass(((CtPrimitiveType) ctClasse).getWrapperName()));
    } else if (ctClasse.isArray()) {
      try {
        return Array.newInstance(toClass(ctClasse.getComponentType()), 0).getClass();
      } catch (NotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return toClass(ctClasse.getName());
  }

  protected Class<?> toClass(String nomClasse) {
    try {
      return chargerClasse(nomClasse, "");
    } catch (Throwable ex) {
      control.addErreur("Classe en erreur de chargement : " + currentReadingClass + "" + ex.getMessage());
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

    boolean erreurAjoutee = control.addErreur(libelle);
    if (erreurAjoutee && !ScanneClasspath.isNullOrEmpty(nomMethode)) {
      control.getClassesReferenceesNonTrouveesOuChargees().putIfAbsent(nomClasse, libelle + (" - Classe appelante : " + currentReadingClass));
    }
    return null;
  }

  protected T getControl() {
    return control;
  }
}
