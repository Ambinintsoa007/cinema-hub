package hei.school.cinema.endpoint.rest.dto;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateMovieRequest {

    private String title;
    private String description;
    private String duration;
    private Set<Genre> genres;
}