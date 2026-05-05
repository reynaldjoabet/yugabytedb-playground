package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.Model;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Embeddable
@Data
@EqualsAndHashCode(callSuper = false)
public class AlertDefinitionLabelKey extends Model {
  @JsonIgnore private UUID definitionUUID;
  @NotNull private String name;
}
