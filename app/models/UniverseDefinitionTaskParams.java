package models;

import com.fasterxml.jackson.annotation.*;
import com.google.common.collect.ImmutableSet;
import io.ebean.annotation.EnumValue;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiModelProperty.AccessMode;

import java.util.*;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import play.data.validation.Constraints;
import models.CustomerTask.TaskType;
import models.UniverseTaskBase.*;

/**
 * This class captures the user intent for creation of the universe. Note some nuances in the way
 * the intent is specified.
 *
 * <p>Single AZ deployments: Exactly one region should be specified in the 'regionList'.
 *
 * <p>Multi-AZ deployments: 1. There is at least one region specified which has a at least
 * 'replicationFactor' number of AZs.
 *
 * <p>2. There are multiple regions specified, and the sum total of all AZs in those regions is
 * greater than or equal to 'replicationFactor'. In this case, the preferred region can be specified
 * to hint which region needs to have a majority of the data copies if applicable, as well as
 * serving as the primary leader. Note that we do not currently support ability to place leaders in
 * a preferred region.
 *
 * <p>NOTE #1: The regions can potentially be present in different clouds.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UniverseDefinitionTaskParams {

  private static final Set<String> AWS_INSTANCE_WITH_EPHEMERAL_STORAGE_ONLY =
      ImmutableSet.of(
          "g5.", "g6.", "g6e.", "gr6.", "i3.", "i3en.", "i4g.", "i4i.", "im4gn.", "is4gen.", "p5.",
          "p5e.", "trn1.", "trn1n.", "x1.", "x1e.");

  public static final String UPDATING_TASK_UUID_FIELD = "updatingTaskUUID";
  public static final String PLACEMENT_MODIFICATION_TASK_UUID_FIELD =
      "placementModificationTaskUuid";

  public static final Set<SoftwareUpgradeState> IN_PROGRESS_UNIV_SOFTWARE_UPGRADE_STATES =
      ImmutableSet.of(
          SoftwareUpgradeState.Upgrading,
          SoftwareUpgradeState.RollingBack,
          SoftwareUpgradeState.Finalizing);

  @Constraints.Required()
  @Size(min = 1)
  public List<Cluster> clusters = new LinkedList<>();

  // This is set during configure to figure out which cluster type is intended to be modified.
  @ApiModelProperty public ClusterType currentClusterType;

  public ClusterType getCurrentClusterType() {
    return currentClusterType == null ? ClusterType.PRIMARY : currentClusterType;
  }

  // This should be a globally unique name - it is a combination of the customer id and the universe
  // id. This is used as the prefix of node names in the universe.
  @ApiModelProperty public String nodePrefix = null;

  // Runtime flags to be set when creating the Universe
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @ApiModelProperty
  public Map<String, String> runtimeFlags = null;

  // The UUID of the rootCA to be used to generate node certificates and facilitate TLS
  // communication between database nodes.
  @ApiModelProperty public UUID rootCA = null;

  // The UUID of the clientRootCA to be used to generate client certificates and facilitate TLS
  // communication between server and client.
  // This is made 'protected' to make sure there is no direct setting/getting
  @ApiModelProperty protected UUID clientRootCA = null;

  // This flag represents whether user has chosen to use same certificates for node to node and
  // client to server communication.
  // Default is set to true to ensure backward compatability
  @ApiModelProperty public boolean rootAndClientRootCASame = true;

  // This flag represents whether user has chosen to provide placement info
  // In Edit Universe if this flag is set we go through the NEW_CONFIG_FROM_PLACEMENT_INFO path
  @ApiModelProperty public boolean userAZSelected = false;

  // Set to true if resetting Universe form (in EDIT mode), false otherwise.
  @ApiModelProperty public boolean resetAZConfig = false;

  // TODO: Add a version number to prevent stale updates.
  // Set to true when an create/edit/destroy intent on the universe is started.
  @ApiModelProperty public boolean updateInProgress = false;

  // Type of task which set updateInProgress flag.
  @ApiModelProperty public TaskType updatingTask = null;

  // UUID of task which set updateInProgress flag.
  @ApiModelProperty public UUID updatingTaskUUID = null;

  // This tracks that if latest operation on this universe has successfully completed. This flag is
  // reset each time a new operation on the universe starts, and is set at the very end of that
  // operation.
  @ApiModelProperty public boolean updateSucceeded = true;

  // This tracks whether the universe is in the paused state or not.
  @ApiModelProperty public boolean universePaused = false;

  // UUID of last failed task that applied modification to cluster state.
  @ApiModelProperty public UUID placementModificationTaskUuid = null;

  @ApiModelProperty public SoftwareUpgradeState softwareUpgradeState = SoftwareUpgradeState.Ready;

  // Set to true when software rollback is allowed.
  @ApiModelProperty(
      value = "Available since YBA version 2.20.2.0",
      accessMode = AccessMode.READ_ONLY)
  public boolean isSoftwareRollbackAllowed = false;

  public enum SoftwareUpgradeState {
    @EnumValue("Ready")
    Ready,
    @EnumValue("Upgrading")
    Upgrading,
    @EnumValue("UpgradeFailed")
    UpgradeFailed,
    @EnumValue("PreFinalize")
    PreFinalize,
    @EnumValue("Finalizing")
    Finalizing,
    @EnumValue("FinalizeFailed")
    FinalizeFailed,
    @EnumValue("RollingBack")
    RollingBack,
    @EnumValue("RollbackFailed")
    RollbackFailed
  }

  // The next cluster index to be used when a new read-only cluster is added.
  @ApiModelProperty public int nextClusterIndex = 1;

  // Flag to mark if the universe was created with insecure connections allowed.
  // Ideally should be false since we would never want to allow insecure connections,
  // but defaults to true since we want universes created through pre-TLS YW to be
  // unaffected.
  @ApiModelProperty public boolean allowInsecure = true;

  // Flag to check whether the txn_table_wait_ts_count gflag has to be set
  // while creating the universe or not. By default it should be false as we
  // should not set this flag for operations other than create universe.
  @ApiModelProperty public boolean setTxnTableWaitCountFlag = false;

  // Development flag to download package from s3 bucket.
  @ApiModelProperty public String itestS3PackagePath = "";

  @ApiModelProperty public String remotePackagePath = "";

  // EDIT mode: Set to true if nodes could be resized without full move.
  @ApiModelProperty public boolean nodesResizeAvailable = false;

  // This flag indicates whether the Kubernetes universe will use new
  // naming style of the Helm chart. The value cannot be changed once
  // set during universe creation. Default is set to false for
  // backward compatibility.
  @ApiModelProperty public boolean useNewHelmNamingStyle = false;

  // Place all masters into default region flag.
  @ApiModelProperty public boolean mastersInDefaultRegion = true;

  // true iff created through a k8s CR and controlled by the
  // Kubernetes Operator.
  @ApiModelProperty public boolean isKubernetesOperatorControlled = false;

  // @ApiModelProperty public Map<ClusterAZ, String> existingLBs = null;

  // Override the default DB present in pre-built Ami
  @ApiModelProperty(hidden = true)
  public boolean overridePrebuiltAmiDBVersion = false;

  // local value for sequence number
  @ApiModelProperty(hidden = true)
  public int sequenceNumber = -1;

  // if we want to use a different SSH_USER instead of  what is defined in the accessKey
  // Use imagebundle to overwrite the sshPort
  @Nullable @ApiModelProperty @Deprecated public String sshUserOverride;



  /** Allowed states for an imported universe. */
  public enum ImportedState {
    NONE, // Default, and for non-imported universes.
    STARTED,
    MASTERS_ADDED,
    TSERVERS_ADDED,
    IMPORTED
  }

  // State of the imported universe.
  @ApiModelProperty public ImportedState importedState = ImportedState.NONE;

  /** Type of operations that can be performed on the universe. */
  public enum Capability {
    READ_ONLY,
    EDITS_ALLOWED // Default, and for non-imported universes.
  }

  // Capability of the universe.
  @ApiModelProperty public Capability capability = Capability.EDITS_ALLOWED;

  /** Types of Clusters that can make up a universe. */
  public enum ClusterType {
    PRIMARY,
    ASYNC,
    ADDON
  }

  /** Allowed states for an exposing service of a universe */
  public enum ExposingServiceState {
    NONE, // Default, and means the universe was created before addition of the flag.
    EXPOSED,
    UNEXPOSED
  }

  /**
   * This are available update options when user clicks "Save" on EditUniverse page. UPDATE and
   * FULL_MOVE are handled by the same task (EditUniverse), the difference is that for FULL_MOVE ui
   * acts a little different. SMART_RESIZE_NON_RESTART - we don't need any confirmations for that as
   * it is non-restart. SMART_RESIZE - upgrade that handled by ResizeNode task GFLAGS_UPGRADE - for
   * the case of toggling "enable YSQL" and so on.
   */
  public enum UpdateOptions {
    UPDATE,
    FULL_MOVE,
    SMART_RESIZE_NON_RESTART,
    SMART_RESIZE,
    GFLAGS_UPGRADE
  }

  @ApiModelProperty public Set<UpdateOptions> updateOptions = new HashSet<>();


  @ApiModelProperty(value = "YbaApi Internal. OpenTelemetry Collector enabled for universe")
  
  public boolean otelCollectorEnabled = false;

  @ApiModelProperty(
      hidden = true,
      value = "YbaApi Internal. Skip user intent match with task params")

  public boolean skipMatchWithUserIntent = false;

  @ApiModelProperty(value = "YbaApi Internal. Install node agent in background if it is true")
  
  public boolean installNodeAgent = false;


  /** A wrapper for all the clusters that will make up the universe. */
  @JsonInclude(value = JsonInclude.Include.NON_NULL)
  @Slf4j
  public static class Cluster {

    public UUID uuid = UUID.randomUUID();

    @ApiModelProperty(required = false)
    public void setUuid(UUID uuid) {
      this.uuid = uuid;
    }

    // The type of this cluster.
    @Constraints.Required()
    public ClusterType clusterType;

    // The configuration for the universe the user intended.
    @Constraints.Required()
    public UserIntent userIntent;

    // The cluster index by which node names are sorted when shown in UI.
    // This is set internally by the placement util in the server, client should not set it.
    @ApiModelProperty
    public int index = 0;

    @ApiModelProperty(accessMode = AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public List<Region> regions;

    @Setter
    @Getter
    @ApiModelProperty(
            hidden = true,
            value = "YbaApi Internal. Kubernetes per AZ statefulset gflags checksum map")

    private Map<UUID, Map<ServerType, String>> perAZServerTypeGflagsChecksumMap = new HashMap<>();

    /**
     * Default to PRIMARY.
     */
    private Cluster() {
      this(ClusterType.PRIMARY, new UserIntent());
    }

    /**
     * @param clusterType One of [PRIMARY, ASYNC]
     * @param userIntent  Customized UserIntent describing the desired state of this Cluster.
     */
    public Cluster(ClusterType clusterType, UserIntent userIntent) {
      assert clusterType != null && userIntent != null;
      this.clusterType = clusterType;
      this.userIntent = userIntent;
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(17, 37).append(uuid).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || obj.getClass() != getClass()) {
        return false;
      }
      Cluster other = (Cluster) obj;
      return uuid.equals(other.uuid);
    }

    @JsonIgnore
    public int getExpectedNumberOfNodes() {
      return userIntent.dedicatedNodes
              ? userIntent.numNodes + userIntent.replicationFactor
              : userIntent.numNodes;
    }

  }
    /**
     * The user defined intent for the universe.
     */
    @Slf4j
    public static class UserIntent {

      // Nice name for the universe.
      @Constraints.Required()
      @ApiModelProperty
      public String universeName;

      // TODO: https://github.com/yugabyte/yugabyte-db/issues/8190
      // The cloud provider UUID.
      @ApiModelProperty
      public String provider;

      // The cloud provider type as an enum. This is set in the middleware from the provider UUID
      // field above.
      @ApiModelProperty
      public CloudType providerType = CloudType.unknown;

      // The replication factor.
      @Constraints.Min(1)
      @ApiModelProperty
      public int replicationFactor = 3;

      // The list of regions that the user wants to place data replicas into.
      @ApiModelProperty
      public List<UUID> regionList;

      // The regions that the user wants to nominate as the preferred region. This makes sense only
      // for a multi-region setup.
      @ApiModelProperty
      public UUID preferredRegion;

      // Cloud Instance Type that the user wants for tserver nodes.
      @Constraints.Required()
      @ApiModelProperty(
              value =
                      "Instance type that is used for tserver nodes "
                              + "in current cluster. Could be modified in payload for /resize_node API call")
      public String instanceType;

      // Used only for k8s universes when instance type is set to custom.
      @ApiModelProperty
      public K8SNodeResourceSpec masterK8SNodeResourceSpec;

      @ApiModelProperty
      public K8SNodeResourceSpec tserverK8SNodeResourceSpec;

      @Data
      public static class K8SNodeResourceSpec {
        // Memory in GiB
        public Double memoryGib = 4.0;
        // CPU in core count
        public Double cpuCoreCount = 2.0;

        public K8SNodeResourceSpec clone() {
          K8SNodeResourceSpec spec = new K8SNodeResourceSpec();
          spec.memoryGib = memoryGib;
          spec.cpuCoreCount = cpuCoreCount;
          return spec;
        }
      }

      public static final double MIN_CPU = 0.5;
      public static final double MAX_CPU = 100.0;
      public static final double MIN_MEMORY = 2.0;
      public static final double MAX_MEMORY = 1000.0;

      // The number of nodes to provision. These include ones for both masters and tservers.
      @Constraints.Min(1)
      @ApiModelProperty
      public int numNodes;

      // Universe level overrides for kubernetes universes.
      @ApiModelProperty
      public String universeOverrides;

      // AZ level overrides for kubernetes universes.
      @ApiModelProperty
      public Map<String, String> azOverrides;

      // The software version of YB to install.
      @Constraints.Required()
      @ApiModelProperty
      public String ybSoftwareVersion;

      @Constraints.Required()
      @ApiModelProperty
      public String accessKeyCode;


      @ApiModelProperty(notes = "default: true")
      public boolean assignPublicIP = true;

      @ApiModelProperty(value = "Whether to assign static public IP")
      public boolean assignStaticPublicIP = false;

      @ApiModelProperty(notes = "default: false")
      public boolean useSpotInstance = false;

      @ApiModelProperty(notes = "Max price we are willing to pay for spot instance")
      public Double spotPrice = 0.0;

      @ApiModelProperty()
      public boolean useTimeSync = false;

      @ApiModelProperty()
      public boolean enableYCQL = true;

      @ApiModelProperty()
      public String ysqlPassword;

      @ApiModelProperty()
      public String ycqlPassword;

      @ApiModelProperty(hidden = true)
      public boolean defaultYsqlPassword = false;

      @ApiModelProperty(hidden = true)
      public boolean defaultYcqlPassword = false;

      @ApiModelProperty()
      public Long kubernetesOperatorVersion;

      @ApiModelProperty()
      public boolean enableYSQLAuth = false;

      @ApiModelProperty()
      public boolean enableYCQLAuth = false;

      @ApiModelProperty()
      public boolean enableYSQL = true;

      @ApiModelProperty()
      public boolean enableConnectionPooling = false;

      @ApiModelProperty(notes = "default: true")
      public boolean enableYEDIS = true;

      @ApiModelProperty()
      public boolean enableNodeToNodeEncrypt = false;

      @ApiModelProperty()
      public boolean enableClientToNodeEncrypt = false;

      @ApiModelProperty()
      public boolean enableVolumeEncryption = false;

      @ApiModelProperty()
      public boolean enableIPV6 = false;

      @ApiModelProperty()
      public UUID imageBundleUUID;

      @ApiModelProperty(
              hidden = true,
              value = "YbaApi Internal. Default service scope for Kubernetes universes")

      public boolean defaultServiceScopeAZ = true;

      @ApiModelProperty
      public String awsArnString;

      @ApiModelProperty()
      public boolean enableLB = false;

      // When this is set to true, YW will setup the universe to communicate by way of hostnames
      // instead of ip addresses. These hostnames will have been provided during on-prem provider
      // setup and will be in-place of privateIP
      @Deprecated
      @ApiModelProperty()
      public boolean useHostname = false;

      @ApiModelProperty()
      public Boolean useSystemd = false;


      // Flags for YB-Controller.
      @ApiModelProperty
      public Map<String, String> ybcFlags = new HashMap<>();

      // Instance tags
      @ApiModelProperty
      public Map<String, String> instanceTags = new HashMap<>();

      // True if user wants to have dedicated nodes for master and tserver processes.
      @ApiModelProperty
      public boolean dedicatedNodes = false;

      // Instance type used for dedicated master nodes.
      @Nullable
      @ApiModelProperty(
              "Instance type that is used for master nodes in current cluster "
                      + "(in dedicated masters mode). "
                      + "Could be modified in payload for /resize_node API call")
      public String masterInstanceType;


      @Getter
      @Setter
      @ApiModelProperty(value = "YbaApi Internal. Use clockbound as time source")
      private boolean useClockbound = false;

      @Override
      public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UserIntent for universe=").append(universeName);
        sb.append(", type=").append(instanceType);
        sb.append(", spotInstance=").append(useSpotInstance);
        sb.append(", useSpotInstance=").append(useSpotInstance);
        sb.append(", spotPrice=").append(spotPrice);
        sb.append(", useSpotInstance=").append(useSpotInstance);
        sb.append(", numNodes=").append(numNodes);
        sb.append(", prov=").append(provider);
        sb.append(", provType=").append(providerType);
        sb.append(", RF=").append(replicationFactor);
        sb.append(", regions=").append(regionList);
        sb.append(", pref=").append(preferredRegion);
        sb.append(", ybVersion=").append(ybSoftwareVersion);
        sb.append(", accessKey=").append(accessKeyCode);
        sb.append(", timeSync=").append(useTimeSync);
        sb.append(", publicIP=").append(assignPublicIP);
        sb.append(", staticPublicIP=").append(assignStaticPublicIP);
        sb.append(", tags=").append(instanceTags);
        sb.append(", masterInstanceType=").append(masterInstanceType);
        sb.append(", imageBundleUUID=").append(imageBundleUUID);
        sb.append(", kubernetesOperatorVersion=").append(kubernetesOperatorVersion);
        return sb.toString();
      }

      @Override
      public UserIntent clone() {
        UserIntent newUserIntent = new UserIntent();
        newUserIntent.universeName = universeName;
        newUserIntent.provider = provider;
        newUserIntent.providerType = providerType;
        newUserIntent.replicationFactor = replicationFactor;
        if (regionList != null) {
          newUserIntent.regionList = new ArrayList<>(regionList);
        }
        newUserIntent.preferredRegion = preferredRegion;
        newUserIntent.instanceType = instanceType;
        newUserIntent.numNodes = numNodes;
        newUserIntent.ybSoftwareVersion = ybSoftwareVersion;
        newUserIntent.useSystemd = useSystemd;
        newUserIntent.accessKeyCode = accessKeyCode;
        newUserIntent.assignPublicIP = assignPublicIP;
        newUserIntent.useSpotInstance = useSpotInstance;
        newUserIntent.spotPrice = spotPrice;
        newUserIntent.assignStaticPublicIP = assignStaticPublicIP;
        newUserIntent.useTimeSync = useTimeSync;
        newUserIntent.enableYSQL = enableYSQL;
        newUserIntent.enableConnectionPooling = enableConnectionPooling;
        newUserIntent.enableYCQL = enableYCQL;
        newUserIntent.enableYSQLAuth = enableYSQLAuth;
        newUserIntent.enableYCQLAuth = enableYCQLAuth;
        newUserIntent.ysqlPassword = ysqlPassword;
        newUserIntent.ycqlPassword = ycqlPassword;
        newUserIntent.enableYEDIS = enableYEDIS;
        newUserIntent.enableNodeToNodeEncrypt = enableNodeToNodeEncrypt;
        newUserIntent.enableClientToNodeEncrypt = enableClientToNodeEncrypt;
        newUserIntent.instanceTags = new HashMap<>(instanceTags);
        newUserIntent.enableLB = enableLB;
        if (masterK8SNodeResourceSpec != null) {
          newUserIntent.masterK8SNodeResourceSpec = masterK8SNodeResourceSpec.clone();
        }
        if (tserverK8SNodeResourceSpec != null) {
          newUserIntent.tserverK8SNodeResourceSpec = tserverK8SNodeResourceSpec.clone();
        }
        return newUserIntent;
      }


      public Integer getCGroupSize(@NotNull NodeDetails nodeDetails) {
        return getCGroupSize(null);
      }

      @JsonIgnore
      public String getBaseInstanceType() {
        return getInstanceType(null);
      }

      public String getInstanceType(@Nullable UUID azUUID) {
        return null;//getInstanceType(null, azUUID);
      }


      public String getInstanceTypeForNode(NodeDetails nodeDetails) {
        return null;//getInstanceType(nodeDetails.dedicatedTo, nodeDetails.getAzUuid());
      }


      @Override
      public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(universeName)
                .append(provider)
                .append(providerType)
                .append(replicationFactor)
                .append(regionList)
                .append(preferredRegion)
                .append(instanceType)
                .append(numNodes)
                .append(ybSoftwareVersion)
                .append(accessKeyCode)
                .append(assignPublicIP)
                .append(useSpotInstance)
                .append(spotPrice)
                .append(assignStaticPublicIP)
                .append(useTimeSync)
                .append(useSystemd)
                .append(dedicatedNodes)
                .append(masterInstanceType)
                .append(masterInstanceType)
                .build();
      }

      // NOTE: If new fields are checked, please add them to the toString() as well.
      @Override
      public boolean equals(Object obj) {
        if (this == obj) {
          return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
          return false;
        }
        UserIntent other = (UserIntent) obj;
        if (universeName.equals(other.universeName)
                && provider.equals(other.provider)
                && providerType == other.providerType
                && replicationFactor == other.replicationFactor
                && compareRegionLists(regionList, other.regionList)
                && Objects.equals(preferredRegion, other.preferredRegion)
                && instanceType.equals(other.instanceType)
                && numNodes == other.numNodes
                && ybSoftwareVersion.equals(other.ybSoftwareVersion)
                && (accessKeyCode == null || accessKeyCode.equals(other.accessKeyCode))
                && assignPublicIP == other.assignPublicIP
                && useSpotInstance == other.useSpotInstance
                && spotPrice.equals(other.spotPrice)
                && assignStaticPublicIP == other.assignStaticPublicIP
                && useTimeSync == other.useTimeSync
                && useSystemd == other.useSystemd
                && dedicatedNodes == other.dedicatedNodes
                && Objects.equals(masterInstanceType, other.masterInstanceType)
        ) {
          return true;
        }
        return false;
      }

      public boolean onlyRegionsChanged(UserIntent other) {
        if (universeName.equals(other.universeName)
                && provider.equals(other.provider)
                && providerType == other.providerType
                && replicationFactor == other.replicationFactor
                && newRegionsAdded(regionList, other.regionList)
                && Objects.equals(preferredRegion, other.preferredRegion)
                && instanceType.equals(other.instanceType)
                && numNodes == other.numNodes
                && ybSoftwareVersion.equals(other.ybSoftwareVersion)
                && (accessKeyCode == null || accessKeyCode.equals(other.accessKeyCode))
                && assignPublicIP == other.assignPublicIP
                && useSpotInstance == other.useSpotInstance
                && spotPrice.equals(other.spotPrice)
                && assignStaticPublicIP == other.assignStaticPublicIP
                && useTimeSync == other.useTimeSync
                && dedicatedNodes == other.dedicatedNodes
                && Objects.equals(masterInstanceType, other.masterInstanceType)) {
          return true;
        }
        return false;
      }
    }

    private static boolean newRegionsAdded(List<UUID> left, List<UUID> right) {
      return (new HashSet<>(left)).containsAll(new HashSet<>(right));
    }

    /**
     * Helper API to check if the set of regions is the same in two lists. Does not validate that
     * the UUIDs correspond to actual, existing Regions.
     *
     * @param left  First list of Region UUIDs.
     * @param right Second list of Region UUIDs.
     * @return true if the unordered, unique set of UUIDs is the same in both lists, else false.
     */
    private static boolean compareRegionLists(List<UUID> left, List<UUID> right) {
      return (new HashSet<>(left)).equals(new HashSet<>(right));
    }


  
  // --------------------------------------------------------------------------------
  // End of XCluster.
 }
