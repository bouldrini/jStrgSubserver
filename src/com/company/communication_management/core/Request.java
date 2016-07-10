package com.company.communication_management.core;

import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;

public class Request extends Thread implements Runnable {

    // ATTRIBUTES
    public Socket m_socket;
    public Request.type m_request_type;
    public boolean m_status;
    public String m_request_string;
    public Thread m_thread;

    // CONSTRUCTORS
    public Request(Socket _socket, String _client_request) throws GeneralSecurityException, IOException {
        String username = "";
        String password = "";
        String key = "";
        String value = "";
        // TODO: keys from settings
//        m_request_string = Cryptor.decrypt(, _client_request);
        for (String line : m_request_string.split(";")) {
            key = line.split(":")[0];
            value = line.split(":")[1];
            if (key.equals("username")) {
                username = value;
            } else if (key.equals("password")) {
                password = value;
            } else if (key.equals("request_type")) {
                if (value.equals(type.DOWNLOAD_FILE.toString())) {
                    m_request_type = type.DOWNLOAD_FILE;
                } else if (value.equals(type.UPLOAD_FILE.toString())) {
                    m_request_type = type.UPLOAD_FILE;
                } else if (value.equals(type.CREATE_USER.toString())) {
                    m_request_type = type.CREATE_USER;
                } else if (value.equals(type.DELETE_FILE.toString())) {
                    m_request_type = type.DELETE_FILE;
                } else if (value.equals(type.DELETE_FOLDER.toString())) {
                    m_request_type = type.DELETE_FOLDER;
                } else if (value.equals(type.UPLOAD_FILE_REQUEST.toString())) {
                    m_request_type = type.UPLOAD_FILE_REQUEST;
                }
            }
        }
    }

    @Override
    public void run() {
    }

    public void start() {
        if (m_thread == null) {
            m_thread = new Thread(this, "Requestthread");
            m_thread.start();
        }
    }
    // CONSTANTS
    public enum type {
        DOWNLOAD_FILE, UPLOAD_FILE, CREATE_USER, DELETE_FILE, DELETE_FOLDER, UPLOAD_FILE_REQUEST
    }
}
