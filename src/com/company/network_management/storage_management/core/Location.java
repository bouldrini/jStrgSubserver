package jStrg.network_management.storage_management.core;

import jStrg.data_types.exceptions.ConfigException;
import jStrg.database.IGenericDao;
import jStrg.database.ILocationDao;
import jStrg.environment.Environment;
import jStrg.file_system.FileVersion;
import jStrg.file_system.Settings;
import jStrg.file_system.User;
import jStrg.network_management.storage_management.CacheFileLock;
import jStrg.network_management.storage_management.config.ILocationConfig;
import jStrg.network_management.storage_management.config.ILocationStandardValues;
import jStrg.network_management.storage_management.external.AmazonS3Bucket;
import jStrg.network_management.storage_management.external.GoogleCloudBucket;
import jStrg.network_management.storage_management.internal.MasterCache;
import jStrg.network_management.storage_management.internal.StorageServer;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * StorageCell backend interface, two methods: write_file and stage_file_to_cache are the ones you want to use
 */
public interface Location extends ILocationStandardValues {


    Logger LOGGER = Logger.getLogger(Settings.location_logging_target);

    /**
     * Takes File object and writes the file returned by get_real_file() to storage backends.
     * Typical the file that is written to cache.
     *
     * @param _file    file to write
     * @param _version file to write
     * @return return list with location id's where the file was written
     */
    static Set<StorageCell> make_file_persistent(java.io.File _file, FileVersion _version) {
        Set<StorageCell> ret = new LinkedHashSet<>();
        IGenericDao connectorblueprintdao = Environment.data().get_dao(StorageCell.class);
        ILocationDao locationdao = Environment.data().get_dao_location();

        for (TYPE type : _version.user().storagepools()) {
            boolean alreadyuploaded = false;
            for (FileVersion fileversion : FileVersion.find_by_chksum(_version.get_checksum())) {
                for (StorageCell location : fileversion.get_location()) {
                    try {
                        if (location.get_type() == type
                                && !location.get_maintenance()
                                && location.contains(_version.get_checksum())) { //TODO database query with join
                            alreadyuploaded = true; // deduplication
                            ret.add(location);
                            break;
                        }
                    } catch (Exception e) {
                        LOGGER.info("Problem with location: " + location + " >> " + e);
                    }
                }
            }

            if (!alreadyuploaded) {
                StorageCell uploaded_to = make_file_persistent_internal(_file, _version, type);
                if (uploaded_to != null) {
                    ret.add(uploaded_to);
                    uploaded_to.db_update();
                }
            }

        }
        return ret;
    }

    /**
     * simple upload to a type of location, no dedup - mut be checked before!
     *
     * @param _file    javaio file on filesystem directly accessible by system
     * @param _version const var, used to pass checksum and size
     * @param _type    of location
     * @return location written to
     */
    static StorageCell make_file_persistent_internal(java.io.File _file, FileVersion _version, TYPE _type) {
        ILocationDao locationdao = Environment.data().get_dao_location();
        StorageCell cell = null;

        for (Location location : locationdao.find_with_and_condition(_type, _version.user().application())) {
            if (location.get_maintenance())
                continue;

            cell = location.write_file(_file, _version);
            if (cell != null) {
                break;
            }
        }
        return cell;
    }

    static Boolean enough_space_available(User _user, long _fileSize) {
        Boolean returnval = true;
        ILocationDao locationDao = Environment.data().get_dao_location();

        for (Location.TYPE _type : _user.storagepools()) {
            Boolean enough_space_per_type = false;
            for (Location _location : locationDao.find_all_by_type(_type)) {
                if (_location.get_free_space() >= _fileSize && !_location.get_maintenance()) {
                    enough_space_per_type = true;
                    break;
                }
            }
            returnval = returnval && enough_space_per_type;
        }
        return returnval;
    }

    static List<StorageCell> find_by_type(TYPE _type) {
        return Environment.data().get_dao_location().find_all_by_type(_type);
    }

    /**
     * gets the data for a file and writes it to a available cache location. Updates the Object, when returned true then
     * get_real_file() of File objects returns the cache object.
     *
     * @param _fileversion File object to retrieve
     * @return the file that is written to cache
     */
    static java.io.File stage_file_to_cache(FileVersion _fileversion) {
        java.io.File returnfile = null;

        CacheFileLock filelock = CacheFileLock.getInstance();
        String destination = filelock.isLocked(_fileversion.get_id());
        if (destination == null) {
            destination = get_cache_location_for_size(_fileversion.get_size()).get_path()
                    + "/"
                    + _fileversion.get_id()
                    + ".tmp";
            filelock.lock(_fileversion.get_id(), destination);
        } else {
            filelock.lock(_fileversion.get_id(), destination);
            while (!filelock.isReadable(_fileversion)) {
                // A wait mechanism must be done here to block until a active thread does a notify(), for now we have a single thread
                LOGGER.severe("only with mutlithreading");
            }
            return new File(destination);
        }

        // Need some algorithm to choose
        for (Location get_location : _fileversion.get_location()) {
            if (get_location == null) {
                LOGGER.severe("programm error: location has been deleted but db has references, please run scrub");
                continue;
            }

            returnfile = get_location.stage_file_to_cache_location(_fileversion, destination);
            if (returnfile != null) {
                filelock.set_readable(_fileversion.get_id());
                break;
            }
        }
        if (returnfile == null) {
            filelock.release(_fileversion);
        }

        return returnfile;
    }

