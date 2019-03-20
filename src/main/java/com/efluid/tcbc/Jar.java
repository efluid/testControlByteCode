package com.efluid.tcbc;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Représente un Jar qui est contrôlé. Contient la liste des erreurs remontées lors de la lecture du byteCode.
 */
public class Jar implements Comparable<Jar> {

  private String nom = "";
  private List<Classe> classesEnErreur = new ArrayList<>();
  private Map<String, AtomicLong> dependances = new HashMap<>();

  public Jar(String nom) {
    this.nom = nom;
  }

  public List<Classe> getClassesEnErreur() {
    return classesEnErreur;
  }

  public void addDependance(String dependance) {
    dependances.putIfAbsent(dependance, new AtomicLong(0)).incrementAndGet();
  }

  public Map<String, AtomicLong> getDependances() {
    return Collections.unmodifiableMap(dependances);
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
}
