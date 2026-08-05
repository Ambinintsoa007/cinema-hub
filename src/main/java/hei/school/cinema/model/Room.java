package hei.school.cinema.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Room {

  private final UUID id;
  private final String number;
  private final int capacity;
}
