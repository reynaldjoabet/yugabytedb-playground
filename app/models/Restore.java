package models;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import io.ebean.ExpressionList;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.PersistenceContextScope;
import io.ebean.Query;
import io.ebean.annotation.EnumValue;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import io.swagger.annotations.ApiModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApiModel(description = "Universe level restores")
@Entity
@Getter
@Setter
public class Restore extends Model {
  public static final Logger LOG = LoggerFactory.getLogger(Restore.class);
  public static final String BACKUP_UNIVERSE_UUID =
          "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
  public static final Pattern PATTERN = Pattern.compile(BACKUP_UNIVERSE_UUID);
  public static final String BACKUP_CREATED_DATE =
          "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}";
  public static final Pattern PATTERN_DATE = Pattern.compile(BACKUP_CREATED_DATE);

  public static final Finder<UUID, Restore> find = new Finder<UUID, Restore>(Restore.class) {
  };


  @Id
  private UUID restoreUUID;

  @Column(nullable = false)
  private UUID customerUUID;

  @Column(nullable = false)
  private UUID universeUUID;

  @Column
  private UUID sourceUniverseUUID;

  @Column
  private UUID storageConfigUUID;

  @Column
  private String sourceUniverseName;


  @Column(unique = true)
  private UUID taskUUID;

  @Column(nullable = false)
  private boolean hidden = false;

  @Column
  private long restoreSizeInBytes = 0L;

  @Column(nullable = false)
  private boolean alterLoadBalancer = true;

  @WhenCreated
  private Date createTime;

  @WhenModified
  private Date updateTime;

  @Column
  private Date backupCreatedOnDate;
}