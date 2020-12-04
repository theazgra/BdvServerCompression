package cz.it4i.qcmp;

import cz.it4i.qcmp.benchmark.CompressionBenchmark;
import cz.it4i.qcmp.cli.CliConstants;
import cz.it4i.qcmp.cli.CompressionOptionsCLIParser;
import cz.it4i.qcmp.cli.CustomFunctionBase;
import cz.it4i.qcmp.cli.functions.DebugFunction;
import cz.it4i.qcmp.compression.ImageCompressor;
import cz.it4i.qcmp.compression.ImageDecompressor;
import cz.it4i.qcmp.fileformat.IQvcFile;
import cz.it4i.qcmp.io.FileTypeInspector;
import cz.it4i.qcmp.io.QuantizationCacheManager;
import cz.it4i.qcmp.io.QvcFileReader;
import cz.it4i.qcmp.utilities.ColorConsole;
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

        final CompressionOptionsCLIParser parsedOptions = new CompressionOptionsCLIParser(cmd);
        // NOTE(Moravec): From this point we need to dispose of possible existing SCIFIO context.
        if (parsedOptions.parseError()) {
            System.err.println(parsedOptions.getParseError());
            ScifioWrapper.dispose();
            return;
        }

        if (parsedOptions.isVerbose()) {
            System.out.println(parsedOptions.report());
        }

        switch (parsedOptions.getMethod()) {
            case Compress: {
                final String label =
                        parsedOptions.getQuantizationType().toString() + " " + parsedOptions.getQuantizationVector().toString();
                //                final Stopwatch stopwatch = Stopwatch.startNew();
                final ImageCompressor compressor = new ImageCompressor(parsedOptions);
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
                final ImageDecompressor decompressor = new ImageDecompressor(parsedOptions);
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
                CompressionBenchmark.runBenchmark(parsedOptions);
            }
            break;
            case TrainCodebook: {
                final ImageCompressor compressor = new ImageCompressor(parsedOptions);
                if (!compressor.trainAndSaveCodebook()) {
                    System.err.println("Errors occurred during training/saving of codebook.");
                }
            }
            break;
            case CustomFunction: {
                // NOTE(Moravec): Custom function class here |
                //                                           V
                // final CustomFunctionBase customFunction = new MeasurePlaneErrorFunction(parsedCliOptions);
                // final CustomFunctionBase customFunction = new EntropyCalculation(compressionOptionsCLIParsed);
                // final CustomFunctionBase cf = new CalculateDifference(compressionOptionsCLIParsed);
                final CustomFunctionBase cf = new DebugFunction(parsedOptions);
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
                final FileTypeInspector.FileType fileType = FileTypeInspector.inspectFile(parsedOptions.getInputDataInfo().getFilePath());
                switch (fileType) {
                    case Qcmp: {
                        final ImageDecompressor decompressor = new ImageDecompressor(parsedOptions);
                        try {
                            System.out.println(decompressor.inspectCompressedFile());
                        } catch (final IOException e) {
                            System.err.println("Errors occurred during inspecting file.");
                            System.err.println(e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    break;
                    case Qvc: {
                        QuantizationCacheManager.inspectCacheFile(parsedOptions.getInputDataInfo().getFilePath(),
                                                                  parsedOptions.isVerbose());
                    }
                    break;
                    case InvalidPath:
                        exitWithInvalidFilePath();
                        break;
                    case Unknown:
                        exitWithUnknownFile();
                        break;
                }
            }
            break;
            case Convert: {
                final FileTypeInspector.FileType fileType = FileTypeInspector.inspectFile(parsedOptions.getInputDataInfo().getFilePath());
                switch (fileType) {
                    case Qcmp: {
                        System.err.println("Qcmp file conversion isn't supported yet");
                    }
                    break;
                    case Qvc: {
                        IQvcFile cacheFile = null;
                        try {
                            cacheFile = QvcFileReader.readCacheFile(parsedOptions.getInputDataInfo().getFilePath());
                        } catch (final IOException e) {
                            System.err.println("Unable to read QVC file. Error: " + e.getMessage());
                            exitApplication(1);
                        }
                        final boolean inPlace = parsedOptions.getOutputFilePath() == null;
                        try {
                            assert (cacheFile != null);
                            cacheFile.convertToNewerVersion(inPlace, parsedOptions.getInputDataInfo().getFilePath(),
                                                            parsedOptions.getOutputFilePath());
                        } catch (final IOException e) {
                            System.err.println("Unable to convert specified QVC file. Error: " + e.getMessage());
                            exitApplication(1);
                        }

                        if (parsedOptions.isVerbose()) {
                            System.err.println("Qvc file is converted.");
                        }
                    }
                    break;
                    case InvalidPath:
                        exitWithInvalidFilePath();
                        break;
                    case Unknown:
                        exitWithUnknownFile();
                        break;
                }
            }
            break;
        }
        ScifioWrapper.dispose();
    }

    private static void exitWithUnknownFile() {
        ColorConsole.fprintf(ColorConsole.Target.stderr,
                             ColorConsole.Color.Red,
                             "Provided file is of unknown type. Only QCMP and QVC files are supported.\n");

        exitApplication(1);
    }

    private static void exitWithInvalidFilePath() {
        ColorConsole.fprintf(ColorConsole.Target.stderr,
                             ColorConsole.Color.Red,
                             "File specified by provided path doesn't exist.\n");
        exitApplication(1);
    }

    private static void exitApplication(final int exitCode) {
        ScifioWrapper.dispose();
        System.exit(exitCode);
    }
}
