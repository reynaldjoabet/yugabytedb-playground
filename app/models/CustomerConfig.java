package models;
import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;
import static play.mvc.Http.Status.BAD_REQUEST;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.Encrypted;
import io.ebean.annotation.EnumValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import play.libs.Json;

@Entity
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(
    description =
        "Customer configuration. Includes storage, alerts, password policy, and call-home level.")
public class CustomerConfig extends Model {
  public static final Logger LOG = LoggerFactory.getLogger(CustomerConfig.class);
  public static final String ALERTS_PREFERENCES = "preferences";
  public static final String SMTP_INFO = "smtp info";
  public static final String PASSWORD_POLICY = "password policy";
  public static final String CALLHOME_PREFERENCES = "callhome level";

  public enum ConfigType {
    @EnumValue("STORAGE")
    STORAGE,

    @EnumValue("ALERTS")
    ALERTS,

    @EnumValue("CALLHOME")
    CALLHOME,

    @EnumValue("PASSWORD_POLICY")
    PASSWORD_POLICY;

    public static boolean isValid(String type) {
      for (ConfigType t : ConfigType.values()) {
        if (t.name().equals(type)) {
          return true;
        }
      }

      return false;
    }
  }

  public enum ConfigState {
    @EnumValue("Active")
    Active,

    @EnumValue("QueuedForDeletion")
    QueuedForDeletion
  }

  @Id
  @ApiModelProperty(value = "Config UUID", accessMode = READ_ONLY)
  private UUID configUUID;

  @NotNull
  @Size(min = 1, max = 100)
  @Column(length = 100, nullable = true)
  @ApiModelProperty(value = "Config name", example = "backup20-01-2021")
  private String configName;

  public CustomerConfig setConfigName(String configName) {
    this.configName = configName.trim();
    return this;
  }

  @NotNull
  @Column(nullable = false)
  @ApiModelProperty(value = "Customer UUID", accessMode = READ_ONLY)
  private UUID customerUUID;

  @NotNull
  @Column(length = 25, nullable = false)
  @ApiModelProperty(value = "Config type", example = "STORAGE")
  private ConfigType type;

  @NotNull
  @Size(min = 1, max = 50)
  @Column(length = 100, nullable = false)
  @ApiModelProperty(value = "Name", example = "S3")
  private String name;

  @NotNull
  @Column(nullable = false, columnDefinition = "TEXT")
  @DbJson
  @Encrypted
  @ApiModelProperty(
          value = "Configuration data",
          required = true,
          dataType = "Object",
          example = "{\"AWS_ACCESS_KEY_ID\": \"AK****************ZD\"}")
  private ObjectNode data;

  @ApiModelProperty(
          value = "state of the customerConfig. Possible values are Active, QueuedForDeletion.",
          accessMode = READ_ONLY)
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private ConfigState state = ConfigState.Active;

  public static final Finder<UUID, CustomerConfig> find =
          new Finder<UUID, CustomerConfig>(CustomerConfig.class) {
          };


  public static List<CustomerConfig> getAll(UUID customerUUID) {
    return CustomerConfig.find.query().where().eq("customer_uuid", customerUUID).findList();
  }

  public static CustomerConfig get(UUID customerUUID, UUID configUUID) {
    return CustomerConfig.find
            .query()
            .where()
            .eq("customer_uuid", customerUUID)
            .idEq(configUUID)
            .findOne();
  }

  public static CustomerConfig get(UUID configUUID) {
    return CustomerConfig.find.query().where().idEq(configUUID).findOne();
  }

  public static CustomerConfig get(UUID customerUUID, String configName) {
    return CustomerConfig.find
            .query()
            .where()
            .eq("customer_uuid", customerUUID)
            .eq("config_name", configName)
            .findOne();
  }


  public static CustomerConfig getAlertConfig(UUID customerUUID) {
    return getConfig(customerUUID, ConfigType.ALERTS, ALERTS_PREFERENCES);
  }

  public static List<CustomerConfig> getAlertConfigs(Collection<UUID> customerUUIDs) {
    return getConfigs(customerUUIDs, ConfigType.ALERTS, ALERTS_PREFERENCES);
  }

  public static CustomerConfig getSmtpConfig(UUID customerUUID) {
    return getConfig(customerUUID, ConfigType.ALERTS, SMTP_INFO);
  }

  public static CustomerConfig getPasswordPolicyConfig(UUID customerUUID) {
    return getConfig(customerUUID, ConfigType.PASSWORD_POLICY, PASSWORD_POLICY);
  }

  public static CustomerConfig getConfig(UUID customerUUID, ConfigType type, String name) {
    return CustomerConfig.find
            .query()
            .where()
            .eq("customer_uuid", customerUUID)
            .eq("type", type.toString())
            .eq("name", name)
            .findOne();
  }

  public static List<CustomerConfig> getConfigs(
          Collection<UUID> customerUUIDs, ConfigType type, String name) {
    if (CollectionUtils.isEmpty(customerUUIDs)) {
      return Collections.emptyList();
    }
    return CustomerConfig.find
            .query()
            .where()
            .in("customer_uuid", customerUUIDs)
            .eq("type", type.toString())
            .eq("name", name)
            .findList();
  }

  public static List<CustomerConfig> getAllStorageConfigsQueuedForDeletion(UUID customerUUID) {
    List<CustomerConfig> configList =
            CustomerConfig.find
                    .query()
                    .where()
                    .eq("customer_uuid", customerUUID)
                    .eq("type", ConfigType.STORAGE)
                    .eq("state", ConfigState.QueuedForDeletion)
                    .findList();
    return configList;
  }


  public static CustomerConfig getCallhomeConfig(UUID customerUUID) {
    return getConfig(customerUUID, ConfigType.CALLHOME, CALLHOME_PREFERENCES);
  }


  public void updateState(ConfigState newState) {
    if (this.state == newState) {
      LOG.debug("Invalid State transition as no change requested");
      return;
    }
    if (this.state == ConfigState.QueuedForDeletion) {
      LOG.debug("Invalid State transition {} to {}", this.state, newState);
      return;
    }
    LOG.info("Customer config: transitioned from {} to {}", this.state, newState);
    this.state = newState;
    this.save();
  }


}
