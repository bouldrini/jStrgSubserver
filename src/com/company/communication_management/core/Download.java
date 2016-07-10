package com.company.communication_management.core;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;

public class Download {

    // ATTRIBUTES
    public File m_file;
    public String m_transaction_id;
    public Socket m_socket;

    public Download(Socket _socket, File _file, String _transaction_id){
        m_socket = _socket;
        m_transaction_id = _transaction_id;
    }
}
