package hei.school.cinema.endpoint.rest.dto;

import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

  private UUID id;
  private String firstName;
  private String lastName;
  private LocalDate birthdate;
  private String email;
  private String phone;
  private UserRole role;
  private UserStatus status;
}
