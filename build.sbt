name := """yugabytedb-playground"""

ThisBuild / scalaVersion := "3.9.0"
ThisBuild / version      := "0.1.0-SNAPSHOT"

// Scala 3.3.x cannot be a cross target while Play 3.1.x is in the graph: its artifacts are built
// with Scala 3.8.3, and TASTy is forward-incompatible, so a 3.3 compiler cannot read them.
ThisBuild / crossScalaVersions := Seq("3.9.0")

ThisBuild / scalacOptions := Seq(
  "-encoding",
  "UTF-8",
  "-no-indent",
  "-deprecation",
  "-feature",
  "-unchecked",
  // "-Werror",
  // "-Wunused:all",
  "-Wvalue-discard",
  "-Wnonunit-statement",
  "-language:strictEquality",
  "-Xcheck-macros",
  "-Xmax-inlines:64"
)

// JDK 23 stopped running annotation processors found on the classpath unless asked, so Lombok
// silently no-ops and every generated getter and @Slf4j `log` disappears. The CI matrix is Java
// 21/25, and -proc:full exists from 21 on, so this needs no version guard.
ThisBuild / javacOptions ++= Seq("-proc:full")

Global / onChangedBuildSource := ReloadOnSourceChanges

// Pekko's serialization brings an older jackson-module-scala, which refuses to load next to a
// newer databind ("requires Jackson Databind >= x and < x+1"). Pin the whole family to one version.
val jacksonVersion = "2.19.4"

// Jackson 3 moved to the tools.jackson.* org. It coexists with the Jackson 2 family above, which
// stays pinned because Play and Pekko are still Jackson 2 libraries.
val jackson3Version = "3.2.2"

val jacksonLibs = Seq(
  "com.fasterxml.jackson.core"       % "jackson-core",
  "com.fasterxml.jackson.core"       % "jackson-annotations",
  "com.fasterxml.jackson.core"       % "jackson-databind",
  "com.fasterxml.jackson.datatype"   % "jackson-datatype-jdk8",
  "com.fasterxml.jackson.datatype"   % "jackson-datatype-jsr310",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml",
  "com.fasterxml.jackson.module"     % "jackson-module-parameter-names",
  "com.fasterxml.jackson.module"    %% "jackson-module-scala"
)

val jacksonOverrides = jacksonLibs.map(_ % jacksonVersion)

