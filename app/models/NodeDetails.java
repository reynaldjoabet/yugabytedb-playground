package models;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableSet;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiModelProperty.AccessMode;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static models.NodeActionType.*;

/** Represents all the details of a cloud node that are of interest. */
@JsonIgnoreProperties(
    // Ignore auto-generated boolean properties: https://stackoverflow.com/questions/32270422
    value = {"master", "tserver", "redisServer", "yqlServer", "ysqlServer"},
    ignoreUnknown = true)
@ApiModel(description = "Details of a cloud node")
public class NodeDetails {
  public static final Logger LOG = LoggerFactory.getLogger(NodeDetails.class);

  // The id of the node. This is usually present in the node name.
  @ApiModelProperty(value = "Node ID")
  public int nodeIdx = -1;

  // Name of the node.
  @ApiModelProperty(value = "Node name")
  public String nodeName;

  // The UUID of the node we are using.
  // TODO: only used for onprem at the moment.
  @ApiModelProperty(value = "Node UUID")
  public UUID nodeUuid;


  // The AZ UUID (the YB UUID for the AZ) into which the node is deployed.
  @ApiModelProperty(value = "The availability zone's UUID")
  public UUID azUuid;

  // The UUID of the cluster that this node belongs to.
  @ApiModelProperty(value = "UUID of the cluster to which this node belongs")
  public UUID placementUuid;

  @ApiModelProperty(value = "Machine image name")
  public String machineImage;

  @ApiModelProperty(value = "SSH user override for the AMI")
  public String sshUserOverride;

  @ApiModelProperty(value = "SSH port override for the AMI")
  public Integer sshPortOverride;

  // Indicates that disks in fstab are mounted using uuid (not as by path).
  @ApiModelProperty(value = "Disks are mounted by uuid")
  public boolean disksAreMountedByUUID;

  @ApiModelProperty(value = "True if this a custom YB AMI")
  public boolean ybPrebuiltAmi;

  // This field is persistent across tasks until the master is not stopped.
  @ApiModelProperty(
      value = "WARNING: This is a preview API that could change. Used by auto master failover",
      accessMode = AccessMode.READ_ONLY)
  public boolean autoSyncMasterAddrs;