    /**
     * returns the location object for a given id
     *
     * @param _location_id int
     * @return Location
     */
    static StorageCell find(int _location_id) {
        return (StorageCell) Environment.data().get_dao(StorageCell.class).findById(_location_id);
    }

    /**
     * choose cache location with most free space
     *
     * @param _size size that is needed
     * @return Location, null of none found
     */
    static Location get_cache_location_for_size(long _size) {
        Location return_location = null;
        try {
            for (StorageCell cache_location : Environment.data().get_dao_location().find_all_by_type(TYPE.CACHE)) {
                // only with sufficient space and local filesystem, s3 possible but chunked uploads are expensive
                if (cache_location.get_free_space() >= _size) {
                    if (return_location == null) {
                        return_location = cache_location;
                    } else {
                        if (cache_location.get_free_space() > return_location.get_free_space()) {
                            return_location = cache_location;
                        }
                    }
                }
            }
        } catch (NullPointerException e) {
            LOGGER.severe("system has no cache location.");
        }
        return return_location;
    }

    /**
     * old but can be used later to verify. This is now done "on-the-fly".
     * Used by tests.
     *
     * @param _file file on filesystem
     * @return checksum
     */
    static String file_checksum(java.io.File _file) {
        try {
            FileInputStream in = new FileInputStream(_file);
            BufferedInputStream buffered_input = new BufferedInputStream(in);
            MessageDigest digester = MessageDigest.getInstance(Settings.default_hashing_algorithm);
            byte[] buffer = new byte[32768];
            int read;
            while ((read = buffered_input.read(buffer)) != -1) {
                digester.update(buffer, 0, read);
            }

            buffered_input.close();
            in.close();
            return DatatypeConverter.printHexBinary(digester.digest());
        } catch (IOException e) {
            LOGGER.warning("Error calculating checksum: " + e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe("Digest Algorithm not found: " + e);
        }
        return null;
    }

    /**
     * creates a new storage cell and writes it into database
     *
     * @param _type   type of the new storage cell
     * @param _config configuration object generated by ILocationConfig
     * @return new created storage cell
     * @see ILocationConfig
     */
    static StorageCell create_location(Location.TYPE _type, ILocationConfig _config) {
        StorageCell newconnector = null;
        try {
            if (!ILocationConfig.is_valid_for(_type, _config)) {
                throw new ConfigException("passed invalid config. Type: " + _type.toString() + " config: " + _config.toString());
            }
            switch (_type) {
                case CACHE:
                    if (!_config.get_application().m_setting.m_use_disk_location) {
                        throw new ConfigException("tried to create a " + _type + " location. But feature is disabled.");
                    }
                    newconnector = new MasterCache(_config, true);
                    break;
                case DISK:
                    if (!_config.get_application().m_setting.m_use_disk_location) {
                        throw new ConfigException("tried to create a " + _type + " location. But feature is disabled.");
                    }
                    newconnector = new MasterCache(_config);
                    break;
                case GOOGLE:
                    if (!_config.get_application().m_setting.m_use_google_location) {
                        throw new ConfigException("tried to create a " + _type + " location. But feature is disabled.");
                    }
                    newconnector = new GoogleCloudBucket(_config);
                    break;
                case S3:
                    if (!_config.get_application().m_setting.m_use_s3_location) {
                        throw new ConfigException("tried to create a " + _type + " location. But feature is disabled.");
                    }
                    newconnector = new AmazonS3Bucket(_config);
                    break;
                case SERVER:
                    if (!_config.get_application().m_setting.m_use_local_cluster_location) {
                        throw new ConfigException("tried to create a " + _type + " location. But feature is disabled.");
                    }
                    newconnector = new StorageServer(_config);
                    break;
            }
        } catch (ConfigException e) {
            LOGGER.warning(e.toString());
        } catch (IOException e) {
            LOGGER.warning("Server couldnt be created");
            LOGGER.warning(e.toString());
        }

        if (newconnector == null) {
            LOGGER.warning("failed to create a " + _type + " location with param: " + _config);
            return null;
        }

        IGenericDao connDao = Environment.data().get_dao(StorageCell.class);
        connDao.create(newconnector);
        return newconnector;
    }

    boolean get_maintenance();

    //

    /**
     * Faulty Locations are set o maintenance, so that no error spam would be produced.
     *
     * @param m_maintenance true for maintenance mode
     */
    void set_maintenance(boolean m_maintenance);

    Location.TYPE get_type();

    int get_id();

    /**
     * Writes the file to user's locations.
     * Needs the _version to determine user and checksum of the file for deduplication (const var)
     *
     * @param _file    file that is written to a cache location.
     * @param _version the fileversion to make persistent
     * @return id of location which got the file
     */
    StorageCell write_file(java.io.File _file, FileVersion _version);

    /**
     * check if a file is available at location
     *
     * @param _checksum expected resource
     * @return found
     */
    boolean contains(String _checksum) throws Exception;

    /**
     * deletes file at location, WARNING, do not use unless you are aware of internal effects.
     * Better use Fileversion.delete();
     *
     * @param _checksum resource to delete
     * @return success
     * @see FileVersion
     */
    boolean delete(String _checksum);

    long get_free_space();

    /**
     * Interface class, Retrives data from backend and writes it to a cache location.
     *
     * @param _fileversion fileversion to retrieve
     * @param _destination where the file will e placed
     * @return the file written to cache
     */
    java.io.File stage_file_to_cache_location(FileVersion _fileversion, String _destination);

    /**
     * db operation, unregister from cluster object
     */
    void delete();

    enum TYPE {
        CACHE,
        DISK,
        S3,
        GOOGLE,
        SERVER
    }
}