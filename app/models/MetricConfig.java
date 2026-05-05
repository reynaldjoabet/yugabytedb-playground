package models;

import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;
import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_WRITE;

import com.fasterxml.jackson.databind.JsonNode;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.DbJson;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import play.libs.Json;

@ApiModel(description = "Metric configuration key and value for Prometheus")
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class MetricConfig extends Model {

  public static final String METRICS_CONFIG_PATH = "metric/metrics.yml";

  @ApiModelProperty(value = "Metrics configuration key", accessMode = READ_ONLY)
  @Id
  @Column(name = "config_key", length = 100)
  private String key;


  public static final Finder<String, MetricConfig> find =
      new Finder<String, MetricConfig>(MetricConfig.class) {};

  /**
   * returns metric config for the given key
   *
   * @param configKey
   * @return MetricConfig
   */
  public static MetricConfig get(String configKey) {
    return MetricConfig.find.byId(configKey);
  }


  
}
