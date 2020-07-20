package azgracompress.cli;

import azgracompress.compression.CompressionOptions;
import azgracompress.compression.CompressorDecompressorBase;
import azgracompress.compression.Interval;
import azgracompress.data.V2i;
import azgracompress.data.V3i;
import azgracompress.fileformat.QuantizationType;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.nio.file.Paths;

public class ParsedCliOptions extends CompressionOptions implements Cloneable {
    private static final int DEFAULT_BITS_PER_PIXEL = 8;

    /**
     * Chosen program method.
     */
    private ProgramMethod method;

    /**
     * Flag whether the CLI app should be verbose while running.
     */
    private boolean verbose;

    /**
     * Flag whether parse error occurred.
     */
    private boolean parseErrorOccurred;

    /**
     * Parse error information.
     */
    private String parseError;

    /**
     * Parse provided command line arguments.
     *
     * @param cmdInput Command line arguments.
     */
    public ParsedCliOptions(CommandLine cmdInput) {
        parseCLI(cmdInput);
    }

    /**
     * Creates default output file path depending on the chosen program method.
     *
     * @param inputPath Input file path.
     * @return Default ouput file path.
     */
    private String getDefaultOutputFilePath(final String inputPath) {
        if (method == ProgramMethod.CustomFunction)
            return "";

        final File inputFile = new File(inputPath);
        final File outputFile = new File(Paths.get("").toAbsolutePath().toString(), inputFile.getName());


        String defaultValue = outputFile.getAbsolutePath();

        switch (method) {
            case Compress: {
                defaultValue += CompressorDecompressorBase.EXTENSION;
            }
            break;
            case Decompress: {
                if (defaultValue.toUpperCase().endsWith(CompressorDecompressorBase.EXTENSION)) {
                    defaultValue = defaultValue.substring(0,
                            defaultValue.length() - CompressorDecompressorBase.EXTENSION.length());
                }
            }
            break;
            case Benchmark: {
                defaultValue = new File(inputFile.getParent(), "benchmark").getAbsolutePath();
            }
            break;
            case PrintHelp:
                break;
            case InspectFile:
                defaultValue += ".txt";
                break;
        }

        return defaultValue;
    }

    /**
     * Parses provided command line arguments to program options.
     *
     * @param cmd Provided command line arguments.
     */
    private void parseCLI(final CommandLine cmd) {
        StringBuilder errorBuilder = new StringBuilder("Errors:\n");
        parseErrorOccurred = false;

        parseProgramMethod(cmd, errorBuilder);
        if (method == ProgramMethod.PrintHelp)
            return;

        parseCompressionType(cmd, errorBuilder);

        parseBitsPerPixel(cmd, errorBuilder);

        setUseMiddlePlane(cmd.hasOption(CliConstants.USE_MIDDLE_PLANE_LONG));

        final String[] fileInfo = cmd.getArgs();
        parseInputFilePart(errorBuilder, fileInfo);

        verbose = cmd.hasOption(CliConstants.VERBOSE_LONG);

        if (cmd.hasOption(CliConstants.WORKER_COUNT_LONG)) {
            final String wcString = cmd.getOptionValue(CliConstants.WORKER_COUNT_LONG);
            ParseResult<Integer> pr = tryParseInt(wcString);
            if (pr.isSuccess()) {
                setWorkerCount(pr.getValue());
            } else {
                parseErrorOccurred = true;
                errorBuilder.append("Unable to parse worker count. Expected int got: ").append(wcString).append('\n');
            }
        }
        setCodebookCacheFolder(cmd.getOptionValue(CliConstants.CODEBOOK_CACHE_FOLDER_LONG, null));

        if (!parseErrorOccurred) {
            setOutputFilePath(cmd.getOptionValue(CliConstants.OUTPUT_LONG, getDefaultOutputFilePath(getInputFilePath())));
        }

        parseError = errorBuilder.toString();
    }

