

package models;
import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;
import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_WRITE;
import static play.mvc.Http.Status.BAD_REQUEST;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import io.ebean.ExpressionList;
import io.ebean.Finder;
import io.ebean.Junction;
import io.ebean.Model;
import io.ebean.PersistenceContextScope;
import io.ebean.Query;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.EnumValue;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import filters.BackupFilter;

@ApiModel(
    description =
        "A single backup. Includes the backup's status, expiration time, and configuration.")
@Entity
@Getter
@Setter
public class Backup extends Model {
  public static final Logger LOG = LoggerFactory.getLogger(Backup.class);



  public enum BackupState {
    @EnumValue("In Progress")
    InProgress,

    @EnumValue("Completed")
    Completed,

    @EnumValue("Failed")
    Failed,

    // This state is no longer used in Backup V2 APIs.
    @EnumValue("Deleted")
    Deleted,

    @EnumValue("Skipped")
    Skipped,

    // Complete or partial failure to delete
    @EnumValue("FailedToDelete")
    FailedToDelete,

    @EnumValue("Stopping")
    Stopping,

    @EnumValue("Stopped")
    Stopped,

    @EnumValue("QueuedForDeletion")
    QueuedForDeletion,

    @EnumValue("QueuedForForcedDeletion")
    QueuedForForcedDeletion,

    @EnumValue("DeleteInProgress")
    DeleteInProgress;
  }

  private static final Multimap<BackupState, BackupState> ALLOWED_TRANSITIONS =
      ImmutableMultimap.<BackupState, BackupState>builder()
          .put(BackupState.InProgress, BackupState.Completed)
          .put(BackupState.InProgress, BackupState.Failed)
          .put(BackupState.Completed, BackupState.Deleted)
          .put(BackupState.FailedToDelete, BackupState.Deleted)
          .put(BackupState.Failed, BackupState.Deleted)
          .put(BackupState.Stopped, BackupState.Deleted)
          .put(BackupState.InProgress, BackupState.Skipped)
          .put(BackupState.InProgress, BackupState.FailedToDelete)
          .put(BackupState.QueuedForDeletion, BackupState.FailedToDelete)
          .put(BackupState.DeleteInProgress, BackupState.FailedToDelete)
          .put(BackupState.DeleteInProgress, BackupState.QueuedForDeletion)
          .put(BackupState.Failed, BackupState.FailedToDelete)
          .put(BackupState.Completed, BackupState.FailedToDelete)
          .put(BackupState.InProgress, BackupState.Stopping)
          .put(BackupState.Failed, BackupState.Stopping)
          .put(BackupState.Completed, BackupState.Stopping)
          .put(BackupState.InProgress, BackupState.Stopped)
          .put(BackupState.Stopping, BackupState.Stopped)
          .put(BackupState.Failed, BackupState.Stopped)
          .put(BackupState.Completed, BackupState.Stopped)
          .put(BackupState.Failed, BackupState.QueuedForDeletion)
          .put(BackupState.Stopped, BackupState.QueuedForDeletion)
          .put(BackupState.Stopped, BackupState.InProgress)
          .put(BackupState.Stopped, BackupState.FailedToDelete)
          .put(BackupState.Stopping, BackupState.QueuedForDeletion)
          .put(BackupState.InProgress, BackupState.QueuedForDeletion)
          .put(BackupState.Completed, BackupState.QueuedForDeletion)
          .put(BackupState.Skipped, BackupState.QueuedForDeletion)
          .put(BackupState.FailedToDelete, BackupState.QueuedForDeletion)
          .put(BackupState.Deleted, BackupState.QueuedForDeletion)
          .put(BackupState.QueuedForDeletion, BackupState.DeleteInProgress)
          .put(BackupState.QueuedForDeletion, BackupState.QueuedForForcedDeletion)
          .put(BackupState.QueuedForForcedDeletion, BackupState.FailedToDelete)
          .put(BackupState.QueuedForForcedDeletion, BackupState.DeleteInProgress)
          .build();

