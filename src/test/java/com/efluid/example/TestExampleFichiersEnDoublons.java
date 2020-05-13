package com.efluid.example;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Ignore;

import com.efluid.tcbc.TestControleFichiersEnDoublon;

public class TestExampleFichiersEnDoublons extends TestControleFichiersEnDoublon {

  @Override
  protected void isValid(int erreurs) {
    assertThat(2).isEqualTo(erreurs);
    assertThat(fichiersEnDoublon.get(Ignore.class.getName() + ".class")).isNotNull();
    assertThat(fichiersEnDoublon.get("META-INF.maven.commons-cli.commons-cli.pom.properties")).isNotNull();
  }
}