    /**
     * Parse input file info from command line arguments.
     *
     * @param errorBuilder String error builder.
     * @param fileInfo     Input file info strings.
     */
    private void parseInputFilePart(StringBuilder errorBuilder, final String[] fileInfo) {
        if ((method == ProgramMethod.Decompress) || (method == ProgramMethod.InspectFile)) {
            if (fileInfo.length > 0) {
                setInputFilePath(fileInfo[0]);
            } else {
                parseErrorOccurred = true;
                errorBuilder.append("Missing input file for decompression");
            }
        } else {
            // Compression part.

            // We require the file path and dimensions, like input.raw 1920x1080x5
            if (fileInfo.length < 2) {
                if (method == ProgramMethod.CustomFunction) {
                    return;
                }
                parseErrorOccurred = true;
                errorBuilder.append("Both filepath and file dimensions are required arguments\n");
            } else {
                // The first string must be file path.
                setInputFilePath(fileInfo[0]);

                parseImageDims(fileInfo[1], errorBuilder);

                if (fileInfo.length > 2) {

                    int rangeSepIndex = fileInfo[2].indexOf("-");
                    if (rangeSepIndex != -1) {
                        final String fromIndexString = fileInfo[2].substring(0, rangeSepIndex);
                        final String toIndexString = fileInfo[2].substring(rangeSepIndex + 1);
                        final ParseResult<Integer> indexFromResult = tryParseInt(fromIndexString);
                        final ParseResult<Integer> indexToResult = tryParseInt(toIndexString);

                        if (indexFromResult.isSuccess() && indexToResult.isSuccess()) {
                            setPlaneRange(new Interval<>(indexFromResult.getValue(), indexToResult.getValue()));
                        } else {
                            parseErrorOccurred = true;
                            errorBuilder.append("Plane range index is wrong. Expected format D-D, got: ").append(
                                    fileInfo[2]).append('\n');
                        }
                    } else {
                        final ParseResult<Integer> parseResult = tryParseInt(fileInfo[2]);
                        if (parseResult.isSuccess()) {
                            setPlaneIndex(parseResult.getValue());
                        } else {
                            parseErrorOccurred = true;
                            errorBuilder.append("The second argument after file name must be plane index\n");
                        }
                    }
                }
            }
        }
    }

    /**
     * Parse image dimensions from the command line.
     *
     * @param dimsString   Dimensions string.
     * @param errorBuilder String error builder.
     */
    private void parseImageDims(final String dimsString, StringBuilder errorBuilder) {
        // We thing of 3x3x1 and 3x3 as the same thing

        final int firstDelimIndex = dimsString.indexOf('x');
        if (firstDelimIndex == -1) {
            parseErrorOccurred = true;
            errorBuilder.append("Error parsing image dimensions. We require DxDxD or DxD [=DxDx1]\n");
            return;
        }
        final String num1String = dimsString.substring(0, firstDelimIndex);
        final String secondPart = dimsString.substring(firstDelimIndex + 1);

        final int secondDelimIndex = secondPart.indexOf('x');
        if (secondDelimIndex == -1) {
            final ParseResult<Integer> n1Result = tryParseInt(num1String);
            final ParseResult<Integer> n2Result = tryParseInt(secondPart);
            if (n1Result.isSuccess() && n2Result.isSuccess()) {
                setImageDimension(new V3i(n1Result.getValue(), n2Result.getValue(), 1));
            } else {
                parseErrorOccurred = true;
                errorBuilder.append("Failed to parse image dimensions of format DxD, got: ");
                errorBuilder.append(String.format("%sx%s\n", num1String, secondPart));
            }
        } else {
            final String num2String = secondPart.substring(0, secondDelimIndex);
            final String num3String = secondPart.substring(secondDelimIndex + 1);

            final ParseResult<Integer> n1Result = tryParseInt(num1String);
            final ParseResult<Integer> n2Result = tryParseInt(num2String);
            final ParseResult<Integer> n3Result = tryParseInt(num3String);

            if (n1Result.isSuccess() && n2Result.isSuccess() && n3Result.isSuccess()) {
                setImageDimension(new V3i(n1Result.getValue(), n2Result.getValue(), n3Result.getValue()));
            } else {
                parseErrorOccurred = true;
                errorBuilder.append("Failed to parse image dimensions of format DxDxD, got: ");
                errorBuilder.append(String.format("%sx%sx%s\n", num1String, num2String, num3String));
            }
        }

    }

