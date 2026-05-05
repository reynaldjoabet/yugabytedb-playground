$version: "2.0"

namespace nodeagent.server

use nodeagent.server#Error
use smithy.api#http
use smithy.api#streaming

service NodeAgent {
    version: "2024-01-01"
    operations: [
        Ping
        ExecuteCommand
        SubmitTask
        DescribeTask
        AbortTask
        UploadFile
        DownloadFile
        Update
    ]
}

structure ServerInfo {
    version: String
    restartNeeded: Boolean
    offloadable: Boolean
    compressor: String
}

structure PingRequest {}

structure PingResponse {
    serverInfo: ServerInfo
}

@http(method: "POST", uri: "/Ping")
operation Ping {
    input: PingRequest
    output: PingResponse
}

structure ExecuteCommandRequest {
    command: StringList
    user: String
}

structure ChunkData {
    chunkData: Blob
}

structure Output {
    output: String
}

@streaming
union ExecuteCommandData {
    output: Output
    error: Error
}

structure ExecuteCommandResponse {
    data: ExecuteCommandData
}

@http(method: "POST", uri: "/ExecuteCommand")
operation ExecuteCommand {
    input: ExecuteCommandRequest
    output: ExecuteCommandResponse
}

structure CommandInput {
    command: StringList
}

union SubmitTaskInputData {
    commandInput: CommandInput
    preflightCheckInput: PreflightCheckInput
    serverControlInput: ServerControlInput
    configureServiceInput: ConfigureServiceInput
    installSoftwareInput: InstallSoftwareInput
    serverGFlagsInput: ServerGFlagsInput
    installYbcInput: InstallYbcInput
    configureServerInput: ConfigureServerInput
    installOtelCollectorInput: InstallOtelCollectorInput
    setupCGroupInput: SetupCGroupInput
    downloadSoftwareInput: DownloadSoftwareInput
    destroyServerInput: DestroyServerInput
    ynpPreflightCheckInput: YnpPreflightCheckInput
}

structure SubmitTaskRequest {
    user: String
    taskId: String
    data: SubmitTaskInputData
}

structure SubmitTaskResponse {
    taskId: String
}

@http(method: "POST", uri: "/SubmitTask")
operation SubmitTask {
    input: SubmitTaskRequest
    output: SubmitTaskResponse
}

structure DescribeTaskRequest {
    taskId: String
}

structure DescribeTaskResponse{
    state: String
    data: DescribeTaskResponseData
}

structure DescribeTaskStreamingResponse{
    state: String
    data: DescribeTaskResponseData
}

@streaming
union DescribeTaskResponseData {
    output: Output
    error: Error
    preflightCheckOutput: PreflightCheckOutput
    serverControlOutput: ServerControlOutput
    configureServiceOutput: ConfigureServiceOutput
    installSoftwareOutput: InstallSoftwareOutput
    serverGFlagsOutput: ServerGFlagsOutput
    installYbcOutput: InstallYbcOutput
    configureServerOutput: ConfigureServerOutput
    installOtelCollectorOutput: InstallOtelCollectorOutput
    setupCGroupOutput: SetupCGroupOutput
    downloadSoftwareOutput: DownloadSoftwareOutput
    destroyServerOutput: DestroyServerOutput
    ynpPreflightCheckOutput: YnpPreflightCheckOutput
}

@http(method: "POST", uri: "/DescribeTask")
operation DescribeTask {
    input: DescribeTaskRequest
    output: DescribeTaskResponse
}

structure AbortTaskRequest {
    taskId: String
}

structure AbortTaskResponse {
    taskId: String
}

@http(method: "POST", uri: "/AbortTask")
operation AbortTask {
    input: AbortTaskRequest
    output: AbortTaskResponse
}

structure FileInfo {
    filename: String
}

structure UploadFileResponse {}

union UploadFileData {
    fileInfo: FileInfo
    chunkData: Blob
}

@streaming
blob UploadFileDataBlob
structure UploadFileRequest {
  data: UploadFileData
    user: String
    chmod: Integer
}

@http(method: "POST", uri: "/UploadFile")
operation UploadFile {
    input: UploadFileRequest
    output: UploadFileResponse
}

structure DownloadFileRequest {
    filename: String
    user: String
}

@streaming
blob StreamingBlob
structure DownloadFileResponse {
    @required
    chunkData: StreamingBlob
}

@http(method: "POST", uri: "/DownloadFile")
operation DownloadFile {
    input: DownloadFileRequest
    output: DownloadFileResponse
}

structure UpgradeInfo {
    packagePath: String
    certDir: String
}

structure UpdateRequest {
    state: String
    upgradeInfo: UpgradeInfo
}

structure UpdateResponse {
    home: String
}

@http(method: "POST", uri: "/Update")
operation Update {
    input: UpdateRequest
    output: UpdateResponse
}
