package azgracompress.data;

import azgracompress.utilities.TypeConverter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ChunkIO {
    public static Chunk3D[] loadChunks(final String fileName) {

        ArrayList<Chunk3D> chunks = new ArrayList<Chunk3D>();
        try {
            FileInputStream is = new FileInputStream(fileName);
            ByteBuffer buffer = ByteBuffer.allocate((int) (is.getChannel().size()));
            is.getChannel().read(buffer);
            final int bufferSize = buffer.capacity();
            buffer.position(0);

            while (buffer.position() != bufferSize) {
                final V3i chunkDims = new V3i(buffer.getInt(), buffer.getInt(), buffer.getInt());
                final V3l chunkOffset = new V3l(buffer.getLong(), buffer.getLong(), buffer.getLong());

                final int dataLen = 2 * (chunkDims.getX() * chunkDims.getY() * chunkDims.getZ());
                byte[] chunkData = new byte[dataLen];
                buffer.get(chunkData);

                chunks.add(new Chunk3D(chunkDims, chunkOffset, TypeConverter.unsignedShortBytesToIntArray(chunkData)));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        Chunk3D[] loadedChunks = new Chunk3D[chunks.size()];
        chunks.toArray(loadedChunks);
        return loadedChunks;
    }

    public static void saveChunks(final int[] chunkDims, final long[] chunkOffset,
                                  final byte[] chunkData, final String fileName) {
        assert ((chunkDims[0] * chunkDims[1] * chunkDims[2]) == (chunkData.length / 2)) : "Data does not correspond to dimensions";
        try {
            FileOutputStream dumpStream = new FileOutputStream(fileName, true);

            //                                      cellDim + cellOffset + cellData
            ByteBuffer buffer = ByteBuffer.allocate((3 * 4) + (3 * 8) + chunkData.length);
            buffer.putInt(chunkDims[0]);
            buffer.putInt(chunkDims[1]);
            buffer.putInt(chunkDims[2]);

            buffer.putLong(chunkOffset[0]);
            buffer.putLong(chunkOffset[1]);
            buffer.putLong(chunkOffset[2]);

            buffer.put(chunkData);

            dumpStream.write(buffer.array());
            dumpStream.flush();
            dumpStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
