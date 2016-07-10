package jStrg.network_management.storage_management.cluster;

import jStrg.database.IGenericDao;
import jStrg.environment.Environment;
import jStrg.file_system.Application;
import jStrg.network_management.core.Server;
import jStrg.network_management.storage_management.config.ILocationConfig;
import jStrg.network_management.storage_management.core.Location;
import jStrg.network_management.storage_management.external.AmazonS3Bucket;

import javax.persistence.Entity;

@Entity
public class AmazonS3BucketCluster extends Cluster {

    public AmazonS3BucketCluster() {

    }

    public AmazonS3BucketCluster(Application _application) {
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

    public AmazonS3Bucket register(ILocationConfig _config) {
        AmazonS3Bucket amazon_bucket = (AmazonS3Bucket) Location.create_location(Location.TYPE.S3, _config);
        this.m_storage_cells.add(amazon_bucket);
        return amazon_bucket;
    }
}
