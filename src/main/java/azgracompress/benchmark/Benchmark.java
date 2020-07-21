package azgracompress.benchmark;

import azgracompress.cli.ParsedCliOptions;

public class Benchmark extends BenchmarkBase {


    protected Benchmark(ParsedCliOptions options) {
        super(options);
    }

    @Override
    public void startBenchmark() {

        // TODO(Moravec): Support PlaneLoader API.
//        ParsedCliOptions compressOps;
//        ParsedCliOptions decompressOps;
//        try {
//            compressOps = (ParsedCliOptions) options.clone();
//            decompressOps = (ParsedCliOptions) options.clone();
//        } catch (CloneNotSupportedException e) {
//            e.printStackTrace();
//            return;
//        }
//
//        boolean dirCreated = new File(options.getOutputFilePath()).mkdirs();
//        //"%d_cb%d.raw.qcmp"
//        final String qcmpFilePath = getFileNamePathIntoOutDir(String.format(COMPRESSED_FILE_TEMPLATE,
//                                                                            options.getPlaneIndex(),
//                                                                            codebookSize));
//        compressOps.setOutputFilePath(qcmpFilePath);
//        ImageCompressor compressor = new ImageCompressor(compressOps);
//        if (!compressor.compress()) {
//            System.err.println("Errors occurred during compression.");
//            return;
//        }
//
//        decompressOps.setInputFileInfo(new InputFileInfo(qcmpFilePath));
//
//
//        final String decompressedFile = getFileNamePathIntoOutDir(String.format(QUANTIZED_FILE_TEMPLATE,
//                                                                                options.getPlaneIndex(),
//                                                                                codebookSize));
//
//        decompressOps.setOutputFilePath(decompressedFile);
//        ImageDecompressor decompressor = new ImageDecompressor(decompressOps);
//        if (!decompressor.decompress()) {
//            System.err.println("Errors occurred during decompression.");
//        }
//
//        final int[] originalData;
//        try {
//            originalData = RawDataIO.loadImageU16(options.getInputFilePath(),
//                                                  options.getImageDimension(),
//                                                  options.getPlaneIndex()).getData();
//        } catch (IOException e) {
//            e.printStackTrace();
//            return;
//        }
//        final int[] quantizedData;
//        try {
//            quantizedData = RawDataIO.loadImageU16(decompressedFile,
//                                                   options.getImageDimension().toV2i().toV3i(), 0).getData();
//        } catch (IOException e) {
//            e.printStackTrace();
//            return;
//        }
//
//        final int[] diffArray = Utils.getDifference(originalData, quantizedData);
//        final double mse = Utils.calculateMse(diffArray);
//        final double PSNR = Utils.calculatePsnr(mse, U16.Max);
//        System.out.println(String.format("MSE: %.4f\tPSNR: %.4f(dB)", mse, PSNR));
//
//        final int[] absDifferenceData = Utils.asAbsoluteValues(diffArray);
//
//        final String diffFilePath = getFileNamePathIntoOutDir(String.format(DIFFERENCE_FILE_TEMPLATE,
//                                                                        options.getPlaneIndex(),
//                                                                        codebookSize));
//
//        final String absDiffFilePath = getFileNamePathIntoOutDir(String.format(ABSOLUTE_DIFFERENCE_FILE_TEMPLATE,
//                                                                           options.getPlaneIndex(),
//                                                                           codebookSize));
//
//        ImageU16 img = new ImageU16(rawImageDims.getX(),
//                                    rawImageDims.getY(),
//                                    absDifferenceData);
//        try {
//            // NOTE(Moravec): Use little endian so that gnuplot can read the array.
//            RawDataIO.writeImageU16(absDiffFilePath, img, true);
//            System.out.println("Saved absolute difference to: " + absDiffFilePath);
//
//            RawDataIO.writeDataI32(diffFilePath, diffArray, true);
//            System.out.println("Saved difference to: " + absDiffFilePath);
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.err.println("Failed to save difference.");
//            return;
//        }
    }
}