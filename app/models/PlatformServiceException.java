package models;

public class PlatformServiceException extends RuntimeException {
    public PlatformServiceException(int notFound, String message) {
        super(message);
    }
}
