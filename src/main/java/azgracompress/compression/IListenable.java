package azgracompress.compression;

import azgracompress.compression.listeners.IProgressListener;
import azgracompress.compression.listeners.IStatusListener;

public interface IListenable {
    /**
     * Set status listener. Status messages are reported to this listener.
     *
     * @param listener Status listener.
     */
    void setStatusListener(IStatusListener listener);

    /**
     * Set progress listener. Status messages with progress information are reported to this listener.
     *
     * @param listener Progress listener.
     */
    void setProgressListener(IProgressListener listener);
}
