package hei.school.cinema.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import hei.school.cinema.conf.FacadeIT;
import hei.school.cinema.endpoint.rest.dto.ApiError;
import hei.school.cinema.endpoint.rest.dto.RoomPageResponse;
import hei.school.cinema.endpoint.rest.dto.RoomResponse;
import hei.school.cinema.endpoint.rest.dto.SeatResponse;
import hei.school.cinema.repository.RoomRepository;
import hei.school.cinema.repository.model.RoomEntity;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

class RoomIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private RoomRepository roomRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private static final UUID SALLE1_ID = UUID.fromString("00000000-0000-0000-0000-000000000021");
  private static final UUID UNKNOWN_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

  @BeforeEach
  void resetRoomData() {
    jdbcTemplate.update("DELETE FROM seats");
    jdbcTemplate.update("DELETE FROM rooms");
    jdbcTemplate.update(
        "INSERT INTO rooms (id, number, capacity) VALUES ('00000000-0000-0000-0000-000000000021',"
            + " 'SALLE1', 15)");
  }

  @Test
  void list_rooms_returns_paginated_result() {
    insertRoom("Salle B", 20);
    insertRoom("Salle C", 10);

    ResponseEntity<RoomPageResponse> response =
        restTemplate.getForEntity("/rooms?page=0&pageSize=20", RoomPageResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getTotalElements()).isEqualTo(3);
    assertThat(response.getBody().getData()).hasSize(3);
    assertThat(response.getBody().getPage()).isZero();
    assertThat(response.getBody().getPageSize()).isEqualTo(20);
    assertThat(response.getBody().getTotalPages()).isEqualTo(1);
    assertThat(response.getBody().getData().get(0).getNumber()).isEqualTo("SALLE1");
  }

  @Test
  void list_rooms_paginates_across_pages() {
    insertRoom("Salle", 20);
    insertRoom("Salle2", 10);

    ResponseEntity<RoomPageResponse> response =
        restTemplate.getForEntity("/rooms?page=1&pageSize=2", RoomPageResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData()).hasSize(1);
    assertThat(response.getBody().getPage()).isEqualTo(1);
    assertThat(response.getBody().getTotalElements()).isEqualTo(3);
  }

  @Test
  void list_rooms_with_invalid_page_size_returns_400() {
    ResponseEntity<ApiError> response =
        restTemplate.getForEntity("/rooms?pageSize=0", ApiError.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void get_room_by_id_returns_room() {
    ResponseEntity<RoomResponse> response =
        restTemplate.getForEntity("/rooms/" + SALLE1_ID, RoomResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getNumber()).isEqualTo("SALLE1");
    assertThat(response.getBody().getCapacity()).isEqualTo(15);
  }

  @Test
  void get_room_by_unknown_id_returns_404() {
    ResponseEntity<ApiError> response =
        restTemplate.getForEntity("/rooms/" + UNKNOWN_ID, ApiError.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void get_room_by_invalid_id_returns_400() {
    ResponseEntity<ApiError> response =
        restTemplate.getForEntity("/rooms/not-a-uuid", ApiError.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void get_room_seats_returns_seats_ordered_by_number() {
    UUID roomId = insertRoom("Salle", 2).getId();
    insertSeats(roomId, 2);

    ResponseEntity<SeatResponse[]> response =
        restTemplate.getForEntity("/rooms/" + roomId + "/seats", SeatResponse[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(2);
    assertThat(response.getBody()[0].getNumber()).isEqualTo("A1");
    assertThat(response.getBody()[0].getRoomId()).isEqualTo(roomId);
  }

  @Test
  void get_room_seats_when_room_has_no_seats_returns_empty() {
    UUID roomId = insertRoom("Salle", 2).getId();

    ResponseEntity<SeatResponse[]> response =
        restTemplate.getForEntity("/rooms/" + roomId + "/seats", SeatResponse[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEmpty();
  }

  @Test
  void get_room_seats_of_unknown_room_returns_404() {
    ResponseEntity<ApiError> response =
        restTemplate.getForEntity("/rooms/" + UNKNOWN_ID + "/seats", ApiError.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void get_room_seats_with_invalid_room_id_returns_400() {
    ResponseEntity<ApiError> response =
        restTemplate.getForEntity("/rooms/not-a-uuid/seats", ApiError.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  private RoomEntity insertRoom(String number, int capacity) {
    return roomRepository.save(
        RoomEntity.builder().id(UUID.randomUUID()).number(number).capacity(capacity).build());
  }

  private void insertSeats(UUID roomId, int count) {
    for (int seat = 1; seat <= count; seat++) {
      jdbcTemplate.update(
          "INSERT INTO seats (id, room_id, number) VALUES (?, ?, ?)",
          UUID.randomUUID(),
          roomId,
          "A" + seat);
    }
  }
}
