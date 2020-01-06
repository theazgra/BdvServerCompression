import compression.data.*;
import compression.io.RawDataIO;
import compression.quantization.vector.LBGVectorQuantizer;
import compression.quantization.vector.VectorQuantizer;
import compression.utilities.Utils;

import java.io.IOException;
import java.util.Random;


public class DataCompressor {


    public static void main(String[] args) throws IOException {
        //        test2DChunking();
        //        test3DChunking();
        //        test2DVectorChunking();
        //
        //        new ScalarQuantizationBenchmark("D:\\biology\\tiff_data\\benchmark\\fused_tp_10_ch_0_16bit_edited
        //        .raw",
        //                                        "D:\\biology\\benchmark\\tmp",
        //                                        new int[]{351},
        //                                        new V3i(1041, 996, 946)).startBenchmark();


        ImageU16 img = null;
        try {
            img = RawDataIO.loadImageU16("D:\\biology\\tiff_data\\benchmark\\fused_tp_10_ch_0_16bit.raw",
                                         new V3i(1041, 996, 946),
                                         (351 - 1));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        //         1D Vectors
        Chunk2D imageChunk = img.as2dChunk();
        //int[] codebookSizes = new int[]{4, 8, 16, 32, 64, 128, 256};
        int[] codebookSizes = new int[]{64};

        for (final int cbS : codebookSizes) {
            // 1D
            int[][] imageVectors = imageChunk.divideIntoVectors(9);
            var lbgVQInitializer = new LBGVectorQuantizer(imageVectors, cbS);

            var lbgResult = lbgVQInitializer.findOptimalCodebook();

            VectorQuantizer vq = new VectorQuantizer(lbgResult.getCodebook());
            final int[][] quantizedVectors = vq.quantize(imageVectors);

            Chunk2D reconstructedChunk = new Chunk2D(new V2i(1041, 996), new V2l(0, 0));
            reconstructedChunk.reconstructFromVectors(quantizedVectors);
            ImageU16 reconstructedImage = reconstructedChunk.asImageU16();

            final int[] rawDiff =  Utils.getDifference(img.getData(), reconstructedImage.getData());

            RawDataIO.writeDataI32("raw_diff.data", rawDiff, true);

            /*
            ImageU16 diffImage = img.difference(reconstructedImage);


            assert (!imageChunk.equals(reconstructedChunk));
            try {
                RawDataIO.writeImageU16(String.format("D:\\biology\\benchmark\\ch0_1d_vq\\p351_cb%d.raw", cbS),
                                        reconstructedImage,
                                        false);
                RawDataIO.writeImageU16(String.format("D:\\biology\\benchmark\\ch0_1d_vq\\p351_cb%d_diff.raw", cbS),
                                        diffImage,
                                        true);
            } catch (IOException e) {
                e.printStackTrace();
            }
             */

            // 2D Vectors
            //            Chunk2D[] chunks = imageChunk.divideIntoChunks(new V2i(3, 3));
            //            int[][] image2DVectors = Chunk2D.chunksAsImageVectors(chunks);
            //            LBGVectorQuantizer lbgVQInitializer = new LBGVectorQuantizer(image2DVectors, cbS);
            //            var lbgResult = lbgVQInitializer.findOptimalCodebook();
            //
            //            VectorQuantizer vq = new VectorQuantizer(lbgResult.getCodebook());
            //            final int[][] quantized2DVectors = vq.quantize(image2DVectors);
            //
            //            Chunk2D.updateChunkData(chunks, quantized2DVectors);
            //            Chunk2D reconstructedChunk = new Chunk2D(new V2i(1041, 996), new V2l(0, 0));
            //            reconstructedChunk.reconstructFromChunks(chunks);
            //            ImageU16 reconstructedImage = reconstructedChunk.asImageU16();
            //
            //            ImageU16 diffImage = img.difference(reconstructedImage);
            //
            //            try {
            //                //            RawDataIO.writeImageU16("original_image.raw", img, false);
            //                RawDataIO.writeImageU16("vq2d_image.raw", reconstructedImage, false);
            //                RawDataIO.writeImageU16("vq2d_diff.raw", diffImage, true);
            //
            //                RawDataIO.writeImageU16(String.format("D:\\biology\\benchmark\\ch0_2d_vq\\p351_cb%d.raw",
            //         cbS),
            //                                        reconstructedImage,
            //                                        false);
            //                RawDataIO.writeImageU16(String.format("D:\\biology\\benchmark\\ch0_2d_vq\\p351_cb%d_diff
            //         .raw", cbS),
            //                                        diffImage,
            //                                        true);
            //            } catch (IOException e) {
            //                e.printStackTrace();
            //            }
        }


        //        ScalarQuantizationBenchmark[] benchmarks = new ScalarQuantizationBenchmark[3];
        //        benchmarks[0] = new ScalarQuantizationBenchmark
        //        ("D:\\biology\\tiff_data\\benchmark\\fused_tp_10_ch_0_16bit.raw",
        //                                                        "D:\\biology\\benchmark\\fused_tp_10_ch_0_16bit",
        //                                                        new int[]{198, 240, 280, 351, 573, 663, 695},
        //                                                        new V3i(1041, 996, 946));
        //
        //        benchmarks[1] = new ScalarQuantizationBenchmark(
        //                "D:\\biology\\tiff_data\\benchmark\\fused_tp_10_ch_0_16bit_edited.raw",
        //                "D:\\biology\\benchmark\\fused_tp_10_ch_0_16bit_edited",
        //                new int[]{198, 240, 280, 351, 573, 663, 695},
        //                new V3i(1041, 996, 946));
        //
        //        benchmarks[2] = new ScalarQuantizationBenchmark
        //        ("D:\\biology\\tiff_data\\benchmark\\fused_tp_10_ch_1_16bit.raw",
        //                                                        "D:\\biology\\benchmark\\fused_tp_10_ch_1_16bit",
        //                                                        new int[]{235, 295, 366, 464, 594, 683, 697},
        //                                                        new V3i(1041, 996, 946));
        //
        //        for (ScalarQuantizationBenchmark benchmark : benchmarks)
        //        {
        //            benchmark.startBenchmark();
        //        }
        //        ScalarQuantizationBenchmark sqBenchmark = new ScalarQuantizationBenchmark(
        //                "D:\\biology\\tiff_data\\fused_tp_10_ch_1_16bit_edited.raw",
        //                "D:\\biology\\benchmark\\scalar_edited",
        //                0,
        //                0,
        //                new V3i(1041, 996, 1));

        //sqBenchmark.setUseDiffEvolution(true);

        //sqBenchmark.startBenchmark();
    }

    static void test2DVectorChunking() {
        final int xs = 761;
        final int ys = 438;
        final int[] data = getRandomData(xs * ys);
        final Chunk2D src = new Chunk2D(new V2i(xs, ys), new V2l(0), data);
        final int[][] vectors = src.divideIntoVectors(3);

        final Chunk2D reconstructed = new Chunk2D(new V2i(xs, ys), new V2l(0));
        reconstructed.reconstructFromVectors(vectors);

        if (src.equals(reconstructed)) {
            System.out.println("2D vector Reconstruction successful.");
        } else {
            System.out.println("2D vector Reconstruction failed !!!");
        }
    }

    static void test2DChunking() {
        final int xs = 1920;
        final int ys = 1080;
        final int[] data = getRandomData(xs * ys);
        final Chunk2D src = new Chunk2D(new V2i(xs, ys), new V2l(0), data);
        final Chunk2D[] chunks = src.divideIntoChunks(new V2i(3));

        final Chunk2D reconstructed = new Chunk2D(new V2i(xs, ys), new V2l(0));
        reconstructed.reconstructFromChunks(chunks);

        if (src.equals(reconstructed)) {
            System.out.println("2D Reconstruction successful.");
        } else {
            System.out.println("2D Reconstruction failed !!!");
        }
    }

    static void test3DChunking() {
        //Chunk3D[] loadedChunks = ChunkIO.loadChunks("D:\\tmp\\server-dump\\chunks.bin");
        final int xs = 11;
        final int ys = 16;
        final int zs = 16;
        final int[] data = getRandomData(xs * ys * zs);
        final Chunk3D src = new Chunk3D(new V3i(xs, ys, zs), new V3l(0), data);
        final Chunk3D[] chunks = src.divideIntoChunks(new V3i(4));

        final Chunk3D reconstructed = new Chunk3D(new V3i(xs, ys, zs), new V3l(0));
        reconstructed.reconstructFromChunks(chunks);

        if (src.equals(reconstructed)) {
            System.out.println("3D Reconstruction successful.");
        } else {
            System.out.println("3D Reconstruction failed !!!");
        }
    }

    static int[] getRandomData(int len) {
        Random r = new Random();
        int[] data = new int[len];
        for (int i = 0; i < data.length; i++) {
            data[i] = r.nextInt(20000);
        }
        return data;
    }
}
