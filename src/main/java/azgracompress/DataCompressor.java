package azgracompress;

import azgracompress.benchmark.CompressionBenchmark;
import azgracompress.cache.QuantizationCacheManager;
import azgracompress.cli.CliConstants;
import azgracompress.cli.CustomFunctionBase;
import azgracompress.cli.ParsedCliOptions;
import azgracompress.cli.functions.EntropyCalculation;
import azgracompress.compression.ImageCompressor;
import azgracompress.compression.ImageDecompressor;
import azgracompress.fileformat.FileExtensions;
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
            if (e.getMessage().startsWith("Missing required option:")) {
                System.err.println("Error: Missing required option, see usage below. :^)");
                formatter.printHelp(CliConstants.MAIN_HELP, options);
            } else {
                System.err.println("Error: " + e.getMessage());
            }
            return;
        }

        ParsedCliOptions parsedCliOptions = new ParsedCliOptions(cmd);
        // NOTE(Moravec): From this point we need to dispose of possible existing SCIFIO context.
        if (parsedCliOptions.parseError()) {
            System.err.println(parsedCliOptions.getParseError());
            ScifioWrapper.dispose();
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
            }
            break;
            case Decompress: {
                ImageDecompressor decompressor = new ImageDecompressor(parsedCliOptions);
                if (!decompressor.decompressToFile()) {
                    System.err.println("Errors occurred during decompression.");
                }
            }
            break;

            case Benchmark: {
                CompressionBenchmark.runBenchmark(parsedCliOptions);
            }
            break;
            case TrainCodebook: {
                ImageCompressor compressor = new ImageCompressor(parsedCliOptions);
                if (!compressor.trainAndSaveCodebook()) {
                    System.err.println("Errors occurred during training/saving of codebook.");
                }
            }
            break;
            case CustomFunction: {
                // NOTE(Moravec): Custom function class here |
                //                                           V
                //CustomFunctionBase customFunction = new MeasurePlaneErrorFunction(parsedCliOptions);
                CustomFunctionBase customFunction = new EntropyCalculation(parsedCliOptions);
                if (!customFunction.run()) {
                    System.err.println("Errors occurred during custom function.");
                }
            }
            break;

            case PrintHelp: {
                formatter.printHelp(CliConstants.MAIN_HELP, options);
            }
            break;
            case InspectFile: {
                if (parsedCliOptions.getInputDataInfo().getFilePath().endsWith(FileExtensions.CACHE_FILE_EXT)) {
                    QuantizationCacheManager.inspectCacheFile(parsedCliOptions.getInputDataInfo().getFilePath(),
                                                              parsedCliOptions.isVerbose());
                } else {
                    ImageDecompressor decompressor = new ImageDecompressor(parsedCliOptions);
                    try {
                        System.out.println(decompressor.inspectCompressedFile());
                    } catch (IOException e) {
                        System.err.println("Errors occurred during inspecting file.");
                        System.err.println(e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            break;
        }
        ScifioWrapper.dispose();
    }
}
