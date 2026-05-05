package models;
import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;
import static play.mvc.Http.Status.BAD_REQUEST;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;

import io.ebean.ExpressionList;
import io.ebean.FetchGroup;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.EnumValue;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import play.data.validation.Constraints;

@Entity
@ApiModel(description = "Task information")
@Getter
@Setter
public class TaskInfo extends Model {

  private static final FetchGroup<TaskInfo> GET_SUBTASKS_FG =
      FetchGroup.of(TaskInfo.class, "uuid, subTaskGroupType, taskState");


  public static final Set<State> COMPLETED_STATES =
      Sets.immutableEnumSet(State.Success, State.Failure, State.Aborted);

  public static final Set<State> ERROR_STATES = Sets.immutableEnumSet(State.Failure, State.Aborted);

  public static final Set<State> INCOMPLETE_STATES =
      Sets.immutableEnumSet(State.Created, State.Initializing, State.Running, State.Abort);

  public static final Finder<UUID, TaskInfo> find = new Finder<UUID, TaskInfo>(TaskInfo.class) {};

  /** These are the various states of the task and taskgroup. */
  public enum State {
    @EnumValue("Created")
    Created(3),

    @EnumValue("Initializing")
    Initializing(1),

    @EnumValue("Running")
    Running(4),

    @EnumValue("Success")
    Success(2),

    @EnumValue("Failure")
    Failure(7),

    @EnumValue("Unknown")
    Unknown(0),

    @EnumValue("Abort")
    Abort(5),

    @EnumValue("Aborted")
    Aborted(6);

    // State override precedence to report the aggregated state for a SubGroupType.
    private final int precedence;

    private State(int precedence) {
      this.precedence = precedence;
    }

    public int getPrecedence() {
      return precedence;
    }
  }

  // The task UUID.
  @Id
  @ApiModelProperty(value = "Task UUID", accessMode = READ_ONLY)
  @JsonProperty("uuid")
  private UUID uuid;

  // The UUID of the parent task (if any; CustomerTasks have no parent)
  @ApiModelProperty(value = "Parent task UUID", accessMode = READ_ONLY)
  @JsonProperty("parentUuid")
  private UUID parentUuid;

  // The position within the parent task's taskQueue (-1 for a CustomerTask)
  @Column(columnDefinition = "integer default -1")
  @ApiModelProperty(
      value = "The task's position with its parent task's queue",
      accessMode = READ_ONLY)
  @JsonProperty("position")
  private Integer position = -1;


  // The task state.
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  @ApiModelProperty(value = "Task state", accessMode = READ_ONLY)
  @JsonProperty("taskState")
  private State taskState = State.Created;

  // The subtask group type (if it is a subtask)
  @Enumerated(EnumType.STRING)
  @ApiModelProperty(value = "Subtask type", accessMode = READ_ONLY)
  @JsonProperty("subTaskGroupType")
  private UserTaskDetails.SubTaskGroupType subTaskGroupType;

  // The task creation time.
  @WhenCreated
  @ApiModelProperty(value = "Creation time", accessMode = READ_ONLY, example = "1624295239113")
  @JsonProperty("createTime")
  private Date createTime;

  // The task update time. Time of the latest update (including heartbeat updates) on this task.
  @WhenModified
  @ApiModelProperty(value = "Updated time", accessMode = READ_ONLY, example = "1624295239113")
  @JsonProperty("updateTime")
  private Date updateTime;

  // The percentage completeness of the task, which is a number from 0 to 100.
  @Column(columnDefinition = "integer default 0")
  @ApiModelProperty(value = "Percentage complete", accessMode = READ_ONLY)
  @JsonProperty("percentDone")
  private Integer percentDone = 0;

  // Task input parameters.
  @Constraints.Required
  @Column(columnDefinition = "TEXT default '{}'", nullable = false)
  @DbJson
  @ApiModelProperty(value = "Task params", accessMode = READ_ONLY, required = true)
  @JsonProperty("taskParams")
  private JsonNode taskParams;



  // Identifier of the process owning the task.
  @Constraints.Required
  @Column(nullable = false)
  @ApiModelProperty(
      value = "ID of the process that owns this task",
      accessMode = READ_ONLY,
      required = true)
  @JsonProperty("owner")
  private String owner;


  @JsonCreator
  public TaskInfo(
      @JsonProperty("uuid") UUID uuid,
      @JsonProperty("parentUuid") UUID parentUuid,
      @JsonProperty("position") Integer position,
      @JsonProperty("taskState") State taskState,
      @JsonProperty("subTaskGroupType") UserTaskDetails.SubTaskGroupType subTaskGroupType,
      @JsonProperty("createTime") Date createTime,
      @JsonProperty("updateTime") Date updateTime,
      @JsonProperty("percentDone") Integer percentDone,
      @JsonProperty("taskParams") JsonNode taskParams,
      @JsonProperty("owner") String owner) {
    this.uuid = uuid;
    this.parentUuid = parentUuid;
    this.position = position != null ? position : -1;
    this.taskState = taskState != null ? taskState : State.Created;
    this.subTaskGroupType = subTaskGroupType;
    this.createTime = createTime;
    this.updateTime = updateTime;
    this.percentDone = percentDone != null ? percentDone : 0;
    this.taskParams = taskParams;
    this.owner = owner;
  }
  
}
