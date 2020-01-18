package cli;

import compression.CompressorDecompressorBase;
import compression.data.V2i;
import compression.data.V3i;
import compression.fileformat.QuantizationType;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.nio.file.Paths;

public class ParsedCliOptions {

    private static final int DEFAULT_BITS_PER_PIXEL = 8;

    private ProgramMethod method;
    private QuantizationType quantizationType;

    private String inputFile;
    private String outputFile;

    private int bitsPerPixel;
    private V2i vectorDimension = new V2i(0);
    private V3i imageDimension = new V3i(0);

    private boolean planeIndexSet = false;
    private int planeIndex;

    private boolean refPlaneIndexSet = false;
    private int referencePlaneIndex;

    private boolean verbose;

    private boolean errorOccurred;
    private String error;

    public ParsedCliOptions(CommandLine cmdInput) {
        parseCLI(cmdInput);
    }

    private String getDefaultOutputFilePath(final String inputPath) {
        final File inputFile = new File(inputPath);
        final File outputFile = new File(Paths.get("").toAbsolutePath().toString(), inputFile.getName());

        String defaultValue = outputFile.getAbsolutePath();

        switch (method) {
            case Compress:
                defaultValue += CompressorDecompressorBase.EXTENSTION;
                break;
            case Decompress: {
                if (defaultValue.endsWith(CompressorDecompressorBase.EXTENSTION)) {
                    defaultValue = defaultValue.substring(0,
                                                          defaultValue.length() - CompressorDecompressorBase.EXTENSTION.length());
                }
            }
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

        if (!errorOccurred) {
            outputFile = cmd.getOptionValue(CliConstants.OUTPUT_LONG, getDefaultOutputFilePath(inputFile));
        }

        error = errorBuilder.toString();
    }

    private void parseInputFilePart(StringBuilder errorBuilder, final String[] fileInfo) {
        if ((method == ProgramMethod.Decompress) || (method == ProgramMethod.InspectFile)) {
            if (fileInfo.length > 0) {
                inputFile = fileInfo[0];
            } else {
                errorOccurred = true;
                errorBuilder.append("Missing input file for decompression");
            }
        } else {
            // Compression part.

            // We require the file path and dimensions, like input.raw 1920x1080x5
            if (fileInfo.length < 2) {
                errorOccurred = true;
                errorBuilder.append("Both filepath and file dimensions are required arguments\n");
            } else {
                // The first string must be file path.
                inputFile = fileInfo[0];

                parseImageDims(fileInfo[1], errorBuilder);

                if (fileInfo.length > 2) {
                    final var parseResult = tryParseInt(fileInfo[2]);
                    if (parseResult.isSuccess()) {
                        planeIndexSet = true;
                        planeIndex = parseResult.getValue();
                    } else {
                        errorOccurred = true;
                        errorBuilder.append("The second argument after file name must be plane index\n");
                    }
                }
            }
        }
    }

    private void parseImageDims(final String dimsString, StringBuilder errorBuilder) {
        // We thing of 3x3x1 and 3x3 as the same thing

        final int firstDelimIndex = dimsString.indexOf('x');
        if (firstDelimIndex == -1) {
            errorOccurred = true;
            errorBuilder.append("Error parsing image dimensions. We require DxDxD or DxD [=DxDx1]\n");
            return;
        }
        final String num1String = dimsString.substring(0, firstDelimIndex);
        final String secondPart = dimsString.substring(firstDelimIndex + 1);

        final int secondDelimIndex = secondPart.indexOf('x');
        if (secondDelimIndex == -1) {
            final var n1Result = tryParseInt(num1String);
            final var n2Result = tryParseInt(secondPart);
            if (n1Result.isSuccess() && n2Result.isSuccess()) {
                imageDimension = new V3i(n1Result.getValue(), n2Result.getValue(), 1);
            } else {
                errorOccurred = true;
                errorBuilder.append("Failed to parse image dimensions of format DxD, got: ");
                errorBuilder.append(String.format("%sx%s\n", num1String, secondPart));
            }
        } else {
            final String num2String = secondPart.substring(0, secondDelimIndex);
            final String num3String = secondPart.substring(secondDelimIndex + 1);

            final var n1Result = tryParseInt(num1String);
            final var n2Result = tryParseInt(num2String);
            final var n3Result = tryParseInt(num3String);

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
            final var parseResult = tryParseInt(rpString);
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
            final var parseResult = tryParseInt(bitsString);
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

    private void parseCompressionType(CommandLine cmd, StringBuilder errorBuilder) {
        if (method == ProgramMethod.Compress) {
            if (cmd.hasOption(CliConstants.SCALAR_QUANTIZATION_LONG)) {
                quantizationType = QuantizationType.Scalar;
            } else if (cmd.hasOption(CliConstants.VECTOR_QUANTIZATION_LONG)) {
                final String vectorDefinition = cmd.getOptionValue(CliConstants.VECTOR_QUANTIZATION_LONG);

                final int delimiterIndex = vectorDefinition.indexOf('x');
                if (delimiterIndex == -1) {
                    final var parseResult = tryParseInt(vectorDefinition);
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

                    final var firstNumberParseResult = tryParseInt(firstNumberString);
                    final var secondNumberParseResult = tryParseInt(secondNumberString);
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
                                errorOccurred = true;
                                errorBuilder.append("There is nothing wrong with the vector ").
                                        append(vectorDimension.toString()).append(
                                        " but we do not support column vectors yet").append('\n');
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
        } else if (cmd.hasOption(CliConstants.INSPECT_LONG)) {
            method = ProgramMethod.InspectFile;
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

        if (method == ProgramMethod.Compress) {
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
        sb.append("OutputDirectory: ").append(outputFile).append('\n');
        sb.append("InputFile: ").append(inputFile).append('\n');

        if (method == ProgramMethod.Compress) {
            sb.append("Input image dims: ").append(imageDimension.toString()).append('\n');
        }

        if (planeIndexSet) {
            sb.append("PlaneIndex: ").append(planeIndex).append('\n');
        }
        if (refPlaneIndexSet) {
            sb.append("ReferencePlaneIndex: ").append(referencePlaneIndex).append('\n');
        }


        return sb.toString();
    }
}
