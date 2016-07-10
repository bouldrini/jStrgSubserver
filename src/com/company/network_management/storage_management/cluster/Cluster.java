package jStrg.network_management.storage_management.cluster;

import jStrg.database.DatabaseEntity;
import jStrg.database.IClusterDao;
import jStrg.database.IGenericDao;
import jStrg.environment.Environment;
import jStrg.file_system.Application;
import jStrg.file_system.File;
import jStrg.file_system.FileVersion;
import jStrg.file_system.Settings;
import jStrg.network_management.storage_management.core.Location;
import jStrg.network_management.storage_management.core.StorageCell;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.List;
import java.util.logging.Logger;

@Entity
public abstract class Cluster extends DatabaseEntity {
    public final static Logger LOGGER = Logger.getLogger(Settings.location_logging_target);
    public int m_total_space;
    public int m_used_space;
    @OneToOne
    Application m_application;
    @OneToMany
    List<StorageCell> m_storage_cells;

    public Cluster() {
    }

    public Cluster(Application _application) {
        m_application = _application;
    }

    // DATABASE
    protected static IGenericDao genericdao() {
        return Environment.data().get_dao(Cluster.class);
    }

    private static IClusterDao specific_dao() {
        return Environment.data().get_dao_cluster();
    }

    public static List<Cluster> find_by_app(Application _app) {
        return specific_dao().find(_app);
    }

    public static void delete_all() {
        for (Application app : Application.all()) {
            app.m_amazon_s3_bucket_cluster = null;
            app.m_google_cloud_bucket_cluster = null;
            app.m_local_storage_cluster = null;
            app.db_update();
        }
        genericdao().deleteAll();
    }

    public void db_update() {
        genericdao().update(this);
    }

    public boolean has_enough_space(long _file_size) {
        for (Location storage_cell : this.m_storage_cells) {
            if (storage_cell.get_free_space() > _file_size) {
                return true;
            }
        }
        return false;
    }

    public boolean write_file(File _file, FileVersion _file_version){
        StorageCell best_cell = null;
        for(StorageCell cell : this.m_storage_cells){
            if(best_cell == null || (best_cell.get_free_space() < cell.get_free_space())){
                best_cell = cell;
            }
        }
        best_cell.write_file(_file.get_real_file(), _file_version);
        return true;
    }

    public void unregister(StorageCell _cell) {
        this.m_storage_cells.remove(_cell);
    }

 /*   public boolean upload(File _file){
        long best_free_space = 0;

        for(Location storage_cell : this.m_storage_cells){
            if (storage_cell.get_maintenance()) {
                Location.LOGGER.finest("ignoring storage_cell under mainenance: " + storage_cell.get_type() + storage_cell.get_path());
                continue;
            }
            Location.LOGGER.finest("Checking Location: " + storage_cell + " for size: " + _size);
            long check_for_space = storage_cell.get_free_space();
            if (check_for_space > best_free_space && check_for_space > _size) { // Need better mechanism to choose
                return_value = storage_cell;
                best_free_space = check_for_space;
            }
        }
    }*/
}
