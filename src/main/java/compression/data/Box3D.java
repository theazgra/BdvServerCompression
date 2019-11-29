package compression.data;

import compression.utilities.Utils;

public class Box3D {
    private final int[] data;
    private final int xSize;
    private final int ySize;
    private final int zSize;

    private int xOffset = 0;
    private int yOffset = 0;
    private int zOffset = 0;

    public Box3D(final int xSize, final int ySize, final int zSize, final int[] data) {
        this.data = data;
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;

        assert (data.length == (xSize * ySize * zSize)) : "Wrong box data.";
    }

    public Box3D(final int xSize, final int ySize, final int zSize, final short[] data) {
        this(xSize, ySize, zSize, Utils.convertShortArrayToIntArray(data));
    }

    public void setOffsets(final int xOffset, final int yOffset, final int zOffset) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
    }

    private int computeIndex(final int x, final int y, final int z) {
        if (((x < 0) || (x >= xSize)) || (y < 0) || (y >= ySize) || (z < 0) || (z >= zSize)) {
            throw new IndexOutOfBoundsException("One of index x,y,z is out of bounds of the 3D box");
        }

        /// NOTE    In higher dimensions, the last dimension specified is the fastest changing on disk.
        //          So if we have a four dimensional dataset A, then the first element on disk would be A[0][0][0][0],
        //          the second A[0][0][0][1], the third A[0][0][0][2], and so on.
        //          SOURCE: https://support.hdfgroup.org/HDF5/doc/Advanced/Chunking/index.html


        /*
            (row*colCount) + col
            (x * (xSize+ySize)) + (y * ySize) + z
            0*(2+2)+0*2+0=0
            0*(2+2)+0*2+1=1
            0*(2+2)+1*2+0=2
            0*(2+2)+1*2+1=3
            1*(2+2)+0*2+0=4
            1*(2+2)+0*2+1=5
            1*(2+2)+1*2+0=6
            1*(2+2)+1*2+1=7
            3D 2x2x2 box
            0           1           2           3           4           5           6           7
            A[0][0][0]  A[0][0][1]  A[0][1][0]  A[0][1][1]  A[1][0][0]  A[1][0][1]  A[1][1][0]  A[1][1][1]
         */

        final int index = (x * (xSize + ySize)) + (y * ySize) + z;
        return index;
    }


    public int valueAt(final int x, final int y, final int z) {
        return data[computeIndex(x, y, z)];
    }

    public int getxSize() {
        return xSize;
    }

    public int getySize() {
        return ySize;
    }

    public int getzSize() {
        return zSize;
    }

    public int getxOffset() {
        return xOffset;
    }

    public int getyOffset() {
        return yOffset;
    }

    public int getzOffset() {
        return zOffset;
    }
}
