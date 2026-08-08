package hei.school.cinema.endpoint.rest.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {

    private UUID id;
    private String title;
    private String description;
    private String duration;
    private List<Genre> genres;
}