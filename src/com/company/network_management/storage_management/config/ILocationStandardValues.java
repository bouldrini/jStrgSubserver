package jStrg.network_management.storage_management.config;

/**
 * Created by henne on 01.06.16.
 */
public interface ILocationStandardValues {

    long get_max_usage();

    void set_max_usage(long _max_usage);

    /**
     * get path string, meaning varies for the storage types.
     *
     * @return location path
     */
    String get_path();
}
