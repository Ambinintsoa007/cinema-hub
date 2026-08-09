package hei.school.cinema.mapper;

import hei.school.cinema.endpoint.rest.dto.Genre;
import hei.school.cinema.endpoint.rest.dto.MovieResponse;
import hei.school.cinema.model.Movie;
import hei.school.cinema.repository.model.GenreEntity;
import hei.school.cinema.repository.model.MovieEntity;
import hei.school.cinema.repository.model.MovieGenreEntity;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MovieMapper {

  public Movie toDomain(MovieEntity entity) {
    Set<hei.school.cinema.model.Genre> genres =
        entity.getGenres().stream()
            .map(movieGenre -> toDomainGenre(movieGenre.getId().getGenre()))
            .collect(Collectors.toSet());

    return Movie.builder()
        .id(entity.getId())
        .title(entity.getTitle())
        .description(entity.getDescription())
        .duration(java.time.Duration.ofSeconds(entity.getDurationSeconds()))
        .genres(genres)
        .build();
  }

  public MovieEntity toEntity(Movie movie) {
    MovieEntity entity =
        MovieEntity.builder()
            .id(movie.getId())
            .title(movie.getTitle())
            .description(movie.getDescription())
            .durationSeconds(movie.getDuration().toSeconds())
            .build();

    entity.setGenres(
        movie.getGenres().stream()
            .map(
                genre ->
                    MovieGenreEntity.builder()
                        .id(new MovieGenreEntity.MovieGenreId(entity.getId(), toEntityGenre(genre)))
                        .movie(entity)
                        .build())
            .collect(Collectors.toSet()));

    return entity;
  }

  public MovieResponse toDto(Movie movie) {
    return new MovieResponse(
        movie.getId(),
        movie.getTitle(),
        movie.getDescription(),
        movie.getDuration().toString(),
        movie.getGenres().stream().map(this::toDtoGenre).toList());
  }

  public hei.school.cinema.model.Genre toDomainGenre(GenreEntity entityGenre) {
    return hei.school.cinema.model.Genre.valueOf(entityGenre.name());
  }

  public GenreEntity toEntityGenre(hei.school.cinema.model.Genre domainGenre) {
    return GenreEntity.valueOf(domainGenre.name());
  }

  public Genre toDtoGenre(hei.school.cinema.model.Genre domainGenre) {
    return Genre.valueOf(domainGenre.name());
  }

  public hei.school.cinema.model.Genre toDomainGenre(Genre dtoGenre) {
    return hei.school.cinema.model.Genre.valueOf(dtoGenre.name());
  }

  public Set<hei.school.cinema.model.Genre> toDomainGenres(Set<Genre> dtoGenres) {
    if (dtoGenres == null) {
      return null;
    }

    return dtoGenres.stream().map(this::toDomainGenre).collect(Collectors.toSet());
  }
}
