package hei.school.cinema.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import hei.school.cinema.exception.ApiException;
import hei.school.cinema.mapper.RoomMapper;
import hei.school.cinema.model.Room;
import hei.school.cinema.model.Seat;
import hei.school.cinema.repository.RoomRepository;
import hei.school.cinema.repository.SeatRepository;
import hei.school.cinema.repository.model.RoomEntity;
import hei.school.cinema.repository.model.SeatEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

  @Mock private RoomRepository roomRepository;
  @Mock private SeatRepository seatRepository;

  private RoomService roomService;

  private static final UUID ROOM_ID = UUID.fromString("00000000-0000-0000-0000-000000000021");

  @BeforeEach
  void setUp() {
    roomService = new RoomService(roomRepository, seatRepository, new RoomMapper());
  }

  @Test
  void create_should_generate_seats_and_compute_capacity() {
    when(roomRepository.existsByNumberIgnoreCase("Salle A")).thenReturn(false);
    when(roomRepository.save(any(RoomEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Room created = roomService.create("  Salle A  ", 3, 5);

    assertEquals("Salle A", created.getNumber());
    assertEquals(15, created.getCapacity());

    ArgumentCaptor<RoomEntity> captor = ArgumentCaptor.forClass(RoomEntity.class);
    org.mockito.Mockito.verify(roomRepository).save(captor.capture());
    Set<SeatEntity> seats = captor.getValue().getSeats();
    assertThat(seats).hasSize(15);
    assertThat(seats)
        .extracting(SeatEntity::getNumber)
        .containsExactlyInAnyOrder(
            "A1", "A2", "A3", "A4", "A5", "B1", "B2", "B3", "B4", "B5", "C1", "C2", "C3", "C4",
            "C5");
  }

  @Test
  void create_should_reject_blank_number() {
    ApiException exception =
        assertThrows(ApiException.class, () -> roomService.create("   ", 3, 5));

    assertEquals(400, exception.getStatus().value());
  }

  @Test
  void create_should_reject_zero_rows() {
    ApiException exception =
        assertThrows(ApiException.class, () -> roomService.create("Salle A", 0, 5));

    assertEquals(400, exception.getStatus().value());
  }

  @Test
  void create_should_reject_zero_seats_per_row() {
    ApiException exception =
        assertThrows(ApiException.class, () -> roomService.create("Salle A", 3, 0));

    assertEquals(400, exception.getStatus().value());
  }

  @Test
  void create_should_reject_duplicate_number() {
    when(roomRepository.existsByNumberIgnoreCase("salle a")).thenReturn(true);

    ApiException exception =
        assertThrows(ApiException.class, () -> roomService.create("  salle a  ", 3, 5));

    assertEquals(409, exception.getStatus().value());
  }

  @Test
  void getById_should_return_room_when_found() {
    when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(entityOf(ROOM_ID, "SALLE1", 15)));

    Room room = roomService.getById(ROOM_ID);

    assertEquals(ROOM_ID, room.getId());
    assertEquals("SALLE1", room.getNumber());
    assertEquals(15, room.getCapacity());
  }

  @Test
  void getById_should_return_404_when_not_found() {
    when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

    ApiException exception = assertThrows(ApiException.class, () -> roomService.getById(ROOM_ID));

    assertEquals(404, exception.getStatus().value());
  }

  @Test
  void getSeats_should_return_seats_ordered_by_number() {
    when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(entityOf(ROOM_ID, "SALLE1", 2)));
    when(seatRepository.findByRoom_IdOrderByNumberAsc(ROOM_ID))
        .thenReturn(List.of(seatOf("A1"), seatOf("A2")));

    List<Seat> seats = roomService.getSeats(ROOM_ID);

    assertEquals(2, seats.size());
    assertEquals("A1", seats.get(0).getNumber());
    assertEquals(ROOM_ID, seats.get(0).getRoomId());
  }

  @Test
  void getSeats_should_return_404_when_room_not_found() {
    when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

    ApiException exception = assertThrows(ApiException.class, () -> roomService.getSeats(ROOM_ID));

    assertEquals(404, exception.getStatus().value());
  }

  private RoomEntity entityOf(UUID id, String number, int capacity) {
    return RoomEntity.builder().id(id).number(number).capacity(capacity).build();
  }

  private SeatEntity seatOf(String number) {
    return SeatEntity.builder()
        .id(UUID.randomUUID())
        .number(number)
        .room(RoomEntity.builder().id(ROOM_ID).build())
        .build();
  }
}
