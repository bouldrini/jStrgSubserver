package com.company;

import com.company.file_system.Settings;
import com.company.network_management.core.SocketListener;

import java.io.IOException;

public class Main {
    public static Settings m_settings;

    public static void main(String[] args) {
        Settings settings = Settings.read_global_config();
        Main.m_settings = settings;

        SocketListener socketlistener = new SocketListener(m_settings.m_network_communication_port);

        try {
            socketlistener.listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
