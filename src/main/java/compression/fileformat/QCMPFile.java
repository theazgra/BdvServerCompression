package compression.fileformat;

public class QCMPFile {
    private final QCMPFileHeader header;
    private final byte[] data;

    public QCMPFile(QCMPFileHeader header, byte[] data) {
        this.header = header;
        this.data = data;
    }
}
