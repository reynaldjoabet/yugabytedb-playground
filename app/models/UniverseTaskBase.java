package models;

import static play.mvc.Http.Status.INTERNAL_SERVER_ERROR;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.google.api.client.util.Throwables;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.net.HostAndPort;
import io.ebean.Model;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.MDC;
import play.libs.Json;
import play.mvc.Http;
import models.CustomerTask.TaskType;
//import models.UniverseUpdaterConfig;
@Slf4j
public abstract class UniverseTaskBase {

  @Builder
  @Getter
  public static class AllowedTasks {
    private boolean restricted;
    private boolean rerun;
    private TaskType lockedTaskType;
    // Allowed task types.
    @Singular
    private Set<TaskType> taskTypes;


  }

  // Tasks that modify cluster placement.
  // If one of such tasks is failed, we should not allow starting most of other tasks,
  // until failed task is retried.
  private static final Set<TaskType> PLACEMENT_MODIFICATION_TASKS =
          ImmutableSet.of(
                  TaskType.CreateUniverse,
                  TaskType.CreateKubernetesUniverse,
                  TaskType.ReadOnlyClusterCreate,
                  TaskType.ReadOnlyClusterDelete,
                  TaskType.ResizeNode,
                  TaskType.KubernetesOverridesUpgrade,
                  TaskType.GFlagsKubernetesUpgrade,
                  TaskType.SoftwareKubernetesUpgrade,
                  TaskType.SoftwareKubernetesUpgradeYB,
                  TaskType.EditKubernetesUniverse,
                  TaskType.RestartUniverseKubernetesUpgrade,
                  TaskType.CertsRotateKubernetesUpgrade,
                  TaskType.GFlagsUpgrade,
                  TaskType.SoftwareUpgrade,
                  TaskType.SoftwareUpgradeYB,
                  TaskType.FinalizeUpgrade,
                  TaskType.FinalizeKubernetesUpgrade,
                  TaskType.RollbackUpgrade,
                  TaskType.RollbackKubernetesUpgrade,
                  TaskType.RestartUniverse,
                  TaskType.RebootNodeInUniverse,
                  TaskType.VMImageUpgrade,
                  TaskType.ThirdpartySoftwareUpgrade,
                  TaskType.CertsRotate,
                  TaskType.TlsToggle,
                  TaskType.MasterFailover,
                  TaskType.SyncMasterAddresses,
                  TaskType.PauseUniverse,
                  TaskType.ResumeUniverse,
                  TaskType.PauseXClusterUniverses,
                  TaskType.ResumeXClusterUniverses,
                  TaskType.DecommissionNode);

  // Tasks that are allowed to run if cluster placement modification task failed.
  // This mapping blocks/allows actions on the UI done by a mapping defined in
  // UNIVERSE_ACTION_TO_FROZEN_TASK_MAP in "./managed/ui/src/redesign/helpers/constants.ts".
  private static final Set<TaskType> SAFE_TO_RUN_IF_UNIVERSE_BROKEN =
          ImmutableSet.of(
                  TaskType.CreateBackup,
                  TaskType.BackupUniverse,
                  TaskType.MultiTableBackup,
                  TaskType.RestoreBackup,
                  TaskType.CreatePitrConfig,
                  TaskType.DeletePitrConfig,
                  TaskType.CreateXClusterConfig,
                  TaskType.EditXClusterConfig,
                  TaskType.DeleteXClusterConfig,
                  TaskType.RestartXClusterConfig,
                  TaskType.SyncXClusterConfig,
                  TaskType.CreateDrConfig,
                  TaskType.SetTablesDrConfig,
                  TaskType.RestartDrConfig,
                  TaskType.EditDrConfig,
                  TaskType.SwitchoverDrConfig,
                  TaskType.FailoverDrConfig,
                  TaskType.SyncDrConfig,
                  TaskType.DeleteDrConfig,
                  TaskType.DestroyUniverse,
                  TaskType.DestroyKubernetesUniverse,
                  TaskType.ReinstallNodeAgent,
                  TaskType.CreateSupportBundle,
                  TaskType.CreateBackupSchedule,
                  TaskType.CreateBackupScheduleKubernetes,
                  TaskType.EditBackupSchedule,
                  TaskType.EditBackupScheduleKubernetes,
                  TaskType.DeleteBackupSchedule,
                  TaskType.DeleteBackupScheduleKubernetes,
                  TaskType.EnableNodeAgentInUniverse);

