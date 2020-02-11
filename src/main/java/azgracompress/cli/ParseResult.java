package azgracompress.cli;

public class ParseResult<T> {
    private final boolean success;
    private final T value;
    private final String errorMessage;

    public ParseResult(final String errorMessage) {
        this.success = false;
        this.value = null;
        this.errorMessage = errorMessage;
    }

    public ParseResult(T value) {
        this.success = true;
        this.value = value;
        this.errorMessage = null;
    }

    public boolean isSuccess() {
        return success;
    }

    public T getValue() {
        return value;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
