package com.company.file_system;

// REQUIREMENTS
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Settings {
    // CONSTRUCTORS
    public Settings() {
    }
    public Settings(int _id, String _app_title) {

    }

    // ATTRIBUTES
    public String m_default_cache_location = "/tmp/";
    public String m_network_communication_secret1;
    public String m_network_communication_secret2;
    public int m_network_communication_port;
    public String m_internal_communication_secret;

    // HELPER

    /**
     * Takes the logging level as string and builds the proper Level. Defaults to Level.ALL
     *
     * @param _level the logging level case insensitive.
     * @return loglevel that can be passed to logging facility
     */
    private static Level get_log_level(String _level) {
        _level = _level.toLowerCase();
        Level level;
        switch (_level) {
            case "finest":
                level = Level.FINEST;
                break;
            case "fine":
                level = Level.FINE;
                break;
            case "info":
                level = Level.INFO;
                break;
            case "warning":
                level = Level.WARNING;
                break;
            case "severe":
                level = Level.SEVERE;
                break;
            default:
                level = Level.ALL;
        }
        return level;
    }

    /**
     * Updates default config. Values are read from $HOME/.jstrg.config
     *
     * @return success
     */
    public static Settings read_global_config() {
        Settings settings = new Settings();
        try {
            Properties prop = new Properties();
            String propFileName = System.getProperty("user.home") + "/.jstrg-subserver.conf";
            FileInputStream inputStream = new FileInputStream(propFileName);
            prop.load(inputStream);
            inputStream.close();

            if (prop.getProperty("network_communication_port") != null) {
                settings.m_network_communication_port = Integer.parseInt(prop.getProperty("network_communication_port"));
            }
            if (prop.getProperty("network_communication_secret1") != null) {
                settings.m_network_communication_secret1 = prop.getProperty("network_communication_secret1");
            }
            if (prop.getProperty("network_communication_secret2") != null) {
                settings.m_network_communication_secret2 = prop.getProperty("network_communication_secret2");
            }
            if (prop.getProperty("internal_communication_secret") != null) {
                settings.m_internal_communication_secret = prop.getProperty("gcloud_bucket");
            }
            if (prop.getProperty("cache_default") != null) {
                settings.m_default_cache_location = prop.getProperty("cache_default");
            }
            return settings;
        } catch (FileNotFoundException e) {
            System.out.println("config file not found: " + e);
        } catch (IOException e) {
            System.out.println("Exception while accessing config file: " + e);
        }
        return null;
    }

    public String toString() {
        StringBuilder returnstring = new StringBuilder("<Settings::{");
        returnstring.append(", m_network_communication_port: " + m_network_communication_port);
        returnstring.append(", m_network_communication_secret1 : " + m_network_communication_secret1);
        returnstring.append(", m_network_communication_secret2: " + m_network_communication_secret2);
        returnstring.append(", m_internal_communication_secret: " + m_internal_communication_secret);
        returnstring.append(", m_default_cache_location: '" + m_default_cache_location);
        return returnstring.append(" }>").toString();
    }
}


