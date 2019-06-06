package com.efluid.example;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.*;

import com.efluid.tcbc.*;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.FixedValue;

/**
 * Test an error usage case when a signature is modified by changing the return type from String to void.
 */
public class TestExampleControlByteCode extends TestControleByteCode {

  private static final String NAME_CLASS_CREATED_BY_BYTE_BUDDY = "ClassCreatedByByteBuddy";
  private static final String TARGET_TEST_CLASSES = "target/test-classes";

  @Override
  public void init() {
    super.init();
    changeTheReturnTypeFromStringToVoid();
  }

  @Override
  protected void isValid(int erreurs) {
    assertThat(erreurs).isEqualTo(1);
    Classe classInError = jarsTraites.stream().filter(Jar::isEnErreur).flatMap(jar -> jar.getClassesEnErreur().stream()).findFirst().get();
    assertThat(classInError.getNom()).contains(NAME_CLASS_CREATED_BY_BYTE_BUDDY);
  }

  private void changeTheReturnTypeFromStringToVoid() {
    try {
      new ByteBuddy()
        .subclass(TestExampleControlByteCode.class)
        .name(NAME_CLASS_CREATED_BY_BYTE_BUDDY)
        .defineMethod("test", Void.class, Visibility.PUBLIC)
        .intercept(FixedValue.nullValue())
        .make()
        .saveIn(new File(TARGET_TEST_CLASSES));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected boolean isScanneRepertoireClasses() {
    return true;
  }

  public String test() {
    return "ok";
  }
}
