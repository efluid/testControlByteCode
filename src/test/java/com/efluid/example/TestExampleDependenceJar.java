package com.efluid.example;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.assertj.core.api.Condition;
import org.slf4j.*;

import com.efluid.tcbc.TestDependenceJar;
import com.efluid.tcbc.object.Jar;

/**
 * Test the detection of a dependence, in this case sfl4j.<br>
 * Generate a file <br>
 * Example of file generated "dependenceJar.dot" :
 *
 * @startuml
 * digraph dependence {
 * layout=dot;concentrate=true;node [shape=box];edge [color=blue];classes [color=red];
 * "slf4j-api-1.7.25.jar" -> "slf4j-simple-1.7.25.jar" [label="7"];
 * "test-classes" -> "byte-buddy-1.9.13.jar" [label="8"];
 * "test-classes" -> "slf4j-api-1.7.25.jar" [label="1"];
 * "test-classes" -> "assertj-core-3.11.1.jar" [label="13"];
 * "test-classes" -> "classes" [label="10"];
 * "junit-4.12.jar" -> "hamcrest-core-1.3.jar" [label="45"];
 * "slf4j-simple-1.7.25.jar" -> "slf4j-api-1.7.25.jar" [label="14"];
 * "javassist-3.24.0-GA.jar" -> "/jdk.attach" [label="3"];
 * "javassist-3.24.0-GA.jar" -> "/jdk.jdi" [label="21"];
 * "junit-rt.jar" -> "test-classes" [label="1"];
 * "junit-rt.jar" -> "idea_rt.jar" [label="17"];
 * "junit-rt.jar" -> "junit-4.12.jar" [label="126"];
 * "classes" -> "assertj-core-3.11.1.jar" [label="4"];
 * "classes" -> "snakeyaml-1.23.jar" [label="2"];
 * "classes" -> "slf4j-api-1.7.25.jar" [label="23"];
 * "classes" -> "javassist-3.24.0-GA.jar" [label="20"];
 * "assertj-core-3.11.1.jar" -> "hamcrest-core-1.3.jar" [label="5"];
 * "assertj-core-3.11.1.jar" -> "junit-4.12.jar" [label="3"];
 * "idea_rt.jar" -> "junit-4.12.jar" [label="1"];
 * }
 * @enduml
 */
public class TestExampleDependenceJar extends TestDependenceJar {

  private static final Logger LOG = LoggerFactory.getLogger(TestExampleDependenceJar.class);

  @Override
  protected void isValid(int erreurs) {
    assertThat(erreurs).isEqualTo(0);
    Jar jarTestClasses = getJarsTraites().stream().filter(jar -> jar.getNom().contains("test-classes")).findFirst().get();
    assertThat(jarTestClasses.getDependences()).hasKeySatisfying(new Condition<>(k -> k.contains("slf4j"), "No dependence with sfl4j"));
    assertThat(new File(NOM_FICHIER_GRAPHVIZ)).isFile().exists();
  }
}
