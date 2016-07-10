package jStrg.network_management.storage_management.internal;

import jStrg.data_types.exceptions.ConfigException;
import jStrg.database.IUserDao;
import jStrg.environment.Environment;
import jStrg.file_system.FileVersion;
import jStrg.file_system.Settings;
import jStrg.network_management.core.Server;
import jStrg.network_management.storage_management.config.ILocationConfig;
import jStrg.network_management.storage_management.core.StorageCell;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

@Entity
public class StorageServer extends StorageCell {
    // ATTRIBUTES
    private final static Logger LOGGER = Logger.getLogger(Settings.location_logging_target);
    // RELATIONS
    @OneToOne
    Server m_server;

    // CONSTRUCTORS
    protected StorageServer() {
    }

    public StorageServer(ILocationConfig _config) throws ConfigException, IOException {
        super(TYPE.SERVER, _config);

        Server server = new Server(_config.get_servername(), _config.get_ip_address(), _config.get_port(), _config.get_network_interface());
        m_server = server;
        m_cluster = _config.get_application().m_local_storage_cluster;
        update_free_space();

    }

    // DATABASE
    private static IUserDao specific_dao() {
        return Environment.data().get_dao_user();
    }

    public Server server() {
        return m_server;
    }

    // HELPERe

    @Override
    protected void update_free_space() {
        // invoke Request to the Server asking for free space
        m_free_space = 10000000;
        set_maintenance(true);
    }

    @Override
    public StorageCell write_file(File _file, FileVersion _version) {
        // establish connection
        // send file upload request
        // send file
        // receive answer
        return this;
    }

    /**
     * checks if a file with the checksum is located on the server
     *
     ** @return bool
     */

    @Override
    public boolean contains(String _checksum) throws Exception {

        // Implement RequestType for that in jStrgStorageServer and jStrg Project
        return false;
    }

    /**
     * delete a file with checksum on that server
     *
     * @return bool
     */

    @Override
    public boolean delete(String _checksum) {

        // find file by checksum

        Server server = m_server;
        // invoke delete file request and send it to the server

        return false;
    }

    @Override
    public File stage_file_to_cache_location(FileVersion _fileversion, String _destination) {
        return null;
    }

    public String toString() {
        return "<StorageServer::{m_id: " + this.get_id() + ", m_ip_address: " + this.m_server.m_ip_address + ", m_servername: " + this.m_server.m_servername + ", m_port: '" + this.m_server.m_port + "', m_type: " + this.m_type + "}>";
    }
}
