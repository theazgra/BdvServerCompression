package azgracompress.cli;

import azgracompress.data.V2i;
import azgracompress.data.V3i;
import azgracompress.fileformat.FileType;

/**
 * Information about the input file.
 */
public class InputFileInfo {
    /**
     * Input file type.
     */
    private final FileType fileType;
    /**
     * Input file path.
     */
    private final String filePath;

    private V3i dimension;

    private boolean planeIndexSet = false;
    private int planeIndex;

    private boolean planeRangeSet = false;
    private V2i planeRange;

    public InputFileInfo(final FileType fileType, final String filePath) {
        this.fileType = fileType;
        this.filePath = filePath;
    }

    /**
     * Get number of selected planes to be compressed.
     *
     * @return Number of planes for compression.
     */
    public int getNumberOfPlanes() {
        if (planeIndexSet) {
            return 1;
        } else if (planeRangeSet) {
            return ((planeRange.getY() + 1) - planeRange.getX());
        } else {
            return dimension.getZ();
        }
    }

    public void setDimension(final V3i dimension) {
        this.dimension = dimension;
    }

    public void setPlaneIndex(final int planeIndex) {
        this.planeIndexSet = true;
        this.planeIndex = planeIndex;
    }

    public void setPlaneRange(final V2i planeRange) {
        this.planeRangeSet = true;
        this.planeRange = planeRange;
    }

    public FileType getFileType() {
        return fileType;
    }

    public String getFilePath() {
        return filePath;
    }

    public V3i getDimensions() {
        return dimension;
    }

    public boolean isPlaneIndexSet() {
        return planeIndexSet;
    }

    public int getPlaneIndex() {
        return planeIndex;
    }

    public boolean isPlaneRangeSet() {
        return planeRangeSet;
    }

    public V2i getPlaneRange() {
        return planeRange;
    }
}
