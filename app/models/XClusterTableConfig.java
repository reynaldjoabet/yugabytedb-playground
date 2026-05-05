package models;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonValue;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.DbEnumValue;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@IdClass(XClusterTableConfig.XClusterTableConfigPK.class)
@Entity
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Getter
@Setter
public class XClusterTableConfig extends Model {

  public static final Finder<String, XClusterTableConfig> find =
      new Finder<String, XClusterTableConfig>(XClusterTableConfig.class) {};

  @Id
  @ManyToOne
  @JoinColumn(name = "config_uuid", referencedColumnName = "uuid")
  @ApiModelProperty(value = "The XCluster config that this table is a participant of")
  @JsonIgnore
  private XClusterConfig config;

  @Id
  @Column(length = 64)
  @ApiModelProperty(value = "Table ID", example = "000033df000030008000000000004005")
  @ToString.Include
  private String tableId;

  @Column(length = 64)
  @ApiModelProperty(
      value = "Stream ID if replication is setup; bootstrap ID if the table is bootstrapped",
      example = "a9d2470786694dc4b34e0e58e592da9e")
  @ToString.Include
  private String streamId;

  @ApiModelProperty(value = "YbaApi Internal. Whether replication is set up for this table")

  @ToString.Include
  private boolean replicationSetupDone;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  @ApiModelProperty(
      value =
          "Time of replication setup, ie, table added to the replication group "
              + "on the target universe",
      example = "2022-12-12T13:07:18Z")
  @JsonIgnore
  private Date replicationSetupTime;

  @ApiModelProperty(
      value = "YbaApi Internal. Whether this table needs bootstrap process for replication setup")
  private boolean needBootstrap;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  @ApiModelProperty(value = "Time of the bootstrap of the table", example = "2022-12-12T13:07:18Z")
  private Date bootstrapCreateTime;

  @ApiModelProperty(value = "The backup config used to do bootstrapping for this table")
  @ManyToOne
  @JoinColumn(name = "backup_uuid", referencedColumnName = "backup_uuid")
  @JsonIgnore
  private Backup backup;

  @ApiModelProperty(value = "The restore config used to do bootstrapping for this table")
  @ManyToOne
  @JoinColumn(name = "restore_uuid", referencedColumnName = "restore_uuid")
  @JsonIgnore
  private Restore restore;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  @ApiModelProperty(
      value = "Time of the last try to restore data to the target universe",
      example = "2022-12-12T13:07:18Z")
  private Date restoreTime;

  // If its main table is not part the config, it will be false; otherwise, it indicates whether the
  // table is an index table.
  @ApiModelProperty(
      value =
          "YbaApi Internal. Whether this table is an index table and its main table is in"
              + " replication")

  private boolean indexTable;

  @ApiModelProperty(
      value = "Status",
      allowableValues =
          "Validated, Running, Updating, Warning, Error, Bootstrapping, Failed, UnableToFetch")
  @ToString.Include
  private Status status;



  // Statuses are declared in reverse severity for showing tables in UI with specific order based on
  // code.
  public enum Status {
    UnableToFetch("UnableToFetch", 0), // Not stored in YBA DB.
    Updating("Updating", 1),
    Bootstrapping("Bootstrapping", 2),
    Validated("Validated", 3),
    Running("Running", 4),
    // The following statuses will leads to alert creation.
    Failed("Failed", -1),
    Error("Error", -2), // Not stored in YBA DB.
    Warning("Warning", -3), // Not stored in YBA DB.
    DroppedFromSource("DroppedFromSource", -5), // Not stored in YBA DB.
    DroppedFromTarget("DroppedFromTarget", -6), // Not stored in YBA DB.
    ExtraTableOnSource("ExtraTableOnSource", -7), // Not stored in YBA DB.
    ExtraTableOnTarget("ExtraTableOnTarget", -8); // Not stored in YBA DB.

    private final String status;
    @Getter private final int code;

    Status(String status, int code) {
      this.status = status;
      this.code = code;
    }

    @Override
    @DbEnumValue
    public String toString() {
      return this.status;
    }
  }

  /** This class is the primary key for XClusterTableConfig. */
  @Embeddable
  @EqualsAndHashCode
  public static class XClusterTableConfigPK implements Serializable {
    @Column(name = "config_uuid")
    public UUID config;

    public String tableId;
  }
}
