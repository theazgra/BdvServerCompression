package azgracompress.compression;

import azgracompress.compression.listeners.IProgressListener;
import azgracompress.compression.listeners.IStatusListener;

public interface IListenable {
    /**
     * Add status listener. Status messages are reported to this listener.
     *
     * @param listener Status listener.
     */
    void addStatusListener(IStatusListener listener);

    /**
     * Add progress listener. Status messages with progress information are reported to this listener.
     *
     * @param listener Progress listener.
     */
    void addProgressListener(IProgressListener listener);
}
