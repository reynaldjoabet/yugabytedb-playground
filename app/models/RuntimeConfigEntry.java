package models;
import io.ebean.Model;
import java.util.*;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
public class RuntimeConfigEntry extends Model  {

  //@EmbeddedId private final RuntimeConfigEntryKey key;

  private String value;





  // @Override
  // public RuntimeConfigEntryKey getId() {
  //   return key;
  // }
}
