package cz.it4i.qcmp.compression;

import cz.it4i.qcmp.compression.listeners.IProgressListener;
import cz.it4i.qcmp.compression.listeners.IStatusListener;

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

    /**
     * Remove all active listeners.
     */
    void clearAllListeners();
}
