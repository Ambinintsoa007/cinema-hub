package hei.school.cinema.mapper;

import hei.school.cinema.gen.model.RoomResponse;
import hei.school.cinema.gen.model.SeatResponse;
import hei.school.cinema.model.Room;
import hei.school.cinema.model.Seat;
import hei.school.cinema.repository.model.RoomEntity;
import hei.school.cinema.repository.model.SeatEntity;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {

  public Room toDomain(RoomEntity entity) {
    return Room.builder()
        .id(entity.getId())
        .number(entity.getNumber())
        .capacity(entity.getCapacity())
        .build();
  }

  public RoomEntity toEntity(Room room) {
    return RoomEntity.builder()
        .id(room.getId())
        .number(room.getNumber())
        .capacity(room.getCapacity())
        .build();
  }

  public RoomResponse toDto(Room room) {
    return new RoomResponse(room.getId(), room.getNumber(), room.getCapacity());
  }

  public Seat toDomain(SeatEntity entity) {
    return Seat.builder()
        .id(entity.getId())
        .number(entity.getNumber())
        .roomId(entity.getRoom().getId())
        .build();
  }

  public SeatResponse toDto(Seat seat) {
    return new SeatResponse(seat.getId(), seat.getNumber(), seat.getRoomId());
  }
}
