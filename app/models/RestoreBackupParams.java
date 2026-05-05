package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import play.data.validation.Constraints;

@NoArgsConstructor
public class RestoreBackupParams {

  public enum ActionType {
    RESTORE,
    RESTORE_KEYS
  }

  @Constraints.Required
  @ApiModelProperty(value = "Customer UUID")
  public UUID customerUUID;

  @Constraints.Required
  @ApiModelProperty(value = "Universe UUID", required = true)
  @Getter
  @Setter
  private UUID universeUUID;

  @ApiModelProperty(value = "KMS configuration UUID")
  public UUID kmsConfigUUID = null;

  // Intermediate states to resume ybc backups
  @ApiModelProperty(value = "Prefix UUID")
  public UUID prefixUUID;

  @ApiModelProperty(value = "Current index", hidden = true)
  public int currentIdx;

  @ApiModelProperty(value = "Current ybc task id", hidden = true)
  public String currentYbcTaskId;

  @ApiModelProperty(value = "Node IP")
  public String nodeIp;

  // Should backup script enable verbose logging.
  @ApiModelProperty(value = "Is verbose logging enabled")
  public boolean enableVerboseLogs = false;

  @ApiModelProperty(value = "Storage config uuid")
  public UUID storageConfigUUID;

  @ApiModelProperty(value = "Alter load balancer state")
  public boolean alterLoadBalancer = true;

  @ApiModelProperty(value = "Disable checksum")
  public Boolean disableChecksum = false;

  @ApiModelProperty(value = "Is tablespaces information included")
  public Boolean useTablespaces = false;

  @ApiModelProperty(value = "Disable multipart upload")
  public boolean disableMultipart = false;

  // The number of concurrent commands to run on nodes over SSH
  @ApiModelProperty(value = "Number of concurrent commands to run on nodes over SSH")
  public int parallelism = 8;

  @ApiModelProperty(value = "Restore TimeStamp")
  public String restoreTimeStamp = null;

  @ApiModelProperty(value = "Restore timestamp in millis")
  public long restoreToPointInTimeMillis;

  @ApiModel(description = "Backup Storage Info for doing restore operation")
  public static class BackupStorageInfo {

    // Specifies the backup storage location. In case of S3 it would have
    // the S3 url based on universeUUID and timestamp.
    @ApiModelProperty(value = "Storage location")
    public String storageLocation;

    @ApiModelProperty(value = "Keyspace name")
    public String keyspace;

    @ApiModelProperty(value = "Tables")
    public List<String> tableNameList;

    @ApiModelProperty(value = "Is SSE")
    public boolean sse = false;

    @ApiModelProperty(value = "User name of the current tables owner")
    public String oldOwner = "postgres";

    @ApiModelProperty(value = "User name of the new tables owner")
    public String newOwner = null;

    @ApiModelProperty(
        value = "Only restore selected tables instead of restoring all tables in backup")
    public boolean selectiveTableRestore = false;

    @ApiModelProperty(value = "Use tablespaces during restore")
    @Getter
    @Setter
    private boolean useTablespaces = false;

    // During restore, alter tables/schemas to be owned by the restored roles.
    @ApiModelProperty(value = "Backup global ysql roles", hidden = true)
    @Getter
    @Setter
    private Boolean useRoles = true;

    // When set, ybc backups will ignore all new flags that came with roles backup. Useful for
    // taking
    // backups on older universes.
    @ApiModelProperty(hidden = true)
    @Getter
    @Setter
    private Boolean revertToPreRolesBehaviour = false;

    /* Error handling flags */

    // This will be be harcoded to true if success marker does not have dump_role_checks set to
    // true.
    // Default true until testing is complete.
    @ApiModelProperty(
        value = "WARNING: This is a preview API that could change. Ignore all restore errors")
    @Getter
    @Setter
    private Boolean ignoreErrors = true;
  }


}
