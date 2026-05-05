package models;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import play.libs.Json;
import models.UniverseDefinitionTaskParams.Cluster;
import models.UniverseDefinitionTaskParams.ClusterType;
import models.UniverseDefinitionTaskParams.Cluster;
import models.UniverseTaskBase.ServerType;
import models.TaskExecutor;

import static org.reflections.Reflections.log;

/**
 * Abstract base class for all tasks that create/edit the universe definition. These include the
 * create universe task and all forms of edit universe tasks. Note that the delete universe task
 * extends the UniverseTaskBase, as it does not depend on the universe definition.
 */
@Slf4j
public abstract class UniverseDefinitionTaskBase extends UniverseTaskBase {



  // Enum for specifying the universe operation type.
  public enum UniverseOpType {
    CREATE,
    EDIT
  }

  public enum PortType {
    HTTP,
    RPC
  }

  // Constants needed for parsing a templated node name tag (for AWS).
  public static final String NODE_NAME_KEY = "Name";
  public static final String TABLET_SERVERS_URL_FORMAT = "http://%s:%d/api/v1/tablet-servers";

  private static class TemplatedTags {

    private static final String DOLLAR = "$";
    private static final String LBRACE = "{";
    private static final String PREFIX = DOLLAR + LBRACE;
    private static final int PREFIX_LEN = PREFIX.length();
    private static final String SUFFIX = "}";
    private static final int SUFFIX_LEN = SUFFIX.length();
    private static final String UNIVERSE = PREFIX + "universe" + SUFFIX;
    private static final String INSTANCE_ID = PREFIX + "instance-id" + SUFFIX;
    private static final String ZONE = PREFIX + "zone" + SUFFIX;
    private static final String REGION = PREFIX + "region" + SUFFIX;
    private static final Set<String> RESERVED_TAGS =
        ImmutableSet.of(
            UNIVERSE.substring(PREFIX_LEN, UNIVERSE.length() - SUFFIX_LEN),
            ZONE.substring(PREFIX_LEN, ZONE.length() - SUFFIX_LEN),
            REGION.substring(PREFIX_LEN, REGION.length() - SUFFIX_LEN),
            INSTANCE_ID.substring(PREFIX_LEN, INSTANCE_ID.length() - SUFFIX_LEN));
  }





  

  // Check allowed patterns for tagValue.
  public static void checkTagPattern(String tagValue) {
    if (tagValue == null || tagValue.isEmpty()) {
      throw new IllegalArgumentException("Invalid value '" + tagValue + "' for " + NODE_NAME_KEY);
    }

    int numPrefix = StringUtils.countMatches(tagValue, TemplatedTags.PREFIX);
    int numSuffix = StringUtils.countMatches(tagValue, TemplatedTags.SUFFIX);
    if (numPrefix != numSuffix) {
      throw new IllegalArgumentException(
          "Number of '"
              + TemplatedTags.PREFIX
              + "' does not "
              + "match '"
              + TemplatedTags.SUFFIX
              + "' count in "
              + tagValue);
    }

    // Find all the content repeated within all the "{" and "}". These will be matched againt
    // supported keywords for tags.
    Pattern pattern =
        Pattern.compile(
            "\\"
                + TemplatedTags.DOLLAR
                + "\\"
                + TemplatedTags.LBRACE
                + "(.*?)\\"
                + TemplatedTags.SUFFIX);
    Matcher matcher = pattern.matcher(tagValue);
    Set<String> keys = new HashSet<String>();
    while (matcher.find()) {
      String match = matcher.group(1);
      if (keys.contains(match)) {
        throw new IllegalArgumentException("Duplicate " + match + " in " + NODE_NAME_KEY + " tag.");
      }
      if (!TemplatedTags.RESERVED_TAGS.contains(match)) {
        throw new IllegalArgumentException(
            "Invalid variable "
                + match
                + " in "
                + NODE_NAME_KEY
                + " tag. Should be one of "
                + TemplatedTags.RESERVED_TAGS);
      }
      keys.add(match);
    }
    log.trace("Found tags keys : {}", keys);

    if (!tagValue.contains(TemplatedTags.INSTANCE_ID)) {
      throw new IllegalArgumentException(
          "'"
              + TemplatedTags.INSTANCE_ID
              + "' should be part of "
              + NODE_NAME_KEY
              + " value "
              + tagValue);
    }
  }

  private static String getTagBasedName(
      String tagValue, Cluster cluster, int nodeIdx, String region, String az) {
    return tagValue
        .replace(TemplatedTags.UNIVERSE, cluster.userIntent.universeName)
        .replace(TemplatedTags.INSTANCE_ID, Integer.toString(nodeIdx))
        .replace(TemplatedTags.ZONE, az)
        .replace(TemplatedTags.REGION, region);
  }

  /**
   * Method to derive the expected node name from the input parameters.
   *
   * @param cluster The cluster containing the node.
   * @param tagValue Templated name tag to use to derive the final node name.
   * @param prefix Name prefix if not templated.
   * @param nodeIdx index to be used in node name.
   * @param region region in which this node is present.
   * @param az zone in which this node is present.
   * @return a string which can be used as the node name.
   */
  public static String getNodeName(
      Cluster cluster, String tagValue, String prefix, int nodeIdx, String region, String az) {
    if (!tagValue.isEmpty()) {
      checkTagPattern(tagValue);
    }

    String newName = "";
    if (cluster.clusterType == ClusterType.ASYNC || cluster.clusterType == ClusterType.ADDON) {
      String discriminator;
      switch (cluster.clusterType) {
        case ASYNC:
          discriminator = Universe.READONLY;
          break;
        case ADDON:
          discriminator = Universe.ADDON;
          break;
        default:
          throw new IllegalArgumentException("Invalid cluster type " + cluster.clusterType);
      }

      if (tagValue.isEmpty()) {
        newName = prefix + discriminator + cluster.index + Universe.NODEIDX_PREFIX + nodeIdx;
      } else {
        newName =
            getTagBasedName(tagValue, cluster, nodeIdx, region, az) + discriminator + cluster.index;
      }
    } else {
      if (tagValue.isEmpty()) {
        newName = prefix + Universe.NODEIDX_PREFIX + nodeIdx;
      } else {
        newName = getTagBasedName(tagValue, cluster, nodeIdx, region, az);
      }
    }

    log.info("Node name " + newName + " at index " + nodeIdx);

    return newName;
  }

  
}