  // Possible states in which this node can exist.
  public enum NodeState {
    // Set when a new node needs to be added into a Universe and has not yet been created.
    ToBeAdded(DELETE, ADD),
    // Set when a new node is created in the cloud provider.
    InstanceCreated(DELETE, ADD),
    // Set when a node has gone through the Ansible set-up task.
    ServerSetup(DELETE, ADD),
    // Set when a new node is provisioned and configured, but before it is added into
    // the existing cluster.
    ToJoinCluster(REMOVE, ADD),
    // Set when re-provisioning node. Used for third-party software upgrades.
    Reprovisioning(),
    // Set after the node (without any configuration) is created using the IaaS provider at the
    // end of the provision step before it is set up and configured.
    Provisioned(DELETE, ADD),
    // Set after the YB software installed and some basic configuration done on a provisioned node.
    SoftwareInstalled(START, DELETE, ADD),
    // Set after the YB master software is upgraded via Rolling Restart.
    UpgradeMasterSoftware(),
    // Set after the YB tserver software is upgraded via Rolling Restart.
    UpgradeSoftware(),
    // set when software version is rollback.
    RollbackUpgrade(),
    // set when software version is finalized after upgrade.
    FinalizeUpgrade(),
    // Set after the YB specific GFlags are updated via Rolling Restart.
    UpdateGFlags(),
    // Set after all the services (master, tserver, etc) on a node are successfully running.
    // Setting state to Live must be towards the end as ADD cannot be an option here.
    Live(STOP, REMOVE, QUERY, REBOOT, HARD_REBOOT, REPLACE, DECOMMISSION),
    // Set when node is about to enter the stopped state.
    // The actions in Live state should apply because of the transition from Live to Stopping.
    Stopping(STOP, REMOVE),
    // Set when node is about to be set to live state.
    // The actions in Stopped state should apply because of the transition from Stopped to Starting.
    Starting(START, REMOVE),
    // Set when node has been stopped and no longer has a master or a tserver running.
    Stopped(START, REMOVE, QUERY, REPROVISION),
    // Nodes are never set to Unreachable, this is just one of the possible return values in a
    // status query.
    Unreachable(),
    // Nodes are never set to MetricsUnavailable, this is just one of the possible return values in
    // a status query
    MetricsUnavailable(),
    // Set when a node is marked for removal. Note that we will wait to get all its data out.
    ToBeRemoved(REMOVE, DECOMMISSION),
    // Set when a node is about to be removed (unjoined) from the cluster.
    Removing(REMOVE),
    // Set after the node has been removed (unjoined) from the cluster.
    Removed(ADD, RELEASE),
    // Set when node is about to enter the Live state from Removed/Decommissioned state.
    // RELEASE is an option for convenience-
    // If stuck in Adding stuck, we can just RELEASE instead of REMOVE and then RELEASE.
    Adding(DELETE, RELEASE, ADD, REMOVE),
    // Set when a stopped/removed node is about to enter the Decommissioned state.
    // The actions in Removed state should apply because of the transition from Removed to
    // BeingDecommissioned.
    BeingDecommissioned(RELEASE),
    // After a stopped/removed node is returned back to the IaaS.
    Decommissioned(ADD, DELETE),
    // Set when the cert is being updated.
    UpdateCert(),
    // Set when TLS params (node-to-node and client-to-node) is being toggled
    ToggleTls(),
    // Set when configuring DB Apis
    ConfigureDBApis(),
    // Set when the node is being resized to a new intended type
    Resizing(),
    // Set when the node is being upgraded to systemd from cron
    SystemdUpgrade(),
    // Set just before sending the request to the IaaS provider to terminate this node.
    // In this state, the node is no longer a part of any cluster.
    Terminating(RELEASE, DELETE),
    // Set after the node has been terminated in the IaaS provider.
    // If the node is still hanging around due to failure, it can be deleted.
    Terminated(DELETE),
    // Set when the node is being rebooted.
    Rebooting(REBOOT),
    // Set when the node is being stopped + started.
    HardRebooting(HARD_REBOOT),
    // Set when upgrading vm image for node.
    VMImageUpgrade();


    NodeState(NodeActionType nodeActionType, NodeActionType nodeActionType1) {
    }

    NodeState() {
      
    }

    NodeState(NodeActionType nodeActionType) {
    }

    NodeState(NodeActionType nodeActionType, NodeActionType nodeActionType1, NodeActionType nodeActionType2, NodeActionType nodeActionType3) {
    }

    NodeState(NodeActionType nodeActionType, NodeActionType nodeActionType1, NodeActionType nodeActionType2, NodeActionType nodeActionType3, NodeActionType nodeActionType4, NodeActionType nodeActionType5, NodeActionType nodeActionType6) {
    }

    NodeState(NodeActionType nodeActionType, NodeActionType nodeActionType1, NodeActionType nodeActionType2) {
    }

    public ImmutableSet<NodeActionType> allowedActions() {
      return  null;//ImmutableSet.copyOf(allowedActions);
    }

    public static Set<NodeState> allowedStatesForAction(NodeActionType actionType) {
      return Arrays.stream(NodeState.values())
          .filter(state -> state.allowedActions().contains(actionType))
          .collect(Collectors.toSet());
    }
  }

  // Intermediate master state during universe update.
  // The state is cleared once the Universe update succeeds.
  public enum MasterState {
    None,
    ToStart,
    Configured,
    ToStop
  }

  // The current state of the node.
  @ApiModelProperty(value = "Node state", example = "Provisioned")
  public NodeState state;

  // The current intermediate state of the master process.
  @ApiModelProperty(value = "Master state", example = "ToStart")
  public MasterState masterState;

  // True if this node is a master, along with port info.
  @ApiModelProperty(value = "True if this node is a master")
  public boolean isMaster;

  @ApiModelProperty(value = "Master HTTP port")
  public int masterHttpPort = 7000;

  @ApiModelProperty(value = "Master RPC port")
  public int masterRpcPort = 7100;

  // True if this node is a tserver, along with port info.
  @ApiModelProperty(value = "True if this node is a Tablet server")
  public boolean isTserver = true;

