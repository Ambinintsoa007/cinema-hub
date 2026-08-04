package hei.school.cinema.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import hei.school.cinema.conf.FacadeIT;
import hei.school.cinema.gen.model.ApiError;
import hei.school.cinema.gen.model.Genre;
import hei.school.cinema.gen.model.MoviePageResponse;
import hei.school.cinema.gen.model.MovieResponse;
import hei.school.cinema.repository.MovieRepository;
import hei.school.cinema.repository.model.GenreEntity;
import hei.school.cinema.repository.model.MovieEntity;
import hei.school.cinema.repository.model.MovieGenreEntity;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

class MovieIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private MovieRepository movieRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private static final UUID UNKNOWN_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

  @BeforeEach
  void clearMovieData() {
    jdbcTemplate.update("DELETE FROM projections");
    jdbcTemplate.update("DELETE FROM movie_genres");
    jdbcTemplate.update("DELETE FROM movies");
  }

  @Test
  void list_movies_returns_paginated_result() {
    insertMovie("Inception", 8100L, List.of(GenreEntity.ACTION, GenreEntity.SCI_FI));
    insertMovie("Titanic", 11880L, List.of(GenreEntity.ROMANCE, GenreEntity.DRAMA));
    insertMovie("Joker", 7320L, List.of(GenreEntity.THRILLER));

    ResponseEntity<MoviePageResponse> response =
        restTemplate.getForEntity("/movies?page=0&pageSize=20", MoviePageResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getTotalElements()).isEqualTo(3);
    assertThat(response.getBody().getData()).hasSize(3);
    assertThat(response.getBody().getPage()).isZero();
    assertThat(response.getBody().getPageSize()).isEqualTo(20);
    assertThat(response.getBody().getTotalPages()).isEqualTo(1);
    assertThat(response.getBody().getData().get(0).getTitle()).isEqualTo("Inception");
  }

  @Test
  void list_movies_filters_by_title_case_insensitive() {
    insertMovie("Inception", 8100L, List.of(GenreEntity.ACTION));
    insertMovie("Titanic", 11880L, List.of(GenreEntity.DRAMA));

    ResponseEntity<MoviePageResponse> response =
        restTemplate.getForEntity("/movies?title=tita", MoviePageResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData()).hasSize(1);
    assertThat(response.getBody().getData().get(0).getTitle()).isEqualTo("Titanic");
  }

  @Test
  void list_movies_filters_by_genre() {
    insertMovie("Inception", 8100L, List.of(GenreEntity.ACTION, GenreEntity.SCI_FI));
    insertMovie("Titanic", 11880L, List.of(GenreEntity.DRAMA));

    ResponseEntity<MoviePageResponse> response =
        restTemplate.getForEntity("/movies?genre=SCI_FI", MoviePageResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData()).hasSize(1);
    assertThat(response.getBody().getData().get(0).getTitle()).isEqualTo("Inception");
  }

  @Test
  void list_movies_with_unknown_genre_returns_empty() {
    insertMovie("Inception", 8100L, List.of(GenreEntity.ACTION));

    ResponseEntity<MoviePageResponse> response =
        restTemplate.getForEntity("/movies?genre=COMEDY", MoviePageResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData()).isEmpty();
  }

  @Test
  void list_movies_paginates_across_pages() {
    insertMovie("Inception", 8100L, List.of(GenreEntity.ACTION));
    insertMovie("Titanic", 11880L, List.of(GenreEntity.DRAMA));

    ResponseEntity<MoviePageResponse> response =
        restTemplate.getForEntity("/movies?page=1&pageSize=1", MoviePageResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData()).hasSize(1);
    assertThat(response.getBody().getPage()).isEqualTo(1);
    assertThat(response.getBody().getTotalElements()).isEqualTo(2);
  }

  @Test
  void list_movies_page_beyond_total_returns_empty() {
    insertMovie("Inception", 8100L, List.of(GenreEntity.ACTION));

    ResponseEntity<MoviePageResponse> response =
        restTemplate.getForEntity("/movies?page=10&pageSize=20", MoviePageResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData()).isEmpty();
    assertThat(response.getBody().getTotalElements()).isEqualTo(1);
  }

  @Test
  void list_movies_with_invalid_page_size_returns_400() {
    ResponseEntity<ApiError> response =
        restTemplate.getForEntity("/movies?pageSize=0", ApiError.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void get_movie_by_id_returns_movie() {
    MovieEntity movie =
        insertMovie("Inception", 8100L, List.of(GenreEntity.ACTION, GenreEntity.SCI_FI));

    ResponseEntity<MovieResponse> response =
        restTemplate.getForEntity("/movies/" + movie.getId(), MovieResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getTitle()).isEqualTo("Inception");
    assertThat(response.getBody().getDuration()).isEqualTo("PT2H15M");
    assertThat(response.getBody().getGenres())
        .containsExactlyInAnyOrder(Genre.ACTION, Genre.SCI_FI);
  }

  @Test
  void get_movie_by_unknown_id_returns_404() {
    ResponseEntity<ApiError> response =
        restTemplate.getForEntity("/movies/" + UNKNOWN_ID, ApiError.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void get_movie_by_invalid_id_returns_400() {
    ResponseEntity<ApiError> response =
        restTemplate.getForEntity("/movies/not-a-uuid", ApiError.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  private MovieEntity insertMovie(String title, long durationSeconds, List<GenreEntity> genres) {
    MovieEntity movie =
        MovieEntity.builder()
            .id(UUID.randomUUID())
            .title(title)
            .description("Desc")
            .durationSeconds(durationSeconds)
            .build();
    Set<MovieGenreEntity> genreEntities =
        genres.stream()
            .map(
                genre ->
                    MovieGenreEntity.builder()
                        .id(new MovieGenreEntity.MovieGenreId(movie.getId(), genre))
                        .movie(movie)
                        .build())
            .collect(Collectors.toSet());
    movie.setGenres(genreEntities);
    return movieRepository.save(movie);
  }
}