// play-ebean 8.5.0 asks for play-jdbc-evolutions 3.0.9, and nothing else in the graph requests
// evolutions at all, so there is no conflict to evict it and it resolves alone at 3.0.9 -- a build
// from before Play moved to jakarta.inject. Guice 7 dropped the javax.inject bridge, so that module
// dies at load time with NoClassDefFoundError: javax/inject/Provider. Pin it to the Play in use.
val playOverrides = Seq(
  "org.playframework" %% "play-jdbc-evolutions" % play.core.PlayVersion.current
)

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(
    dependencyOverrides ++= jacksonOverrides ++ playOverrides,
    libraryDependencies ++= Seq(
      javaForms,
      jodaForms,
      guice,
      javaWs,
      javaJdbc,
      javaCore,
      javaClusterSharding,
      ehcache,
      "org.postgresql"                    % "postgresql"               % "42.7.13",
      "net.logstash.logback"              % "logstash-logback-encoder" % "9.0",
      "ch.qos.logback"                    % "logback-classic"          % "1.6.4",
      "org.codehaus.janino"               % "janino"                   % "3.1.12",
      "org.apache.commons"                % "commons-lang3"            % "3.20.0",
      "org.apache.commons"                % "commons-collections4"     % "4.6.0",
      "org.apache.commons"                % "commons-compress"         % "1.28.0",
      "org.apache.commons"                % "commons-csv"              % "1.14.1",
      "org.apache.httpcomponents.core5"   % "httpcore5"                % "5.4.4",
      "org.apache.httpcomponents.core5"   % "httpcore5-h2"             % "5.4.4",
      "org.apache.httpcomponents.client5" % "httpclient5"              % "5.6.4",
      "org.apache.mina"                   % "mina-core"                % "2.2.9",
      "org.flywaydb"                     %% "flyway-play"              % "9.1.0",
      // https://github.com/YugaByte/cassandra-java-driver/releases
      "com.yugabyte"                 % "cassandra-driver-core"               % "3.10.3-yb-3",
      "org.yaml"                     % "snakeyaml"                           % "2.7",
      "org.bouncycastle"             % "bc-fips"                             % "2.1.3",
      "org.bouncycastle"             % "bcpkix-fips"                         % "2.1.13",
      "org.bouncycastle"             % "bctls-fips"                          % "2.1.25",
      "org.bouncycastle"             % "bcpkix-jdk18on"                      % "1.86",
      "org.bouncycastle"             % "bcprov-jdk18on"                      % "1.86",
      "org.mindrot"                  % "jbcrypt"                             % "0.4",
      "org.springframework.security" % "spring-security-core"                % "7.1.1",
      "com.amazonaws"                % "aws-java-sdk-ec2"                    % "1.12.797",
      "com.amazonaws"                % "aws-java-sdk-kms"                    % "1.12.797",
      "com.amazonaws"                % "aws-java-sdk-iam"                    % "1.12.797",
      "com.amazonaws"                % "aws-java-sdk-sts"                    % "1.12.797",
      "com.amazonaws"                % "aws-java-sdk-s3"                     % "1.12.797",
      "com.amazonaws"                % "aws-java-sdk-elasticloadbalancingv2" % "1.12.797",
      "com.amazonaws"                % "aws-java-sdk-route53"                % "1.12.797",
      "com.amazonaws"                % "aws-java-sdk-cloudtrail"             % "1.12.797",
      "net.minidev"                  % "json-smart"                          % "2.6.0",
      "com.cronutils"                % "cron-utils"                          % "9.2.1",
      // Be careful when changing azure library versions.
      // Make sure all itests and existing functionality works as expected.
      // Used below azure versions from azure-sdk-bom:1.2.6
      "com.azure"                 % "azure-core"                                % "1.59.1",
      "com.azure"                 % "azure-identity"                            % "1.18.6",
      "com.azure"                 % "azure-security-keyvault-keys"              % "4.11.2",
      "com.azure"                 % "azure-storage-blob"                        % "12.35.1",
      "com.azure"                 % "azure-storage-blob-batch"                  % "12.31.2",
      "com.azure.resourcemanager" % "azure-resourcemanager"                     % "2.64.0",
      "com.azure.resourcemanager" % "azure-resourcemanager-marketplaceordering" % "1.0.0",
      "jakarta.mail"              % "jakarta.mail-api"                          % "2.1.5",
      "org.eclipse.angus"         % "jakarta.mail"                              % "2.0.5",
      "javax.validation"          % "validation-api"                            % "2.0.1.Final",
      "io.prometheus"             % "simpleclient"                              % "0.16.0",
      "io.prometheus"             % "simpleclient_hotspot"                      % "0.16.0",
      "io.prometheus"             % "simpleclient_servlet"                      % "0.16.0",
      "org.glassfish.jaxb"        % "jaxb-runtime"                              % "4.0.9",
      // pac4j and nimbusds libraries need to be upgraded together.
      //   "org.pac4j" %% "play-pac4j" % "11.0.0-PLAY2.8",
      //   "org.pac4j" % "pac4j-oauth" % "5.7.7" exclude ("commons-io", "commons-io"),
      //   "org.pac4j" % "pac4j-oidc" % "5.7.7" exclude ("commons-io", "commons-io"),
      "com.nimbusds"             % "nimbus-jose-jwt"              % "10.10",
      "com.nimbusds"             % "oauth2-oidc-sdk"              % "11.38.2",
      "org.playframework"       %% "play-json"                    % "3.0.6",
      "commons-validator"        % "commons-validator"            % "1.11.0",
      "org.apache.velocity"      % "velocity-engine-core"         % "2.4.1",
      "com.fasterxml.woodstox"   % "woodstox-core"                % "7.2.2",
      "com.jayway.jsonpath"      % "json-path"                    % "3.0.0",
      "commons-io"               % "commons-io"                   % "2.22.0",
      "commons-codec"            % "commons-codec"                % "1.22.1",
      "com.google.apis"          % "google-api-services-compute"  % "v1-rev20260908-2.0.0",
      "com.google.apis"          % "google-api-services-iam"      % "v2-rev20250502-2.0.0",
      "com.google.cloud"         % "google-cloud-compute"         % "1.108.0",
      "com.google.cloud"         % "google-cloud-storage"         % "2.74.0",
      "com.google.cloud"         % "google-cloud-kms"             % "2.101.0",
      "com.google.cloud"         % "google-cloud-resourcemanager" % "1.100.0",
      "com.google.cloud"         % "google-cloud-logging"         % "3.39.0",
      "com.google.oauth-client"  % "google-oauth-client"          % "1.39.0",
      "org.projectlombok"        % "lombok"                       % "1.18.48",
      "com.squareup.okhttp3"     % "okhttp"                       % "5.5.0",
      "tools.jackson.core"       % "jackson-core"                 % jackson3Version,
      "tools.jackson.core"       % "jackson-databind"             % jackson3Version,
      "tools.jackson.dataformat" % "jackson-dataformat-xml"       % jackson3Version,
      "com.google.protobuf"      % "protobuf-java-util"           % "4.36.2",
      "io.kamon"                %% "kamon-bundle"                 % "2.8.1",
      "io.kamon"                %% "kamon-prometheus"             % "2.8.1",
      "org.unix4j"               % "unix4j-command"               % "0.6",
      "com.bettercloud"          % "vault-java-driver"            % "5.1.0",
      "org.apache.directory.api" % "api-all"                      % "2.1.9",
      "io.fabric8"               % "kubernetes-client"            % "8.0.0",
      "io.fabric8"               % "kubernetes-client-api"        % "8.0.0",
      "io.fabric8"               % "kubernetes-model"             % "6.14.0",
      "org.modelmapper"          % "modelmapper"                  % "3.2.6",
      ("com.datadoghq"           % "datadog-api-client"           % "2.61.0").classifier("shaded-jar"),
      "javax.xml.bind"           % "jaxb-api"                     % "2.3.1",
      "io.jsonwebtoken"          % "jjwt-api"                     % "0.13.0",
      "io.jsonwebtoken"          % "jjwt-impl"                    % "0.13.0",
      "io.jsonwebtoken"          % "jjwt-jackson"                 % "0.13.0",
      "io.swagger"               % "swagger-annotations"          % "1.6.16", // needed for annotations in prod code
      "de.dentrassi.crypto"      % "pem-keystore"                 % "3.0.0",
      "org.playframework"       %% "play-ebean"                   % "9.0.0-M2",
      "jakarta.validation"       % "jakarta.validation-api"       % "3.1.1",
      "qa.hedgehog"             %% "hedgehog-sbt"                 % "0.15.0" % Test,
      // https://hedgehog.qa/
      "qa.hedgehog" %% "hedgehog-core" % "0.15.0" % Test
    )
  )

addCommandAlias("fmt", "scalafmtAll; scalafmtSbt")
addCommandAlias("fmtCheck", "scalafmtCheckAll; scalafmtSbtCheck")

Test / parallelExecution := true

ThisBuild / outputStrategy := Some(StdoutOutput)
