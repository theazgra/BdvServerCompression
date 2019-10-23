import org.apache.commons.math3.distribution.CauchyDistribution;
import quantization.LloydMaxU16ScalarQuantization;
import quantization.Utils;

import java.io.FileNotFoundException;



public class DataCompressor {
    public static void main(String[] args) throws FileNotFoundException {

        final String sourceFile = "D:\\tmp\\server-dump\\small.bin";
        final int NumberOfBits = 8;
        int[] values = Utils.convertU16BytesToInt(Utils.readFileBytes(sourceFile));

        LloydMaxU16ScalarQuantization quantization = new LloydMaxU16ScalarQuantization(values, NumberOfBits);
        quantization.train();

        System.out.println("Finished learning...");

    }
}
