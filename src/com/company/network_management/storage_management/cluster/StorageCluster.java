package jStrg.network_management.storage_management.cluster;

import jStrg.database.IGenericDao;
import jStrg.environment.Environment;
import jStrg.file_system.Application;
import jStrg.file_system.File;
import jStrg.file_system.FileVersion;
import jStrg.network_management.core.Server;
import jStrg.network_management.storage_management.config.ILocationConfig;
import jStrg.network_management.storage_management.core.Location;
import jStrg.network_management.storage_management.core.StorageCell;
import jStrg.network_management.storage_management.internal.StorageServer;

import javax.persistence.Entity;

@Entity
public class StorageCluster extends Cluster {

    public StorageCluster() {
    }

    public StorageCluster(Application _application) {
        super(_application);
        dao().create(this);
    }

    /**
     * get dao object of this class
     *
     * @return dao object
     */
    public static IGenericDao dao() {
        return Environment.data().get_dao(Server.class);
    }

    public StorageServer register(ILocationConfig _config) {
        StorageServer storage_server = (StorageServer) Location.create_location(Location.TYPE.SERVER, _config);
        this.m_storage_cells.add(storage_server);
        return storage_server;
    }
}
