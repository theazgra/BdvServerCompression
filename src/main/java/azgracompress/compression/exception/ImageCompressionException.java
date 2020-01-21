package azgracompress.compression.exception;

public class ImageCompressionException extends Exception {
    private final Exception innerException;

    public ImageCompressionException(final String message, final Exception innerException) {
        super(message);
        this.innerException = innerException;
    }

    public ImageCompressionException(final String message) {
        super(message);
        this.innerException = null;
    }

    @Override
    public String getMessage() {
        String msg = super.getMessage();
        if (msg != null && innerException != null) {
            msg += "\nInner exception:\n" + innerException.getMessage();
        }
        return msg;
    }
}
