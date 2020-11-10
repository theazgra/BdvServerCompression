package cz.it4i.qcmp.benchmark;

import cz.it4i.qcmp.U16;
import cz.it4i.qcmp.cli.CompressionOptionsCLIParser;
import cz.it4i.qcmp.compression.ImageCompressor;
import cz.it4i.qcmp.compression.ImageDecompressor;
import cz.it4i.qcmp.data.ImageU16Dataset;
import cz.it4i.qcmp.io.FileInputData;
import cz.it4i.qcmp.io.RawDataIO;
import cz.it4i.qcmp.io.loader.IPlaneLoader;
import cz.it4i.qcmp.io.loader.PlaneLoaderFactory;
import cz.it4i.qcmp.utilities.TypeConverter;
import cz.it4i.qcmp.utilities.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class Benchmark extends BenchmarkBase {


    protected Benchmark(final CompressionOptionsCLIParser options) {
        super(options);
    }

    @Override
    public void startBenchmark() {
        assert (options.getInputDataInfo().isPlaneIndexSet());
        final CompressionOptionsCLIParser compressOps;
        final CompressionOptionsCLIParser decompressOps;
        try {
            // NOTE: This works, right?
            compressOps = (CompressionOptionsCLIParser) options.clone();
            decompressOps = (CompressionOptionsCLIParser) options.clone();
        } catch (final CloneNotSupportedException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return;
        }
        // dirCreated is ignored.
        final boolean dirCreated = new File(options.getOutputFilePath()).mkdirs();

        final String qcmpFilePath = getFileNamePathIntoOutDir(String.format(COMPRESSED_FILE_TEMPLATE,
                                                                            options.getInputDataInfo().getPlaneIndex(),
                                                                            codebookSize));

        compressOps.setOutputFilePath(qcmpFilePath);
        final ImageCompressor compressor = new ImageCompressor(compressOps);
        if (!compressor.compress()) {
            System.err.println("Errors occurred during compression.");
            return;
        }

        decompressOps.setInputDataInfo(new FileInputData(qcmpFilePath, null));

        final String decompressedFile = getFileNamePathIntoOutDir(String.format(QUANTIZED_FILE_TEMPLATE,
                                                                                options.getInputDataInfo().getPlaneIndex(),
                                                                                codebookSize));

        decompressOps.setOutputFilePath(decompressedFile);
        final ImageDecompressor decompressor = new ImageDecompressor(decompressOps);

        final Optional<ImageU16Dataset> maybeDataset = decompressor.decompressInMemory();
        if (!maybeDataset.isPresent()) {
            System.err.println("Errors occurred during decompression.");
            return;
        }

        final int[] originalData;
        try {
            final IPlaneLoader loader = PlaneLoaderFactory.getPlaneLoaderForInputFile(options.getInputDataInfo());
            originalData = loader.loadPlaneData(0, options.getInputDataInfo().getPlaneIndex());
        } catch (final Exception e) {
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
        } catch (final IOException e) {
            System.err.println("Failed to save difference.");
            e.printStackTrace();
        }
    }
}