    /**
     * Parse bits per codebook index.
     *
     * @param cmd          Command line arguments.
     * @param errorBuilder String error builder.
     */
    private void parseBitsPerPixel(CommandLine cmd, StringBuilder errorBuilder) {
        if (cmd.hasOption(CliConstants.BITS_LONG)) {
            final String bitsString = cmd.getOptionValue(CliConstants.BITS_LONG);
            final ParseResult<Integer> parseResult = tryParseInt(bitsString);
            if (parseResult.isSuccess()) {
                setBitsPerCodebookIndex(parseResult.getValue());
            } else {
                parseErrorOccurred = true;
                errorBuilder.append("Failed to parse bits per pixel.").append('\n');
                errorBuilder.append(parseResult.getErrorMessage()).append('\n');
            }
        } else {
            setBitsPerCodebookIndex(DEFAULT_BITS_PER_PIXEL);
        }
    }

    /**
     * Check if quantization type option is required for chosen program method..
     *
     * @param method Chosen program method.
     * @return True if quantization type option is required.
     */
    private boolean hasQuantizationType(final ProgramMethod method) {
        return (method == ProgramMethod.Compress) ||
                (method == ProgramMethod.Benchmark) ||
                (method == ProgramMethod.TrainCodebook);
    }

    /**
     * Parse compression type and vector dimensions for VQ.
     * @param cmd Command line arguments.
     * @param errorBuilder String error builder.
     */
    private void parseCompressionType(CommandLine cmd, StringBuilder errorBuilder) {
        if (hasQuantizationType(method)) {

            if (cmd.hasOption(CliConstants.SCALAR_QUANTIZATION_LONG)) {
                setQuantizationType(QuantizationType.Scalar);
            } else if (cmd.hasOption(CliConstants.VECTOR_QUANTIZATION_LONG)) {
                final String vectorDefinition = cmd.getOptionValue(CliConstants.VECTOR_QUANTIZATION_LONG);

                final int delimiterIndex = vectorDefinition.indexOf('x');
                if (delimiterIndex == -1) {
                    final ParseResult<Integer> parseResult = tryParseInt(vectorDefinition);
                    if (parseResult.isSuccess()) {
                        setQuantizationType(QuantizationType.Vector1D);
                        setVectorDimension(new V2i(parseResult.getValue(), 1));
                    } else {
                        parseErrorOccurred = true;
                        errorBuilder.append("1D vector quantization requires vector size").append('\n').append(
                                parseResult.getErrorMessage()).append('\n');
                    }
                } else {
                    final String firstNumberString = vectorDefinition.substring(0, delimiterIndex);
                    final String secondNumberString = vectorDefinition.substring(delimiterIndex + 1);

                    final ParseResult<Integer> firstNumberParseResult = tryParseInt(firstNumberString);
                    final ParseResult<Integer> secondNumberParseResult = tryParseInt(secondNumberString);
                    if (firstNumberParseResult.isSuccess() && secondNumberParseResult.isSuccess()) {
                        setVectorDimension(new V2i(firstNumberParseResult.getValue(), secondNumberParseResult.getValue()));

                        if ((getVectorDimension().getX() <= 0) || (getVectorDimension().getY() <= 0)) {
                            parseErrorOccurred = true;
                            errorBuilder.append("Wrong quantization vector: ").append(getVectorDimension().toString());
                        } else {
                            if ((getVectorDimension().getX() > 1) && (getVectorDimension().getY() == 1)) {
                                setQuantizationType(QuantizationType.Vector1D);
                            } else if ((getVectorDimension().getX() == 1) && (getVectorDimension().getY() > 1)) {
                                // This is actually Vector1D, but Vector2D implementation works here just fine.
                                setQuantizationType(QuantizationType.Vector2D);
                            } else {
                                setQuantizationType(QuantizationType.Vector2D);
                            }
                        }
                    } else {
                        parseErrorOccurred = true;
                        errorBuilder.append("Failed to parse vector dimension. Expected DxD, got: ").append(
                                vectorDefinition);
                    }
                }
            } else {
                parseErrorOccurred = true;
                errorBuilder.append("Quantization type wasn't set for compression").append('\n');
            }
        }
    }

