$version: "2.0"

namespace nodeagent.server

list StringList {
    member: String
}

map StringMap {
    key: String
    value: String
}

list NodeConfigList {
    member: NodeConfig
}

structure PreflightCheckInput {
    ybHomeDir: String
    skipProvisioning: Boolean
    airGapInstall: Boolean
    installNodeExporter: Boolean
    sshPort: Integer
    mountPaths: StringList
    masterHttpPort: Integer
    masterRpcPort: Integer
    tserverHttpPort: Integer
    tserverRpcPort: Integer
    redisServerHttpPort: Integer
    redisServerRpcPort: Integer
    nodeExporterPort: Integer
    ycqlServerHttpPort: Integer
    ycqlServerRpcPort: Integer
    ysqlServerHttpPort: Integer
    ysqlServerRpcPort: Integer
    ybControllerHttpPort: Integer
    ybControllerRpcPort: Integer
}

structure PreflightCheckOutput {
    nodeConfigs: NodeConfigList
}

structure NodeConfig {
    type: String
    value: String
}

enum Service {
    EARLYOOM
}

structure GetServiceStateRequest {
    service: Service
}

structure EarlyoomState {
    kills_last_hour: Integer
    total_kills: Integer
}

union GetServiceStateResponseState {
    earlyoomState: EarlyoomState
}

structure GetServiceStateResponse {
    enabled: Boolean
    state: GetServiceStateResponseState
}

structure EarlyoomConfig {
    startArgs: String
}

union ServiceConfigData {
    earlyoomConfig: EarlyoomConfig
}

structure ServiceConfig {
    ybHomeDir: String
    data: ServiceConfigData
}

structure ConfigureServiceInput {
    service: Service
    enabled: Boolean
    config: ServiceConfig
}

structure ConfigureServiceOutput {
    service: Service
    success: Boolean
    error: Error
}

enum ServerControlType {
    START
    STOP
}

structure ServerControlInput {
    controlType: ServerControlType
    serverName: String
    serverHome: String
    deconfigure: Boolean
    numVolumes: Integer
}

structure ServerControlOutput {
    pid: Integer
}

structure InstallSoftwareInput {
    ybPackage: String
    remoteTmp: String
    ybHomeDir: String
    symLinkFolders: StringList
}

structure InstallSoftwareOutput {
    pid: Integer
}

structure DownloadSoftwareInput {
    ybPackage: String
    s3RemoteDownload: Boolean
    awsAccessKey: String
    awsSecretKey: String
    gcsRemoteDownload: Boolean
    gcsCredentialsJson: String
    httpRemoteDownload: Boolean
    httpPackageChecksum: String
    iTestS3PackagePath: String
    remoteTmp: String
    ybHomeDir: String
    numReleasesToKeep: Integer
}

structure DownloadSoftwareOutput {
    pid: Integer
}

structure ServerGFlagsInput {
    gflags: StringMap
    serverName: String
    serverHome: String
    resetMasterState: Boolean
}

structure ServerGFlagsOutput {}

structure InstallYbcInput {
    ybcPackage: String
    remoteTmp: String
    ybHomeDir: String
    mountPoints: StringList
}

structure InstallYbcOutput {
    pid: Integer
}

structure ConfigureServerInput {
    remoteTmp: String
    ybHomeDir: String
    processes: StringList
    mountPoints: StringList
    numCoresToKeep: Integer
    configureCgroup: Boolean
}

structure ConfigureServerOutput {
    pid: Integer
}

structure InstallOtelCollectorInput {
    ybHomeDir: String
    mountPoints: StringList
    otelColPackagePath: String
    ycqlAuditLogLevel: String
    otelColConfigFile: String
    otelColAwsAccessKey: String
    otelColAwsSecretKey: String
    otelColGcpCredsFile: String
    remoteTmp: String
    otelColMaxMemory: Integer
}

structure InstallOtelCollectorOutput {
    pid: Integer
}

structure SetupCGroupInput {
    ybHomeDir: String
    pgMaxMemMb: Integer
}

structure SetupCGroupOutput {
    pid: Integer
}

structure DestroyServerInput {
    ybHomeDir: String
    isProvisioningCleanup: Boolean
}

structure DestroyServerOutput {}

structure YnpPreflightCheckInput {
    configJson: String
    remoteTmp: String
}

structure YnpPreflightCheckOutput {
    exitCode: Integer
    output: String
}