  private static final Set<TaskType> SKIP_CONSISTENCY_CHECK_TASKS =
          ImmutableSet.of(
                  TaskType.CreateBackup,
                  TaskType.CreateBackupSchedule,
                  TaskType.CreateBackupScheduleKubernetes,
                  TaskType.CreateKubernetesUniverse,
                  TaskType.CreateSupportBundle,
                  TaskType.CreateUniverse,
                  TaskType.BackupUniverse,
                  TaskType.DeleteBackupSchedule,
                  TaskType.DeleteBackupScheduleKubernetes,
                  TaskType.DeleteDrConfig,
                  TaskType.DeletePitrConfig,
                  TaskType.DeleteXClusterConfig,
                  TaskType.DestroyUniverse,
                  TaskType.DestroyKubernetesUniverse,
                  TaskType.EditBackupSchedule,
                  TaskType.EditBackupScheduleKubernetes,
                  TaskType.MultiTableBackup,
                  TaskType.ResumeKubernetesUniverse,
                  TaskType.ReadOnlyClusterDelete,
                  TaskType.FailoverDrConfig,
                  TaskType.ResumeUniverse);

  private static final Set<TaskType> RERUNNABLE_PLACEMENT_MODIFICATION_TASKS =
          ImmutableSet.of(
                  TaskType.GFlagsUpgrade,
                  TaskType.RestartUniverse,
                  TaskType.VMImageUpgrade,
                  TaskType.GFlagsKubernetesUpgrade,
                  TaskType.KubernetesOverridesUpgrade,
                  TaskType.EditKubernetesUniverse /* Partially allowing this for resource spec changes */,
                  TaskType.PauseUniverse /* TODO Validate this, added for YBM only */,
                  TaskType.ResumeUniverse /* TODO Validate this, added for YBM only */,
                  TaskType.PauseXClusterUniverses /* TODO Validate this, added for YBM only */,
                  TaskType.ResumeXClusterUniverses /* TODO Validate this, added for YBM only */);


  protected Set<UUID> lockedXClusterUniversesUuidSet = null;

  protected static final String MIN_WRITE_READ_TABLE_CREATION_RELEASE = "2.6.0.0";

  protected String ysqlPassword;
  protected String ycqlPassword;



  public enum VersionCheckMode {
    NEVER,
    ALWAYS,
    HA_ONLY
  }

  // Enum for specifying the server type.
  public enum ServerType {
    MASTER,
    TSERVER,
    CONTROLLER,
    // TODO: Replace all YQLServer with YCQLserver
    YQLSERVER,
    YSQLSERVER,
    REDISSERVER,
    EITHER
  }

  public static final String DUMP_ENTITIES_URL_SUFFIX = "/dump-entities";
  public static final String TABLET_REPLICATION_URL_SUFFIX = "/api/v1/tablet-replication";
  public static final String LEADERLESS_TABLETS_KEY = "leaderless_tablets";



  private final AtomicReference<ExecutionContext> executionContext = new AtomicReference<>();

  public class ExecutionContext {
    private final UUID universeUuid;
    private final boolean blacklistLeaders;
    private final int leaderBacklistWaitTimeMs;
    private final Duration waitForServerReadyTimeout;
    private final boolean followerLagCheckEnabled;
    private boolean loadBalancerOff = false;

      public ExecutionContext(UUID universeUuid, boolean blacklistLeaders, int leaderBacklistWaitTimeMs, Duration waitForServerReadyTimeout, boolean followerLagCheckEnabled) {
          this.universeUuid = universeUuid;
          this.blacklistLeaders = blacklistLeaders;
          this.leaderBacklistWaitTimeMs = leaderBacklistWaitTimeMs;
          this.waitForServerReadyTimeout = waitForServerReadyTimeout;
          this.followerLagCheckEnabled = followerLagCheckEnabled;
      }

      public boolean isLoadBalancerOff() {
      return loadBalancerOff;
    }

    public boolean isBlacklistLeaders() {
      return blacklistLeaders;
    }

    public boolean isFollowerLagCheckEnabled() {
      return followerLagCheckEnabled;
    }

    public Duration getWaitForServerReadyTimeout() {
      return waitForServerReadyTimeout;
    }


  }
    
    
}