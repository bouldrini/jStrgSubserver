package jStrg.network_management.storage_management.cluster;

import jStrg.database.IGenericDao;
import jStrg.environment.Environment;
import jStrg.file_system.Application;
import jStrg.file_system.File;
import jStrg.file_system.FileVersion;
import jStrg.network_management.storage_management.config.ILocationConfig;
import jStrg.network_management.storage_management.core.Location;
import jStrg.network_management.storage_management.core.StorageCell;
import jStrg.network_management.storage_management.external.GoogleCloudBucket;

import javax.persistence.Entity;

@Entity
public class GoogleCloudBucketCluster extends Cluster {

    public GoogleCloudBucketCluster() {

    }

    public GoogleCloudBucketCluster(Application _application) {
        super(_application);
        dao().create(this);
    }

    /**
     * get dao object of this class
     *
     * @return dao object
     */
    public static IGenericDao dao() {
        return Environment.data().get_dao(GoogleCloudBucket.class);
    }

    public GoogleCloudBucket register(ILocationConfig _config) {
        GoogleCloudBucket google_bucket = (GoogleCloudBucket) Location.create_location(Location.TYPE.GOOGLE, _config);
        this.m_storage_cells.add(google_bucket);
        return google_bucket;
    }
}
