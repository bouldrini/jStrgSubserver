package jStrg.network_management.storage_management.core;

import jStrg.database.DatabaseEntity;
import jStrg.database.IGenericDao;
import jStrg.database.ILocationDao;
import jStrg.environment.Environment;
import jStrg.file_system.FileVersion;
import jStrg.file_system.Settings;
import jStrg.network_management.storage_management.cluster.Cluster;
import jStrg.network_management.storage_management.config.ILocationConfig;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;

/**
 * Standard attributes every storage cell must implement.
 */

@Entity
abstract public class StorageCell extends DatabaseEntity implements Location {

    protected final static Logger LOGGER = Logger.getLogger(Settings.location_logging_target);
    @ManyToOne
    public Cluster m_cluster;
    protected Location.TYPE m_type;
    /**
     * URL, kann bucket, pfad oä sein
     */
    protected String m_path;
    protected boolean m_maintenance;
    protected long m_max_usage;
    // private LinkedHashMap<String, Path> m_files = new LinkedHashMap();
    /**
     * cached free space value
     */
    protected long m_free_space;

    protected StorageCell() {
    }

    public StorageCell(Location.TYPE _type, ILocationConfig _config) {
        m_path = _config.get_path();
        m_maintenance = false;
        m_type = _type;
        m_max_usage = _config.get_max_usage();
    }

    /**
     * searches for a suitable location for a file to write. By now, this is choosen by most available space.
     *
     * @param _size that is needed to write
     * @param _type storage type to look for
     * @return location object
     */
    public static Location get_location_for_size(long _size, Location.TYPE _type) {
        ILocationDao locationdao = Environment.data().get_dao_location();
        // check for type
        if (!locationdao.contains_type(_type)) {
            return null;
        }
        Location return_value = null;
        long best_free_space = 0L;
        for (StorageCell location : locationdao.find_all_by_type(_type)) {
            if (location.get_maintenance()) {
                Location.LOGGER.finest("ignoring location under mainenance: " + location.get_type() + location.get_path());
                continue;
            }
            Location.LOGGER.finest("Checking Location: " + location + " for size: " + _size);
            long check_for_space = location.get_free_space();
            if (check_for_space > best_free_space && check_for_space > _size) { // Need better mechanism to choose
                return_value = location;
                best_free_space = check_for_space;
            }
        }
        return return_value;
    }

    /**
     * Little helper to verify the file gotten from storage backend.
     *
     * @param _got_digest  computed while download
     * @param _need_digest value from database
     * @param _destination location of the real file
     * @return real file, null in case of failure
     */
    protected static java.io.File verify_cachefile(String _got_digest, String _need_digest, String _destination) {
        java.io.File returnfile = null;
        if (_got_digest.equals(_need_digest)) {
            returnfile = new java.io.File(_destination);
            LOGGER.finest("Download to cache done. checksum: " + _got_digest);
        } else {
            returnfile = null;
            LOGGER.warning("checksum mismatch: got " + _got_digest + ", need: " + _need_digest);
        }
        return returnfile;
    }

    public static void delete_all() {
        for (StorageCell cell : all()) {
            cell.delete();
        }
    }

    public static List<StorageCell> all() {
        return genericdao().findAll();
    }

    // DATABASE
    private static IGenericDao genericdao() {
        return Environment.data().get_dao(StorageCell.class);
    }

    private static ILocationDao specific_dao() {
        return Environment.data().get_dao_location();
    }

    protected void inc_used_space(long _inc) {
        this.m_free_space -= _inc;
        if (m_free_space < 0) {
            LOGGER.severe("Invalid free space: " + m_free_space + " for " + this);
        }
    }

    protected void dec_used_space(long _dec) {
        this.m_free_space += _dec;
    }

    abstract protected void update_free_space();

    protected void enter_maintenance() {
        set_maintenance(true);
        db_update();
    }

    /**
     * updates object in database
     */
    protected void db_update() {
        genericdao().update(this);
    }

    public void delete() {
        Cluster cluster = m_cluster;
        m_cluster = null;
        if (cluster != null) {
            cluster.unregister(this);
            cluster.db_update();
        }
        genericdao().delete(this);
    }

    // GETTER / SETTETSException Description: This class does not define a public default constructor, or the constructor raised an exception.

    public int get_id() {
        return m_id;
    }

    public boolean get_maintenance() {
        return m_maintenance;
    }

    public void set_maintenance(boolean m_maintenance) {
        this.m_maintenance = m_maintenance;
    }

    public String get_path() {
        return m_path;
    }

    public Location.TYPE get_type() {
        return m_type;
    }

    public long get_free_space() {
        return m_free_space;
    }

    public long get_max_usage() {
        return m_max_usage;
    }

    public void set_max_usage(long _max_usage) {
        m_max_usage = _max_usage;
    }
}
