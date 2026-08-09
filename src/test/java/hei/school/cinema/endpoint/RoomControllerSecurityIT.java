package hei.school.cinema.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import hei.school.cinema.conf.FacadeIT;
import hei.school.cinema.endpoint.rest.dto.ApiError;
import hei.school.cinema.endpoint.rest.dto.RoomResponse;
import hei.school.cinema.endpoint.rest.dto.SeatResponse;
import hei.school.cinema.repository.RoomRepository;
import hei.school.cinema.repository.model.RoomEntity;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class RoomControllerSecurityIT extends FacadeIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private RoomRepository roomRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void resetRoomData() {
    jdbcTemplate.update("DELETE FROM projections");
    jdbcTemplate.update("DELETE FROM seats");
    jdbcTemplate.update("DELETE FROM rooms");
    jdbcTemplate.update(
        "INSERT INTO rooms (id, number, capacity) VALUES ('00000000-0000-0000-0000-000000000021',"
            + " 'SALLE1', 15)");
  }

  @Test
  void create_room_without_authentication_returns_401() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/rooms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(viableBody())))
            .andExpect(status().isUnauthorized())
            .andReturn();

    ApiError error = toError(result);
    assertThat(error.getType()).isEqualTo("UNAUTHORIZED");
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void create_room_as_employee_returns_403() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/rooms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(viableBody())))
            .andExpect(status().isForbidden())
            .andReturn();

    ApiError error = toError(result);
    assertThat(error.getType()).isEqualTo("FORBIDDEN");
  }

  @Test
  @WithMockUser(roles = "CLIENT")
  void create_room_as_client_returns_403() throws Exception {
    mockMvc
        .perform(
            post("/rooms").contentType(MediaType.APPLICATION_JSON).content(toJson(viableBody())))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_room_generates_seats_and_returns_201() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/rooms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(Map.of("number", "Salle A", "rows", 3, "seatsPerRow", 5))))
            .andExpect(status().isCreated())
            .andReturn();

    RoomResponse room =
        objectMapper.readValue(result.getResponse().getContentAsString(), RoomResponse.class);
    assertThat(room.getId()).isNotNull();
    assertThat(room.getNumber()).isEqualTo("Salle A");
    assertThat(room.getCapacity()).isEqualTo(15);

    MvcResult seatsResult =
        mockMvc
            .perform(get("/rooms/" + room.getId() + "/seats"))
            .andExpect(status().isOk())
            .andReturn();
    SeatResponse[] seats =
        objectMapper.readValue(
            seatsResult.getResponse().getContentAsString(), SeatResponse[].class);
    assertThat(seats).hasSize(15);
    assertThat(seats[0].getNumber()).isEqualTo("A1");
    assertThat(seats[14].getNumber()).isEqualTo("C5");
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_room_with_duplicate_number_returns_409() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/rooms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(Map.of("number", "salle1", "rows", 2, "seatsPerRow", 3))))
            .andExpect(status().isConflict())
            .andReturn();

    ApiError error = toError(result);
    assertThat(error.getType()).isEqualTo("CONFLICT");
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_room_with_blank_number_returns_400() throws Exception {
    ApiError error = errorAsError(Map.of("number", "   ", "rows", 2, "seatsPerRow", 3), 400);

    assertThat(error.getType()).isEqualTo("BAD_REQUEST");
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_room_with_rows_below_one_returns_400() throws Exception {
    ApiError error = errorAsError(Map.of("number", "Salle A", "rows", 0, "seatsPerRow", 3), 400);

    assertThat(error.getType()).isEqualTo("BAD_REQUEST");
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_room_with_twenty_seven_rows_returns_400() throws Exception {
    ApiError error = errorAsError(Map.of("number", "Salle A", "rows", 27, "seatsPerRow", 1), 400);

    assertThat(error.getType()).isEqualTo("BAD_REQUEST");
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_room_with_missing_rows_returns_400() throws Exception {
    ApiError error = errorAsError(Map.of("number", "Salle A", "seatsPerRow", 5), 400);

    assertThat(error.getType()).isEqualTo("BAD_REQUEST");
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_room_with_missing_seats_per_row_returns_400() throws Exception {
    ApiError error = errorAsError(Map.of("number", "Salle A", "rows", 3), 400);

    assertThat(error.getType()).isEqualTo("BAD_REQUEST");
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_room_with_hundred_seats_per_row_returns_201() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/rooms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(Map.of("number", "Salle 100", "rows", 1, "seatsPerRow", 100))))
            .andExpect(status().isCreated())
            .andReturn();

    RoomResponse room =
        objectMapper.readValue(result.getResponse().getContentAsString(), RoomResponse.class);
    assertThat(room.getCapacity()).isEqualTo(100);
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_room_with_hundred_and_one_seats_per_row_returns_400() throws Exception {
    ApiError error = errorAsError(Map.of("number", "Salle A", "rows", 1, "seatsPerRow", 101), 400);

    assertThat(error.getType()).isEqualTo("BAD_REQUEST");
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_room_with_eleven_seats_per_row_orders_naturally() throws Exception {
    MvcResult createdResult =
        mockMvc
            .perform(
                post("/rooms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(Map.of("number", "Salle A", "rows", 1, "seatsPerRow", 11))))
            .andExpect(status().isCreated())
            .andReturn();
    RoomResponse room =
        objectMapper.readValue(
            createdResult.getResponse().getContentAsString(), RoomResponse.class);
    assertThat(room.getCapacity()).isEqualTo(11);

    MvcResult seatsResult =
        mockMvc
            .perform(get("/rooms/" + room.getId() + "/seats"))
            .andExpect(status().isOk())
            .andReturn();
    SeatResponse[] seats =
        objectMapper.readValue(
            seatsResult.getResponse().getContentAsString(), SeatResponse[].class);
    assertThat(seats).hasSize(11);
    assertThat(seats)
        .extracting(SeatResponse::getNumber)
        .containsExactly("A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "A10", "A11");
  }

  @Test
  void get_room_seats_is_public_without_authentication() throws Exception {
    RoomEntity room =
        roomRepository
            .findById(UUID.fromString("00000000-0000-0000-0000-000000000021"))
            .orElseThrow();

    mockMvc.perform(get("/rooms/" + room.getId() + "/seats")).andExpect(status().isOk());
  }

  private Map<String, Object> viableBody() {
    return Map.of("number", "Salle A", "rows", 3, "seatsPerRow", 5);
  }

  private ApiError errorAsError(Map<String, Object> body, int expectedStatus) throws Exception {
    MvcResult result =
        mockMvc
            .perform(post("/rooms").contentType(MediaType.APPLICATION_JSON).content(toJson(body)))
            .andExpect(status().is(expectedStatus))
            .andReturn();
    return toError(result);
  }

  private ApiError toError(MvcResult result) throws Exception {
    return objectMapper.readValue(result.getResponse().getContentAsString(), ApiError.class);
  }

  private String toJson(Map<String, Object> body) throws Exception {
    return objectMapper.writeValueAsString(body);
  }
}
