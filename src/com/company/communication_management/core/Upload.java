package com.company.communication_management.core;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;

public class Upload {

    // ATTRIBUTES
    public File m_file;
    public String m_file_name;
    public String m_transaction_id;
    public long m_file_size;
    public String m_file_path;
    public Socket m_socket;

    public Upload(Socket _socket, String _file_name, String _file_path, long _file_size, String _transaction_id) throws GeneralSecurityException, IOException {
        m_socket = _socket;
        m_file_name = _file_name;
        m_file_size = _file_size;
        m_file_path = _file_path;
        m_transaction_id = _transaction_id;
    }
}
