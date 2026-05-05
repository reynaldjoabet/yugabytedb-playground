package models;
import io.ebean.ExpressionList;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.PersistenceContextScope;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.annotation.JsonIgnore;
import filters.AlertDefinitionFilter;

@Entity
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class AlertDefinition extends Model {

  @Id
  @Column(nullable = false, unique = true)
  private UUID uuid;

  @NotNull
  @Column(nullable = false)
  private UUID customerUUID;

  @NotNull
  @Column(nullable = false)
  private UUID configurationUUID;

  @NotNull
  @Column(nullable = false)
  @JsonIgnore
  private boolean configWritten = false;

  @NotNull
  @Column(nullable = false)
  @JsonIgnore
  private boolean active = true;

  @Version
  @Column(nullable = false)
  private int version;

  @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true)
  @Valid
  private List<AlertDefinitionLabel> labels;

  private static final Finder<UUID, AlertDefinition> find =
      new Finder<UUID, AlertDefinition>(AlertDefinition.class) {};



  public AlertDefinition generateUUID() {
    this.uuid = UUID.randomUUID();
    //this.labels.forEach(label -> label.setDefinition(this));
    return this;
  }

  @JsonIgnore
  public boolean isNew() {
    return uuid == null;
  }




  


  
}
