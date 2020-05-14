package com.efluid.tcbc.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.*;

public class ScanneClasspathTest {

  private ScanneClasspath tested;

  @Before
  public void init() {
    tested = spy(ScanneClasspath.class);
  }

  @Test
  public void should_return_empty_list_on_get_fichiers_class() throws IOException {
    String pathThatExist = "";
    String pathThatDoesNotExist = "Path/that/does/not/exist";

    List<Path> shoudlNotBeEmpty = tested.getFichiers(pathThatExist);
    assertThat(shoudlNotBeEmpty.isEmpty()).isFalse();

    List<Path> shoudlBeEmpty = tested.getFichiers(pathThatDoesNotExist);
    assertThat(shoudlBeEmpty.isEmpty()).isTrue();
  }
}
