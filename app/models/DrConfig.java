package models;

import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_WRITE;
import static org.reflections.Reflections.log;
import static play.mvc.Http.Status.BAD_REQUEST;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.Transactional;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import models.XClusterConfigCreateFormData.BootstrapParams;
import models.CustomerConfig.ConfigType;
import models.XClusterConfigRestartFormData;
import models.DrConfigCreateForm.PitrParams;



@Slf4j
@Entity
@ApiModel(description = "disaster recovery config object")
@Getter
@Setter
public class DrConfig extends Model {

  private static final Finder<UUID, DrConfig> find = new Finder<>(DrConfig.class) {};
  private static final Finder<UUID, XClusterConfig> findXClusterConfig =
      new Finder<>(XClusterConfig.class) {};

  @Id
  @ApiModelProperty(value = "DR config UUID")
  private UUID uuid;

  @ApiModelProperty(value = "Disaster recovery config name")
  private String name;

  @ApiModelProperty(value = "Create time of the DR config", example = "2022-12-12T13:07:18Z")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private Date createTime;

  @ApiModelProperty(value = "Last modify time of the DR config", example = "2022-12-12T13:07:18Z")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private Date modifyTime;

  @OneToMany(mappedBy = "drConfig", cascade = CascadeType.ALL)
  @JsonIgnore
  private List<XClusterConfig> xClusterConfigs;

  @ManyToOne
  @JoinColumn(name = "storage_config_uuid", referencedColumnName = "config_uuid")
  @JsonIgnore
  private UUID storageConfigUuid;

  @OneToMany(mappedBy = "drConfig", cascade = CascadeType.ALL)
  private List<Webhook> webhooks;

  @JsonIgnore private int parallelism;


  @ApiModelProperty(value = "PITR Retention Period in seconds", accessMode = READ_WRITE)
  private Long pitrRetentionPeriodSec;

  @ApiModelProperty(value = "PITR Retention Period in seconds", accessMode = READ_WRITE)
  private Long pitrSnapshotIntervalSec;

  



  public static DrConfig getOrBadRequest(UUID drConfigUuid) {
    return maybeGet(drConfigUuid)
        .orElseThrow(
            () ->
                new PlatformServiceException(
                    BAD_REQUEST, "Cannot find drConfig with uuid " + drConfigUuid));
  }

  public static Optional<DrConfig> maybeGet(UUID drConfigUuid) {
    DrConfig drConfig =
        find.query().fetch("xClusterConfigs").where().eq("uuid", drConfigUuid).findOne();
    if (drConfig == null) {
      log.info("Cannot find drConfig {} with uuid ", drConfig);
      return Optional.empty();
    }
    return Optional.of(drConfig);
  }

  public static Optional<DrConfig> maybeGetByName(String drConfigName) {
    DrConfig drConfig =
        find.query().fetch("xClusterConfigs", "").where().eq("name", drConfigName).findOne();
    if (drConfig == null) {
      log.info("Cannot find drConfig {} with uuid ", drConfig);
      return Optional.empty();
    }
    return Optional.of(drConfig);
  }

  
  @JsonIgnore
  public XClusterConfigRestartFormData.RestartBootstrapParams getBootstrapBackupParams() {
    XClusterConfigRestartFormData.RestartBootstrapParams bootstrapParams =
        new XClusterConfigRestartFormData.RestartBootstrapParams();
    BootstrapParams.BootstrapBackupParams backupRequestParams =
        new BootstrapParams.BootstrapBackupParams();
    backupRequestParams.storageConfigUUID = this.storageConfigUuid;
    backupRequestParams.parallelism = this.parallelism;
    bootstrapParams.backupRequestParams = backupRequestParams;
    return bootstrapParams;
  }

  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DrConfig drConfig = (DrConfig) o;
    return Objects.equals(uuid, drConfig.uuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid);
  }
}
