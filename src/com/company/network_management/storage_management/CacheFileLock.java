package jStrg.network_management.storage_management;

import jStrg.file_system.FileVersion;
import jStrg.file_system.Settings;

import java.util.LinkedHashMap;
import java.util.logging.Logger;

public class CacheFileLock {
    private final static Logger LOGGER = Logger.getLogger(Settings.location_logging_target);
    private static CacheFileLock m_instance;
    private LinkedHashMap<Integer, Integer> m_lockmap;
    private LinkedHashMap<Integer, String> m_pathmap;
    private LinkedHashMap<Integer, Boolean> m_readable;

    private CacheFileLock() {
        m_lockmap = new LinkedHashMap<>();
        m_pathmap = new LinkedHashMap<>();
        m_readable = new LinkedHashMap<>();
        m_instance = this;
    }

    public static CacheFileLock getInstance() {
        if (m_instance == null)
            m_instance = new CacheFileLock();
        return m_instance;
    }

    public synchronized void lock(int _FileVersionId, String _path) {
        if (m_lockmap.containsKey(_FileVersionId)) {
            m_lockmap.put(_FileVersionId, m_lockmap.get(_FileVersionId) + 1);
        } else {
            m_lockmap.put(_FileVersionId, 1);
            m_pathmap.put(_FileVersionId, _path);
        }
    }

    public synchronized boolean release(FileVersion _FileVersion) {
        boolean ret = false;
        if (m_lockmap.get(_FileVersion.get_id()) == 1) {
            java.io.File file = new java.io.File(m_pathmap.get(_FileVersion.get_id()));
            if (file.delete()) {
                LOGGER.finest("last lock released. Deleting file: " + _FileVersion.get_id());
                ret = true;
            } else {
                LOGGER.warning("error while deleting: " + m_pathmap.get(_FileVersion.get_id()));
                ret = false;
            }
            m_lockmap.remove(_FileVersion.get_id());
            m_pathmap.remove(_FileVersion.get_id());
            m_readable.remove(_FileVersion.get_id());
        } else {
            m_lockmap.put(_FileVersion.get_id(), m_lockmap.get(_FileVersion.get_id()) - 1);
            ret = true;
        }
        return ret;
    }

    /**
     * check if a filelock for a specific version exists
     *
     * @param _FileVersionId
     * @return null if no lock exists, if exists -> the path to the locked file
     */
    public String isLocked(int _FileVersionId) {
        String ret = null;
        if (m_pathmap.containsKey(_FileVersionId)) {
            ret = m_pathmap.get(_FileVersionId);
        }
        return ret;
    }

    public boolean isReadable(FileVersion _FileVersion) {
        Boolean ret = m_readable.get(_FileVersion.get_id());
        if (ret == null) ret = false;
        return ret;
    }

    public void set_readable(int _FileVersionId) {
        m_readable.put(_FileVersionId, true);
    }

}
