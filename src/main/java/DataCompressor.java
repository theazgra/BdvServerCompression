import cli.ImprovedOptionGroup;
import compression.tool.ImageCompressor;
import compression.tool.ImageDecompressor;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.Paths;

public class DataCompressor {

    private static final String COMPRESS_SHORT = "c";
    private static final String COMPRESS_LONG = "compress";

    private static final String DECOMPRESS_SHORT = "d";
    private static final String DECOMPRESS_LONG = "decompress";

    private static final String BITS_SHORT = "b";
    private static final String BITS_LONG = "bits";

    //    private static final String INPUT_SHORT = "i";
    //    private static final String INPUT_LONG = "input";

    private static final String OUTPUT_SHORT = "o";
    private static final String OUTPUT_LONG = "output";

    private static final String VERBOSE_SHORT = "v";
    private static final String VERBOSE_LONG = "verbose";

    public static void main(String[] args) throws IOException {

        // TODO(Moravec):   We have created nice API for single files, but ...
        //                  we are dealing with tiffs or RAW files, which means
        //                  that we have to pass image dimensions and plane index.

        Options options = new Options();

        OptionGroup methodGroup = new ImprovedOptionGroup();

        methodGroup.setRequired(true);
        methodGroup.addOption(new Option(COMPRESS_SHORT, COMPRESS_LONG, false, "Compress 16 bit raw image"));
        methodGroup.addOption(new Option(DECOMPRESS_SHORT, DECOMPRESS_LONG, false, "Decompress 16 bit raw image"));
        methodGroup.addOption(new Option("h", "help", false, "Print help"));
        methodGroup.addOption(new Option(VERBOSE_SHORT, VERBOSE_LONG, false, "Make program verbose"));

        options.addOptionGroup(methodGroup);
        options.addOption(BITS_SHORT, BITS_LONG, true, "Bit count per pixel [Default 8]");
        //        options.addRequiredOption(INPUT_SHORT, INPUT_LONG, true, "Input file");
        options.addOption(OUTPUT_SHORT, OUTPUT_LONG, true, "Custom output directory");

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

        if (cmd.hasOption("help")) {
            formatter.printHelp("ijava -jar DataCompressor.jar", options);
            return;
        }

        // Default output directory to current directory.
        final String currentOutputDirectory = cmd.getOptionValue(OUTPUT_LONG,
                                                                 Paths.get("").toAbsolutePath().toString());

        // Default bit count per pixel to 8.
        final int bits = Integer.parseInt(cmd.getOptionValue(BITS_LONG, "8"));

        // Files to compress decompress
        final String[] files = cmd.getArgs();
        System.out.println("Output directory: " + currentOutputDirectory);
        System.out.println("Bit count: " + bits);

        final boolean verbose = cmd.hasOption(VERBOSE_LONG);

        if (cmd.hasOption(COMPRESS_SHORT) || cmd.hasOption(COMPRESS_LONG)) {
            System.out.println("Compressing: ...");
            ImageCompressor compressor = new ImageCompressor(currentOutputDirectory, bits, verbose);
            if (!compressor.compressRawImages(files)) {
                // TODO(Moravec): Report error.
            }
            return;
        }
        if (cmd.hasOption(DECOMPRESS_SHORT) || cmd.hasOption(DECOMPRESS_LONG)) {
            System.out.println("Decompressing: ...");
            ImageDecompressor decompressor = new ImageDecompressor(currentOutputDirectory, bits, verbose);
            if (!decompressor.decompressCompressedImages(files)) {
                // TODO(Moravec): Report error.
            }
        }


    }
}
