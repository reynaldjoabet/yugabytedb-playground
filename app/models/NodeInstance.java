package models;


import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;
import static play.mvc.Http.Status.BAD_REQUEST;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.gdata.util.common.base.Preconditions;
import io.ebean.DB;
import io.ebean.ExpressionList;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.Query;
import io.ebean.RawSql;
import io.ebean.RawSqlBuilder;
import io.ebean.SqlUpdate;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.EnumValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@ApiModel(description = "A single node instance, attached to a provider and availability zone")
public class NodeInstance extends Model {
  public static final Logger LOG = LoggerFactory.getLogger(NodeInstance.class);
  private static final Map<UUID, Set<InflightNodeInstanceInfo>> CLUSTER_INFLIGHT_NODE_INSTANCES =
          new ConcurrentHashMap<>();

  @Data
  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  static class InflightNodeInstanceInfo {
    private final String nodeName;
    private final UUID zoneUuid;
    @EqualsAndHashCode.Include
    private final UUID nodeUuid;

    InflightNodeInstanceInfo(String nodeName, UUID nodeUuid, UUID zoneUuid) {
      this.nodeName = nodeName;
      this.nodeUuid = nodeUuid;
      this.zoneUuid = zoneUuid;
    }
  }

  @Builder
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class UniverseMetadata {
    @Builder.Default
    private boolean useSystemd = true;
    private boolean assignStaticPublicIp;
    private boolean otelCollectorEnabled;
  }

  public enum State {
    @EnumValue("DECOMMISSIONED")
    DECOMMISSIONED,
    @EnumValue("USED")
    USED,
    @EnumValue("FREE")
    FREE
  }

  @Id
  @ApiModelProperty(value = "The node's UUID", accessMode = READ_ONLY)
  private UUID nodeUuid;

  @Column
  @ApiModelProperty(value = "The node's type code", example = "c5large")
  private String instanceTypeCode;

  @Column(nullable = false)
  @ApiModelProperty(
          value = "The node's name in a universe",
          example = "universe_node1",
          accessMode = READ_ONLY)
  private String nodeName;

  @Column(nullable = false)
  @ApiModelProperty(value = "The node instance's name", example = "Mumbai instance")
  private String instanceName;

  @Column(nullable = false)
  @ApiModelProperty(value = "The availability zone's UUID")
  private UUID zoneUuid;

  @ApiModelProperty(
          value =
                  "True if the node is in use  <b style=\"color:#ff0000\">Deprecated since "
                          + "YBA version 2024.1.0.0.</b> Use NodeInstance.state instead")
  @Transient
  private boolean inUse;

  @Column(nullable = false)
  @ApiModelProperty(value = "State of on-prem node", accessMode = READ_ONLY)
  @Enumerated(EnumType.STRING)
  private State state;

  @Column(nullable = false)
  @ApiModelProperty(value = "Manually set to decommissioned state by user", accessMode = READ_ONLY)
  private boolean manuallyDecommissioned;

  @DbJson
  @JsonIgnore
  private UniverseMetadata universeMetadata;

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  @Column(nullable = false)
  @ApiModelProperty(value = "Node details (as a JSON object)")
  private String nodeDetailsJson;


}