    /**
     * Parse chosen program method.
     * @param cmd Command line arguments.
     * @param errorBuilder String error builder.
     */
    private void parseProgramMethod(CommandLine cmd, StringBuilder errorBuilder) {
        if (cmd.hasOption(CliConstants.HELP_LONG)) {
            method = ProgramMethod.PrintHelp;
        } else if (cmd.hasOption(CliConstants.COMPRESS_LONG)) {
            method = ProgramMethod.Compress;
        } else if (cmd.hasOption(CliConstants.DECOMPRESS_LONG)) {
            method = ProgramMethod.Decompress;
        } else if (cmd.hasOption(CliConstants.BENCHMARK_LONG)) {
            method = ProgramMethod.Benchmark;
        } else if (cmd.hasOption(CliConstants.TRAIN_LONG)) {
            method = ProgramMethod.TrainCodebook;
        } else if (cmd.hasOption(CliConstants.INSPECT_LONG)) {
            method = ProgramMethod.InspectFile;
        } else if (cmd.hasOption(CliConstants.CUSTOM_FUNCTION_LONG)) {
            method = ProgramMethod.CustomFunction;
        } else {
            parseErrorOccurred = true;
            errorBuilder.append("No program method was matched\n");
        }
    }

    /**
     * Try to parse int from string.
     * @param string Possible integer value.
     * @return Parse result.
     */
    private ParseResult<Integer> tryParseInt(final String string) {
        try {
            final int result = Integer.parseInt(string);
            return new ParseResult<>(result);
        } catch (NumberFormatException e) {
            return new ParseResult<>(e.getMessage());
        }
    }

    public ProgramMethod getMethod() {
        return method;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean parseError() {
        return parseErrorOccurred;
    }

    public String getParseError() {
        return parseError;
    }

    public boolean hasCodebookCacheFolder() {
        return (getCodebookCacheFolder() != null);
    }

    public String report() {
        StringBuilder sb = new StringBuilder();

        sb.append("Method: ");
        switch (method) {
            case Compress:
                sb.append("Compress\n");
                break;
            case Decompress:
                sb.append("Decompress\n");
                break;
            case Benchmark:
                sb.append("Benchmark\n");
                break;
            case TrainCodebook:
                sb.append("TrainCodebook\n");
                break;
            case PrintHelp:
                sb.append("PrintHelp\n");
                break;
            case CustomFunction:
                sb.append("CustomFunction\n");
                break;
            case InspectFile:
                sb.append("InspectFile\n");
                break;
        }


        if (hasQuantizationType(method)) {
            sb.append("Quantization type: ");
            switch (getQuantizationType()) {
                case Scalar:
                    sb.append("Scalar\n");
                    break;
                case Vector1D:
                    sb.append(String.format("Vector1D %s\n", getVectorDimension().toString()));
                    break;
                case Vector2D:
                    sb.append(String.format("Vector2D %s\n", getVectorDimension().toString()));
                    break;
            }
        }


        sb.append("InputFile: ").append(getInputFilePath()).append('\n');
        sb.append("Output: ").append(getOutputFilePath()).append('\n');
        sb.append("BitsPerCodebookIndex: ").append(getBitsPerCodebookIndex()).append('\n');
        if (hasCodebookCacheFolder()) {
            sb.append("CodebookCacheFolder: ").append(getCodebookCacheFolder()).append('\n');
        }

        if (hasQuantizationType(method)) {
            sb.append("Input image dims: ").append(getImageDimension().toString()).append('\n');
        }

        if (getPlaneIndex() != null) {
            sb.append("PlaneIndex: ").append(getPlaneIndex()).append('\n');
        }
        if (shouldUseMiddlePlane()) {
            sb.append("Use middle plane for codebook training\n");
        }
        if (isPlaneRangeSet()) {
            sb.append("FromPlaneIndex: ").append(getPlaneRange().getFrom()).append('\n');
            sb.append("ToPlaneIndex: ").append(getPlaneRange().getInclusiveTo()).append('\n');
        }

        sb.append("Verbose: ").append(verbose).append('\n');
        sb.append("ThreadWorkerCount: ").append(getWorkerCount()).append('\n');

        return sb.toString();
    }

    /**
     * Get number of planes to be compressed.
     *
     * @return Number of planes for compression.
     */
    public int getNumberOfPlanes() {
        if (getPlaneIndex() != null) {
            return 1;
        } else if (getPlaneRange() != null) {
            final Interval<Integer> planeRange = getPlaneRange();
            return ((planeRange.getInclusiveTo() + 1) - planeRange.getFrom());
        } else {
            return getImageDimension().getZ();
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
