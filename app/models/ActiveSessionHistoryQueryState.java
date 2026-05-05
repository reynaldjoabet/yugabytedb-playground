package models;
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
public class ActiveSessionHistoryQueryState extends Model {


  // @EmbeddedId
  // private ActiveSessionHistoryQueryStateId id;

  private Instant lastSampleTime;
}
