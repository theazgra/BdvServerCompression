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

    public static char[] convertBytesToU16(final byte[] bytes) {
        assert ((bytes.length % 2) == 0);

        char[] values = new char[bytes.length / 2];
        int index = 0;
        for (int i = 0; i < bytes.length; i += 2) {
            short value = 0;

            values[index++] = (char) (((bytes[i] & 0xFF) << 8) | (bytes[i + 1] & 0xFF));
        }
        return values;
    }
}
