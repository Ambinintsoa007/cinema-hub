CREATE TABLE projections (
    id UUID PRIMARY KEY,
    movie_id UUID NOT NULL,
    room_id UUID NOT NULL,
    start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at TIMESTAMP WITH TIME ZONE NOT NULL,
    seat_price NUMERIC(19, 2) NOT NULL,

    CONSTRAINT fk_projections_movie
        FOREIGN KEY (movie_id)
        REFERENCES movies (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_projections_room
        FOREIGN KEY (room_id)
        REFERENCES rooms (id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_projections_dates
        CHECK (end_at > start_at),

    CONSTRAINT chk_projections_seat_price
        CHECK (seat_price > 0)
);

CREATE INDEX idx_projections_movie_id
    ON projections (movie_id);

CREATE INDEX idx_projections_room_id
    ON projections (room_id);

CREATE INDEX idx_projections_start_at
    ON projections (start_at);
