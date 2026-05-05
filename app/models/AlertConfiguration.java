
package models;
import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;
import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_WRITE;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.ExpressionList;
import io.ebean.FetchGroup;
import io.ebean.Finder;
import io.ebean.Junction;
import io.ebean.Model;
import io.ebean.PersistenceContextScope;
import io.ebean.Query;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.Formula;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.Accessors;
import org.apache.commons.collections4.CollectionUtils;

@Entity
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(description = "Alert configuration")
public class AlertConfiguration extends Model {

  private static final String RAW_FIELDS =
      "uuid, customerUUID, name, description, createTime, "
          + "targetType, target, thresholds, thresholdUnit, template, durationSec, active, "
          + "destinationUUID, defaultDestination, maintenanceWindowUuids";


  @ApiModel
  public enum TargetType {
    PLATFORM,
    UNIVERSE
  }

  @ApiModel
  public enum Severity {
    SEVERE(2),
    WARNING(1);

    private final int priority;

    Severity(int priority) {
      this.priority = priority;
    }

    public int getPriority() {
      return priority;
    }
  }

  @Id
  @ApiModelProperty(value = "Configuration UUID", accessMode = READ_ONLY)
  private UUID uuid;

  @NotNull
  @ApiModelProperty(value = "Customer UUID", accessMode = READ_ONLY)
  private UUID customerUUID;

  @NotNull
  @Size(min = 1, max = 1000)
  @ApiModelProperty(value = "Name", accessMode = READ_WRITE)
  private String name;

  @NotNull
  @ApiModelProperty(value = "Description", accessMode = READ_WRITE)
  private String description;

  @NotNull
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  @ApiModelProperty(
      value = "Creation time",
      accessMode = READ_ONLY,
      example = "2022-12-12T13:07:18Z")
  private Date createTime;

  @NotNull
  @Enumerated(EnumType.STRING)
  @ApiModelProperty(value = "Target type", accessMode = READ_WRITE)
  private TargetType targetType;



  @NotNull
  @Min(0)
  @ApiModelProperty(
      value = "Duration in seconds, while condition is met to raise an alert",
      accessMode = READ_WRITE)
  private Integer durationSec = 0;

  @NotNull
  @ApiModelProperty(value = "Is configured alerts raised or not", accessMode = READ_WRITE)
  private boolean active = true;

  @ApiModelProperty(value = "Alert destination UUID", accessMode = READ_WRITE)
  private UUID destinationUUID;

  @NotNull
  @ApiModelProperty(value = "Is default destination used for this config", accessMode = READ_WRITE)
  private boolean defaultDestination;

  @DbJson
  @ApiModelProperty(
      value = "Maintenance window UUIDs, applied to this alert config",
      accessMode = READ_ONLY)
  private Set<UUID> maintenanceWindowUuids;

  @DbJson
  @ApiModelProperty(value = "Labels", accessMode = READ_WRITE)
  @EqualsAndHashCode.Exclude
  private Map<String, String> labels;

  private static final String ALERT_COUNT_JOIN =
      "left join "
          + "(select _ac.uuid, count(*) as alert_count "
          + "from alert_configuration _ac "
          + "left join alert _a on _ac.uuid = _a.configuration_uuid "
          + "where _a.state in ('ACTIVE', 'ACKNOWLEDGED') group by _ac.uuid"
          + ") as _ac "
          + "on _ac.uuid = ${ta}.uuid";

  @Transient
  @Formula(select = "alert_count", join = ALERT_COUNT_JOIN)
  @EqualsAndHashCode.Exclude
  Double alertCount;

  private static final String TARGET_INDEX_JOIN =
      "left join "
          + "(select uuid, universe_name "
          + "from ("
          + "select uuid, "
          + "universe_name, "
          + "rank() OVER (PARTITION BY uuid ORDER BY universe_name asc) as rank "
          + "from ("
          + "select uuid, universe_names.name as universe_name "
          + "from ("
          + "(select uuid, replace(universe_uuid::text, '\"', '')::uuid as universe_uuid "
          + "from ("
          + "select uuid, json_array_elements(target::json->'uuids') as universe_uuid "
          + "from alert_configuration "
          + "where nullif(target::json->>'uuids', '') is not null"
          + ") as tmp"
          + ") as targets "
          + "left join universe on targets.universe_uuid = universe.universe_uuid"
          + ") as universe_names"
          + ") as ranked_universe_names"
          + ") as sorted_universe_names"
          + ") as _un "
          + "on _un.uuid = ${ta}.uuid";

  @Transient
  @Formula(
      select =
          "(case"
              + " when target like '%\"all\":true%' then 'ALL'"
              + " else _un.universe_name end)",
      join = TARGET_INDEX_JOIN)
  @EqualsAndHashCode.Exclude
  @JsonIgnore
  private String targetName;

  @Transient
  @Formula(
      select =
          "(case"
              + " when thresholds like '%SEVERE%' then 2"
              + " when thresholds like '%WARNING%' then 1"
              + " else 0 end)")
  @EqualsAndHashCode.Exclude
  @JsonIgnore
  private Integer severityIndex;

  @Transient
  @Formula(
      select =
          "(case"
              + " when ${ta}.default_destination = true then 'Use default'"
              + " when _ad.name is not null then _ad.name"
              + " else 'No destination' end)",
      join = "left join alert_destination as _ad on _ad.uuid = ${ta}.destination_uuid")
  @EqualsAndHashCode.Exclude
  @JsonIgnore
  private String destinationName;

  private static final Finder<UUID, AlertConfiguration> find =
      new Finder<UUID, AlertConfiguration>(AlertConfiguration.class) {};



  public AlertConfiguration generateUUID() {
    this.uuid = UUID.randomUUID();
    return this;
  }

  @JsonIgnore
  public boolean isNew() {
    return uuid == null;
  }

  @JsonIgnore
  public Set<UUID> getMaintenanceWindowUuidsSet() {
    if (maintenanceWindowUuids == null) {
      return Collections.emptySet();
    }
    return new TreeSet<>(maintenanceWindowUuids);
  }

  public AlertConfiguration addMaintenanceWindowUuid(UUID maintenanceWindowUuid) {
    if (this.maintenanceWindowUuids == null) {
      maintenanceWindowUuids = new TreeSet<>();
    }
    maintenanceWindowUuids.add(maintenanceWindowUuid);
    return this;
  }

  public AlertConfiguration removeMaintenanceWindowUuid(UUID maintenanceWindowUuid) {
    if (this.maintenanceWindowUuids == null) {
      return this;
    }
    maintenanceWindowUuids.remove(maintenanceWindowUuid);
    if (maintenanceWindowUuids.isEmpty()) {
      // To make it easier to query for empty list in DB
      this.maintenanceWindowUuids = null;
    }
    return this;
  }



  public Map<String, String> labelsHashMap() {
    if (labels == null) {
      return null;
    }
    return new HashMap<>(labels);
  }

  public boolean configEquals(AlertConfiguration other) {
    // if (Objects.equals(getName(), other.getName())
    //     && Objects.equals(getDescription(), other.getDescription())
    //     //&& Objects.equals(getTemplate(), other.getTemplate())
    //     && Objects.equals(getDurationSec(), other.getDurationSec())
    //     && Objects.equals(getThresholds(), other.getThresholds())
    //     && Objects.equals(getThresholdUnit(), other.getThresholdUnit())
    //     && Objects.equals(isActive(), other.isActive())) {
    //   return true;
    // }
    return false;
  }

  @Value
  @Builder
  public static class QuerySettings {
    boolean queryCount;
    boolean queryTargetIndex;
    boolean queryDestinationIndex;
  }
}
