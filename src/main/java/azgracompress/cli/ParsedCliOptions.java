package azgracompress.cli;

import azgracompress.compression.CompressionOptions;
import azgracompress.ScifioWrapper;
import azgracompress.compression.CompressorDecompressorBase;
import azgracompress.data.V2i;
import azgracompress.data.V3i;
import azgracompress.fileformat.FileExtensions;
import azgracompress.fileformat.QuantizationType;
import io.scif.FormatException;
import io.scif.Plane;
import io.scif.Reader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class ParsedCliOptions extends CompressionOptions implements Cloneable {
    private static final int DEFAULT_BITS_PER_PIXEL = 8;

    /**
     * Chosen program method.
     */
    private ProgramMethod method;

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

    private String removeQCMPFileExtension(final String originalPath) {
        if (originalPath.toUpperCase().endsWith(CompressorDecompressorBase.EXTENSION)) {
            return originalPath.substring(0, originalPath.length() - CompressorDecompressorBase.EXTENSION.length());
        }
        return originalPath;
    }

    /**
     * Creates default output file path depending on the chosen program method.
     *
     * @param inputPath Input file path.
     * @return Default ouput file path.
     */
    private String getDefaultOutputFilePath(final String inputPath) {
        // No default output file for custom function.
        if (method == ProgramMethod.CustomFunction)
            return "";

        final File inputFile = new File(inputPath);
        final File outputFile = new File(Paths.get("").toAbsolutePath().toString(), inputFile.getName());


        // Default value is current directory with input file name.
        String defaultValue = outputFile.getAbsolutePath();

        switch (method) {
            case Compress: {
                // Add compressed file extension.
                defaultValue += CompressorDecompressorBase.EXTENSION;
            }
            break;
            case Decompress: {
                // If it ends with QCMP file extension remove the extension.
                defaultValue = removeQCMPFileExtension(defaultValue);
                // Remove the old extension and add RAW extension
                defaultValue = defaultValue.replace(FilenameUtils.getExtension(defaultValue),
                        CompressorDecompressorBase.RAW_EXTENSION_NO_DOT);

            }
            break;
            case Benchmark: {
                defaultValue = new File(inputFile.getParent(), "benchmark").getAbsolutePath();
            }
            break;
            case InspectFile:
                defaultValue += ".txt";
                break;
            case TrainCodebook:
            case PrintHelp:
            case CustomFunction:
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

        setVerbose(cmd.hasOption(CliConstants.VERBOSE_LONG));

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
            setOutputFilePath(cmd.getOptionValue(CliConstants.OUTPUT_LONG, getDefaultOutputFilePath(getInputFileInfo().getFilePath())));
            setCodebookCacheFolder(cmd.getOptionValue(CliConstants.CODEBOOK_CACHE_FOLDER_LONG, null));
        }

        parseError = errorBuilder.toString();
    }


    /**
     * Parse input file info from command line arguments.
     *
     * @param errorBuilder       String error builder.
     * @param inputFileArguments Input file info strings.
     */

    private void parseInputFilePart(StringBuilder errorBuilder, final String[] inputFileArguments) {

        if (inputFileArguments.length < 1) {
            parseErrorOccurred = true;
            errorBuilder.append("Missing input file option");
            return;
        }

        setInputFileInfo(new InputFileInfo(inputFileArguments[0]));

        // Decompress and Inspect methods doesn't require additional file information.
        if ((method == ProgramMethod.Decompress) || (method == ProgramMethod.InspectFile)) {
            return;
        }

        // Check if input file exists.
        if (!new File(getInputFileInfo().getFilePath()).exists()) {
            parseErrorOccurred = true;
            errorBuilder.append("Input file doesn't exist.\n");
            return;
        }

        final String extension = FilenameUtils.getExtension(inputFileArguments[0]).toLowerCase();
        if (FileExtensions.RAW.equals(extension)) {
            parseRawFileArguments(errorBuilder, inputFileArguments);
        } else {
            // Default loading through SCIFIO.
            parseSCIFIOFileArguments(errorBuilder, inputFileArguments);
        }
    }


    private void parseSCIFIOFileArguments(StringBuilder errorBuilder, final String[] inputFileArguments) {

        getInputFileInfo().setIsRaw(false);
        Reader reader;
        try {
            reader = ScifioWrapper.getReader(getInputFileInfo().getFilePath());
        } catch (IOException | FormatException e) {
            parseErrorOccurred = true;
            errorBuilder.append("Failed to get SCIFIO reader for file.\n");
            errorBuilder.append(e.getMessage());
            return;
        }

        final int imageCount = reader.getImageCount();
        if (imageCount != 1) {
            parseErrorOccurred = true;
            errorBuilder.append("We are currently not supporting files with multiple images.\n");
            return;
        }

        final long planeCount = reader.getPlaneCount(0);
        if (planeCount > (long) Integer.MAX_VALUE) {
            parseErrorOccurred = true;
            errorBuilder.append("Too many planes.\n");
        }

        long planeWidth, planeHeight;
        try {
            Plane plane = reader.openPlane(0, 0);
            planeWidth = plane.getLengths()[0];
            planeHeight = plane.getLengths()[1];

            if ((planeWidth > (long) Integer.MAX_VALUE) ||
                    (planeHeight > (long) Integer.MAX_VALUE)) {
                parseErrorOccurred = true;
                errorBuilder.append("We are currently supporting planes with maximum size of Integer.MAX_VALUE x Integer.MAX_VALUE");
            }


        } catch (FormatException | IOException e) {
            parseErrorOccurred = true;
            errorBuilder.append("Unable to open first plane of the first image.\n")
                    .append(e.getMessage());
            return;
        }

        getInputFileInfo().setDimension(new V3i(
                (int) planeWidth,
                (int) planeHeight,
                (int) planeCount
        ));

        if (inputFileArguments.length > 1) {
            parseInputFilePlaneOptions(errorBuilder, inputFileArguments, 1);
        }
    }

    private void parseRawFileArguments(StringBuilder errorBuilder, String[] inputFileArguments) {
        // We require the file path and dimensions, like input.raw 1920x1080x5
        // First argument is input file name.
        if (inputFileArguments.length < 2) {
            parseErrorOccurred = true;
            errorBuilder.append("Raw file requires its dimension as additional information.")
                    .append("e.g.: 1920x1080x1\n");
            return;
        }

        getInputFileInfo().setIsRaw(true);
        parseImageDims(inputFileArguments[1], errorBuilder);

        // User specified plane index or plane range.
        if (inputFileArguments.length > 2) {
            parseInputFilePlaneOptions(errorBuilder, inputFileArguments, 2);
        }
    }

    /**
     * Parse optional user specified plane index or plane range. (e.g. 5 or 5-50)
     *
     * @param errorBuilder             String builder for the error message.
     * @param inputFileArguments       Input file arguments.
     * @param inputFileArgumentsOffset Offset of the plane argument.
     */
    private void parseInputFilePlaneOptions(StringBuilder errorBuilder,
                                            final String[] inputFileArguments,
                                            final int inputFileArgumentsOffset) {
        int rangeSeparatorIndex = inputFileArguments[inputFileArgumentsOffset].indexOf("-");
        if (rangeSeparatorIndex != -1) {
// Here we parse the plane range option.
            final String fromIndexString =
                    inputFileArguments[inputFileArgumentsOffset].substring(0, rangeSeparatorIndex);
            final String toIndexString =
                    inputFileArguments[inputFileArgumentsOffset].substring(rangeSeparatorIndex + 1);

            final ParseResult<Integer> indexFromResult = tryParseInt(fromIndexString);
            final ParseResult<Integer> indexToResult = tryParseInt(toIndexString);

            if (indexFromResult.isSuccess() && indexToResult.isSuccess()) {
                getInputFileInfo().setPlaneRange(new V2i(indexFromResult.getValue(), indexToResult.getValue()));
            } else {
                parseErrorOccurred = true;
                errorBuilder.append("Plane range index is wrong. Expected format D-D, got: ").append(
                        inputFileArguments[inputFileArgumentsOffset]).append('\n');
            }
        } else {
// Here we parse single plane index option.
            final ParseResult<Integer> parseResult = tryParseInt(inputFileArguments[inputFileArgumentsOffset]);
            if (parseResult.isSuccess()) {
                getInputFileInfo().setPlaneIndex(parseResult.getValue());
            } else {
                parseErrorOccurred = true;
                errorBuilder.append("Failed to parse plane index option, expected integer, got: ")
                        .append(inputFileArguments[inputFileArgumentsOffset])
                        .append('\n');
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

        final int firstDelimiterIndex = dimsString.indexOf('x');
        if (firstDelimiterIndex == -1) {
            parseErrorOccurred = true;
            errorBuilder.append("Error parsing image dimensions. We require DxDxD or DxD [=DxDx1]\n");
            return;
        }
        final String num1String = dimsString.substring(0, firstDelimiterIndex);
        final String secondPart = dimsString.substring(firstDelimiterIndex + 1);

        final int secondDelimiterIndex = secondPart.indexOf('x');
        if (secondDelimiterIndex == -1) {
            final ParseResult<Integer> n1Result = tryParseInt(num1String);
            final ParseResult<Integer> n2Result = tryParseInt(secondPart);
            if (n1Result.isSuccess() && n2Result.isSuccess()) {
                getInputFileInfo().setDimension(new V3i(n1Result.getValue(), n2Result.getValue(), 1));
            } else {
                parseErrorOccurred = true;
                errorBuilder.append("Failed to parse image dimensions of format DxD, got: ");
                errorBuilder.append(String.format("%sx%s\n", num1String, secondPart));
            }
        } else {
            final String num2String = secondPart.substring(0, secondDelimiterIndex);
            final String num3String = secondPart.substring(secondDelimiterIndex + 1);

            final ParseResult<Integer> n1Result = tryParseInt(num1String);
            final ParseResult<Integer> n2Result = tryParseInt(num2String);
            final ParseResult<Integer> n3Result = tryParseInt(num3String);

            if (n1Result.isSuccess() && n2Result.isSuccess() && n3Result.isSuccess()) {
                getInputFileInfo().setDimension(new V3i(n1Result.getValue(), n2Result.getValue(), n3Result.getValue()));
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
     *
     * @param cmd          Command line arguments.
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
     *
     * @param cmd          Command line arguments.
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
     *
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

    public boolean parseError() {
        return parseErrorOccurred;
    }

    public String getParseError() {
        return parseError;
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


        sb.append("InputFile: ").append(getInputFileInfo().getFilePath()).append('\n');
        sb.append("Output: ").append(getOutputFilePath()).append('\n');
        sb.append("BitsPerCodebookIndex: ").append(getBitsPerCodebookIndex()).append('\n');
        if (hasCodebookCacheFolder()) {
            sb.append("CodebookCacheFolder: ").append(getCodebookCacheFolder()).append('\n');
        }

        if (hasQuantizationType(method)) {

            sb.append("Input image dims: ").append(getInputFileInfo().getDimensions().toString()).append('\n');
        }
        if (getInputFileInfo().isPlaneIndexSet()) {
            sb.append("PlaneIndex: ").append(getInputFileInfo().getPlaneIndex()).append('\n');
        }

        if (shouldUseMiddlePlane()) {
            sb.append("Use middle plane for codebook training\n");
        }

        if (getInputFileInfo().isPlaneRangeSet()) {
            sb.append("FromPlaneIndex: ").append(getInputFileInfo().getPlaneRange().getX()).append('\n');
            sb.append("ToPlaneIndex: ").append(getInputFileInfo().getPlaneRange().getY()).append('\n');
        }

        sb.append("Verbose: ").append(isVerbose()).append('\n');
        sb.append("ThreadWorkerCount: ").append(getWorkerCount()).append('\n');

        return sb.toString();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
