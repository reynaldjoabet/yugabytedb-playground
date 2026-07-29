package models;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Aborts the current request with a specific HTTP status. The message is either plain text or, for
 * validation failures, a JSON body describing the offending fields.
 */
public class PlatformServiceException extends RuntimeException {

    private final int httpStatus;

    private final JsonNode contentJson;

    private final JsonNode requestJson;

    public PlatformServiceException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.contentJson = null;
        this.requestJson = null;
    }

    public PlatformServiceException(int httpStatus, JsonNode contentJson) {
        this(httpStatus, contentJson, null);
    }

    public PlatformServiceException(int httpStatus, JsonNode contentJson, JsonNode requestJson) {
        super(contentJson == null ? null : contentJson.toString());
        this.httpStatus = httpStatus;
        this.contentJson = contentJson;
        this.requestJson = requestJson;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /** The error body, when this exception was raised with JSON content. */
    public JsonNode getContentJson() {
        return contentJson;
    }

    /** The request body that failed validation, when the caller attached it. */
    public JsonNode getRequestJson() {
        return requestJson;
    }
}
