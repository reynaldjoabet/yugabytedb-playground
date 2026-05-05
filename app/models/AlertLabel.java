
package models;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.ebean.Model;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class AlertLabel extends Model{

  @EmbeddedId private AlertLabelKey key;

  @Column(nullable = false)
  private String value;

  @ManyToOne @JsonIgnore @EqualsAndHashCode.Exclude @ToString.Exclude private Alert alert;

  public AlertLabel() {
    this.key = new AlertLabelKey();
  }

  public AlertLabel(String name, String value) {
    this();
    this.value = value;
  }

  public AlertLabel(Alert definition, String name, String value) {
    this(name, value);
    //setAlert(definition);
  }

  
  @JsonIgnore
  public boolean valueEquals(AlertLabel other) {
    return false;
  }
}
