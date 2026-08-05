package hei.school.cinema.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "movie_genres")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieGenreEntity {

  @EmbeddedId private MovieGenreId id;

  @MapsId("movieId")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "movie_id")
  private MovieEntity movie;

  @Embeddable
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MovieGenreId implements Serializable {

    @Column(name = "movie_id")
    private UUID movieId;

    @Enumerated(EnumType.STRING)
    @Column(name = "genre", length = 30)
    private GenreEntity genre;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof MovieGenreId that)) {
        return false;
      }
      return Objects.equals(movieId, that.movieId) && genre == that.genre;
    }

    @Override
    public int hashCode() {
      return Objects.hash(movieId, genre);
    }
  }
}
