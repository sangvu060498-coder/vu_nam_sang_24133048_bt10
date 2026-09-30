package vn.iotstar.exception;

public class InvalidJwtSignatureException extends RuntimeException {
    public InvalidJwtSignatureException(String message) {
        super(message);
    }
}
