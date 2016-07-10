package jStrg.network_management.storage_management.core;

import jStrg.file_system.*;
import jStrg.network_management.storage_management.CacheFileLock;

import java.util.*;
import java.util.logging.Logger;

public class ClusterManager {

    private final static Logger LOGGER = Logger.getLogger(Settings.location_logging_target);

    public ClusterManager(Application _application) {
        m_application = _application;
        m_placement_mode = _application.m_setting.m_placement_mode;
    }

    public boolean can_upload(User _user, long _file_size) {
        boolean result = false;
        if (_user.has_enough_space(_file_size)){
            result = this.has_enough_space(_file_size);
        } else {
            result = false;
        }
        return result;
    }

    public enum PLACEMENT_MODE {MIRRORED, CONCAT}

    public Application m_application;
    public PLACEMENT_MODE m_placement_mode;

    public boolean upload(File _file, FileVersion _file_version) {
        if (m_placement_mode == PLACEMENT_MODE.MIRRORED) {
            // INTERNAL STORAGE LOCATIONS
            if (m_application.m_setting.m_use_local_cluster_location) {
                m_application.m_local_storage_cluster.write_file(_file, _file_version);
            }

//          EXTERNAL STORAGE LOCATIONS
            if (m_application.m_setting.m_use_s3_location) {
                m_application.m_amazon_s3_bucket_cluster.write_file(_file, _file_version);
            }

            if (m_application.m_setting.m_use_google_location) {
                m_application.m_google_cloud_bucket_cluster.write_file(_file, _file_version);
            }
            return true;
        } else if (m_placement_mode == PLACEMENT_MODE.CONCAT) {

        }
        return false;
    }

    public boolean has_enough_space(long _file_size) {
        if (m_placement_mode == PLACEMENT_MODE.MIRRORED) {
            // INTERNAL STORAGE LOCATIONS
            if (m_application.m_setting.m_use_local_cluster_location) {
                // CHECK LOCAL STORAGE FOR SPACE
                if (!m_application.m_local_storage_cluster.has_enough_space(_file_size)) {
                    return false;
                }
                ;
            }

//          EXTERNAL STORAGE LOCATIONS
            if (m_application.m_setting.m_use_s3_location) {
                // CHECK S3 STORAGE FOR SPACE
                if (!m_application.m_amazon_s3_bucket_cluster.has_enough_space(_file_size)) {
                    return false;
                }
                ;
            }

            if (m_application.m_setting.m_use_google_location) {
                // CHECK GOOGLE STORAGE FOR SPACE
                if (!m_application.m_google_cloud_bucket_cluster.has_enough_space(_file_size)) {
                    return false;
                }
                ;
            }

            if (m_application.m_setting.m_use_disk_cluster_location) {
                // CHECK GOOGLE STORAGE FOR SPACE
                if (!m_application.m_disk_storage_cluster.has_enough_space(_file_size)) {
                    return false;
                }
                ;
            }
            return true;
        } else if (m_placement_mode == PLACEMENT_MODE.CONCAT) {
            // INTERNAL STORAGE LOCATIONS
            if (m_application.m_setting.m_use_local_cluster_location) {
                // CHECK LOCAL STORAGE FOR SPACE
                if (m_application.m_local_storage_cluster.has_enough_space(_file_size)) {
                    return true;
                }
                ;
            }

//          EXTERNAL STORAGE LOCATIONS
            if (m_application.m_setting.m_use_s3_location) {
                // CHECK S3 STORAGE FOR SPACE
                if (m_application.m_amazon_s3_bucket_cluster.has_enough_space(_file_size)) {
                    return true;
                }
                ;
            }

            if (m_application.m_setting.m_use_google_location) {
                // CHECK GOOGLE STORAGE FOR SPACE
                if (m_application.m_google_cloud_bucket_cluster.has_enough_space(_file_size)) {
                    return true;
                }
                ;
            }
            return false;
        }
        return false;
    }

    /**
     * checking entrys in database against real world to find discrepancies. In this context,
     * light means entrys and existence of objects are checked, not the binary data.
     *
     * @return if one failure occurs, then function returns false. you can use zombie_versions to determine which
     */
    public Boolean lightScrub() {
        Boolean success = true;
        switch (m_placement_mode) {
            case CONCAT:
                // if failure found, there is nothing we can do
                return zombie_versions().size() != 0;
            case MIRRORED:
                for (FileVersion version : zombie_versions()) {
                    success = success && recover(version);
                }
                break;
        }
        return success;
    }

    /**
     * find all versions of this application and checks its registered storagecells for existence
     *
     * @return
     */
    public Collection<FileVersion> zombie_versions() {
        Set<FileVersion> zombies = new LinkedHashSet<>();
        for (FileVersion version : FileVersion.find_by_app(m_application)) {
            for (Location location : version.get_location()) {
                try {
                    if (!location.get_maintenance() && !location.contains(version.get_checksum())) {
                        zombies.add(version);
                    }
                } catch (Exception e) {
                    LOGGER.warning("Error communicating with " + location);
                }
            }
        }
        return zombies;
    }

    /**
     * checks locations for given version and tries to get it from one location to recover it at failed locations.
     *
     * @param _version typical from zombie_versions()
     * @return full success, one failure turns to false
     */
    public Boolean recover(FileVersion _version) {

        // first find a location we can download from and bad locations we need to refill
        Location good_location = null;
        List<Location> bad_locations = new ArrayList<>();
        for (Location location : _version.get_location()) {
            if (location.get_maintenance())
                continue; // doesnt make sense to check locations under maintenance

            try {
                if (location.contains(_version.get_checksum())) {
                    good_location = location;
                } else {
                    bad_locations.add(location);
                }
            } catch (Exception e) {
                LOGGER.warning("Error communicating with " + location);
            }
        }

        // check if we have a good location
        if (good_location == null)
            return false;
        // no bad locations found
        if (bad_locations.size() == 0)
            return true;

        // download a copy of that version
        java.io.File file = Location.stage_file_to_cache(_version);
        if (!CacheFileLock.getInstance().isReadable(_version)) {
            LOGGER.severe("error getting readable bytes for version: " + _version);
            return false;
        }
        Boolean success = true;

        // iterate through bad locations and try to recover. Write to any location and delete old
        for (Location location : bad_locations) {
            _version.remove_location((StorageCell) location);
            StorageCell newcell = Location.make_file_persistent_internal(file, _version, location.get_type());

            if (newcell != null) {
                _version.add_location(newcell);
                _version.db_sync();
                LOGGER.finest("Successful recover of " + _version + " for one location");
            } else {
                success = false;
                LOGGER.warning("Error while recover fileversion: " + _version);
            }
        }

        if (CacheFileLock.getInstance().isReadable(_version)) {
            CacheFileLock.getInstance().release(_version);
        }
        return success;
    }
}
