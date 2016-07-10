package jStrg.network_management.storage_management.cluster;

import jStrg.database.IGenericDao;
import jStrg.environment.Environment;
import jStrg.file_system.Application;
import jStrg.network_management.core.Server;
import jStrg.network_management.storage_management.config.ILocationConfig;
import jStrg.network_management.storage_management.core.Location;
import jStrg.network_management.storage_management.core.StorageCell;

import javax.persistence.Entity;

@Entity
public class DiskCluster extends Cluster {

    public DiskCluster() {

    }

    public DiskCluster(Application _application) {
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

    public StorageCell register(ILocationConfig _config) {
        StorageCell cell = Location.create_location(Location.TYPE.DISK, _config);
        this.m_storage_cells.add(cell);
        return cell;
    }
}
