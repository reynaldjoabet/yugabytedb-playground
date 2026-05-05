package models;
import io.ebean.Finder;
import io.ebean.Model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Getter
@Setter
public class ExtraMigration extends Model {
  

  public static final Logger LOG = LoggerFactory.getLogger(ExtraMigration.class);

  @Id
  @Column(nullable = false, unique = true)
  private String migration;

  public static final Finder<String, ExtraMigration> find =
      new Finder<String, ExtraMigration>(ExtraMigration.class) {};

  public static List<ExtraMigration> getAll() {
    return find.query().orderBy("migration asc").findList();
  }


}
