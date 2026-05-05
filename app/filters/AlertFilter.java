package filters;
import models.Alert;
import models.AlertConfiguration;
import models.AlertConfiguration.Severity;
import models.AlertConfiguration.TargetType;
import models.AlertLabel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class AlertFilter {
  Set<UUID> uuids;
  Set<UUID> excludeUuids;
  UUID customerUuid;
  Set<Alert.State> states;
  Set<UUID> definitionUuids;
  UUID configurationUuid;
  Set<AlertConfiguration.Severity> severities;
  Set<AlertConfiguration.TargetType> configurationTypes;
  AlertLabel label;
  Boolean notificationPending;
  String sourceName;
  Date resolvedDateBefore;
  Set<UUID> sourceUUIDs;

  // Can't use @Builder(toBuilder = true) as it sets null fields as well, which breaks non null
  // checks.
  public AlertFilterBuilder toBuilder() {
    AlertFilterBuilder result = null;//AlertFilter.builder();
    if (uuids != null) {
      result.uuids(uuids);
    }
    if (excludeUuids != null) {
      result.excludeUuids(excludeUuids);
    }
    if (customerUuid != null) {
      result.customerUuid(customerUuid);
    }
    if (label != null) {
      result.label(label);
    }
    if (states != null) {
      result.states(states);
    }
    if (definitionUuids != null) {
      result.definitionUuids(definitionUuids);
    }
   
    if (severities != null) {
      result.severities(severities);
    }
    if (configurationTypes != null) {
      result.configurationTypes(configurationTypes);
    }
    if (notificationPending != null) {
      result.notificationPending(notificationPending);
    }
    if (sourceName != null) {
      result.sourceName(sourceName);
    }
    if (resolvedDateBefore != null) {
      result.resolvedDateBefore(resolvedDateBefore);
    }
    if (sourceUUIDs != null) {
      result.sourceUUIDs(sourceUUIDs);
    }
    return result;
  }

  public static class AlertFilterBuilder {
    Set<UUID> uuids = new HashSet<>();
    Set<UUID> excludeUuids = new HashSet<>();
    Set<Alert.State> states = EnumSet.noneOf(Alert.State.class);
    Set<UUID> definitionUuids = new HashSet<>();
    Set<AlertConfiguration.Severity> severities = new HashSet<>();
    Set<AlertConfiguration.TargetType> configurationTypes = new HashSet<>();
    Set<UUID> sourceUUIDs = new HashSet<>();

    public AlertFilterBuilder uuid(@NonNull UUID uuid) {
      this.uuids.add(uuid);
      return this;
    }

    public AlertFilterBuilder uuids(@NonNull Collection<UUID> uuids) {
      this.uuids.addAll(uuids);
      return this;
    }

    public AlertFilterBuilder excludeUuid(@NonNull UUID uuid) {
      this.excludeUuids.add(uuid);
      return this;
    }

    public AlertFilterBuilder excludeUuids(@NonNull Collection<UUID> uuids) {
      this.excludeUuids.addAll(uuids);
      return this;
    }

    public AlertFilterBuilder customerUuid(@NonNull UUID customerUuid) {
      //this.customerUuid = customerUuid;
      return this;
    }

    public AlertFilterBuilder state(@NonNull Alert.State... state) {
      states.addAll(Arrays.asList(state));
      return this;
    }

    public AlertFilterBuilder states(@NonNull Set<Alert.State> states) {
      this.states.addAll(states);
      return this;
    }


    public AlertFilterBuilder label(@NonNull String name, @NonNull String value) {
     // label = new AlertLabel(name, value);
      return this;
    }

    public AlertFilterBuilder label(@NonNull AlertLabel label) {
     // this.label = label;
      return this;
    }

    public AlertFilterBuilder definitionUuid(@NonNull UUID uuid) {
      this.definitionUuids.add(uuid);
      return this;
    }

    public AlertFilterBuilder definitionUuids(Collection<UUID> definitionUuids) {
      this.definitionUuids = new HashSet<>(definitionUuids);
      return this;
    }

    public AlertFilterBuilder severity(@NonNull Severity... severities) {
      this.severities.addAll(Arrays.asList(severities));
      return this;
    }

    public AlertFilterBuilder severities(@NonNull Set<Severity> severities) {
      this.severities.addAll(severities);
      return this;
    }

    public AlertFilterBuilder configurationType(
        @NonNull AlertConfiguration.TargetType... configurationType) {
      this.configurationTypes.addAll(Arrays.asList(configurationType));
      return this;
    }

    public AlertFilterBuilder configurationTypes(
        @NonNull Set<AlertConfiguration.TargetType> configurationTypes) {
      this.configurationTypes.addAll(configurationTypes);
      return this;
    }

    public AlertFilterBuilder notificationPending(boolean notificationPending) {
      //this.notificationPending = notificationPending;
      return this;
    }

    public AlertFilterBuilder sourceName(@NonNull String sourceName) {
      //this.sourceName = sourceName;
      return this;
    }

    public AlertFilterBuilder resolvedDateBefore(@NonNull Date resolvedDateBefore) {
      //this.resolvedDateBefore = resolvedDateBefore;
      return this;
    }

    public AlertFilterBuilder sourceUUIDs(@NonNull Set<UUID> sourceUUIDs) {
      this.sourceUUIDs.addAll(sourceUUIDs);
      return this;
    }
  }
}
