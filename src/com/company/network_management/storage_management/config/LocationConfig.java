package jStrg.network_management.storage_management.config;

import jStrg.file_system.Application;

/**
 * implements an configuration object for StorageCell
 */
public class LocationConfig implements ILocationConfig {


    private String m_path;
    private long m_max_usage = 0L;

    private String m_s3_key;
    private String m_s3_id;

    private String m_google_projectid;
    private String m_google_json_path;

    private String m_ip_address;
    private String m_servername;
    private String m_port;

    private Application m_application;

    private String m_network_interface;

    public ILocationConfig set_path(String _path) {
        m_path = _path;
        return this;
    }

    public String get_path() {
        return m_path;
    }

    @Override
    public ILocationConfig set_application(Application _application) {
        m_application = _application;
        return this;
    }

    @Override
    public Application get_application() {
        return m_application;
    }

    @Override
    public ILocationConfig set_ip_address(String _ip_address) {
        m_ip_address = _ip_address;
        return this;
    }

    @Override
    public String get_ip_address() {
        return m_ip_address;
    }

    @Override
    public ILocationConfig set_servername(String _servername) {
        m_servername = _servername;
        return this;
    }

    @Override
    public String get_servername() {
        return m_servername;
    }

    @Override
    public ILocationConfig set_port(String _port) {
        m_port = _port;
        return this;
    }

    @Override
    public String get_port() {
        return m_port;
    }

    @Override
    public ILocationConfig set_network_interface(String _network_interface) {
        m_network_interface = _network_interface;
        return this;
    }

    @Override
    public String get_network_interface() {
        return m_network_interface;
    }

    public long get_max_usage() {
        return m_max_usage;
    }

    public void set_max_usage(long _max_usage) {
        m_max_usage = _max_usage;
    }

    public ILocationConfig s3_set_access_key(String _key) {
        m_s3_key = _key;
        return this;
    }

    public String s3_get_access_key() {
        return m_s3_key;
    }

    public ILocationConfig s3_set_access_id(String _key) {
        m_s3_id = _key;
        return this;
    }

    public String s3_get_access_id() {
        return m_s3_id;
    }

    public ILocationConfig google_set_credential_file(String _path) {
        m_google_json_path = _path;
        return this;
    }

    public String google_get_credential_file() {
        return m_google_json_path;
    }

    @Override
    public ILocationConfig google_set_projectid(String _id) {
        m_google_projectid = _id;
        return this;
    }

    @Override
    public String google_get_projectid() {
        return m_google_projectid;
    }
}
