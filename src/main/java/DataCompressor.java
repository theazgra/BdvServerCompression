import compression.benchmark.ScalarQuantizationBenchmark;
import compression.data.*;

import java.io.IOException;
import java.util.Random;


public class DataCompressor {


    public static void main(String[] args) throws IOException {
        //        test2DChunking();
        //        test3DChunking();

        //        ImageU16 img = null;
        //        try {
        //            img = RawDataIO.loadImageU16("D:\\biology\\tiff_data\\benchmark\\fused_tp_10_ch_0_16bit.raw",
        //                                         new V3i(1041, 996, 946),
        //                                         351);
        //        } catch (Exception e) {
        //            e.printStackTrace();
        //            return;
        //        }
        //
        //        Chunk2D imageChunk = img.as2dChunk();
        //        int[][] imageVectors = imageChunk.divideIntoVectors(3);
        //        var vectorQuantizer = new LBGVectorQuantizer(imageVectors, 64, 3);
        //        var codebook = vectorQuantizer.findOptimalCodebook();

        //        Chunk2D[] chunks = imageChunk.divideIntoChunks(new V2i(2, 2));
        //        int[][] imageVectors = new int[chunks.length][4];
        //        for (int i = 0; i < chunks.length; i++) {
        //            imageVectors[i] = chunks[i].getData();
        //        }
        //        LBGVectorQuantizer vectorQuantizer = new LBGVectorQuantizer(imageVectors, 64, 4);
        //        var codebook = vectorQuantizer.findOptimalCodebook();


        //        VectorQuantizer vq = new VectorQuantizer(codebook.getCodebook());

        ScalarQuantizationBenchmark[] benchmarks = new ScalarQuantizationBenchmark[3];
        benchmarks[0] = new ScalarQuantizationBenchmark("D:\\biology\\tiff_data\\benchmark\\fused_tp_10_ch_0_16bit.raw",
                                                        "D:\\biology\\benchmark\\fused_tp_10_ch_0_16bit",
                                                        new int[]{198, 240, 280, 351, 573, 663, 695},
                                                        new V3i(1041, 996, 946));

        benchmarks[1] = new ScalarQuantizationBenchmark(
                "D:\\biology\\tiff_data\\benchmark\\fused_tp_10_ch_0_16bit_edited.raw",
                "D:\\biology\\benchmark\\fused_tp_10_ch_0_16bit_edited",
                new int[]{198, 240, 280, 351, 573, 663, 695},
                new V3i(1041, 996, 946));

        benchmarks[2] = new ScalarQuantizationBenchmark("D:\\biology\\tiff_data\\benchmark\\fused_tp_10_ch_1_16bit.raw",
                                                        "D:\\biology\\benchmark\\fused_tp_10_ch_1_16bit",
                                                        new int[]{235, 295, 366, 464, 594, 683, 697},
                                                        new V3i(1041, 996, 946));

        for (ScalarQuantizationBenchmark benchmark : benchmarks)
        {
            benchmark.startBenchmark();
        }
        //        ScalarQuantizationBenchmark sqBenchmark = new ScalarQuantizationBenchmark(
        //                "D:\\biology\\tiff_data\\fused_tp_10_ch_1_16bit_edited.raw",
        //                "D:\\biology\\benchmark\\scalar_edited",
        //                0,
        //                0,
        //                new V3i(1041, 996, 1));

        //sqBenchmark.setUseDiffEvolution(true);

        //sqBenchmark.startBenchmark();
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
