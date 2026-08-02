CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL,
    projection_id UUID NOT NULL,
    handled_by UUID,
    handled_at TIMESTAMP WITH TIME ZONE,
    total_price NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_reservations_client
        FOREIGN KEY (client_id)
        REFERENCES users (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_reservations_projection
        FOREIGN KEY (projection_id)
        REFERENCES projections (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_reservations_handler
        FOREIGN KEY (handled_by)
        REFERENCES users (id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_reservations_total_price
        CHECK (total_price > 0),

    CONSTRAINT chk_reservations_handling
        CHECK (
            (handled_by IS NULL AND handled_at IS NULL)
            OR
            (handled_by IS NOT NULL AND handled_at IS NOT NULL)
        ),

    CONSTRAINT uk_reservations_id_projection
        UNIQUE (id, projection_id)
);

CREATE TABLE reservation_seats (
    reservation_id UUID NOT NULL,
    projection_id UUID NOT NULL,
    seat_id UUID NOT NULL,

    PRIMARY KEY (reservation_id, seat_id),

    CONSTRAINT fk_reservation_seats_reservation
        FOREIGN KEY (reservation_id, projection_id)
        REFERENCES reservations (id, projection_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_reservation_seats_seat
        FOREIGN KEY (seat_id)
        REFERENCES seats (id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_reservation_seats_projection_seat
        UNIQUE (projection_id, seat_id)
);

CREATE INDEX idx_reservations_client_id
    ON reservations (client_id);

CREATE INDEX idx_reservations_projection_id
    ON reservations (projection_id);

CREATE INDEX idx_reservations_handled_by
    ON reservations (handled_by);
