package hei.school.cinema.conf;

import org.springframework.test.context.DynamicPropertyRegistry;

public class EnvConf {

  public void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("app.jwt.secret", () -> "test-secret-key-that-is-long-enough-for-jwt-tests");

    registry.add("app.jwt.expiration-seconds", () -> 3600);

    registry.add("app.initial-manager.enabled", () -> false);
  }
}
