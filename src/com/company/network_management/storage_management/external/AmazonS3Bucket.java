package jStrg.network_management.storage_management.external;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import jStrg.data_types.exceptions.ConfigException;
import jStrg.file_system.File;
import jStrg.file_system.FileVersion;
import jStrg.file_system.Settings;
import jStrg.network_management.storage_management.config.ILocationConfig;
import jStrg.network_management.storage_management.core.Location;
import jStrg.network_management.storage_management.core.StorageCell;
import org.apache.http.HttpStatus;

import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.xml.bind.DatatypeConverter;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * Amazon S3 StorageCell Bucket.
 */

@Entity
public class AmazonS3Bucket extends StorageCell {

    private final static Logger LOGGER = Logger.getLogger(Settings.location_logging_target);
    /**
     * object that is holding the connection to s3
     */
    @Transient
    private AmazonS3Client m_s3client;

    private String m_s3_key;
    private String m_s3_id;

    protected AmazonS3Bucket() {
    }

    public AmazonS3Bucket(ILocationConfig _config) throws ConfigException {
        super(TYPE.S3, _config);

        m_s3_id = _config.s3_get_access_id();
        m_s3_key = _config.s3_get_access_key();
        m_cluster = _config.get_application().m_amazon_s3_bucket_cluster;

        connect_s3_client();
    }

    /**
     * checks if the amazon s3 servers are available
     *
     * @param _application_id int
     * @return boolean
     */
    public static boolean available_for_application(int _application_id) {
        // ping amazon
        return true;
    }

    private void connect_s3_client() throws ConfigException {
        if (m_s3client != null) {
            return;
        }
        System.setProperty(SDKGlobalConfiguration.ENABLE_S3_SIGV4_SYSTEM_PROPERTY, "true");
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(m_s3_id, m_s3_key);
        m_s3client = new AmazonS3Client(awsCredentials);
        Region frankfurt = Region.getRegion(Regions.fromName(m_s3client.getBucketLocation(m_path)));
        m_s3client.setRegion(frankfurt);
        update_free_space();
    }

    protected void update_free_space() {
        long used = 0L;
        try {
            for (S3ObjectSummary object : m_s3client.listObjects(m_path).getObjectSummaries()) {
                used += object.getSize();
            }
            LOGGER.finest("'" + m_path + "' reported used space: " + used);
        } catch (AmazonS3Exception e) {
            if (e.getErrorCode().equals("NoSuchBucket")) {
                LOGGER.severe("bucket: '" + m_path + "' does not exist.");
            } else {
                LOGGER.severe("unhandled exception: " + e);
            }
            enter_maintenance();
        }
        m_free_space = this.m_max_usage - used;
    }

    public boolean contains(String _checksum) {
        boolean success = false;
        try {
            m_s3client.getObjectMetadata(m_path, _checksum);
            success = true;
        } catch (AmazonServiceException e) {
            if (!(e.getStatusCode() == HttpStatus.SC_NOT_FOUND)) {
                Location.LOGGER.finest("error while looking for " + _checksum + " at " + this);
                enter_maintenance();
            }
        }
        return success;
    }

    public boolean delete(String _checksum) {
        boolean success = false;
        if (!contains(_checksum)) {
            LOGGER.severe("cant delete non existing file: " + _checksum + " at " + this);
        }
        try {
            long size = m_s3client.getObjectMetadata(m_path, _checksum).getContentLength();
            m_s3client.deleteObject(m_path, _checksum);
            dec_used_space(size);
            success = true;
        } catch (AmazonServiceException e) {
            Location.LOGGER.warning("Request was rejected by Amazon" + e);
            enter_maintenance();
        } catch (AmazonClientException e) {
            Location.LOGGER.warning("Error conencting to Amazon: " + e);
            enter_maintenance();
        }
        return success;
    }

    public java.io.File stage_file_to_cache_location(FileVersion _fileversion, String _destination) {
        Location cache_location = Location.get_cache_location_for_size(_fileversion.get_size());
        if (cache_location == null) {
            LOGGER.warning("no suitable cache Location found.");
            return null;
        }
        java.io.File returnfile = null;
        if (_destination == null) {
            LOGGER.severe("No existing filelock! Horrible!");
            return null;
        }
        LOGGER.finest("download start from location: " + this);
        try {
            FileOutputStream fos = new FileOutputStream(_destination);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            GetObjectRequest request = new GetObjectRequest(m_path, _fileversion.get_checksum());
            S3Object returned_object = m_s3client.getObject(request);
            InputStream data = returned_object.getObjectContent();

            MessageDigest digester = MessageDigest.getInstance(Settings.default_hashing_algorithm);

            byte buffer[] = new byte[Settings.bytes_per_upload_chunk];
            int read;
            while ((read = data.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
                digester.update(buffer, 0, read);
            }
            returnfile = verify_cachefile(DatatypeConverter.printHexBinary(digester.digest()),
                    _fileversion.get_checksum(),
                    _destination
            );
            bos.close();
            fos.close();
        } catch (AmazonServiceException e) {
            Location.LOGGER.warning("Request was rejected by Amazon" + e);
            enter_maintenance();
        } catch (AmazonClientException e) {
            Location.LOGGER.warning("Error conencting to Amazon: " + e);
            enter_maintenance();
        } catch (IOException e) {
            LOGGER.warning("Error while accessing disk: " + e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warning("No " + Settings.default_hashing_algorithm + " found: " + e);
        }
        return returnfile;
    }

    public StorageCell write_file(java.io.File _file, FileVersion _version) {
        long filesize = _file.length();
        AmazonS3Bucket s3_location = (AmazonS3Bucket) get_location_for_size(filesize, TYPE.S3);
        if (s3_location == null) {
            Location.LOGGER.severe("No available Location of type S3 for file: " + _file);
            return null;
        } else {
            Location.LOGGER.finest("Found location: " + s3_location);
        }


        try {
            try {
                s3_location.m_s3client.getObjectMetadata(s3_location.m_path, _version.get_checksum());
                Location.LOGGER.finest("file already exists in bucket: " + _file);
            } catch (AmazonServiceException e) {
                if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                    s3_location.m_s3client.putObject(new PutObjectRequest(s3_location.m_path, _version.get_checksum(), _file));
                    Location.LOGGER.finest("file write successful for: " + _file);
                } else {
                    throw e;
                }
            }

        } catch (AmazonServiceException e) {
            Location.LOGGER.warning("Request was rejected by Amazon" + e);
            s3_location.enter_maintenance();
        } catch (AmazonClientException e) {
            Location.LOGGER.warning("Error conencting to Amazon: " + e);
            s3_location.enter_maintenance();
        }
        if (this.contains(_version.get_checksum())) {
            s3_location.inc_used_space(filesize);
            return this;
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder returnstring = new StringBuilder("<AmazonS3Bucket::{m_id: " + m_id + ", m_type: '" + m_type + "'");
        returnstring.append(", m_s3bucket: '" + m_path + "', free: '" + get_free_space() + "'");
        return returnstring.append(" }>").toString();
    }
}
