import quantization.LloydMaxU16ScalarQuantization;
import quantization.Utils;

import java.io.FileNotFoundException;

public class DataCompressor {
    public static void main(String[] args) throws FileNotFoundException {

        final String sourceFile = "D:\\tmp\\server-dump\\small.bin";
        final int NumberOfBits = 3;
        char[] values = Utils.convertBytesToU16(Utils.readFileBytes(sourceFile));

        long leq1000 = 0;
        long gt1000 = 0;
        long zero = 0;

        for (char value : values) {
            if (value == 0) {
                ++zero;
            } else if (value <= 1000) {
                ++leq1000;
            } else {
                ++gt1000;
            }
        }

        double leq100Perc = (double) leq1000 / (double) values.length;
        double zeroPerc = (double) zero / (double) values.length;
        double gt100Perc = (double) gt1000 / (double) values.length;

        System.out.println(String.format("    =0:\t%f%%\n<=1000:\t%f%%\n >1000:\t%f%%", zeroPerc, leq100Perc, gt100Perc));


        LloydMaxU16ScalarQuantization quantization = new LloydMaxU16ScalarQuantization(values, NumberOfBits);

        quantization.train();

        System.out.println("Finished learning...");
    }
}
