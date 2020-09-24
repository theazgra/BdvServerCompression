package azgracompress.cli;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.jetbrains.annotations.NotNull;

public class CliConstants {

    public static final String MAIN_HELP = "azgracompress.DataCompressor [options] input";

    public static final String HELP_SHORT = "h";
    public static final String HELP_LONG = "help";

    public static final String COMPRESS_SHORT = "c";
    public static final String COMPRESS_LONG = "compress";

    public static final String DECOMPRESS_SHORT = "d";
    public static final String DECOMPRESS_LONG = "decompress";

    public static final String BENCHMARK_SHORT = "bench";
    public static final String BENCHMARK_LONG = "benchmark";

    public static final String TRAIN_SHORT = "tcb";
    public static final String TRAIN_LONG = "train-codebook";

    public static final String INSPECT_SHORT = "i";
    public static final String INSPECT_LONG = "inspect";

    public static final String CUSTOM_FUNCTION_SHORT = "cf";
    public static final String CUSTOM_FUNCTION_LONG = "custom-function";

    public static final String BITS_SHORT = "b";
    public static final String BITS_LONG = "bits";

    public static final String OUTPUT_SHORT = "o";
    public static final String OUTPUT_LONG = "output";

    public static final String VERBOSE_SHORT = "v";
    public static final String VERBOSE_LONG = "verbose";

    public static final String WORKER_COUNT_SHORT = "wc";
    public static final String WORKER_COUNT_LONG = "worker-count";

    public static final String CODEBOOK_CACHE_FOLDER_SHORT = "cbc";
    public static final String CODEBOOK_CACHE_FOLDER_LONG = "codebook-cache";

    public static final String SCALAR_QUANTIZATION_SHORT = "sq";
    public static final String SCALAR_QUANTIZATION_LONG = "scalar-quantization";

    public static final String VECTOR_QUANTIZATION_SHORT = "vq";
    public static final String VECTOR_QUANTIZATION_LONG = "vector-quantization";

    public static final String USE_MIDDLE_PLANE_SHORT = "mp";
    public static final String USE_MIDDLE_PLANE_LONG = "middle-plane";

    @NotNull
    public static Options getOptions() {
        final Options options = new Options();

        final OptionGroup methodGroup = new OptionGroup();
        methodGroup.setRequired(true);
        methodGroup.addOption(new Option(CliConstants.COMPRESS_SHORT,
                                         CliConstants.COMPRESS_LONG,
                                         false,
                                         "Compress 16 bit raw image"));
        methodGroup.addOption(new Option(CliConstants.DECOMPRESS_SHORT,
                                         CliConstants.DECOMPRESS_LONG,
                                         false,
                                         "Decompress 16 bit raw image"));
        methodGroup.addOption(new Option(CliConstants.INSPECT_SHORT,
                                         CliConstants.INSPECT_LONG,
                                         false,
                                         "Inspect the compressed file"));

        methodGroup.addOption(new Option(CliConstants.BENCHMARK_SHORT,
                                         CliConstants.BENCHMARK_LONG,
                                         false,
                                         "Benchmark"));

        methodGroup.addOption(new Option(CliConstants.TRAIN_SHORT,
                                         CliConstants.TRAIN_LONG,
                                         false,
                                         "Train codebook and save learned codebook to cache file."));

        methodGroup.addOption(new Option(CliConstants.CUSTOM_FUNCTION_SHORT,
                                         CliConstants.CUSTOM_FUNCTION_LONG,
                                         false,
                                         "Run user compiled custom code."));

        methodGroup.addOption(new Option(CliConstants.HELP_SHORT, CliConstants.HELP_LONG, false, "Print help"));

        final OptionGroup compressionMethodGroup = new OptionGroup();
        compressionMethodGroup.addOption(createSQOption());

        compressionMethodGroup.addOption(createVQOption());

        options.addOptionGroup(methodGroup);
        options.addOptionGroup(compressionMethodGroup);
        options.addOption(createBitsOption());

        options.addOption(CliConstants.USE_MIDDLE_PLANE_SHORT,
                          CliConstants.USE_MIDDLE_PLANE_LONG,
                          false,
                          "Use middle plane for codebook creation");

        options.addOption(createVerboseOption(false));

        options.addOption(new Option(CliConstants.WORKER_COUNT_SHORT,
                                     CliConstants.WORKER_COUNT_LONG,
                                     true,
                                     "Number of worker threads"));

        options.addOption(createCBCMethod());

        options.addOption(CliConstants.OUTPUT_SHORT, CliConstants.OUTPUT_LONG, true, "Custom output file");
        return options;
    }

    public static Option createBitsOption() {
        return new Option(CliConstants.BITS_SHORT, CliConstants.BITS_LONG, true, "Bit count per pixel [Default 8]");
    }

    public static Option createCBCMethod() {
        return new Option(CliConstants.CODEBOOK_CACHE_FOLDER_SHORT,
                          CliConstants.CODEBOOK_CACHE_FOLDER_LONG,
                          true,
                          "Folder of codebook caches");
    }

    public static Option createSQOption() {
        return new Option(CliConstants.SCALAR_QUANTIZATION_SHORT,
                          CliConstants.SCALAR_QUANTIZATION_LONG,
                          false,
                          "Use scalar quantization.");
    }

    public static Option createVQOption() {
        return new Option(CliConstants.VECTOR_QUANTIZATION_SHORT,
                          CliConstants.VECTOR_QUANTIZATION_LONG,
                          true,
                          "Use vector quantization. Need to pass vector size eg. 9,9x1,3x3");
    }

    public static Option createVerboseOption(final boolean onlyLongVersion) {
        return onlyLongVersion
                ? new Option(CliConstants.VERBOSE_LONG, false, "Make program verbose")
                : new Option(CliConstants.VERBOSE_SHORT, CliConstants.VERBOSE_LONG, false, "Make program verbose");
    }
}
