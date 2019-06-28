package com.efluid.tcbc.object;

import java.util.StringJoiner;

/**
 * Représente l'appel d'une méthode.
 */
public class MethodeCall {

  private Classe classeReferencante;
  private Class<?> classeReferencee;
  private String nom;
  private Class<?>[] typesEntree;
  private Class<?> typeRetour;

  public MethodeCall(Classe classeReferencante, Class<?> classeReferencee, String nom, Class<?>[] typeEntree, Class<?> typeRetour) {
    this.classeReferencante = classeReferencante;
    this.classeReferencee = classeReferencee;
    this.nom = nom;
    this.typesEntree = typeEntree;
    this.typeRetour = typeRetour;
  }

  public Class<?> getClasseReferencee() {
    return classeReferencee;
  }

  public String getNom() {
    return nom;
  }

  public Class<?>[] getTypesEntree() {
    return typesEntree;
  }

  public Class<?> getTypeRetour() {
    return typeRetour;
  }

  @Override
  public String toString() {
    return "[" + classeReferencante.getNom() + " appelle la méthode " + typeRetour.getName() + " " + classeReferencee.getName() + "#" + nom + getStringParameterTypes(typesEntree) + "]";
  }

  /**
   * Sert uniquement pour l'affichage dans le log
   */
  private static String getStringParameterTypes(Class<?>[] parameterTypes) {
    StringJoiner retour = new StringJoiner(",", "(", ")");
    for (Class<?> parametre : parameterTypes) {
      retour.add(parametre.toString());
    }
    return retour.toString();
  }
}
