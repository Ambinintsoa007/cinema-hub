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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

class MovieIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private MovieRepository movieRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private static final UUID ROOM_ID = UUID.fromString("00000000-0000-0000-0000-000000000021");
  private static final UUID UNKNOWN_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

  @BeforeEach
  void clearMovieData() {
    jdbcTemplate.update("DELETE FROM projections");
    jdbcTemplate.update("DELETE FROM movie_genres");
    jdbcTemplate.update("DELETE FROM movies");
  }

  @Test
  void create_movie_returns_created_movie() {
    ResponseEntity<MovieResponse> response =
        postMovie(
            Map.of(
                "title", "Interstellar",
                "description", "Space exploration",
                "duration", "PT2H49M",
                "genres", List.of("SCI_FI", "DRAMA")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    MovieResponse movie = response.getBody();
    assertThat(movie.getId()).isNotNull();
    assertThat(movie.getTitle()).isEqualTo("Interstellar");
    assertThat(movie.getDescription()).isEqualTo("Space exploration");
    assertThat(movie.getDuration()).isEqualTo("PT2H49M");
    assertThat(movie.getGenres()).containsExactlyInAnyOrder(Genre.SCI_FI, Genre.DRAMA);
  }

  @Test
  void create_movie_with_duplicate_title_case_insensitive_returns_409() {
    insertMovie("inception", 8100L, List.of(GenreEntity.ACTION));

    ResponseEntity<ApiError> response =
        postMovieAsError(
            "/movies",
            Map.of(
                "title",
                "INCEPTION",
                "description",
                "Desc",
                "duration",
                "PT1H",
                "genres",
                List.of("ACTION")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody().getType()).isEqualTo("CONFLICT");
  }

  @Test
  void create_movie_with_blank_title_returns_400() {
    ResponseEntity<ApiError> response =
        postMovieAsError(
            "/movies",
            Map.of(
                "title",
                "   ",
                "description",
                "Desc",
                "duration",
                "PT1H",
                "genres",
                List.of("ACTION")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void create_movie_with_blank_description_returns_400() {
    ResponseEntity<ApiError> response =
        postMovieAsError(
            "/movies",
            Map.of(
                "title",
                "Interstellar",
                "description",
                "   ",
                "duration",
                "PT1H",
                "genres",
                List.of("ACTION")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void create_movie_with_invalid_duration_returns_400() {
    ResponseEntity<ApiError> response =
        postMovieAsError(
            "/movies",
            Map.of(
                "title",
                "Interstellar",
                "description",
                "Desc",
                "duration",
                "not-a-duration",
                "genres",
                List.of("ACTION")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void create_movie_with_duration_below_one_second_returns_400() {
    ResponseEntity<ApiError> response =
        postMovieAsError(
            "/movies",
            Map.of(
                "title",
                "Interstellar",
                "description",
                "Desc",
                "duration",
                "PT0.5S",
                "genres",
                List.of("ACTION")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void create_movie_without_genres_returns_400() {
    ResponseEntity<ApiError> response =
        postMovieAsError(
            "/movies",
            Map.of(
                "title",
                "Interstellar",
                "description",
                "Desc",
                "duration",
                "PT1H",
                "genres",
                List.of()));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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

  @Test
  void update_movie_returns_updated_movie() {
    MovieEntity movie = insertMovie("Inception", 8100L, List.of(GenreEntity.ACTION));

    ResponseEntity<MovieResponse> response =
        putMovieAs(
            movie.getId(),
            Map.of(
                "title",
                "Inception 2",
                "description",
                "Sequel",
                "duration",
                "PT2H30M",
                "genres",
                List.of("THRILLER")),
            MovieResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getTitle()).isEqualTo("Inception 2");
    assertThat(response.getBody().getDescription()).isEqualTo("Sequel");
    assertThat(response.getBody().getDuration()).isEqualTo("PT2H30M");
    assertThat(response.getBody().getGenres()).containsExactly(Genre.THRILLER);
  }

  @Test
  void update_movie_to_duplicate_title_returns_409() {
    insertMovie("Inception", 8100L, List.of(GenreEntity.ACTION));
    MovieEntity movie = insertMovie("Titanic", 11880L, List.of(GenreEntity.DRAMA));

    ResponseEntity<ApiError> response =
        putMovieAsError(
            movie.getId(),
            Map.of(
                "title",
                "inception",
                "description",
                "Desc",
                "duration",
                "PT1H",
                "genres",
                List.of("DRAMA")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void update_movie_duration_change_with_existing_projection_returns_409() {
    MovieEntity movie = insertMovie("Inception", 8100L, actionGenres());
    insertProjection(movie.getId());

    ResponseEntity<ApiError> response =
        putMovieAsError(
            movie.getId(),
            Map.of(
                "title",
                "Inception",
                "description",
                "Desc",
                "duration",
                "PT3H",
                "genres",
                List.of("ACTION")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void update_movie_with_blank_title_returns_400() {
    MovieEntity movie = insertMovie("Inception", 8100L, actionGenres());

    ResponseEntity<ApiError> response =
        putMovieAsError(
            movie.getId(),
            Map.of(
                "title",
                "   ",
                "description",
                "Desc",
                "duration",
                "PT1H",
                "genres",
                List.of("ACTION")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void update_movie_with_duration_below_one_second_returns_400() {
    MovieEntity movie = insertMovie("Inception", 8100L, actionGenres());

    ResponseEntity<ApiError> response =
        putMovieAsError(
            movie.getId(),
            Map.of(
                "title",
                "Inception",
                "description",
                "Desc",
                "duration",
                "PT0.5S",
                "genres",
                List.of("ACTION")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void update_unknown_movie_returns_404() {
    ResponseEntity<ApiError> response =
        putMovieAsError(
            UNKNOWN_ID,
            Map.of(
                "title",
                "Ghost",
                "description",
                "Desc",
                "duration",
                "PT1H",
                "genres",
                List.of("DRAMA")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  private ResponseEntity<MovieResponse> postMovie(Map<String, Object> body) {
    return restTemplate.exchange(
        "/movies", HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), MovieResponse.class);
  }

  private ResponseEntity<ApiError> postMovieAsError(String path, Map<String, Object> body) {
    return restTemplate.exchange(
        path, HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), ApiError.class);
  }

  private <T> ResponseEntity<T> putMovieAs(
      UUID id, Map<String, Object> body, Class<T> responseType) {
    return restTemplate.exchange(
        "/movies/" + id, HttpMethod.PUT, new HttpEntity<>(body, jsonHeaders()), responseType);
  }

  private ResponseEntity<ApiError> putMovieAsError(UUID id, Map<String, Object> body) {
    return putMovieAs(id, body, ApiError.class);
  }

  private HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
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

  private void insertProjection(UUID movieId) {
    jdbcTemplate.update(
        "INSERT INTO projections (id, movie_id, room_id, start_at, end_at, seat_price) "
            + "VALUES (?, ?, ?, ?::timestamptz, ?::timestamptz, ?)",
        UUID.randomUUID(),
        movieId,
        ROOM_ID,
        "2025-01-01T10:00:00+00:00",
        "2025-01-01T11:00:00+00:00",
        10.00);
  }

  private static List<GenreEntity> actionGenres() {
    return List.of(GenreEntity.ACTION);
  }
}