  public enum BackupCategory {
    @EnumValue("YB_BACKUP_SCRIPT")
    YB_BACKUP_SCRIPT,

    @EnumValue("YB_CONTROLLER")
    YB_CONTROLLER
  }

  public enum BackupVersion {
    @EnumValue("V1")
    V1,

    @EnumValue("V2")
    V2
  }

  public enum StorageConfigType {
    @EnumValue("S3")
    S3,

    @EnumValue("NFS")
    NFS,

    @EnumValue("AZ")
    AZ,

    @EnumValue("GCS")
    GCS,

    @EnumValue("FILE")
    FILE;
  }

  public static final Set<BackupState> IN_PROGRESS_STATES =
      Sets.immutableEnumSet(
          BackupState.InProgress, BackupState.QueuedForDeletion, BackupState.DeleteInProgress);
  public static final List<BackupState> COMPLETED_STATES =
      Arrays.asList(
          BackupState.Completed, BackupState.Failed, BackupState.Stopped, BackupState.Skipped);



  @ApiModelProperty(value = "Backup UUID", accessMode = READ_ONLY)
  @Id
  private UUID backupUUID;

  @ApiModelProperty(value = "Customer UUID that owns this backup", accessMode = READ_WRITE)
  @Column(nullable = false)
  private UUID customerUUID;

  @JsonProperty
  public UUID getCustomerUUID() {
    return customerUUID;
  }

  @JsonIgnore
  @ApiModelProperty(value = "Whether to hide this backup on the UI", accessMode = READ_WRITE)
  @Column(nullable = false)
  private boolean hidden = false;

  @JsonIgnore
  @ApiModelProperty(value = "Universe UUID that created this backup", accessMode = READ_WRITE)
  @Column(nullable = false)
  private UUID universeUUID;

  @ApiModelProperty(value = "Storage Config UUID that created this backup", accessMode = READ_WRITE)
  @Column(nullable = false)
  private UUID storageConfigUUID;

  @ApiModelProperty(value = "Base backup UUID", accessMode = READ_ONLY)
  @Column(nullable = false)
  private UUID baseBackupUUID;

  @ApiModelProperty(value = "Universe name that created this backup", accessMode = READ_WRITE)
  @Column
  private String universeName;

  @ApiModelProperty(value = "State of the backup", example = "DELETED", accessMode = READ_ONLY)
  @Column(nullable = false)
  private BackupState state;


  @ApiModelProperty(value = "Backup UUID", accessMode = READ_ONLY)
  @Column(unique = true)
  private UUID taskUUID;

  @ApiModelProperty(
      value = "Schedule UUID, if this backup is part of a schedule",
      accessMode = READ_WRITE)
  @Column
  private UUID scheduleUUID;

  @ApiModelProperty(
      value = "Schedule Policy Name, if this backup is part of a schedule",
      accessMode = READ_WRITE)
  @Column
  private String scheduleName;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  @ApiModelProperty(
      value = "Expiry time (unix timestamp) of the backup",
      accessMode = READ_WRITE,
      example = "2022-12-12T13:07:18Z")
  @Column
  // Unix timestamp at which backup will get deleted.
  private Date expiry;

  @JsonIgnore
  private void setExpiry(long timeBeforeDeleteFromPresent) {
    this.expiry = new Date(System.currentTimeMillis() + timeBeforeDeleteFromPresent);
  }

  @JsonIgnore
  public void setExpiry(Date expiryTime) {
    this.expiry = expiryTime;
  }

  public void updateExpiryTime(long timeBeforeDeleteFromPresent) {
    setExpiry(timeBeforeDeleteFromPresent);
    save();
  }


