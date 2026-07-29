name := """yugabytedb-playground"""

version := "1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.8.4"

ThisBuild / scalacOptions := Seq(
  "-no-indent",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-source:3.3",
  "-java-output-version:17",
  "-Wvalue-discard",
  "-Wshadow:all",
  "-Wsafe-init",
  "-Xcheck-macros",
  "-Xmax-inlines:64"
)

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(
    libraryDependencies ++= Seq(
      javaForms,
      jodaForms,
      guice,
      javaWs,
      javaJdbc,
      javaCore,
      javaClusterSharding,
      ehcache,
      "org.postgresql" % "postgresql" % "42.7.11",
      "net.logstash.logback" % "logstash-logback-encoder" % "9.0",
      "ch.qos.logback" % "logback-classic" % "1.5.32",
      "org.codehaus.janino" % "janino" % "3.1.12",
      "org.apache.commons" % "commons-lang3" % "3.20.0",
      "org.apache.commons" % "commons-collections4" % "4.5.0",
      "org.apache.commons" % "commons-compress" % "1.28.0",
      "org.apache.commons" % "commons-csv" % "1.14.1",
      "org.apache.httpcomponents.core5" % "httpcore5" % "5.4.2",
      "org.apache.httpcomponents.core5" % "httpcore5-h2" % "5.4.2",
      "org.apache.httpcomponents.client5" % "httpclient5" % "5.6.1",
      "org.apache.mina" % "mina-core" % "2.2.7",
      "org.flywaydb" %% "flyway-play" % "9.1.0",
      // https://github.com/YugaByte/cassandra-java-driver/releases
      "com.yugabyte" % "cassandra-driver-core" % "3.10.3-yb-3",
      "org.yaml" % "snakeyaml" % "2.6",
      "org.bouncycastle" % "bc-fips" % "2.1.2",
      "org.bouncycastle" % "bcpkix-fips" % "2.1.11",
      "org.bouncycastle" % "bctls-fips" % "2.1.23",
      "org.bouncycastle" % "bcpkix-jdk18on" % "1.84",
      "org.bouncycastle" % "bcprov-jdk18on" % "1.84",
      "org.mindrot" % "jbcrypt" % "0.4",
      "org.springframework.security" % "spring-security-core" % "6.5.0",
      "com.amazonaws" % "aws-java-sdk-ec2" % "1.12.785",
      "com.amazonaws" % "aws-java-sdk-kms" % "1.12.785",
      "com.amazonaws" % "aws-java-sdk-iam" % "1.12.785",
      "com.amazonaws" % "aws-java-sdk-sts" % "1.12.785",
      "com.amazonaws" % "aws-java-sdk-s3" % "1.12.785",
      "com.amazonaws" % "aws-java-sdk-elasticloadbalancingv2" % "1.12.785",
      "com.amazonaws" % "aws-java-sdk-route53" % "1.12.785",
      "com.amazonaws" % "aws-java-sdk-cloudtrail" % "1.12.785",
      "net.minidev" % "json-smart" % "2.5.2",
      "com.cronutils" % "cron-utils" % "9.2.1",
      // Be careful when changing azure library versions.
      // Make sure all itests and existing functionality works as expected.
      // Used below azure versions from azure-sdk-bom:1.2.6
      "com.azure" % "azure-core" % "1.55.3",
      "com.azure" % "azure-identity" % "1.16.1",
      "com.azure" % "azure-security-keyvault-keys" % "4.9.4",
      "com.azure" % "azure-storage-blob" % "12.30.0",
      "com.azure" % "azure-storage-blob-batch" % "12.26.0",
      "com.azure.resourcemanager" % "azure-resourcemanager" % "2.51.0",
      "com.azure.resourcemanager" % "azure-resourcemanager-marketplaceordering" % "1.0.0",
      "jakarta.mail" % "jakarta.mail-api" % "2.1.3",
      "org.eclipse.angus" % "jakarta.mail" % "2.0.3",
      "javax.validation" % "validation-api" % "2.0.1.Final",
      "io.prometheus" % "simpleclient" % "0.16.0",
      "io.prometheus" % "simpleclient_hotspot" % "0.16.0",
      "io.prometheus" % "simpleclient_servlet" % "0.16.0",
      "org.glassfish.jaxb" % "jaxb-runtime" % "4.0.5",
      // pac4j and nimbusds libraries need to be upgraded together.
      //   "org.pac4j" %% "play-pac4j" % "11.0.0-PLAY2.8",
      //   "org.pac4j" % "pac4j-oauth" % "5.7.7" exclude ("commons-io", "commons-io"),
      //   "org.pac4j" % "pac4j-oidc" % "5.7.7" exclude ("commons-io", "commons-io"),
      "com.nimbusds" % "nimbus-jose-jwt" % "10.3",
      "com.nimbusds" % "oauth2-oidc-sdk" % "11.25",
      "org.playframework" %% "play-json" % "3.0.6",
      "commons-validator" % "commons-validator" % "1.9.0",
      "org.apache.velocity" % "velocity-engine-core" % "2.4.1",
      "com.fasterxml.woodstox" % "woodstox-core" % "7.1.1",
      "com.jayway.jsonpath" % "json-path" % "2.9.0",
      "commons-io" % "commons-io" % "2.19.0",
      "commons-codec" % "commons-codec" % "1.18.0",
      "com.google.apis" % "google-api-services-compute" % "v1-rev20250415-2.0.0",
      "com.google.apis" % "google-api-services-iam" % "v2-rev20250502-2.0.0",
      "com.google.cloud" % "google-cloud-compute" % "1.73.0",
      "com.google.cloud" % "google-cloud-storage" % "2.52.3",
      "com.google.cloud" % "google-cloud-kms" % "2.66.0",
      "com.google.cloud" % "google-cloud-resourcemanager" % "1.65.0",
      "com.google.cloud" % "google-cloud-logging" % "3.22.4",
      "com.google.oauth-client" % "google-oauth-client" % "1.39.0",
      "org.projectlombok" % "lombok" % "1.18.38",
      "com.squareup.okhttp3" % "okhttp" % "4.12.0",
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.19.0",
      "com.google.protobuf" % "protobuf-java-util" % "4.31.1",
      "io.kamon" %% "kamon-bundle" % "2.7.7",
      "io.kamon" %% "kamon-prometheus" % "2.7.7",
      "org.unix4j" % "unix4j-command" % "0.6",
      "com.bettercloud" % "vault-java-driver" % "5.1.0",
      "org.apache.directory.api" % "api-all" % "2.1.7",
      "io.fabric8" % "crd-generator-apt" % "7.3.1",
      "io.fabric8" % "kubernetes-client" % "7.3.1",
      "io.fabric8" % "kubernetes-client-api" % "7.3.1",
      "io.fabric8" % "kubernetes-model" % "6.13.5",
      "org.modelmapper" % "modelmapper" % "3.2.3",
      "com.datadoghq" % "datadog-api-client" % "2.35.0" classifier "shaded-jar",
      "javax.xml.bind" % "jaxb-api" % "2.3.1",
      "io.jsonwebtoken" % "jjwt-api" % "0.12.6",
      "io.jsonwebtoken" % "jjwt-impl" % "0.12.6",
      "io.jsonwebtoken" % "jjwt-jackson" % "0.12.6",
      "io.swagger" % "swagger-annotations" % "1.6.16", // needed for annotations in prod code
      "de.dentrassi.crypto" % "pem-keystore" % "3.0.0",
      "org.playframework" %% "play-ebean" % "8.5.0",
      "jakarta.validation" % "jakarta.validation-api" % "3.1.1",
      "qa.hedgehog" %% "hedgehog-sbt" % "0.13.0" % Test,
      // https://hedgehog.qa/
      "qa.hedgehog" %% "hedgehog-core" % "0.13.0" % Test
    )
  )

val jacksonVersion = "2.31.3"

val jacksonLibs = Seq(
  "com.fasterxml.jackson.core" % "jackson-core",
  "com.fasterxml.jackson.core" % "jackson-annotations",
  "com.fasterxml.jackson.core" % "jackson-databind",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml",
  "com.fasterxml.jackson.module" % "jackson-module-parameter-names",
  "com.fasterxml.jackson.module" %% "jackson-module-scala"
)

val jacksonOverrides = jacksonLibs.map(_ % jacksonVersion)

addCommandAlias("fmt", "scalafmtAll; scalafmtSbt")
addCommandAlias("fmtCheck", "scalafmtCheckAll; scalafmtSbtCheck")

Test / parallelExecution := true

ThisBuild / outputStrategy := Some(StdoutOutput)
