package hei.school.cinema.endpoint.rest.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {

    private String type;
    private String message;
    private Integer status;
    private OffsetDateTime timestamp;
    private String path;
}