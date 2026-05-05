package models;

import static play.mvc.Http.Status.BAD_REQUEST;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import io.ebean.Model;
import io.ebean.Finder;
import io.ebean.annotation.DbEnumValue;
import io.ebean.annotation.Transactional;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;



@Slf4j
@Entity
@ApiModel(description = "xcluster config object")
@Getter
@Setter
public class XClusterConfig extends Model {

  private static final Finder<UUID, XClusterConfig> find = new Finder<>(XClusterConfig.class) {};

  @Id
  @ApiModelProperty(value = "XCluster config UUID")
  private UUID uuid;

  @Column(name = "config_name")
  @ApiModelProperty(value = "XCluster config name")
  private String name;

  @ManyToOne
  @JoinColumn(name = "source_universe_uuid", referencedColumnName = "universe_uuid")
  @ApiModelProperty(value = "Source Universe UUID")
  private UUID sourceUniverseUUID;

  @ManyToOne
  @JoinColumn(name = "target_universe_uuid", referencedColumnName = "universe_uuid")
  @ApiModelProperty(value = "Target Universe UUID")
  private UUID targetUniverseUUID;


  /**
   * In the application logic, <em>NEVER<em/> read from the following variable. This is only used
   * for UI purposes.
   */
  @ApiModelProperty(
      value =
          "WARNING: This is a preview API that could change. The keyspace name that the xCluster"
              + " task is working on; used for disaster recovery")

  private String keyspacePending;

  public enum XClusterConfigStatusType {
    Initialized("Initialized"),
    Running("Running"),
    Updating("Updating"),
    DeletedUniverse("DeletedUniverse"),
    DeletionFailed("DeletionFailed"),
    Failed("Failed"),

    // New states to handle retry-ability.
    DrainedData("DrainedData");

    private final String status;

    XClusterConfigStatusType(String status) {
      this.status = status;
    }

    @Override
    @DbEnumValue
    public String toString() {
      return this.status;
    }
  }

  public enum TableType {
    UNKNOWN,
    YSQL,
    YCQL;

    @Override
    @DbEnumValue
    public String toString() {
      return super.toString();
    }
  }

  @ApiModelProperty(value = "tableType", allowableValues = "UNKNOWN, YSQL, YCQL")
  private TableType tableType;

  @ApiModelProperty(value = "Whether this xCluster replication config is paused")
  private boolean paused;

  @ApiModelProperty(
      value = "YbaApi Internal. Whether this xCluster replication config was imported")
  private boolean imported;

  @ApiModelProperty(value = "Create time of the xCluster config", example = "2022-12-12T13:07:18Z")
  //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private Date createTime;

  @ApiModelProperty(
      value = "Last modify time of the xCluster config",
      example = "2022-12-12T13:07:18Z")
  //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private Date modifyTime;

  @OneToMany(mappedBy = "config", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnore
  private Set<XClusterTableConfig> tables = new HashSet<>();

  @OneToMany(mappedBy = "config", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<XClusterNamespaceConfig> namespaces = new HashSet<>();

  @ApiModelProperty(value = "Replication group name in the target universe cluster config")
  @JsonProperty
  private String replicationGroupName;

  public enum ConfigType {
    Basic,
    Txn,
    Db;

    @Override
    @DbEnumValue
    public String toString() {
      return super.toString();
    }

    public static ConfigType getFromString(@Nullable String value) {
      if (Objects.isNull(value)) {
        return ConfigType.Basic; // Default value
      }
      return Enum.valueOf(ConfigType.class, value);
    }
  }

  @ApiModelProperty(value = "Whether the config is basic, txn, or db scoped xCluster")
  private ConfigType type;

  @ApiModelProperty(value = "Whether the source is active in txn xCluster")
  private boolean sourceActive;

  @ApiModelProperty(value = "Whether the target is active in txn xCluster")
  private boolean targetActive;

  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
  @JoinColumn(name = "dr_config_uuid", referencedColumnName = "uuid")
  @JsonIgnore
  private DrConfig drConfig;

  @ApiModelProperty(
      value =
          "WARNING: This is a preview API that could change. "
              + "Whether this xCluster config is used as a secondary config for a DR config")

  private boolean secondary;

  @ApiModelProperty(
      value =
          "WARNING: This is a preview API that could change. "
              + "The list of PITR configs used for the txn xCluster config")
  @ManyToMany
  @JoinTable(
      name = "xcluster_pitr",
      joinColumns = @JoinColumn(name = "xcluster_uuid", referencedColumnName = "uuid"),
      inverseJoinColumns = @JoinColumn(name = "pitr_uuid", referencedColumnName = "uuid"))
  private List<PitrConfig> pitrConfigs;

}