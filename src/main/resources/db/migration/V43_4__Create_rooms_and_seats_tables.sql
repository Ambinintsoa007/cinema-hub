CREATE TABLE rooms (
    id UUID PRIMARY KEY,
    number VARCHAR(100) NOT NULL,
    capacity INTEGER NOT NULL,

    CONSTRAINT chk_rooms_capacity
        CHECK (capacity > 0)
);

CREATE UNIQUE INDEX uk_rooms_number_lower
    ON rooms (LOWER(number));

CREATE TABLE seats (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    number VARCHAR(20) NOT NULL,

    CONSTRAINT fk_seats_room
        FOREIGN KEY (room_id)
        REFERENCES rooms (id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_seats_room_number
        UNIQUE (room_id, number)
);

CREATE INDEX idx_seats_room_id
    ON seats (room_id);
