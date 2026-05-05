package models;
import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;
import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_WRITE;
import static play.mvc.Http.Status.BAD_REQUEST;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import io.ebean.ExpressionList;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.Encrypted;
import io.ebean.annotation.Where;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.validation.Constraints;
import models.UniverseDetails.InstanceType;

@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"customer_uuid", "name", "code"}))
@Entity
@Getter
@Setter
public class Provider extends Model {
  public static final Logger LOG = LoggerFactory.getLogger(Provider.class);
  private static final String TRANSIENT_PROPERTY_IN_MUTATE_API_REQUEST =
          "Transient property - only present in create provider API request";

  @ApiModelProperty(value = "Provider uuid", accessMode = READ_ONLY)
  @Id
  private UUID uuid;

  // TODO: Use Enum
  @Column(nullable = false)
  @ApiModelProperty(value = "Provider cloud code", accessMode = READ_WRITE)
  @Constraints.Required()
  private String code;



  @Column(nullable = false)
  @ApiModelProperty(value = "Provider name", accessMode = READ_WRITE)
  @Constraints.Required()
  private String name;

  @Column(nullable = false, columnDefinition = "boolean default true")
  @ApiModelProperty(value = "Provider active status", accessMode = READ_ONLY)
  private Boolean active = true;

  @Column(name = "customer_uuid", nullable = false)
  @ApiModelProperty(value = "Customer uuid", accessMode = READ_ONLY)
  private UUID customerUUID;

  public static final Set<CloudType> InstanceTagsEnabledProviders =
          ImmutableSet.of(
                  CloudType.aws, CloudType.azu, CloudType.gcp, CloudType.local);
  public static final Set<CloudType> InstanceTagsModificationEnabledProviders =
          ImmutableSet.of(CloudType.aws, CloudType.gcp, CloudType.local);


  @ApiModelProperty(
          value =
                  "<b style=\"color:#ff0000\">Deprecated since YBA version 2.17.2.0.</b> Use"
                          + " details.metadata instead")
  @Column(nullable = false, columnDefinition = "TEXT")
  @DbJson
  @Encrypted
  private Map<String, String> config;

  @Column(nullable = false, columnDefinition = "TEXT")
  @DbJson
  @Encrypted
  private ProviderDetails details = new ProviderDetails();

  @OneToMany(cascade = CascadeType.ALL)
  @Where(clause = "t0.active = true")
  @JsonManagedReference(value = "provider-regions")
  private List<Region> regions;

  @OneToMany(cascade = CascadeType.ALL)
  @JsonManagedReference(value = "provider-image-bundles")
  private List<ImageBundle> imageBundles;

  @ApiModelProperty(required = false)
  @OneToMany(cascade = CascadeType.ALL)
  @JsonManagedReference(value = "provider-accessKey")
  private List<AccessKey> allAccessKeys;

  @JsonIgnore
  @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL)
  private Set<InstanceType> instanceTypes;

  @JsonIgnore
  @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL)
  private Set<PriceComponent> priceComponents;

  // Start Transient Properties
  // TODO: These are all transient fields for now. At present these are stored
  //  with CloudBootstrap params. We should move them to Provider and persist with
  //  Provider entity.

  // Custom keypair name to use when spinning up YB nodes.
  // Default: created and managed by YB.

}