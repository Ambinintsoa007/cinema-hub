package hei.school.cinema.model;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class User {

    private final UUID id;
    private final String firstName;
    private final String lastName;
    private final LocalDate birthdate;
    private final String email;
    private final String phone;
    private final String passwordHash;
    private final UserRole role;
    private final UserStatus status;
}