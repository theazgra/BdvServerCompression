package cz.it4i.qcmp;

import cz.it4i.qcmp.benchmark.CompressionBenchmark;
import cz.it4i.qcmp.cache.QuantizationCacheManager;
import cz.it4i.qcmp.cli.CliConstants;
import cz.it4i.qcmp.cli.CompressionOptionsCLIParser;
import cz.it4i.qcmp.cli.CustomFunctionBase;
import cz.it4i.qcmp.cli.functions.CalculateDifference;
import cz.it4i.qcmp.compression.ImageCompressor;
import cz.it4i.qcmp.compression.ImageDecompressor;
import cz.it4i.qcmp.fileformat.FileExtensions;
import org.apache.commons.cli.*;

import java.io.IOException;

public class DataCompressor {
    public static void main(final String[] args) {
        final Options options = CliConstants.getOptions();

        final HelpFormatter formatter = new HelpFormatter();
        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (final ParseException e) {
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

        final CompressionOptionsCLIParser compressionOptionsCLIParsed = new CompressionOptionsCLIParser(cmd);
        // NOTE(Moravec): From this point we need to dispose of possible existing SCIFIO context.
        if (compressionOptionsCLIParsed.parseError()) {
            System.err.println(compressionOptionsCLIParsed.getParseError());
            ScifioWrapper.dispose();
            return;
        }

        if (compressionOptionsCLIParsed.isVerbose()) {
            System.out.println(compressionOptionsCLIParsed.report());
        }

        switch (compressionOptionsCLIParsed.getMethod()) {
            case Compress: {
                final String label =
                        compressionOptionsCLIParsed.getQuantizationType().toString() + " " + compressionOptionsCLIParsed.getQuantizationVector().toString();
                //                final Stopwatch stopwatch = Stopwatch.startNew();
                final ImageCompressor compressor = new ImageCompressor(compressionOptionsCLIParsed);
                if (!compressor.compress()) {
                    System.err.println("Errors occurred during compression.");
                }
                //                stopwatch.stop();
                //                ColorConsole.printf(ColorConsole.Color.Green, label);
                //                ColorConsole.printf(ColorConsole.Color.Green,
                //                                    "Compression completed in %d ms.",
                //                                    stopwatch.getElapsedInUnit(TimeUnit.MILLISECONDS));
            }
            break;
            case Decompress: {
                //                final Stopwatch stopwatch = Stopwatch.startNew();
                final ImageDecompressor decompressor = new ImageDecompressor(compressionOptionsCLIParsed);
                if (!decompressor.decompressToFile()) {
                    System.err.println("Errors occurred during decompression.");
                }
                //                stopwatch.stop();
                //                ColorConsole.printf(ColorConsole.Color.Green,
                //                                    "Decompression completed in %d ms.",
                //                                    stopwatch.getElapsedInUnit(TimeUnit.MILLISECONDS));
            }
            break;

            case Benchmark: {
                CompressionBenchmark.runBenchmark(compressionOptionsCLIParsed);
            }
            break;
            case TrainCodebook: {
                final ImageCompressor compressor = new ImageCompressor(compressionOptionsCLIParsed);
                if (!compressor.trainAndSaveCodebook()) {
                    System.err.println("Errors occurred during training/saving of codebook.");
                }
            }
            break;
            case CustomFunction: {
                // NOTE(Moravec): Custom function class here |
                //                                           V
                //CustomFunctionBase customFunction = new MeasurePlaneErrorFunction(parsedCliOptions);
                // final CustomFunctionBase customFunction = new EntropyCalculation(compressionOptionsCLIParsed);
                final CustomFunctionBase cf = new CalculateDifference(compressionOptionsCLIParsed);
                if (!cf.run()) {
                    System.err.println("Errors occurred during custom function.");
                }
            }
            break;

            case PrintHelp: {
                formatter.printHelp(CliConstants.MAIN_HELP, options);
            }
            break;
            case InspectFile: {
                if (compressionOptionsCLIParsed.getInputDataInfo().getFilePath().endsWith(FileExtensions.CACHE_FILE_EXT)) {
                    QuantizationCacheManager.inspectCacheFile(compressionOptionsCLIParsed.getInputDataInfo().getFilePath(),
                                                              compressionOptionsCLIParsed.isVerbose());
                } else {
                    final ImageDecompressor decompressor = new ImageDecompressor(compressionOptionsCLIParsed);
                    try {
                        System.out.println(decompressor.inspectCompressedFile());
                    } catch (final IOException e) {
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
