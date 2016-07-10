package jStrg.network_management.storage_management.external;


import com.google.gcloud.AuthCredentials;
import com.google.gcloud.storage.*;
import jStrg.data_types.exceptions.ConfigException;
import jStrg.file_system.*;
import jStrg.network_management.storage_management.config.ILocationConfig;
import jStrg.network_management.storage_management.core.Location;
import jStrg.network_management.storage_management.core.StorageCell;

import javax.persistence.Transient;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * StorageCell Connector for google cloud storage.
 */
@javax.persistence.Entity
public class GoogleCloudBucket extends StorageCell {

    private final static Logger LOGGER = Logger.getLogger(Settings.location_logging_target);
    /**
     * Google storage object, connection to google cloud storage
     */
    @Transient
    private com.google.gcloud.storage.Storage m_storage_service;
    /**
     * path to authentication file provided by google
     **/
    private String m_path_to_google_json;

    /**
     * name of google project the bucket (in m_path) belongs to
     **/
    private String m_projectid;

    public GoogleCloudBucket(ILocationConfig _config) throws ConfigException {
        super(TYPE.GOOGLE, _config);

        m_path_to_google_json = _config.google_get_credential_file();
        m_projectid = _config.google_get_projectid();
        m_cluster = _config.get_application().m_google_cloud_bucket_cluster;

        try {
            if (!connectGoogle(this)) {
                LOGGER.warning("get service returned NULL");
            }
            update_free_space();
        } catch (IOException e) {
            LOGGER.warning("Error while retrieving google api key: " + e);
        } catch (GeneralSecurityException e) {
            LOGGER.warning("Security exeption while connecting to gcloud: " + e);
        } catch (StorageException se) {
            LOGGER.severe("Error contacting google cloud storage" + se);
        } finally {
            if (get_storage_service() == null) {
                enter_maintenance();
                LOGGER.severe("Google Localtion was unable to initialize, set to maintenance. " + this);
            }
        }
    }

    protected GoogleCloudBucket() {
    }

    /**
     * checks if the google servers are available
     *
     * @param _application_id int
     * @return boolean
     */
    public static boolean available_for_application(int _application_id) {
        // ping google
        return true;
    }

    protected void update_free_space() {
        long used = 0L;
        try {
            for (BlobInfo blobinfo : m_storage_service.list(get_path()).values()) {
                used += blobinfo.size();
            }
        } catch (Exception e) {
            LOGGER.warning("Failure while retrieving status from: " + this + ", going into maintenance.");
            this.enter_maintenance();
        }
        LOGGER.finest(m_path + " reported usage: " + used);

        m_free_space = m_max_usage - used;
    }

    public boolean contains(String _checksum) throws Exception {
        Blob blob = null;

        try {
            BlobId blobid = BlobId.of(get_path(), _checksum);
            blob = Blob.load(get_storage_service(), blobid);
        } catch (StorageException e) {
            this.enter_maintenance();
            LOGGER.severe("Error while blob load: " + e);
            throw new Exception("Error while blob load");
        }
        return blob != null;
    }

    public boolean delete(String _checksum) {
        try {
            if (!contains(_checksum)) {
                LOGGER.fine("Failure while deleting " + _checksum + " from " + this + " : not found");
                return false;
            }
        } catch (Exception e) {
            LOGGER.fine("Failure while deleting " + _checksum + " from " + this + " : cannot retrieve information");
            return false;
        }
        BlobId blobid = BlobId.of(get_path(), _checksum);
        BlobInfo blobinfo = m_storage_service.get(blobid);
        long size = blobinfo.size();
        if (m_storage_service.delete(blobid)) {
            LOGGER.finest("file " + _checksum + " deleted on location: " + this);
            dec_used_space(size);
            return true;
        } else {
            LOGGER.fine("Failure while deleting " + _checksum + " from " + this + " : not found");
            return false;
        }
    }

