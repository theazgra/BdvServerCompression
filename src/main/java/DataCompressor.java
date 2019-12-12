import compression.data.*;

import java.io.IOException;
import java.util.Random;


public class DataCompressor {


    public static void main(String[] args) throws IOException {

//        ScalarQuantizationBenchmark sqBenchmark = new ScalarQuantizationBenchmark(
//                "D:\\biology\\tiff_data\\fused_tp_10_ch_1_16bit.raw",
//                "D:\\biology\\benchmark\\scalar",
//                358,
//                358,
//                new V3i(1041, 996, 946));
//
//        sqBenchmark.setUseDiffEvolution(true);
//
//        sqBenchmark.startBenchmark();

        test2DChunking();

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
            System.out.println("Reconstruction successful.");
        } else {
            System.out.println("Reconstruction failed !!!");
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
