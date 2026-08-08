package hei.school.cinema.endpoint.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {

  private String number;
  private Integer rows;
  private Integer seatsPerRow;
}
