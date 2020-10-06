package cz.it4i.qcmp.cli;

public abstract class CustomFunctionBase {
    protected final CompressionOptionsCLIParser options;

    /**
     * Base constructor with parsed CLI options.
     *
     * @param options Parsed cli options.
     */
    public CustomFunctionBase(final CompressionOptionsCLIParser options) {
        this.options = options;
    }

    /**
     * Run custom function.
     *
     * @return True if no errors occurred.
     */
    public abstract boolean run();

}
