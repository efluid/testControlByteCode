package com.efluid.example;

import static org.assertj.core.api.Assertions.assertThat;

import com.efluid.tcbc.TestAPIsDependenceJar;

public class TestExampleAPIsDependenceJar extends TestAPIsDependenceJar {

  public TestExampleAPIsDependenceJar() {
    super("bytebuddy");
  }

  @Override
  protected void isValid(int erreurs) {
    assertThat(erreurs).isEqualTo(0);
    assertThat(getApis()).isNotEmpty();
  }
}
