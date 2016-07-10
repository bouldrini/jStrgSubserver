package jStrg.network_management.storage_management.internal;

import com.amazonaws.services.cloudfront.model.InvalidArgumentException;
import jStrg.file_system.FileVersion;
import jStrg.file_system.Settings;
import jStrg.network_management.storage_management.config.ILocationConfig;
import jStrg.network_management.storage_management.core.Location;
import jStrg.network_management.storage_management.core.StorageCell;

import javax.persistence.Entity;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Local StorageCell for the jStrg Master Server
 */

@Entity
public class MasterCache extends StorageCell {

    private final static Logger LOGGER = Logger.getLogger(Settings.location_logging_target);

    protected MasterCache() {

    }

    public MasterCache(ILocationConfig _config) {
        super(TYPE.DISK, _config);
        m_cluster = _config.get_application().m_disk_storage_cluster;
        update_free_space();
    }

    public MasterCache(ILocationConfig _config, boolean _cachetype) {
        super(TYPE.CACHE, _config);
        update_free_space();
        if (!_cachetype)
            throw new InvalidArgumentException("When _cachetype used, then it must be set to true");
    }

    /**
     * returns free space on filesystem
     */
    protected void update_free_space() {
        long space = 0;
        long used = 0L;
        /*if (this.m_maintenance) {
            LOGGER.severe("maintenance set for " + this.toString() + " no space check ");
            return space;
        }*/ // handle by calling function, get_location_for_size
        try {
            space = Files.getFileStore(Paths.get(m_path)).getUsableSpace(); //bytes
            File directory = new File(m_path);
            File[] files = directory.listFiles();
            used = 0L;
            for (int i = 0; i < files.length; i++) {
                used += files[i].length();
            }
        } catch (IOException e) {
            Location.LOGGER.warning("access error for path " + m_path + " : " + e + " Location set to maintenance.");
            this.m_maintenance = true;
        }

        if (m_max_usage - used > space || m_max_usage == 0L) { // exception, disk max usage not set => use all space
            m_free_space = space;
        } else {
            m_free_space = m_max_usage - used;
        }
    }

    public boolean contains(String _checksum) {
        //TODO verify if disk is accessible
        return Files.exists(Paths.get(get_path() + "/" + _checksum));
    }


    public boolean delete(String _checksum) {
        boolean success = false;
        java.io.File file = new java.io.File(get_path() + "/" + _checksum);
        try {
            long size = file.length();
            if (!file.delete()) {
                throw new IOException("delete of " + file + " failed");
            }
            dec_used_space(size);
            success = true;
        } catch (IOException e) {
            LOGGER.warning("Error while deleting " + _checksum + " from " + this + e);
        }
        return success;
    }

    /**
     * Downloads file to cache.
     *
     * @return file that is written to cache
     */
    public java.io.File stage_file_to_cache_location(FileVersion _fileversion, String _destination) {
        Location cache_location = Location.get_cache_location_for_size(_fileversion.get_size());
        if (cache_location == null) {
            LOGGER.warning("no suitable cache Location found.");
            return null;
        }
        java.io.File returnfile = null;
        if (_destination == null) {
            LOGGER.severe("No existing filelock! Horrible!");
            return null;
        }

        try {
            Files.copy(Paths.get(get_path() + "/" + _fileversion.get_checksum()), Paths.get(_destination));
            LOGGER.finest("stage successful");
            returnfile = new java.io.File(_destination);
        } catch (IOException e) {
            LOGGER.warning("Error accessing disk: " + e);
        }
        return returnfile;
    }

    /**
     * Writes file to backend, gets the file object that must contain the get_real_file of the cache location.
     *
     * @param _file to write
     * @return cache location
     */
    @Override
    public StorageCell write_file(java.io.File _file, FileVersion _version) {
        MasterCache disk_location = (MasterCache) get_location_for_size(_file.length(), TYPE.DISK);
        if (disk_location == null) {
            Location.LOGGER.severe("No available Location of type DISK for file: " + _file);
            return null;
        }
        Path disk_path = Paths.get(disk_location.get_path() + "/" + _version.get_checksum());
        Path cache_path = Paths.get(_file.getAbsolutePath());
        try {
            Files.copy(cache_path, disk_path);
            disk_location.inc_used_space(Files.size(cache_path));
            Location.LOGGER.finest("file write successful for: " + _file);
        } catch (IOException e) {
            if (e.getClass().getName().equals("java.nio.file.FileAlreadyExistsException")) {
                Location.LOGGER.finest("duplicate found for file id: " + _version.get_id());
            } else {
                Location.LOGGER.warning("Error while moving: " + e);
            }

        }
        if (this.contains(_version.get_checksum())) {
            return this;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        StringBuilder returnstring = new StringBuilder("<Location::{m_id: " + m_id + ", m_type: '" + m_type + "'");
        returnstring.append(", mpath: '" + m_path + "', free: '" + get_free_space() + "'");
        return returnstring.append(" }>").toString();
    }
}
