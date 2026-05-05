package models;

import io.ebean.annotation.EnumValue;

/*
 * various certificate configuration types
 */
public enum CertConfigType {
  @EnumValue("SelfSigned")
  SelfSigned,

  @EnumValue("CustomCertHostPath")
  CustomCertHostPath,

  @EnumValue("CustomServerCert")
  CustomServerCert,

  @EnumValue("HashicorpVault")
  HashicorpVault,

  @EnumValue("K8SCertManager")
  K8SCertManager
}