    @Override
    public File stage_file_to_cache_location(FileVersion _fileversion, String _destination) {
        Location cache_location = Location.get_cache_location_for_size(_fileversion.get_size());
        if (cache_location == null) {
            LOGGER.warning("no suitable cache Location found.");
            return null;
        }
        File returnfile = null;
        if (_destination == null) {
            LOGGER.severe("No existing filelock! Horrible!");
            return null;
        }
        BlobId blobid = BlobId.of(get_path(), _fileversion.get_checksum());
        Blob blob = Blob.load(get_storage_service(), blobid);
        if (blob == null) {
            LOGGER.warning("No such object");
            return null;
        }
        try {
            FileOutputStream fos = new FileOutputStream(_destination);
            PrintStream write_to = new PrintStream(fos);
            MessageDigest digester = MessageDigest.getInstance(Settings.default_hashing_algorithm);
            LOGGER.finest("download start");
            if (blob.info().size() < Settings.bytes_per_upload_chunk_for_google) {
                // Blob is small read all its content in one request
                byte[] content = blob.content();
                digester.update(content);
                write_to.write(content);
            } else {
                // When Blob size is big or unknown use the blob's channel reader.
                try (BlobReadChannel reader = blob.reader()) {
                    // WritableByteChannel channel = Channels.newChannel(write_to);
                    FileChannel channel = new FileOutputStream(_destination).getChannel();
                    ByteBuffer bytes = ByteBuffer.allocate(Settings.bytes_per_upload_chunk_for_google);
                    while (reader.read(bytes) != -1) {
                        bytes.flip();
                        channel.write(bytes);
                        bytes.flip();
                        digester.update(bytes);
                        bytes.compact();
                    }
                    channel.close();
                }
                write_to.close();
                fos.close();
            }
            returnfile = verify_cachefile(DatatypeConverter.printHexBinary(digester.digest()),
                    _fileversion.get_checksum(),
                    _destination
            );
        } catch (IOException e) {
            LOGGER.warning("failed to open outputstream: " + e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warning("No " + Settings.default_hashing_algorithm + " found: " + e);
        }
        if (returnfile == null)
            LOGGER.finest("something went wrong while staging " + _fileversion + " from " + this);

        return returnfile;
    }

    public StorageCell write_file(File _file, FileVersion _version) { // This method is meant to be static, but cannot be abstract
        long filesize = _file.length();
        GoogleCloudBucket storage = (GoogleCloudBucket) get_location_for_size(filesize, TYPE.GOOGLE);
        if (storage == null) {
            LOGGER.warning("could not find suitable location for file: " + _file);
            return null;
        }
        BlobId blob_id = BlobId.of(storage.get_path(), _version.get_checksum());
        Blob blob = Blob.load(storage.get_storage_service(), blob_id);

        if (blob != null) {
            LOGGER.finest("found duplicate in bucket: " + storage.get_path() + ", file: " + _file);
            return null;
        }
        BlobInfo blobinfo = null;
        Blob new_blob;
        try {
            blobinfo = BlobInfo.builder(blob_id)
                    .contentType(Files.probeContentType(Paths.get(_file.getAbsolutePath())))
                    .build();
        } catch (IOException e) {
            LOGGER.warning("Error while accessing: " + _file);
        }
        LOGGER.finest("upload start");
        if (filesize > Settings.bytes_per_upload_chunk_for_google) {
            // When content is not available or large (1MB or more) it is recommended
            // to write it in chunks via the blob's channel writer.
            if (blobinfo != null) {
                new_blob = new Blob(storage.get_storage_service(), blobinfo);
            } else {
                LOGGER.warning("unable to create blob for chunked upload.");
                return null;
            }
            try (BlobWriteChannel writer = new_blob.writer()) {
                byte[] buffer = new byte[Settings.bytes_per_upload_chunk_for_google];
                try (InputStream input = new FileInputStream(_file)) {
                    BufferedInputStream bis = new BufferedInputStream(input);
                    int limit;
                    while ((limit = bis.read(buffer)) >= 0) {
                        try {
                            writer.write(ByteBuffer.wrap(buffer, 0, limit));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    storage.inc_used_space(filesize);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(_file.getAbsolutePath()));
                // create the blob in one request.
                storage.m_storage_service.create(blobinfo, bytes);
                storage.inc_used_space(filesize);
            } catch (IOException e) {
                LOGGER.warning("trouble getting: " + _file.getAbsolutePath());
            }

        }
        try {
            if (this.contains(_version.get_checksum())) {
                LOGGER.finest("Blob was created in location: " + storage);
                return this;
            }
        } catch (Exception e) {
            this.enter_maintenance();
        }
        return null;
    }

    /**
     * initiates connection to google.
     *
     * @param _bucket the GoogleCloudBucket Object
     * @return success
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private boolean connectGoogle(GoogleCloudBucket _bucket) throws IOException, GeneralSecurityException, ConfigException {
        if (null == _bucket.get_storage_service()) {
            File json_credentials = new File(m_path_to_google_json);
            if (json_credentials.toString().equals("") || !json_credentials.canRead()) {
                throw new ConfigException("Error while retrieving google credentials from: \"" + json_credentials + "\"");
            }
            _bucket.set_storage_service(
                    StorageOptions.builder()
                            .authCredentials(AuthCredentials.createForJson(new FileInputStream(m_path_to_google_json)))
                            .projectId(m_projectid)
                            .build()
                            .service()
            );
        }
        return (_bucket.get_storage_service() != null);
    }

    @Override
    public String toString() {
        StringBuilder returnstring = new StringBuilder("<GoogleClouadBucket::{m_id: " + m_id + ", m_type: '" + m_type + "'");
        returnstring.append(", mpath: '" + m_path + "', free: '" + get_free_space() + "'");
        return returnstring.append(" }>").toString();
    }

    private com.google.gcloud.storage.Storage get_storage_service() {
        return m_storage_service;
    }

    private void set_storage_service(com.google.gcloud.storage.Storage storageService) {
        this.m_storage_service = storageService;
    }


    private void set_free_space(long m_free_space) {
        this.m_free_space = m_free_space;
    }


    public String get_path_to_google_json() {
        return m_path_to_google_json;
    }

    public void set_path_to_google_json(String path_to_google_json) {
        this.m_path_to_google_json = path_to_google_json;
    }


}
