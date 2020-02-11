package azgracompress;

import azgracompress.benchmark.CompressionBenchmark;
import azgracompress.cli.CliConstants;
import azgracompress.cli.CustomFunctionBase;
import azgracompress.cli.ParsedCliOptions;
import azgracompress.cli.functions.MeasurePlaneErrorFunction;
import azgracompress.compression.ImageCompressor;
import azgracompress.compression.ImageDecompressor;
import org.apache.commons.cli.*;

import java.io.IOException;

public class DataCompressor {


    public static void main(String[] args) {
        Options options = CliConstants.getOptions();

        HelpFormatter formatter = new HelpFormatter();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            if ((args.length > 0) && (args[0].equals("-h") || args[0].equals("--help"))) {
                formatter.printHelp(CliConstants.MAIN_HELP, options);
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

        if (parsedCliOptions.isVerbose()) {
            System.out.println(parsedCliOptions.report());
        }

        switch (parsedCliOptions.getMethod()) {

            case Compress: {
                ImageCompressor compressor = new ImageCompressor(parsedCliOptions);
                if (!compressor.compress()) {
                    System.err.println("Errors occurred during compression.");
                }
                return;
            }
            case Decompress: {
                ImageDecompressor decompressor = new ImageDecompressor(parsedCliOptions);
                if (!decompressor.decompress()) {
                    System.err.println("Errors occurred during decompression.");
                }
                return;
            }
            case Benchmark: {
                CompressionBenchmark.runBenchmark(parsedCliOptions);
                return;
            }
            case TrainCodebook: {
                ImageCompressor compressor = new ImageCompressor(parsedCliOptions);
                if (!compressor.trainAndSaveCodebook()) {
                    System.err.println("Errors occurred during training/saving of codebook.");
                }
                return;
            }
            case CustomFunction: {
                // NOTE(Moravec): Custom function class here |
                //                                           V
                CustomFunctionBase customFunction = new MeasurePlaneErrorFunction(parsedCliOptions);
                if (!customFunction.run()) {
                    System.err.println("Errors occurred during custom function.");
                }
                return;

            }

            case PrintHelp: {
                formatter.printHelp(CliConstants.MAIN_HELP, options);
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
}
