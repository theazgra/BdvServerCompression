import cli.CliConstants;
import cli.ParsedCliOptions;
import org.apache.commons.cli.*;

import java.io.IOException;

public class DataCompressor {


    public static void main(String[] args) throws IOException {

        /*
         * Example of usage:
         *   ijava -ea -jar DataCompressor.jar -c -sq input.raw 1000x1000x500
         *   ijava -ea -jar DataCompressor.jar -c -sq -b 7 input.raw 1000x1000x500
         *   ijava -ea -jar DataCompressor.jar -c -sq -b 7 input.raw 1000x1000x500 10
         *   ijava -ea -jar DataCompressor.jar -c -sq -b 7 -rp 10 input.raw 1000x1000x500 10
         *   ijava -ea -jar DataCompressor.jar -c -sq -b 7 -rp 15 input.raw 1000x1000x500 10
         *   ijava -ea -jar DataCompressor.jar -c -sq -b 7 -rp 40 input.raw 1000x1000x500
         *   ijava -ea -jar DataCompressor.jar -c -vq 9 input.raw 1000x1000x500
         *   ijava -ea -jar DataCompressor.jar -c -vq 9x1 input.raw 1000x1000x500
         *   ijava -ea -jar DataCompressor.jar -c -vq 3x3 input.raw 1000x1000x500
         *
         * */
        // TODO(Moravec):   We have created nice API for single files, but ...
        //                  we are dealing with tiffs or RAW files, which means
        //                  that we have to pass image dimensions and plane index.

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
        methodGroup.addOption(new Option(CliConstants.VERBOSE_SHORT,
                                         CliConstants.VERBOSE_LONG,
                                         false,
                                         "Make program verbose"));

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
        //        options.addRequiredOption(INPUT_SHORT, INPUT_LONG, true, "Input file");
        options.addOption(CliConstants.OUTPUT_SHORT, CliConstants.OUTPUT_LONG, true, "Custom output directory");

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

        System.out.println(parsedCliOptions.report());

        switch (parsedCliOptions.getMethod()) {

            case Compress:
                System.out.println("Compress");
                break;
            case Decompress:
                System.out.println("Decompress");
                break;
            case PrintHelp:
                formatter.printHelp("ijava -jar DataCompressor.jar", options);
                break;
            case InspectFile:
                System.err.println("Not supported yet.");
                break;
        }

    }
}
