package models;

import static play.mvc.Http.Status.BAD_REQUEST;

import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.WhenCreated;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.validation.Constraints;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class FileData extends Model {

  public static final Logger LOG = LoggerFactory.getLogger(FileData.class);
  private static final String PUBLIC_KEY_EXTENSION = "pub";

  private static final String UUID_PATTERN =
          "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";


  @Constraints.Required
  private UUID parentUUID;
  // The task creation time.
  @WhenCreated
  private Date timestamp;


  @Constraints.Required
  private String fileContent;

  public FileData() {
    this.timestamp = new Date();
  }

  private static final Finder<UUID, FileData> find = new Finder<UUID, FileData>(FileData.class) {
  };


  public static List<FileData> getFromParentUUID(UUID parentUUID) {
    return find.query().where().eq("parent_uuid", parentUUID).findList();
  }

  public static FileData getFromFile(String file) {
    return find.query().where().eq("file_path", file).findOne();
  }

  public static int getCount() {
    return find.query().findCount();
  }

  public static Set<FileData> getAllNames() {
    Set<FileData> fileData = find.query().select("file").findSet();
    if (CollectionUtils.isNotEmpty(fileData)) {
      return fileData;
    }
    return new HashSet<>();
  }


}

  
