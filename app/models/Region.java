
package models;

import static io.ebean.DB.beginTransaction;
//import static io.ebean.DB.commitTransaction;
//import static io.ebean.DB.endTransaction;
import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;
import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_WRITE;
import static play.mvc.Http.Status.BAD_REQUEST;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import io.ebean.DB;
import io.ebean.ExpressionList;
import io.ebean.Finder;
import io.ebean.Junction;
import io.ebean.Model;
import io.ebean.Query;
import io.ebean.RawSql;
import io.ebean.RawSqlBuilder;
import io.ebean.SqlUpdate;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.Encrypted;
import io.ebean.annotation.Where;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import play.data.validation.Constraints;
import play.libs.Json;

@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(
    description =
        "Region within a given provider. Typically, this maps to a "
            + "single cloud provider region.")
@Getter
@Setter
public class Region extends Model {

  @Id
  @ApiModelProperty(value = "Region UUID", accessMode = READ_ONLY)
  private UUID uuid;

  @Column(length = 25, nullable = false)
  @ApiModelProperty(
      value = "Cloud provider region code",
      example = "us-west-2",
      accessMode = READ_WRITE)
  private String code;

  @Column(length = 100, nullable = false)
  @ApiModelProperty(
      value = "Cloud provider region name",
      example = "US West (Oregon)",
      accessMode = READ_ONLY)
  private String name;

  @ApiModelProperty(
      value =
          "<b style=\"color:#ff0000\">Deprecated since YBA version 2.17.2.0.</b> "
              + "Moved to details.cloudInfo aws/gcp/azure ybImage property",
      example = "TODO",
      accessMode = READ_WRITE)
  private String ybImage;

  @Column(columnDefinition = "float")
  @ApiModelProperty(value = "The region's longitude", example = "-120.01", accessMode = READ_ONLY)
  @Constraints.Min(-180)
  @Constraints.Max(180)
  private double longitude = 0.0;

  @Column(columnDefinition = "float")
  @ApiModelProperty(value = "The region's latitude", example = "37.22", accessMode = READ_ONLY)
  @Constraints.Min(-90)
  @Constraints.Max(90)
  private double latitude = 0.0;

  @Column(nullable = false)
  @ManyToOne
  @JsonBackReference("provider-regions")
  private Provider provider;

  @OneToMany(cascade = CascadeType.ALL)
  @Where(clause = "t0.active = true")
  @JsonManagedReference("region-zones")
  private List<AvailabilityZone> zones;

  @ApiModelProperty(accessMode = READ_ONLY)
  @Column(nullable = false, columnDefinition = "boolean default true")
  private Boolean active = true;



  @Transient
  @ApiModelProperty(hidden = true)
  private String providerCode;

  @Encrypted
  @DbJson
  @Column(columnDefinition = "TEXT")
  @ApiModelProperty
  private RegionDetails details = new RegionDetails();

}
