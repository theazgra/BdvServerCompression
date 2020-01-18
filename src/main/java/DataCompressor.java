import cli.CliConstants;
import cli.ParsedCliOptions;
import compression.ImageCompressor;
import compression.ImageDecompressor;
import org.apache.commons.cli.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class DataCompressor {


    public static void main(String[] args) {
        Options options = getOptions();

        HelpFormatter formatter = new HelpFormatter();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            if ((args.length > 0) && (args[0].equals("-h") || args[0].equals("--help"))) {
                formatter.printHelp("ijava -jar DataCompressor.jar", options);
                return;
            }
            System.err.println("Error: " + e.getMessage());
            return;
        }

        ParsedCliOptions parsedCliOptions = new ParsedCliOptions(cmd);
        if (parsedCliOptions.hasErrorOccured()) {
            System.err.println(parsedCliOptions.getError());
            return;
        }

//        System.out.println(parsedCliOptions.report());

        switch (parsedCliOptions.getMethod()) {

            case Compress: {
                ImageCompressor compressor = new ImageCompressor(parsedCliOptions);
                try {
                    compressor.compress();
                } catch (Exception e) {
                    System.err.println("Errors occurred during compression.");
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                }
                return;
            }
            case Decompress: {
                ImageDecompressor decompressor = new ImageDecompressor(parsedCliOptions);
                try {
                    decompressor.decompress();
                } catch (Exception e) {
                    System.err.println("Errors occurred during decompression.");
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                }
                return;
            }
            case PrintHelp: {
                formatter.printHelp("ijava -jar DataCompressor.jar", options);
            }
            break;
            case InspectFile: {
                ImageDecompressor decompressor = new ImageDecompressor(parsedCliOptions);
                try {
                    System.out.println(decompressor.inspectCompressedFile());
                } catch (IOException e) {
                    System.err.println("Errors occurred during inspecting file.");
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                }
                return;
            }
        }
        return;
    }

    @NotNull
    private static Options getOptions() {
        Options options = new Options();

        OptionGroup methodGroup = new OptionGroup();
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
        methodGroup.addOption(new Option(CliConstants.HELP_SHORT, CliConstants.HELP_LONG, false, "Print help"));

        OptionGroup compressionMethodGroup = new OptionGroup();
        compressionMethodGroup.addOption(new Option(CliConstants.SCALAR_QUANTIZATION_SHORT,
                                                    CliConstants.SCALAR_QUANTIZATION_LONG,
                                                    false,
                                                    "Use scalar quantization."));

        compressionMethodGroup.addOption(new Option(CliConstants.VECTOR_QUANTIZATION_SHORT,
                                                    CliConstants.VECTOR_QUANTIZATION_LONG,
                                                    true,
                                                    "Use vector quantization. Need to pass vector size eg. 9,9x1,3x3"));

        options.addOptionGroup(methodGroup);
        options.addOptionGroup(compressionMethodGroup);
        options.addOption(CliConstants.BITS_SHORT, CliConstants.BITS_LONG, true, "Bit count per pixel [Default 8]");
        options.addOption(CliConstants.REFERENCE_PLANE_SHORT,
                          CliConstants.REFERENCE_PLANE_LONG,
                          true,
                          "Reference plane index");
        options.addOption(new Option(CliConstants.VERBOSE_SHORT,
                                     CliConstants.VERBOSE_LONG,
                                     false,
                                     "Make program verbose"));
        //        options.addRequiredOption(INPUT_SHORT, INPUT_LONG, true, "Input file");
        options.addOption(CliConstants.OUTPUT_SHORT, CliConstants.OUTPUT_LONG, true, "Custom output file");
        return options;
    }
}
