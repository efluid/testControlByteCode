package com.efluid.tcbc;

import java.util.*;

import org.slf4j.*;

/**
 * Représente une classe lors de la lecture du byteCode
 */
public class Classe {

  private static final Logger LOG = LoggerFactory.getLogger(Classe.class);

  private Jar jar;
  private String nom;
  List<String> erreurs = new ArrayList<String>();

  public Classe(Jar jar, String nom) {
    this.jar = jar;
    this.nom = nom;
  }

  public void addErreur(String erreur) {
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
    return erreurs.size();
  }

  public String getNom() {
    return nom;
  }

  public Jar getJar() {
    return jar;
  }

  public String getNomJar() {
    return getJar().getNom();
  }

  @Override
  public String toString() {
    return "Classe " + getNom() + " contenue par la jar " + jar.getNom();
  }

  public List<String> getErreurs() {
    return erreurs;
  }
}
