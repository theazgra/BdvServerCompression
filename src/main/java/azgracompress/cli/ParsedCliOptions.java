package azgracompress.cli;

import azgracompress.compression.CompressorDecompressorBase;
import azgracompress.data.V2i;
import azgracompress.data.V3i;
import azgracompress.fileformat.FileExtensions;
import azgracompress.fileformat.QuantizationType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Paths;

public class ParsedCliOptions {

    private static final int DEFAULT_BITS_PER_PIXEL = 8;

    private ProgramMethod method;
    private QuantizationType quantizationType;

    private String inputFile;
    private String outputFile;
    private String codebookCacheFolder = null;

    private int bitsPerPixel;
    private V2i vectorDimension = new V2i(0);
    private V3i imageDimension = new V3i(0);

    private boolean planeIndexSet = false;
    private int planeIndex;

    private boolean refPlaneIndexSet = false;
    private int referencePlaneIndex = -1;

    private boolean verbose;

    private boolean errorOccurred;
    private String error;

    private boolean planeRangeSet = false;
    private int fromPlaneIndex;
    private int toPlaneIndex;

    private int workerCount = 1;

    public ParsedCliOptions(CommandLine cmdInput) {
        parseCLI(cmdInput);
    }

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

    private void parseCLI(final CommandLine cmd) {
        StringBuilder errorBuilder = new StringBuilder("Errors:\n");
        errorOccurred = false;

        parseProgramMethod(cmd, errorBuilder);
        if (method == ProgramMethod.PrintHelp)
            return;

        parseCompressionType(cmd, errorBuilder);

        parseBitsPerPixel(cmd, errorBuilder);

        parseReferencePlaneIndex(cmd, errorBuilder);

        final String[] fileInfo = cmd.getArgs();
        parseInputFilePart(errorBuilder, fileInfo);

        verbose = cmd.hasOption(CliConstants.VERBOSE_LONG);

        if (cmd.hasOption(CliConstants.WORKER_COUNT_LONG)) {
            final String wcString = cmd.getOptionValue(CliConstants.WORKER_COUNT_LONG);
            ParseResult<Integer> pr = tryParseInt(wcString);
            if (pr.isSuccess()) {
                workerCount = pr.getValue();
            } else {
                errorOccurred = true;
                errorBuilder.append("Unable to parse worker count. Expected int got: ").append(wcString).append('\n');
            }
        }

        codebookCacheFolder = cmd.getOptionValue(CliConstants.CODEBOOK_CACHE_FOLDER_LONG, null);

        if (!errorOccurred) {
            outputFile = cmd.getOptionValue(CliConstants.OUTPUT_LONG, getDefaultOutputFilePath(inputFile));
        }

        error = errorBuilder.toString();
    }

    private void parseInputFilePart(StringBuilder errorBuilder, final String[] inputFileArguments) {

        if (inputFileArguments.length < 1) {
            errorOccurred = true;
            errorBuilder.append("Missing input file option");
            return;
        }

        inputFile = inputFileArguments[0];

        // Decompress and Inspect methods doesn't require additional file information.
        if ((method == ProgramMethod.Decompress) || (method == ProgramMethod.InspectFile)) {
            return;
        }

        File inputFileInfo = new File(inputFile);

        // Check if input file exists.
        if (!inputFileInfo.exists()) {
            errorOccurred = true;
            errorBuilder.append("Input file doesn't exist.\n");
            return;
        }

        final String extension = FilenameUtils.getExtension(inputFile).toLowerCase();
        switch (extension) {
            case FileExtensions.RAW: {
                parseRawFileArguments(errorBuilder, inputFileArguments);
            }
            break;
            case FileExtensions.TIFF: {
                parseTiffFileArguments(errorBuilder, inputFileArguments);
            }
            break;
            default: {
                errorOccurred = true;
                errorBuilder.append("Unsupported input file type. Currently supporting RAW and TIFF files.\n");
            }
        }
    }

    private void parseTiffFileArguments(StringBuilder errorBuilder, String[] inputFileArguments) {
        errorOccurred = true;
        errorBuilder.append("Got TIFF file.\n");
    }

