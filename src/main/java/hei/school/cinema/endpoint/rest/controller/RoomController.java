package hei.school.cinema.endpoint.rest.controller;

import hei.school.cinema.gen.api.RoomsApi;
import hei.school.cinema.gen.model.CreateRoomRequest;
import hei.school.cinema.gen.model.RoomPageResponse;
import hei.school.cinema.gen.model.RoomResponse;
import hei.school.cinema.gen.model.SeatResponse;
import hei.school.cinema.mapper.RoomMapper;
import hei.school.cinema.model.Room;
import hei.school.cinema.service.RoomService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoomController implements RoomsApi {

  private final RoomService roomService;
  private final RoomMapper roomMapper;

  @Override
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<RoomResponse> createRoom(CreateRoomRequest createRoomRequest) {
    Room created =
        roomService.create(
            createRoomRequest.getNumber(),
            createRoomRequest.getRows(),
            createRoomRequest.getSeatsPerRow());
    return ResponseEntity.status(201).body(roomMapper.toDto(created));
  }

  @Override
  public ResponseEntity<RoomResponse> getRoomById(UUID roomId) {
    return ResponseEntity.ok(roomMapper.toDto(roomService.getById(roomId)));
  }

  @Override
  public ResponseEntity<RoomPageResponse> getRooms(Integer page, Integer pageSize) {
    Page<Room> rooms = roomService.getAll(page, pageSize);
    List<RoomResponse> data = rooms.stream().map(roomMapper::toDto).toList();
    RoomPageResponse response =
        new RoomPageResponse(data, page, pageSize, rooms.getTotalElements(), rooms.getTotalPages());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<List<SeatResponse>> getRoomSeats(UUID roomId) {
    List<SeatResponse> seats =
        roomService.getSeats(roomId).stream().map(roomMapper::toDto).toList();
    return ResponseEntity.ok(seats);
  }
}
