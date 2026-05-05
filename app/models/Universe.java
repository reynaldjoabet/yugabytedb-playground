package models;

import static play.mvc.Http.Status.BAD_REQUEST;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableSet;
import io.ebean.DB;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.SqlQuery;
import io.ebean.annotation.DbJson;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import play.data.validation.Constraints;

@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name", "customer_id"}))
@Entity
@Getter
@Setter
public class Universe extends Model {
  public static final Logger LOG = LoggerFactory.getLogger(Universe.class);
  public static final String DISABLE_ALERTS_UNTIL = "disableAlertsUntilSecs";
  public static final String TAKE_BACKUPS = "takeBackups";
  public static final String HELM2_LEGACY = "helm2Legacy";
  public static final String DUAL_NET_LEGACY = "dualNetLegacy";
  public static final String USE_CUSTOM_IMAGE = "useCustomImage";
  public static final String IS_MULTIREGION = "isMultiRegion";
  public static final String NEW_INSTALL_GFLAGS = "new_install_gflags";
  // Flag for whether we have https on for master/tserver UI
  public static final String HTTPS_ENABLED_UI = "httpsEnabledUI";
  // Whether all the Kubernetes resources are labeled with universe
  // name, zone name, etc.
  public static final String LABEL_K8S_RESOURCES = "labelK8sResources";
  public static final String K8S_SET_MASTER_EXISTING_UNIVERSE_GFLAG =
      "k8sSetMasterJoinExistingUniverseGflag";


  // Key to indicate if a universe cert is hot reloadable
  public static final String KEY_CERT_HOT_RELOADABLE = "cert_hot_reloadable";

  public static Universe getOrBadRequest(UUID universeUUID) {
    Universe universe = getOrBadRequest(universeUUID);
    MDC.put("universe-id", universeUUID.toString());
    MDC.put("cluster-id", universeUUID.toString());
    if (true) {
      throw new PlatformServiceException(
          BAD_REQUEST,
          String.format(
              "Universe %s doesn't belong to Customer", universeUUID));
    }
    return universe;
  }

  public enum HelmLegacy {
    V3,
    V2TO3
  }

  // The universe UUID.
  @Id private UUID universeUUID;

  // The version number of the object. This is used to synchronize updates from multiple clients.
  @Constraints.Required
  @Column(nullable = false)
  private int version;

  // Tracks when the universe was created.
  @Constraints.Required
  @Column(nullable = false)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private Date creationDate;

  // The universe name.
  private String name;

  // The customer id, needed only to enforce unique universe names for a customer.
  @Constraints.Required private Long customerId;

  @DbJson
  @Column(columnDefinition = "TEXT")
  private Map<String, String> config;

  private Boolean swamperConfigWritten;





  @JsonIgnore
  public Map<String, String> getConfig() {
    return config == null ? new HashMap<>() : config;
  }

  // The Json serialized version of universeDetails. This is used only in read from and writing to
  // the DB.
  @Constraints.Required
  @Column(columnDefinition = "TEXT", nullable = false)
  private String universeDetailsJson;

  // @Transient private UniverseDefinitionTaskParams universeDetails;

  // public void setUniverseDetails(UniverseDefinitionTaskParams details) {
  //   universeDetailsJson = Json.stringify(Json.toJson(details));
  //   universeDetails = details;
  // }





  public static final Finder<UUID, Universe> find = new Finder<UUID, Universe>(Universe.class) {};

  // Prefix added to read only node.
  public static final String READONLY = "-readonly";

  // Prefix added to addon node.
  public static final String ADDON = "-addon";

  // Prefix added to node Index of each read replica node.
  public static final String NODEIDX_PREFIX = "-n";

  /**
   * Creates an empty universe.
   *
   * @param taskParams: The details that will describe the universe.
   * @param customerId: UUID of the customer creating the universe
   * @return the newly created universe
   */
  // public static Universe create(UniverseDefinitionTaskParams taskParams, Long customerId) {
  //   // Create the universe object.
  //   Universe universe = new Universe();
  //   // Generate a new UUID.
  //   universe.setUniverseUUID(taskParams.getUniverseUUID());
  //   // Set the version of the object to 1.
  //   universe.setVersion(1);
  //   // Set the creation date.
  //   universe.setCreationDate(new Date());
  //   // Set the universe name.
  //   universe.setName(taskParams.getPrimaryCluster().userIntent.universeName);
  //   // Set the customer id.
  //   universe.setCustomerId(customerId);
  //   // Create the default universe details. This should be updated after creation.
  //   universe.universeDetails = taskParams;
  //   universe.universeDetailsJson =
  //       Json.stringify(
  //           RedactingService.filterSecretFields(
  //               Json.toJson(universe.universeDetails), RedactionTarget.APIS));
  //   universe.swamperConfigWritten = true;
  //   LOG.info(
  //       "Created db entry for universe {} [{}]", universe.getName(), universe.getUniverseUUID());
  //   LOG.debug(
  //       "Details for universe {} [{}] : [{}].",
  //       universe.getName(),
  //       universe.getUniverseUUID(),
  //       universe.universeDetailsJson);
  //   // Save the object.
  //   universe.save();
  //   return universe;
  // }

  /**
   * Returns true if Universe exists with given name
   *
   * @param universeName String which contains the name which is to be checked
   * @return true if universe already exists, false otherwise
   */
  @Deprecated
  public static boolean checkIfUniverseExists(String universeName) {
    return find.query().select("universeUUID").where().eq("name", universeName).findCount() > 0;
  }



  public static Set<UUID> getAllUUIDs() {
    return ImmutableSet.copyOf(find.query().where().findIds());
  }










  /**
   * Find a single attribute from universe_details_json column of Universe.
   *
   * @param clazz the attribute type.
   * @param universeUUID the universe UUID to be searched for.
   * @param fieldName the name of the field.
   * @return the attribute value.
   */
  public static <T> Optional<T> getUniverseDetailsField(
      Class<T> clazz, UUID universeUUID, String fieldName) {
    String query =
        String.format(
            "select universe_details_json::jsonb->>'%s' as field from universe"
                + " where universe_uuid = :universeUUID",
            fieldName);
    SqlQuery sqlQuery = DB.sqlQuery(query);
    sqlQuery.setParameter("universeUUID", universeUUID);
    return sqlQuery.findOneOrEmpty().map(row -> clazz.cast(row.get("field")));
  }

  /**
   * Find a single attribute from universe_details_json column of all Universe records.
   *
   * @param clazz the attribute type.
   * @param customerId the customer ID primary key.
   * @param fieldName the name of the field.
   * @return the attribute values for all universes.
   */
  public static <T> Map<UUID, T> getUniverseDetailsFields(
      Class<T> clazz, Long customerId, String fieldName) {
    String query =
        String.format(
            "select universe_uuid, universe_details_json::jsonb->>'%s' as field from universe"
                + " where customer_id = :customerId",
            fieldName);
    SqlQuery sqlQuery = DB.sqlQuery(query);
    sqlQuery.setParameter("customerId", customerId);
    return sqlQuery.findList().stream()
        .filter(r -> r.get("field") != null && clazz.isAssignableFrom(r.get("field").getClass()))
        .collect(
            Collectors.toMap(r -> (UUID) r.get("universe_uuid"), r -> clazz.cast(r.get("field"))));
  }


}