  @ApiModelProperty(value = "Whether the backup has KMS history metadata", accessMode = READ_ONLY)
  @Column(name = "has_kms_history")
  private boolean hasKMSHistory;




  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  @ApiModelProperty(value = "Backup creation time", example = "2022-12-12T13:07:18Z")
  @WhenCreated
  private Date createTime;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  @ApiModelProperty(value = "Backup update time", example = "2022-12-12T13:07:18Z")
  @WhenModified
  private Date updateTime;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  @ApiModelProperty(
      value = "Backup completion time",
      accessMode = READ_ONLY,
      example = "2022-12-12T13:07:18Z")
  @Column
  private Date completionTime;

  @ApiModelProperty(value = "Category of the backup")
  @Column(nullable = false)
  private BackupCategory category = BackupCategory.YB_BACKUP_SCRIPT;

  @ApiModelProperty(value = "Version of the backup in a category")
  @Column(nullable = false)
  private BackupVersion version = BackupVersion.V1;

  @ApiModelProperty(value = "Retry count for backup deletion")
  @Column(nullable = false)
  private int retryCount;

  public static final Finder<UUID, Backup> find = new Finder<UUID, Backup>(Backup.class) {};

  





  @Deprecated
  public static Backup get(UUID customerUUID, UUID backupUUID) {
    return find.query().where().idEq(backupUUID).eq("customer_uuid", customerUUID).findOne();
  }

  public static Backup getOrBadRequest(UUID customerUUID, UUID backupUUID) {
    Backup backup = get(customerUUID, backupUUID);
    if (backup == null) {
      throw new PlatformServiceException(BAD_REQUEST, "Invalid customer or backup UUID");
    }
    return backup;
  }

  public static Optional<Backup> maybeGet(UUID customerUUID, UUID backupUUID) {
    return Optional.ofNullable(get(customerUUID, backupUUID));
  }

  public static Optional<Backup> maybeGet(UUID backupUUID) {
    Backup backup = find.byId(backupUUID);
    if (backup == null) {
      LOG.trace("Cannot find backup {}", backupUUID);
      return Optional.empty();
    }
    return Optional.of(backup);
  }

  public static List<Backup> fetchAllBackupsByTaskUUID(UUID taskUUID) {
    return Backup.find.query().where().eq("task_uuid", taskUUID).findList();
  }

  public static Optional<Backup> fetchLatestByState(UUID customerUuid, BackupState state) {
    return Backup.find
        .query()
        .where()
        .eq("customer_uuid", customerUuid)
        .eq("state", state)
        .orderBy("create_time DESC")
        .findList()
        .stream()
        .findFirst();
  }

  


  /**
   * Fetch list of backups in a given backup chain in descending order of creation time.
   *
   * @param customerUUID The customer UUID
   * @param baseBackupUUID The base backup UUID
   * @param state Optional backup state to fetch backups only belonging to the given state
   * @return List of backups matching the criteria
   */
  public static List<Backup> fetchAllBackupsByBaseBackupUUID(
      UUID customerUUID, UUID baseBackupUUID, @Nullable BackupState state) {
    ExpressionList<Backup> query =
        find.query()
            .where()
            .eq("customer_uuid", customerUUID)
            .eq("base_backup_uuid", baseBackupUUID);
    if (state != null) {
      query.eq("state", state);
    }
    return query.orderBy().desc("create_time").findList();
  }

  /**
   * Get last backup in chain with state = 'Completed'.
   *
   * @param customerUUID
   * @param baseBackupUUID
   */
  public static Backup getLastSuccessfulBackupInChain(UUID customerUUID, UUID baseBackupUUID) {
    List<Backup> backupChain =
        fetchAllBackupsByBaseBackupUUID(customerUUID, baseBackupUUID, BackupState.Completed);
    if (CollectionUtils.isNotEmpty(backupChain)) {
      return backupChain.get(0);
    }
    return null;
  }

  public static List<Backup> fetchAllCompletedBackupsByScheduleUUID(
      UUID customerUUID, UUID scheduleUUID) {
    return find.query()
        .where()
        .eq("customer_uuid", customerUUID)
        .eq("schedule_uuid", scheduleUUID)
        .eq("state", BackupState.Completed)
        .findList();
  }

  
}
