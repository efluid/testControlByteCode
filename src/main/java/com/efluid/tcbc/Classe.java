package com.efluid.tcbc;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente une classe lors de la lecture du byteCode
 */
public class Classe {
  private Jar jar;
  private String nom;
  private int nbErreurs;

  List<String> erreurs = new ArrayList<String>();

  public Classe(Jar jar, String nom) {
    this.jar = jar;
    this.nom = nom;
  }

  public void addErreur(String erreur) {
    nbErreurs++;
    erreurs.add(erreur);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Classe) {
      return (nom.equals(((Classe) obj).getNom()) && jar.equals(((Classe) obj).getJar()));
    }
    return (this == obj);
  }

  @Override
  public int hashCode() {
    return nom.hashCode() + jar.hashCode();
  }

  public int getNbErreurs() {
    return nbErreurs;
  }

  public String getNom() {
    return nom;
  }

  public Jar getJar() {
    return jar;
  }

  public void afficherJar() {
    ScanneClasspath.doLog("\t- " + getJar().getNom());
  }

  @Override
  public String toString() {
    return "Classe " + getNom() + " contenue par la jar " + jar.getNom();
  }
}
