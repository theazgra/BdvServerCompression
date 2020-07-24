package azgracompress.compression.listeners;

public interface IProgressListener {
    void sendProgress(final String message, final int index, final int finalIndex);
}
