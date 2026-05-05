
package models;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.Model;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(exclude = "definition", callSuper = false)
@ToString(exclude = "definition")
public class AlertDefinitionLabel extends Model {

  @NotNull @EmbeddedId private AlertDefinitionLabelKey key;

  @Column(nullable = false)
  @NotNull
  private String value;

  @ManyToOne @JsonIgnore private AlertDefinition definition;

  public AlertDefinitionLabel() {
    this.key = new AlertDefinitionLabelKey();
  }



  @JsonIgnore
  public boolean keyEquals(AlertDefinitionLabel other) {
    return true;
  }


  @JsonIgnore
  public boolean valueEquals(AlertDefinitionLabel other) {
    return keyEquals(other) && Objects.equals(getValue(), other.getValue());
  }
}