  @ApiModelProperty(value = "Tablet server HTTP port")
  public int tserverHttpPort = 9000;

  @ApiModelProperty(value = "Tablet server RPC port")
  public int tserverRpcPort = 9100;

  @ApiModelProperty(value = "Yb controller HTTP port")
  public int ybControllerHttpPort = 14000;

  @ApiModelProperty(value = "Yb controller RPC port")
  public int ybControllerRpcPort = 18018;

  // True if this node is a Redis server, along with port info.
  @ApiModelProperty(value = "True if this node is a REDIS server")
  public boolean isRedisServer = true;

  @ApiModelProperty(value = "REDIS HTTP port")
  public int redisServerHttpPort = 11000;

  @ApiModelProperty(value = "REDIS RPC port")
  public int redisServerRpcPort = 6379;

  // True if this node is a YSQL server, along with port info.
  @ApiModelProperty(value = "True if this node is a YCQL server")
  public boolean isYqlServer = true;

  @ApiModelProperty(value = "YCQL HTTP port")
  public int yqlServerHttpPort = 12000;

  @ApiModelProperty(value = "YCQL RPC port")
  public int yqlServerRpcPort = 9042;

  // True if this node is a YSQL server, along with port info.
  @ApiModelProperty(value = "True if this node is a YSQL server")
  public boolean isYsqlServer = true;

  @ApiModelProperty(value = "YSQL HTTP port")
  public int ysqlServerHttpPort = 13000;

  @ApiModelProperty(value = "YSQL RPC port")
  public int ysqlServerRpcPort = 5433;

  @ApiModelProperty(value = "Internal YSQL RPC port")
  public int internalYsqlServerRpcPort = 6433;

  // Which port node_exporter is running on.
  @ApiModelProperty(value = "Node exporter port")
  public int nodeExporterPort = 9300;

  @ApiModelProperty(value = "Otel collector metrics port")
  public int otelCollectorMetricsPort = 8889;

  // True if cronjobs were properly configured for this node.
  @ApiModelProperty(value = "True if cron jobs were properly configured for this node")
  public boolean cronsActive = true;


  @ApiModelProperty(
      value = "Store last volume update time",
      example = "2022-12-12T13:07:18Z",
      accessMode = ApiModelProperty.AccessMode.READ_ONLY)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  public Date lastVolumeUpdateTime;

  // List of states which are considered in-transit and ops such as upgrade should not be allowed.
  public static final Set<NodeState> IN_TRANSIT_STATES =
      ImmutableSet.of(
          NodeState.Removed,
          NodeState.Stopped,
          NodeState.Decommissioned,
          NodeState.Resizing,
          NodeState.Terminated);

  @Override
  public NodeDetails clone() {
    NodeDetails clone = new NodeDetails();
    clone.isMaster = this.isMaster;
    clone.isTserver = this.isTserver;
    clone.isRedisServer = this.isRedisServer;
    clone.isYqlServer = this.isYqlServer;
    clone.isYsqlServer = this.isYsqlServer;
    clone.state = this.state;
    clone.azUuid = this.azUuid;
    clone.nodeName = this.nodeName;
    clone.nodeIdx = this.nodeIdx;
    clone.nodeUuid = this.nodeUuid;
    clone.placementUuid = this.placementUuid;
    clone.machineImage = this.machineImage;
    clone.sshUserOverride = this.sshUserOverride;
    clone.sshPortOverride = this.sshPortOverride;
    clone.ybPrebuiltAmi = this.ybPrebuiltAmi;
    clone.disksAreMountedByUUID = this.disksAreMountedByUUID;
    return clone;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(getNodeUuid()).append(getNodeName()).toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || obj.getClass() != getClass()) {
      return false;
    }
    NodeDetails other = (NodeDetails) obj;
    UUID thisNodeUuid = getNodeUuid();
    if (thisNodeUuid != null) {
      return thisNodeUuid.equals(other.getNodeUuid());
    }
    String thisNodeName = getNodeName();
    if (thisNodeName != null) {
      return thisNodeName.equals(other.getNodeName());
    }
    // They are not equal as equality cannot be determined.
    return false;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{name: ")
        .append(nodeName)
        .append(", nodeUuid: ")
        .append(nodeUuid)
        .append(", ")
        .append(", isMaster: ")
        .append(isMaster)
        .append(", isTserver: ")
        .append(isTserver)
        .append(", state: ")
        .append(state)
        .append(", azUuid: ")
        .append(azUuid)
        .append(", placementUuid: ")
        .append(placementUuid)
        .append(", dedicatedTo: ")
        .append(", masterState: ")
        .append(masterState)
        .append("}");
    return sb.toString();
  }

