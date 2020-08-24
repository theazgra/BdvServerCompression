package azgracompress.benchmark;

import azgracompress.U16;
import azgracompress.cli.CompressionOptionsCLIParser;
import azgracompress.compression.ImageCompressor;
import azgracompress.compression.ImageDecompressor;
import azgracompress.data.ImageU16Dataset;
import azgracompress.io.FileInputData;
import azgracompress.io.RawDataIO;
import azgracompress.io.loader.IPlaneLoader;
import azgracompress.io.loader.PlaneLoaderFactory;
import azgracompress.utilities.TypeConverter;
import azgracompress.utilities.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class Benchmark extends BenchmarkBase {


    protected Benchmark(CompressionOptionsCLIParser options) {
        super(options);
    }

    @Override
    public void startBenchmark() {
        assert (options.getInputDataInfo().isPlaneIndexSet());
        CompressionOptionsCLIParser compressOps;
        CompressionOptionsCLIParser decompressOps;
        try {
            // NOTE: This works, right?
            compressOps = (CompressionOptionsCLIParser) options.clone();
            decompressOps = (CompressionOptionsCLIParser) options.clone();
        } catch (CloneNotSupportedException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return;
        }
        // dirCreated is ignored.
        boolean dirCreated = new File(options.getOutputFilePath()).mkdirs();

        final String qcmpFilePath = getFileNamePathIntoOutDir(String.format(COMPRESSED_FILE_TEMPLATE,
                options.getInputDataInfo().getPlaneIndex(),
                codebookSize));

        compressOps.setOutputFilePath(qcmpFilePath);
        ImageCompressor compressor = new ImageCompressor(compressOps);
        if (!compressor.compress()) {
            System.err.println("Errors occurred during compression.");
            return;
        }

        decompressOps.setInputDataInfo(new FileInputData(qcmpFilePath));

        final String decompressedFile = getFileNamePathIntoOutDir(String.format(QUANTIZED_FILE_TEMPLATE,
                options.getInputDataInfo().getPlaneIndex(),
                codebookSize));

        decompressOps.setOutputFilePath(decompressedFile);
        ImageDecompressor decompressor = new ImageDecompressor(decompressOps);

        final Optional<ImageU16Dataset> maybeDataset = decompressor.decompressInMemory();
        if (!maybeDataset.isPresent()) {
            System.err.println("Errors occurred during decompression.");
            return;
        }

        int[] originalData;
        try {
            final IPlaneLoader loader = PlaneLoaderFactory.getPlaneLoaderForInputFile(options.getInputDataInfo());
            originalData = loader.loadPlaneData(options.getInputDataInfo().getPlaneIndex());
        } catch (Exception e) {
            System.err.println("Failed to get plane loader. " + e.getMessage());
            e.printStackTrace();
            return;
        }

        final int[] quantizedData = TypeConverter.shortArrayToIntArray(maybeDataset.get().getPlaneData(0));

        final int[] diffArray = Utils.getDifference(originalData, quantizedData);
        final double mse = Utils.calculateMse(diffArray);
        final double PSNR = Utils.calculatePsnr(mse, U16.Max);
        System.out.printf("MSE: %.4f\tPSNR: %.4f(dB)%n", mse, PSNR);

        final int[] absDifferenceData = Utils.asAbsoluteValues(diffArray);

        final String diffFilePath = getFileNamePathIntoOutDir(String.format(DIFFERENCE_FILE_TEMPLATE,
                options.getInputDataInfo().getPlaneIndex(),
                codebookSize));

        final String absDiffFilePath = getFileNamePathIntoOutDir(String.format(ABSOLUTE_DIFFERENCE_FILE_TEMPLATE,
                options.getInputDataInfo().getPlaneIndex(),
                codebookSize));

        try {
            // NOTE(Moravec): Use little endian so that gnuplot can read the array.
            RawDataIO.writeBytesToFile(absDiffFilePath, TypeConverter.unsignedShortArrayToByteArray(absDifferenceData, true));
            System.out.println("Saved absolute difference to: " + absDiffFilePath);

            RawDataIO.writeDataI32(diffFilePath, diffArray, true);
            System.out.println("Saved difference to: " + absDiffFilePath);
        } catch (IOException e) {
            System.err.println("Failed to save difference.");
            e.printStackTrace();
        }
    }
}
