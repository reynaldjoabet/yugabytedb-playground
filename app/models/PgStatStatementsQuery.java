package models;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.ebean.Model;
import java.time.Instant;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;


@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)

public class PgStatStatementsQuery extends Model {




  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
  private Instant scheduledTimestamp;

  private String query;

  private String dbName;

 
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
  private Instant lastActive;
}
