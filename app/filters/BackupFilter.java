package filters;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import models.Backup;
@Value
@Builder(toBuilder = true)
public class BackupFilter {

  Date dateRangeStart;
  Date dateRangeEnd;
  Set<Backup.BackupState> states;
  Set<String> keyspaceList;
  Set<String> universeNameList;
  Set<UUID> storageConfigUUIDList;
  Set<UUID> scheduleUUIDList;
  Set<UUID> universeUUIDList;
  Set<UUID> backupUUIDList;
  UUID customerUUID;
  boolean onlyShowDeletedUniverses;
  boolean onlyShowDeletedConfigs;
  boolean showHidden;
}
