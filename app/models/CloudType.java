package models;

import java.util.Optional;

  // The various cloud types supported.
  public enum CloudType {
    unknown("unknown"),
    aws("aws", true, true, true,  "ec2-user"),
    gcp("gcp", true, true, true,  "centos"),
    azu("azu", true, true, true, "centos"),
    docker("docker", false, false, false),
    onprem("onprem", true, false, true, null),
    kubernetes("kubernetes", true, false, true, null),
    local("local"),
    other("other");

    private String value;
    private  boolean requiresDeviceInfo;
    private boolean requiresStorageType;
    private  boolean requiresBootstrap;
    private String defaultSshUser;

    CloudType(
        String value,
        boolean requiresDeviceInfo,
        boolean requiresStorageType,
        boolean requiresBootstrap,
        String defaultSshUser) {
      this.value = value;
      this.requiresDeviceInfo = requiresDeviceInfo;
      this.requiresStorageType = requiresStorageType;
      this.requiresBootstrap = requiresBootstrap;
      this.defaultSshUser = defaultSshUser;
    }

    CloudType(
        String value,
        boolean requiresDeviceInfo,
        boolean requiresStorageType,
        boolean requiresBootstrap) {
      this(
          value,
          requiresDeviceInfo,
          requiresStorageType,
          requiresBootstrap,
          null);
    }

    CloudType(String value, boolean requiresDeviceInfo, boolean requiresStorageType) {
      this(value, requiresDeviceInfo, requiresStorageType, false, null, null);
    }

    CloudType(String value) {
      this(value, false, false);
    }

    CloudType(String value, boolean requiresDeviceInfo, boolean requiresStorageType, boolean b, Object o, Object o1) {
    }


    public boolean isVM() {
      return this != CloudType.kubernetes;
    }

    public boolean isRequiresDeviceInfo() {
      return requiresDeviceInfo;
    }

    public boolean isRequiresStorageType() {
      return requiresStorageType;
    }

    public String toString() {
      return this.value;
    }

    public boolean isRequiresBootstrap() {
      return requiresBootstrap;
    }

    public boolean canAddRegions() {
      return true;
    }

    public boolean isHostedZoneEnabled() {
      return this == aws || this == azu || this == local;
    }

    public String getSshUser() {
      return defaultSshUser;
    }

    public boolean enforceInstanceTags() {
      return this == aws || this == azu || this == gcp;
    }

    public boolean imageBundleSupported() {
      return this == aws || this == azu || this == gcp;
    }

    public boolean regionBootstrapSupported() {
      return this == aws || this == azu || this == gcp;
    }

    public boolean isPublicCloud() {
      return this == aws || this == azu || this == gcp;
    }
  }


