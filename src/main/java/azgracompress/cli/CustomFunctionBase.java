package azgracompress.cli;

public abstract class CustomFunctionBase {
    protected final ParsedCliOptions options;

    /**
     * Base constructor with parsed CLI options.
     * @param options Parsed cli options.
     */
    public CustomFunctionBase(ParsedCliOptions options) {
        this.options = options;
    }

    /**
     * Run custom function.
     * @return True if no errors occurred.
     */
    public abstract boolean run();

}
