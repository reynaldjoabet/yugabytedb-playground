package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.Model;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Embeddable
@Data
@EqualsAndHashCode(callSuper = false)
public class AlertLabelKey extends Model {
  @JsonIgnore private UUID alertUUID;
  private String name;
}
