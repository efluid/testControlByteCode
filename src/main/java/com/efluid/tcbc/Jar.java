package com.efluid.tcbc;

import java.util.*;

/**
 * Représente un Jar qui est contrôlé. Contient la liste des erreurs remontées lors de la lecture du byteCode.
 */
public class Jar implements Comparable<Jar> {

  private String nom = "";
  private Set<Classe> classesEnErreur = new HashSet<>();

  public Jar(String nom) {
    this.nom = nom;
  }

  public Set<Classe> getClassesEnErreur() {
    return classesEnErreur;
  }

  public Classe addToClassesEnErreur(Classe classe) {
    classesEnErreur.add(classe);
    return classe;
  }

  @Override
  public String toString() {
    return nom;
  }

  public String getNom() {
    return nom;
  }

  @Override
  public int compareTo(Jar o) {
    return nom.compareTo(o.getNom());
  }

  public boolean isErreur() {
    return !classesEnErreur.isEmpty();
  }
}
