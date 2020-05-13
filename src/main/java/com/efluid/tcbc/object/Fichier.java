package com.efluid.tcbc.object;

import java.util.*;

/**
 * Représente un fichier lors de la lecture du classpath.
 */
public class Fichier {

  private Jar jar;
  private String nom;
  private String extension;
  private List<String> erreurs = new ArrayList<>();

  public Fichier(Jar jar, String nom, String extension) {
    this.jar = jar;
    this.nom = nom;
    this.extension = extension;
  }

  public void addErreur(String erreur) {
    erreurs.add(erreur);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Fichier) {
      return (nom.equals(((Fichier) obj).getNom()) && jar.equals(((Fichier) obj).getJar()));
    }
    return (this == obj);
  }

  @Override
  public int hashCode() {
    return nom.hashCode() + jar.hashCode();
  }

  public int getNbErreurs() {
    return erreurs.size();
  }

  public String getNom() {
    return nom;
  }

  public String getNomEtExtension() {
    return nom + "." + extension;
  }

  public Jar getJar() {
    return jar;
  }

  public String getNomJar() {
    return getJar().getNom();
  }

  public String getExtension() {
    return extension;
  }

  public void setExtension(String extension) {
    this.extension = extension;
  }

  @Override
  public String toString() {
    return "Fichier " + getNom() + "." + getExtension() + " contenue par la jar " + jar.getNom();
  }

  public List<String> getErreurs() {
    return erreurs;
  }
}
