package jStrg.network_management.storage_management.config;

import jStrg.file_system.Application;
import jStrg.file_system.File;
import jStrg.network_management.storage_management.core.Location;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Configuration Interface for StorageCells, could be passed to a new to be created storage cell to configure.
 */
public interface ILocationConfig extends ILocationStandardValues {

    final static Logger LOGGER = Logger.getLogger(File.class.getName());

    /**
     * get a new instance of configuration object
     *
     * @return object to configure location
     */
    static ILocationConfig create_configurator() {
        return new LocationConfig();
    }

    // -----------  General  ---------------------

    /**
     * checks if a given config file is valid for a specific type of storage cell. All data needed to setup a minimal
     * storage cell of this type are checked.
     *
     * @param _type   storage cell type
     * @param _config configuration object
     * @return
     */
    static boolean is_valid_for(Location.TYPE _type, ILocationConfig _config) {

        if (_config.get_path() == null || _config.get_path().equals("")) {
            return false;
        }

        switch (_type) {
            case S3:
                if (_config.s3_get_access_id() != null
                        && _config.s3_get_access_key() != null
                        && !_config.s3_get_access_id().equals("")
                        && !_config.s3_get_access_key().equals("")
                        && _config.get_max_usage() != 0L
                        && _config.get_application() != null
                        ) {
                    return true;
                }
                break;
            case GOOGLE:
                if (_config.google_get_credential_file() != null
                        && _config.google_get_projectid() != null
                        && !_config.google_get_credential_file().equals("")
                        && !_config.google_get_projectid().equals("")
                        && _config.get_max_usage() != 0L
                        && _config.get_application() != null
                        ) {
                    if (!Files.exists(Paths.get(_config.google_get_credential_file()))) {
                        LOGGER.warning("specified google credentials file doesnt exist: " + _config.google_get_credential_file());
                    }
                    return true;
                }
                break;
            case SERVER:
                if (!_config.get_port().equals("")
                        && !_config.get_ip_address().equals("")
                        && !_config.get_network_interface().equals("")
                        && !_config.get_servername().equals("")
                        && _config.get_application() != null
                        ) {
                    return true;
                }
                break;
            case DISK:
                if (_config.get_max_usage() != 0L)
                    return true;
            default:
                return true; // if there are no additional checks, the connector only needs path information
        }
        return false;
    }

    String get_path();

    ILocationConfig set_path(String _path);

    String get_ip_address();

    // -----------  Storage Server  --------------
    ILocationConfig set_ip_address(String _ip_address);

    String get_servername();

    ILocationConfig set_servername(String _servername);

    String get_port();

    ILocationConfig set_port(String _port);

    String get_network_interface();

    ILocationConfig set_network_interface(String _network_interface);

    Application get_application();

    ILocationConfig set_application(Application _application);

    // -----------  S3  --------------------------
    ILocationConfig s3_set_access_key(String _key);

    String s3_get_access_key();

    ILocationConfig s3_set_access_id(String _key);

    String s3_get_access_id();

    // -----------  Google  ----------------------
    ILocationConfig google_set_credential_file(String _key);

    String google_get_credential_file();

    ILocationConfig google_set_projectid(String _path);

    String google_get_projectid();
}
