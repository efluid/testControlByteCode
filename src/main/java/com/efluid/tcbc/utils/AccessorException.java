package com.efluid.tcbc.utils;

/**
 * Exception générique survenue lors de l'accès à une propriété annotée
 */
public class AccessorException extends RuntimeException {

  /**
   * @param message Le message d'erreur
   */
  public AccessorException(String message) {
    super(message);
  }

  /**
   * @param message Le message d'erreur
   * @param cause   La cause initiale de l'erreur.
   */
  public AccessorException(String message, Throwable cause) {
    super(message, cause);
  }

}
