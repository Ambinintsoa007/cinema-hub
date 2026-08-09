package hei.school.cinema.endpoint.rest.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

  private String firstName;
  private String lastName;
  private LocalDate birthdate;
  private String email;
  private String phone;
  private String password;
}
