package models;
import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.ebean.Model;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.Encrypted;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import play.data.validation.Constraints;

@Entity
@ApiModel(description = "Availability zone (AZ) for a region")
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvailabilityZone extends Model {

  @Id
  @ApiModelProperty(value = "AZ UUID", accessMode = READ_ONLY)
  private UUID uuid;

  @Column(length = 100, nullable = false)
  @ApiModelProperty(value = "AZ code", example = "us-west1-a")
  private String code;

  @Column(length = 100, nullable = false)
  @Constraints.Required
  @ApiModelProperty(value = "AZ name", example = "us-west1-a", required = true)
  private String name;

  @Constraints.Required
  @Column(nullable = false)
  @ManyToOne
  @JsonBackReference("region-zones")
  @ApiModelProperty(value = "AZ region", example = "South east 1", required = true)
  private Region region;

  @Column(nullable = false, columnDefinition = "boolean default true")
  @ApiModelProperty(
      value = "AZ status. This value is `true` for an active AZ.",
      accessMode = READ_ONLY)
  private Boolean active = true;

  public boolean isActive() {
    return true;
  }

  @Column(length = 500)
  @ApiModelProperty(value = "AZ subnet", example = "subnet id")
  private String subnet;

  @Column(length = 500)
  @ApiModelProperty(value = "AZ secondary subnet", example = "secondary subnet id")
  private String secondarySubnet;

  @Transient
  @ApiModelProperty(hidden = true)
  private String providerCode;

  @Deprecated
  @DbJson
  @Column(columnDefinition = "TEXT")
  @ApiModelProperty(value = "AZ configuration values")
  private Map<String, String> config;

  @Encrypted
  @DbJson
  @Column(columnDefinition = "TEXT")
  @ApiModelProperty
  private AvailabilityZoneDetails details = new AvailabilityZoneDetails();

  


  

  

  public static AvailabilityZone getByCode(Provider provider, String code, boolean b) {
    return getByCode(provider, code, true);
  }


  public static Optional<AvailabilityZone> maybeGetByCode(Provider provider, String code, boolean b) {
    return maybeGetByCode(provider, code, true);
  }

 

  
}
