package quantization;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Utils {
    public static byte[] readFileBytes(final String path) throws FileNotFoundException {
        FileInputStream fileStream = new FileInputStream(path);
        try {
            return fileStream.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    public static int[] convertU16BytesToInt(final byte[] bytes) {
        assert (bytes.length % 2 == 0);
        int[] values = new int[bytes.length / 2];

        int index = 0;
        for (int i = 0; i < bytes.length; i += 2) {
            final int value = (int)(((bytes[i] & 0xff) << 8) | (bytes[i + 1] & 0xff));
            if (value > 0)
            {
                values[index++] = value;
                continue;
            }
            values[index++] = value;
        }
        return values;
    }
}
