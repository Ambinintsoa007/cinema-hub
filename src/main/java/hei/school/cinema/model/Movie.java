package hei.school.cinema.model;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Movie {

  private final UUID id;
  private final String title;
  private final String description;
  private final Duration duration;
  private final Set<Genre> genres;
}
