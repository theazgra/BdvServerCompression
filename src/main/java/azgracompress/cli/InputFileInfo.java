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

    public void setDimension(V3i dimension) {
        this.dimension = dimension;
    }

    public void setPlaneIndexSet(boolean planeIndexSet) {
        this.planeIndexSet = planeIndexSet;
    }

    public void setPlaneIndex(int planeIndex) {
        this.planeIndex = planeIndex;
    }

    public void setPlaneRangeSet(boolean planeRangeSet) {
        this.planeRangeSet = planeRangeSet;
    }

    public void setPlaneRange(V2i planeRange) {
        this.planeRange = planeRange;
    }

    public FileType getFileType() {
        return fileType;
    }

    public String getFilePath() {
        return filePath;
    }

    public V3i getDimension() {
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
