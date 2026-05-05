package models;
import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import io.ebean.ExpressionList;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.PersistenceContextScope;
import io.ebean.annotation.Formula;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import filters.AlertFilter;
@Entity
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(description = "Alert definition. Used to send an alert notification.")
public class Alert extends Model {
// implements AlertLabelsProvider
  private static final String MESSAGE_ANNOTATION = "message";

  public enum State {
    ACTIVE("firing", true),
    ACKNOWLEDGED("acknowledged", true),
    SUSPENDED("suspended", true),
    RESOLVED("resolved", false);

    private final String action;
    private final boolean firing;

    State(String action, boolean firing) {
      this.action = action;
      this.firing = firing;
    }

    public String getAction() {
      return action;
    }

    public boolean isFiring() {
      return firing;
    }

    public static Set<State> getFiringStates() {
      return Arrays.stream(values()).filter(State::isFiring).collect(Collectors.toSet());
    }
  }


  @Id
  @Column(nullable = false, unique = true)
  @ApiModelProperty(value = "Alert UUID", accessMode = READ_ONLY)
  private UUID uuid;

  @NotNull
  @Column(nullable = false)
  @ApiModelProperty(value = "Customer UUID", accessMode = READ_ONLY)
  private UUID customerUUID;



  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  @ApiModelProperty(
      value = "Timestamp at which the alert was acknowledged",
      accessMode = READ_ONLY,
      example = "2022-12-12T13:07:18Z")
  private Date acknowledgedTime;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  @ApiModelProperty(
      value = "Timestamp at which the alert was resolved",
      accessMode = READ_ONLY,
      example = "2022-12-12T13:07:18Z")
  private Date resolvedTime;

  @NotNull
  @Enumerated(EnumType.STRING)
  @ApiModelProperty(value = "Alert configuration severity", accessMode = READ_ONLY)
  private AlertConfiguration.Severity severity;

  @Transient
  @Formula(
      select =
          "(case"
              + " when severity = 'WARNING' then 1"
              + " when severity = 'SEVERE' then 2"
              + " else 0 end)")
  private Integer severityIndex;

  @NotNull
  @Size(min = 1, max = 1000)
  @ApiModelProperty(value = "The alert's name", accessMode = READ_ONLY)
  private String name;

  @NotNull
  @Size(min = 1)
  @Column(columnDefinition = "Text", nullable = false)
  @ApiModelProperty(value = "The alert's message text", accessMode = READ_ONLY)
  private String message;

  @NotNull
  @ApiModelProperty(value = "The source of the alert", accessMode = READ_ONLY)
  private String sourceName;

  @NotNull
  @ApiModelProperty(value = "The sourceUUID of the alert", accessMode = READ_ONLY)
  private UUID sourceUUID;

  @NotNull
  @Enumerated(EnumType.STRING)
  @ApiModelProperty(value = "The alert's state", accessMode = READ_ONLY)
  private State state = State.ACTIVE;

  @Transient
  @Formula(
      select =
          "(case"
              + " when state = 'ACTIVE' then 1"
              + " when state = 'ACKNOWLEDGED' then 2"
              + " when state = 'RESOLVED' then 3"
              + " else 0 end)")
  private Integer stateIndex;

  @NotNull
  @ApiModelProperty(value = "Alert definition UUID", accessMode = READ_ONLY)
  private UUID definitionUuid;

  @NotNull
  @ApiModelProperty(value = "Alert configuration UUID", accessMode = READ_ONLY)
  private UUID configurationUuid;

  @NotNull
  @Enumerated(EnumType.STRING)
  @ApiModelProperty(value = "Alert configuration type", accessMode = READ_ONLY)
  private AlertConfiguration.TargetType configurationType;

  @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, orphanRemoval = true)
  @EqualsAndHashCode.Exclude
  private List<AlertLabel> labels;

  @ApiModelProperty(
      value = "Time of the last notification attempt",
      accessMode = READ_ONLY,
      example = "2022-12-12T13:07:18Z")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private Date notificationAttemptTime;


  @ApiModelProperty(value = "Count of failures to send a notification", accessMode = READ_ONLY)
  @Column(nullable = false)
  private int notificationsFailed = 0;

  @Enumerated(EnumType.STRING)
  @ApiModelProperty(value = "Alert state in the last-sent notification", accessMode = READ_ONLY)
  private State notifiedState;

  private static final Finder<UUID, Alert> find = new Finder<UUID, Alert>(Alert.class) {};

  @VisibleForTesting
  public Alert setUuid(UUID uuid) {
    this.uuid = uuid;
    this.labels.forEach(label -> label.setAlert(this));
    return this;
  }

  public Alert generateUUID() {
    return setUuid(UUID.randomUUID());
  }

  @JsonIgnore
  public boolean isNew() {
    return uuid == null;
  }




  public String getAnnotationValue(String name) {
    if (name.equals(MESSAGE_ANNOTATION)) {
      if (state != State.RESOLVED) {
        return message;
      } else {
        return StringUtils.EMPTY;
      }
    }
    return null;
  }
  
}