  @JsonIgnore
  public boolean isActionAllowedOnState(NodeActionType actionType) {
    return state != null && state.allowedActions().contains(actionType);
  }

  /* Validates if the action is allowed on the state for the node. */
  @JsonIgnore
  public void validateActionOnState(NodeActionType actionType) {
    if (!isActionAllowedOnState(actionType)) {
      String msg =
          String.format(
              "Node %s is in %s state, but not in one of %s, so action %s is not allowed.",
              nodeName,
              state,
              StringUtils.join(NodeState.allowedStatesForAction(actionType), ","),
              actionType);
      throw new RuntimeException(msg);
    }
  }

  @JsonIgnore
  public boolean isNodeRunning() {
    return !(state == NodeState.Unreachable
        || state == NodeState.MetricsUnavailable
        || state == NodeState.ToBeAdded
        || state == NodeState.Adding
        || state == NodeState.BeingDecommissioned
        || state == NodeState.Decommissioned
        || state == NodeState.Terminating
        || state == NodeState.Terminated);
  }

  @JsonIgnore
  public boolean isSoftwareDeleted() {
    return state == NodeState.Decommissioned
        || state == NodeState.Terminating // Software can be partially removed during this state.
        || state == NodeState.Terminated;
  }

  @JsonIgnore
  public boolean isActive() {
    // TODO For some reason ToBeAdded node is treated as 'Active', which it's not the case.
    // Need to better figure out the meaning of 'Active' - and it's usage - as currently it's used
    // for master selection, for example.
    return (isNodeRunning() || state == NodeState.ToBeAdded)
        && !(state == NodeState.ToBeRemoved
            || state == NodeState.Removing
            || state == NodeState.Removed
            || state == NodeState.Starting
            || state == NodeState.Stopping
            || state == NodeState.Stopped
            || state == NodeState.SystemdUpgrade
            || state == NodeState.Rebooting
            || state == NodeState.HardRebooting);
  }

  @JsonIgnore
  public boolean isQueryable() {
    return (state == NodeState.UpgradeSoftware
        || state == NodeState.UpgradeMasterSoftware
        || state == NodeState.FinalizeUpgrade
        || state == NodeState.RollbackUpgrade
        || state == NodeState.UpdateGFlags
        || state == NodeState.Live
        || state == NodeState.ToBeRemoved
        || state == NodeState.Removing
        || state == NodeState.Stopping
        || state == NodeState.UpdateCert
        || state == NodeState.ToggleTls);
  }

  @JsonIgnore
  public boolean isConsideredRunning() {
    return (state == NodeState.Live || state == NodeState.ToBeRemoved);
  }



  @JsonIgnore
  public boolean isInTransit() {
    return IN_TRANSIT_STATES.contains(state);
  }

  @JsonIgnore
  public boolean isInTransit(NodeState omittedState) {
    if (omittedState != state) {
      return isInTransit();
    }
    return false;
  }

  // This is invoked to see if the node can be deleted from the universe JSON.
  @JsonIgnore
  public boolean isRemovable() {
    return isActionAllowedOnState(NodeActionType.DELETE);
  }

  @JsonIgnore
  public boolean isInPlacement(UUID placementUuid) {
    return this.placementUuid != null && this.placementUuid.equals(placementUuid);
  }

 

  public int getNodeIdx() {
    return this.nodeIdx;
  }

  public UUID getAzUuid() {
    return azUuid;
  }

  public String getNodeName() {
    return nodeName;
  }

  public UUID getNodeUuid() {
    return nodeUuid;
  }

  
}
