package models;

import static play.mvc.Http.Status.BAD_REQUEST;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.ebean.DB;
import io.ebean.ExpressionList;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenModified;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/** Schedule for a generic job. */
@Getter
@Setter
@Entity
public class JobSchedule extends Model {
  private static final Finder<UUID, JobSchedule> finder =
      new Finder<UUID, JobSchedule>(JobSchedule.class) {};

  @Id private UUID uuid;

  @Column(nullable = false)
  private UUID customerUuid;

  @Column(nullable = false)
  private String name;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private Date lastStartTime;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private Date lastEndTime;

  @Column(nullable = false)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private Date nextStartTime;

  private UUID lastJobInstanceUuid;

  @Column(nullable = false)
  private long failedCount;

  @Column(nullable = false)
  private long executionCount;


  @WhenModified
  @Column(nullable = false)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private Date createdAt;

  @WhenModified
  @Column(nullable = false)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private Date updatedAt;

  public enum State {
    ACTIVE,
    INACTIVE
  }




  public static JobSchedule getOrBadRequest(UUID uuid) {
    return JobSchedule.maybeGet(uuid)
        .orElseThrow(
            () -> new PlatformServiceException(BAD_REQUEST, "Cannot find job schedule " + uuid));
  }

  public static JobSchedule getOrBadRequest(UUID customerUuid, UUID uuid) {
    return JobSchedule.maybeGet(customerUuid, uuid)
        .orElseThrow(
            () -> new PlatformServiceException(BAD_REQUEST, "Cannot find job schedule " + uuid));
  }

  public static Optional<JobSchedule> maybeGet(UUID uuid) {
    return Optional.ofNullable(finder.byId(uuid));
  }

  public static Optional<JobSchedule> maybeGet(UUID customerUuid, String name) {
    return Optional.ofNullable(
        finder.query().where().eq("customerUuid", customerUuid).eq("name", name).findOne());
  }

  public static Optional<JobSchedule> maybeGet(UUID customerUuid, UUID uuid) {
    return Optional.ofNullable(
        finder.query().where().eq("customerUuid", customerUuid).idEq(uuid).findOne());
  }



  public static List<JobSchedule> getAll() {
    return finder.all();
  }



  
}