    private void parseRawFileArguments(StringBuilder errorBuilder, String[] inputFileArguments) {
        // We require the file path and dimensions, like input.raw 1920x1080x5
        // First argument is input file name.
        if (inputFileArguments.length < 2) {
            errorOccurred = true;
            errorBuilder.append("Raw file requires its dimension as additional information.")
                    .append("e.g.: 1920x1080x1\n");
            return;
        }

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
                fromPlaneIndex = indexFromResult.getValue();
                toPlaneIndex = indexToResult.getValue();
                planeRangeSet = true;
            } else {
                errorOccurred = true;
                errorBuilder.append("Plane range index is wrong. Expected format D-D, got: ").append(
                        inputFileArguments[inputFileArgumentsOffset]).append('\n');
            }
        } else {
            // Here we parse single plane index option.
            final ParseResult<Integer> parseResult = tryParseInt(inputFileArguments[inputFileArgumentsOffset]);
            if (parseResult.isSuccess()) {
                planeIndexSet = true;
                planeIndex = parseResult.getValue();
            } else {
                errorOccurred = true;
                errorBuilder.append("Failed to parse plane index option, expected integer, got: ")
                        .append(inputFileArguments[inputFileArgumentsOffset])
                        .append('\n');
            }
        }
    }

    /**
     * Parse RAW image file dimensions (e.g. 1920x1080x10).
     *
     * @param dimsString   String containing image dimensions.
     * @param errorBuilder Builder for the error message.
     */
    private void parseImageDims(final String dimsString, StringBuilder errorBuilder) {
        // We thing of 3x3x1 and 3x3 as the same thing

        final int firstDelimiterIndex = dimsString.indexOf('x');
        if (firstDelimiterIndex == -1) {
            errorOccurred = true;
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
                imageDimension = new V3i(n1Result.getValue(), n2Result.getValue(), 1);
            } else {
                errorOccurred = true;
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
                imageDimension = new V3i(n1Result.getValue(), n2Result.getValue(), n3Result.getValue());
            } else {
                errorOccurred = true;
                errorBuilder.append("Failed to parse image dimensions of format DxDxD, got: ");
                errorBuilder.append(String.format("%sx%sx%s\n", num1String, num2String, num3String));
            }
        }
    }

    private void parseReferencePlaneIndex(CommandLine cmd, StringBuilder errorBuilder) {
        if (cmd.hasOption(CliConstants.REFERENCE_PLANE_LONG)) {
            final String rpString = cmd.getOptionValue(CliConstants.REFERENCE_PLANE_LONG);
            final ParseResult<Integer> parseResult = tryParseInt(rpString);
            if (parseResult.isSuccess()) {
                referencePlaneIndex = parseResult.getValue();
                refPlaneIndexSet = true;
            } else {
                errorOccurred = true;
                errorBuilder.append("Failed to parse reference plane index").append('\n');
                errorBuilder.append(parseResult.getErrorMessage()).append('\n');
            }
        } else {
            refPlaneIndexSet = false;
        }
    }

    private void parseBitsPerPixel(CommandLine cmd, StringBuilder errorBuilder) {
        if (cmd.hasOption(CliConstants.BITS_LONG)) {
            final String bitsString = cmd.getOptionValue(CliConstants.BITS_LONG);
            final ParseResult<Integer> parseResult = tryParseInt(bitsString);
            if (parseResult.isSuccess()) {
                bitsPerPixel = parseResult.getValue();
            } else {
                errorOccurred = true;
                errorBuilder.append("Failed to parse bits per pixel.").append('\n');
                errorBuilder.append(parseResult.getErrorMessage()).append('\n');
            }
        } else {
            bitsPerPixel = DEFAULT_BITS_PER_PIXEL;
        }
    }

    private boolean hasQuantizationType(final ProgramMethod method) {
        return (method == ProgramMethod.Compress) ||
                (method == ProgramMethod.Benchmark) ||
                (method == ProgramMethod.TrainCodebook);
    }

    private void parseCompressionType(CommandLine cmd, StringBuilder errorBuilder) {
        if (hasQuantizationType(method)) {

            if (cmd.hasOption(CliConstants.SCALAR_QUANTIZATION_LONG)) {
                quantizationType = QuantizationType.Scalar;
            } else if (cmd.hasOption(CliConstants.VECTOR_QUANTIZATION_LONG)) {
                final String vectorDefinition = cmd.getOptionValue(CliConstants.VECTOR_QUANTIZATION_LONG);

                final int delimiterIndex = vectorDefinition.indexOf('x');
                if (delimiterIndex == -1) {
                    final ParseResult<Integer> parseResult = tryParseInt(vectorDefinition);
                    if (parseResult.isSuccess()) {
                        quantizationType = QuantizationType.Vector1D;
                        vectorDimension = new V2i(parseResult.getValue(), 1);
                    } else {
                        errorOccurred = true;
                        errorBuilder.append("1D vector quantization requires vector size").append('\n').append(
                                parseResult.getErrorMessage()).append('\n');
                    }
                } else {
                    final String firstNumberString = vectorDefinition.substring(0, delimiterIndex);
                    final String secondNumberString = vectorDefinition.substring(delimiterIndex + 1);

                    final ParseResult<Integer> firstNumberParseResult = tryParseInt(firstNumberString);
                    final ParseResult<Integer> secondNumberParseResult = tryParseInt(secondNumberString);
                    if (firstNumberParseResult.isSuccess() && secondNumberParseResult.isSuccess()) {
                        vectorDimension = new V2i(firstNumberParseResult.getValue(),
                                                  secondNumberParseResult.getValue());

                        if ((vectorDimension.getX() <= 0) || (vectorDimension.getY() <= 0)) {
                            errorOccurred = true;
                            errorBuilder.append("Wrong quantization vector: ").append(vectorDimension.toString());
                        } else {
                            if ((vectorDimension.getX() > 1) && (vectorDimension.getY() == 1)) {
                                quantizationType = QuantizationType.Vector1D;
                            } else if ((vectorDimension.getX() == 1) && (vectorDimension.getY() > 1)) {
                                // This is actually Vector1D, but Vector2D implementation works here just fine.
                                quantizationType = QuantizationType.Vector2D;
                            } else {
                                quantizationType = QuantizationType.Vector2D;
                            }
                        }
                    } else {
                        errorOccurred = true;
                        errorBuilder.append("Failed to parse vector dimension. Expected DxD, got: ").append(
                                vectorDefinition);
                    }
                }
            } else {
                errorOccurred = true;
                errorBuilder.append("Quantization type wasn't set for compression").append('\n');
            }
        }
    }

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
            errorOccurred = true;
            errorBuilder.append("No program method was matched\n");
        }
    }

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

    public QuantizationType getQuantizationType() {
        return quantizationType;
    }

    public String getInputFile() {
        return inputFile;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public int getBitsPerPixel() {
        return bitsPerPixel;
    }

    public V2i getVectorDimension() {
        return vectorDimension;
    }

    public V3i getImageDimension() {
        return imageDimension;
    }

    public int getPlaneIndex() {
        return planeIndex;
    }

    public int getReferencePlaneIndex() {
        return referencePlaneIndex;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean hasReferencePlaneIndex() {
        return refPlaneIndexSet;
    }

    public boolean hasPlaneIndexSet() {
        return planeIndexSet;
    }

    public boolean hasErrorOccured() {
        return errorOccurred;
    }

    public String getError() {
        return error;
    }

    public boolean hasPlaneRangeSet() {
        return planeRangeSet;
    }

    public int getFromPlaneIndex() {
        return fromPlaneIndex;
    }

    public int getToPlaneIndex() {
        return toPlaneIndex;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public String getCodebookCacheFolder() {
        return codebookCacheFolder;
    }

    public boolean hasCodebookCacheFolder() {
        return (codebookCacheFolder != null);
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
            case PrintHelp:
                sb.append("PrintHelp\n");
                break;
            case InspectFile:
                sb.append("InspectFile\n");
                break;
        }


        if (hasQuantizationType(method)) {
            sb.append("Quantization type: ");
            switch (quantizationType) {
                case Scalar:
                    sb.append("Scalar\n");
                    break;
                case Vector1D:
                    sb.append(String.format("Vector1D %s\n", vectorDimension.toString()));
                    break;
                case Vector2D:
                    sb.append(String.format("Vector2D %s\n", vectorDimension.toString()));
                    break;
            }
        }


        sb.append("BitsPerPixel: ").append(bitsPerPixel).append('\n');
        sb.append("Output: ").append(outputFile).append('\n');
        sb.append("InputFile: ").append(inputFile).append('\n');
        if (hasCodebookCacheFolder()) {
            sb.append("CodebookCacheFolder: ").append(codebookCacheFolder).append('\n');
        }

        if (hasQuantizationType(method)) {
            sb.append("Input image dims: ").append(imageDimension.toString()).append('\n');
        }

        if (planeIndexSet) {
            sb.append("PlaneIndex: ").append(planeIndex).append('\n');
        }
        if (refPlaneIndexSet) {
            sb.append("ReferencePlaneIndex: ").append(referencePlaneIndex).append('\n');
        }
        if (planeRangeSet) {
            sb.append("FromPlaneIndex: ").append(fromPlaneIndex).append('\n');
            sb.append("ToPlaneIndex: ").append(toPlaneIndex).append('\n');
        }

        sb.append("Verbose: ").append(verbose).append('\n');
        sb.append("ThreadWorkerCount: ").append(workerCount).append('\n');

        return sb.toString();
    }

    /**
     * Get number of planes to be compressed.
     *
     * @return Number of planes for compression.
     */
    public int getNumberOfPlanes() {
        if (hasPlaneIndexSet()) {
            return 1;
        } else if (hasPlaneRangeSet()) {
            return ((toPlaneIndex + 1) - fromPlaneIndex);
        } else {
            return imageDimension.getZ();
        }
    }
}
