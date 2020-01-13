import org.apache.commons.cli.*;

import java.io.IOException;

public class DataCompressor {

    private static final String COMPRESS_SHORT = "c";
    private static final String COMPRESS_LONG = "compress";

    private static final String DECOMPRESS_SHORT = "d";
    private static final String DECOMPRESS_LONG = "decompress";

    private static final String BITS_SHORT = "b";
    private static final String BITS_LONG = "bits";

    private static final String INPUT_SHORT = "i";
    private static final String INPUT_LONG = "input";

    private static final String OUTPUT_SHORT = "o";
    private static final String OUTPUT_LONG = "output";

    public static void main(String[] args) throws IOException {


        Options options = new Options();

        OptionGroup methodGroup = new OptionGroup();

        methodGroup.setRequired(true);
        methodGroup.addOption(new Option(COMPRESS_SHORT, COMPRESS_LONG, false, "Compress 16 bit raw image"));
        methodGroup.addOption(new Option(DECOMPRESS_SHORT, DECOMPRESS_LONG, false, "Decompress 16 bit raw image"));
        methodGroup.addOption(new Option("h", "help", false, "Print help"));

        options.addOptionGroup(methodGroup);
        options.addOption(BITS_SHORT, BITS_LONG, true, "Bit count per pixel [Default 8]");
        options.addRequiredOption(INPUT_SHORT, INPUT_LONG, true, "Input file");
        options.addRequiredOption(OUTPUT_SHORT, OUTPUT_LONG, true, "Output file");

        HelpFormatter formatter = new HelpFormatter();


        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            if (args[0].equals("-h") || args[0].equals("--help")) {
                formatter.printHelp("ijava -jar DataCompressor.jar", options);
                return;
            }
            System.err.println("Error: " + e.getMessage());
            return;
        }

        if (cmd.hasOption("help")) {
            formatter.printHelp("ijava -jar DataCompressor.jar", options);
            return;
        }

        final String bitsString = cmd.getOptionValue(BITS_LONG, "8");
        final int bits = Integer.parseInt(bitsString);

        if (cmd.hasOption(COMPRESS_SHORT) || cmd.hasOption(COMPRESS_LONG)) {
            System.out.println(String.format("Compressing %s with %d bits, saving to %s",
                                             cmd.getOptionValue(INPUT_SHORT),
                                             bits,
                                             cmd.getOptionValue(OUTPUT_SHORT)));

            // TODO(Moravec): Do compression here.
            return;
        }

        if (cmd.hasOption(DECOMPRESS_SHORT) || cmd.hasOption(DECOMPRESS_LONG)) {
            System.out.println(String.format("Decompressing %s with %d bits, saving to %s",
                                             cmd.getOptionValue(INPUT_SHORT),
                                             bits,
                                             cmd.getOptionValue(OUTPUT_SHORT)));

            // TODO(Moravec): Do decompression here.
            return;
        }


    }
}
