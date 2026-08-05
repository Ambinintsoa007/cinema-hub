CREATE TABLE movies (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    duration_seconds BIGINT NOT NULL,

    CONSTRAINT chk_movies_duration
        CHECK (duration_seconds > 0)
);

CREATE UNIQUE INDEX uk_movies_title_lower
    ON movies (LOWER(title));

CREATE TABLE movie_genres (
    movie_id UUID NOT NULL,
    genre VARCHAR(30) NOT NULL,

    PRIMARY KEY (movie_id, genre),

    CONSTRAINT fk_movie_genres_movie
        FOREIGN KEY (movie_id)
        REFERENCES movies (id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_movie_genres_genre
        CHECK (
            genre IN (
                'ACTION',
                'ANIMATION',
                'COMEDY',
                'DRAMA',
                'FANTASY',
                'ROMANCE',
                'SCI_FI',
                'THRILLER'
            )
        )
);
