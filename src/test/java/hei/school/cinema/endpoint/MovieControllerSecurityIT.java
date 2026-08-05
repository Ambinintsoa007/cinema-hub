package hei.school.cinema.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import hei.school.cinema.conf.FacadeIT;
import hei.school.cinema.gen.model.ApiError;
import hei.school.cinema.gen.model.Genre;
import hei.school.cinema.gen.model.MovieResponse;
import hei.school.cinema.repository.MovieRepository;
import hei.school.cinema.repository.model.GenreEntity;
import hei.school.cinema.repository.model.MovieEntity;
import hei.school.cinema.repository.model.MovieGenreEntity;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class MovieControllerSecurityIT extends FacadeIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
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
  void create_movie_without_authentication_returns_401() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/movies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(validMovieBody())))
            .andExpect(status().isUnauthorized())
            .andReturn();

    ApiError error = toError(result);
    assertThat(error.getType()).isEqualTo("UNAUTHORIZED");
  }

  @Test
  void update_movie_without_authentication_returns_401() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                put("/movies/" + UNKNOWN_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(validMovieBody())))
            .andExpect(status().isUnauthorized())
            .andReturn();

    ApiError error = toError(result);
    assertThat(error.getType()).isEqualTo("UNAUTHORIZED");
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_movie_as_manager_returns_created() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/movies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        toJson(
                            Map.of(
                                "title",
                                "Interstellar",
                                "description",
                                "Space exploration",
                                "duration",
                                "PT2H49M",
                                "genres",
                                List.of("SCI_FI", "DRAMA")))))
            .andExpect(status().isCreated())
            .andReturn();

    MovieResponse movie =
        objectMapper.readValue(result.getResponse().getContentAsString(), MovieResponse.class);
    assertThat(movie.getId()).isNotNull();
    assertThat(movie.getTitle()).isEqualTo("Interstellar");
    assertThat(movie.getDuration()).isEqualTo("PT2H49M");
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void create_movie_as_employee_returns_403() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/movies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(validMovieBody())))
            .andExpect(status().isForbidden())
            .andReturn();

    ApiError error = toError(result);
    assertThat(error.getType()).isEqualTo("FORBIDDEN");
  }

  @Test
  @WithMockUser(roles = "CLIENT")
  void create_movie_as_client_returns_403() throws Exception {
    mockMvc
        .perform(
            post("/movies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validMovieBody())))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void update_movie_as_employee_returns_403() throws Exception {
    MovieEntity movie = insertMovie("Inception", 8100L);

    MvcResult result =
        mockMvc
            .perform(
                put("/movies/" + movie.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(validMovieBody())))
            .andExpect(status().isForbidden())
            .andReturn();

    ApiError error = toError(result);
    assertThat(error.getType()).isEqualTo("FORBIDDEN");
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void update_movie_as_manager_returns_updated() throws Exception {
    MovieEntity movie = insertMovie("Inception", 8100L);

    MvcResult result =
        mockMvc
            .perform(
                put("/movies/" + movie.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        toJson(
                            Map.of(
                                "title",
                                "Inception 2",
                                "description",
                                "Sequel",
                                "duration",
                                "PT2H30M",
                                "genres",
                                List.of("THRILLER")))))
            .andExpect(status().isOk())
            .andReturn();

    MovieResponse updated =
        objectMapper.readValue(result.getResponse().getContentAsString(), MovieResponse.class);
    assertThat(updated.getTitle()).isEqualTo("Inception 2");
    assertThat(updated.getDuration()).isEqualTo("PT2H30M");
    assertThat(updated.getGenres()).extracting(Genre::getValue).containsExactly("THRILLER");
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void update_movie_preserving_an_existing_genre_returns_200() throws Exception {
    MvcResult createdResult =
        mockMvc
            .perform(
                post("/movies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        toJson(
                            Map.of(
                                "title",
                                "Inception",
                                "description",
                                "A dream within a dream",
                                "duration",
                                "PT2H15M",
                                "genres",
                                List.of("ACTION", "SCI_FI")))))
            .andExpect(status().isCreated())
            .andReturn();
    MovieResponse created =
        objectMapper.readValue(
            createdResult.getResponse().getContentAsString(), MovieResponse.class);
    assertThat(created.getGenres())
        .extracting(Genre::getValue)
        .containsExactlyInAnyOrder("ACTION", "SCI_FI");

    MvcResult result =
        mockMvc
            .perform(
                put("/movies/" + created.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        toJson(
                            Map.of(
                                "title",
                                "Inception",
                                "description",
                                "A dream within a dream",
                                "duration",
                                "PT2H15M",
                                "genres",
                                List.of("ACTION", "DRAMA")))))
            .andExpect(status().isOk())
            .andReturn();

    MovieResponse updated =
        objectMapper.readValue(result.getResponse().getContentAsString(), MovieResponse.class);
    assertThat(updated.getGenres())
        .extracting(Genre::getValue)
        .containsExactlyInAnyOrder("ACTION", "DRAMA");
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_movie_with_missing_duration_returns_400() throws Exception {
    ApiError error =
        createAsError(
            Map.of("title", "Interstellar", "description", "Desc", "genres", List.of("ACTION")),
            400);

    assertThat(error.getStatus()).isEqualTo(400);
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_movie_with_fractional_duration_returns_400() throws Exception {
    ApiError error =
        createAsError(
            Map.of(
                "title",
                "Interstellar",
                "description",
                "Desc",
                "duration",
                "PT1.5S",
                "genres",
                List.of("ACTION")),
            400);

    assertThat(error.getStatus()).isEqualTo(400);
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_movie_with_invalid_duration_returns_400() throws Exception {
    ApiError error =
        createAsError(
            Map.of(
                "title",
                "Interstellar",
                "description",
                "Desc",
                "duration",
                "not-a-duration",
                "genres",
                List.of("ACTION")),
            400);

    assertThat(error.getStatus()).isEqualTo(400);
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_movie_with_duration_below_one_second_returns_400() throws Exception {
    ApiError error =
        createAsError(
            Map.of(
                "title",
                "Interstellar",
                "description",
                "Desc",
                "duration",
                "PT0.5S",
                "genres",
                List.of("ACTION")),
            400);

    assertThat(error.getStatus()).isEqualTo(400);
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_movie_with_blank_description_returns_400() throws Exception {
    ApiError error =
        createAsError(
            Map.of(
                "title",
                "Interstellar",
                "description",
                "   ",
                "duration",
                "PT1H",
                "genres",
                List.of("ACTION")),
            400);

    assertThat(error.getStatus()).isEqualTo(400);
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_movie_without_genres_returns_400() throws Exception {
    ApiError error =
        createAsError(
            Map.of(
                "title",
                "Interstellar",
                "description",
                "Desc",
                "duration",
                "PT1H",
                "genres",
                List.of()),
            400);

    assertThat(error.getStatus()).isEqualTo(400);
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_movie_with_blank_title_returns_400() throws Exception {
    ApiError error =
        createAsError(
            Map.of(
                "title",
                "   ",
                "description",
                "Desc",
                "duration",
                "PT1H",
                "genres",
                List.of("ACTION")),
            400);

    assertThat(error.getStatus()).isEqualTo(400);
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void create_movie_with_duplicate_title_returns_409() throws Exception {
    insertMovie("inception", 8100L);

    ApiError error =
        createAsError(
            Map.of(
                "title",
                "INCEPTION",
                "description",
                "Desc",
                "duration",
                "PT1H",
                "genres",
                List.of("ACTION")),
            409);

    assertThat(error.getStatus()).isEqualTo(409);
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void update_movie_with_blank_description_returns_400() throws Exception {
    MovieEntity movie = insertMovie("Inception", 8100L);

    ApiError error =
        updateAsError(
            movie.getId(),
            Map.of(
                "title",
                "Inception",
                "description",
                "   ",
                "duration",
                "PT1H",
                "genres",
                List.of("ACTION")),
            400);

    assertThat(error.getStatus()).isEqualTo(400);
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void update_movie_with_empty_genres_returns_400() throws Exception {
    MovieEntity movie = insertMovie("Inception", 8100L);

    ApiError error =
        updateAsError(
            movie.getId(),
            Map.of(
                "title",
                "Inception",
                "description",
                "Desc",
                "duration",
                "PT1H",
                "genres",
                List.of()),
            400);

    assertThat(error.getStatus()).isEqualTo(400);
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void update_movie_with_fractional_duration_returns_400() throws Exception {
    MovieEntity movie = insertMovie("Inception", 8100L);

    ApiError error =
        updateAsError(
            movie.getId(),
            Map.of(
                "title",
                "Inception",
                "description",
                "Desc",
                "duration",
                "PT1.5S",
                "genres",
                List.of("ACTION")),
            400);

    assertThat(error.getStatus()).isEqualTo(400);
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void update_movie_with_blank_title_returns_400() throws Exception {
    MovieEntity movie = insertMovie("Inception", 8100L);

    ApiError error =
        updateAsError(
            movie.getId(),
            Map.of(
                "title",
                "   ",
                "description",
                "Desc",
                "duration",
                "PT1H",
                "genres",
                List.of("ACTION")),
            400);

    assertThat(error.getStatus()).isEqualTo(400);
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void update_movie_duration_change_with_existing_projection_returns_409() throws Exception {
    MovieEntity movie = insertMovie("Inception", 8100L);
    insertProjection(movie.getId());

    ApiError error =
        updateAsError(
            movie.getId(),
            Map.of(
                "title",
                "Inception",
                "description",
                "Desc",
                "duration",
                "PT3H",
                "genres",
                List.of("ACTION")),
            409);

    assertThat(error.getStatus()).isEqualTo(409);
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void update_unknown_movie_returns_404() throws Exception {
    ApiError error =
        updateAsError(
            UNKNOWN_ID,
            Map.of(
                "title",
                "Ghost",
                "description",
                "Desc",
                "duration",
                "PT1H",
                "genres",
                List.of("DRAMA")),
            404);

    assertThat(error.getStatus()).isEqualTo(404);
  }

  private ApiError createAsError(Map<String, Object> body, int expectedStatus) throws Exception {
    MvcResult result =
        mockMvc
            .perform(post("/movies").contentType(MediaType.APPLICATION_JSON).content(toJson(body)))
            .andExpect(status().is(expectedStatus))
            .andReturn();
    return toError(result);
  }

  private ApiError updateAsError(UUID id, Map<String, Object> body, int expectedStatus)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                put("/movies/" + id).contentType(MediaType.APPLICATION_JSON).content(toJson(body)))
            .andExpect(status().is(expectedStatus))
            .andReturn();
    return toError(result);
  }

  private ApiError toError(MvcResult result) throws Exception {
    return objectMapper.readValue(result.getResponse().getContentAsString(), ApiError.class);
  }

  private Map<String, Object> validMovieBody() {
    return Map.of(
        "title",
        "Interstellar",
        "description",
        "Space exploration",
        "duration",
        "PT2H49M",
        "genres",
        List.of("SCI_FI"));
  }

  private String toJson(Map<String, Object> body) throws Exception {
    return objectMapper.writeValueAsString(body);
  }

  private MovieEntity insertMovie(String title, long durationSeconds) {
    MovieEntity movie =
        MovieEntity.builder()
            .id(UUID.randomUUID())
            .title(title)
            .description("Desc")
            .durationSeconds(durationSeconds)
            .build();
    Set<MovieGenreEntity> genreEntities =
        Set.of(
            MovieGenreEntity.builder()
                .id(new MovieGenreEntity.MovieGenreId(movie.getId(), GenreEntity.ACTION))
                .movie(movie)
                .build());
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
}
