package hei.school.cinema.endpoint.rest.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomPageResponse {

  private List<RoomResponse> data;
  private Integer page;
  private Integer pageSize;
  private Long totalElements;
  private Integer totalPages;
}
