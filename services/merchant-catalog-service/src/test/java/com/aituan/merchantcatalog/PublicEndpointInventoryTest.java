package com.aituan.merchantcatalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
class PublicEndpointInventoryTest {
  private static final Pattern OPERATION = Pattern.compile(
      "^\\|\\s*\\d+\\s*\\|\\s*`([A-Z]+)`\\s*\\|\\s*`([^`]+)`\\s*\\|.*$");

  @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;

  @Test
  void exposesExactlyTheSixtyFiveOperationsOwnedByMemberB() throws IOException {
    assertThat(actualOperations()).containsExactlyInAnyOrderElementsOf(documentedOperations());
    assertThat(actualOperations()).hasSize(65);
  }

  private Set<String> actualOperations() {
    Set<String> actual = new HashSet<>();
    mappings.getHandlerMethods().forEach((mapping, handler) -> {
      if (!handler.getBeanType().getPackageName().startsWith("com.aituan.merchantcatalog")) return;
      mapping.getMethodsCondition().getMethods().forEach(method ->
          mapping.getPatternValues().stream()
              .filter(path -> path.startsWith("/api/"))
              .forEach(path -> actual.add(method.name() + " " + path)));
    });
    return actual;
  }

  private Set<String> documentedOperations() throws IOException {
    return readSection("### 2.2 ", "### 2.3 ");
  }

  private Set<String> readSection(String start, String end) throws IOException {
    Set<String> operations = new HashSet<>();
    boolean active = false;
    for (String line : Files.readAllLines(findRepositoryRoot()
        .resolve("docs/stage-new-3/微服务接口清单.md"))) {
      if (line.startsWith(start)) {
        active = true;
        continue;
      }
      if (active && line.startsWith(end)) break;
      if (!active) continue;
      Matcher matcher = OPERATION.matcher(line);
      if (matcher.matches()) operations.add(matcher.group(1) + " " + matcher.group(2));
    }
    return operations;
  }

  private Path findRepositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      if (Files.isRegularFile(current.resolve("docs/stage-new-3/微服务接口清单.md"))) return current;
      current = current.getParent();
    }
    throw new IllegalStateException("cannot locate repository root");
  }
}
