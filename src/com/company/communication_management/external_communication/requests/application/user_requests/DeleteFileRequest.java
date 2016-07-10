package com.company.communication_management.external_communication.requests.application.user_requests;


import com.company.communication_management.core.Request;

import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;

public class DeleteFileRequest extends Request {
    // ATTRIBUTES
    public String m_file_path = "";
    public String m_file_name = "";
    // CONSTRUCTORS
    public DeleteFileRequest(Socket _socket, String _client_request_string) throws GeneralSecurityException, IOException {
        super(_socket, _client_request_string);
        String key = "";
        String value = "";
        for (String line : m_request_string.split(";")) {
            key = line.split(":")[0];
            value = line.split(":")[1];
            if (key.equals("file_path")) {
                m_file_path = value;
            } else if (key.equals("file_name")) {
                m_file_name = value;
            }
        }
    }

    // HANDLE THE REQUEST
    public void run() {

    }

    // HELPER
    @Override
    public String toString() {
        return "<DeleteFileRequest::{m_status: " + m_status + ", m_request_type: " + m_request_type + ", m_file_path: " + m_file_path + ", m_file_name: " + m_file_name + "}>";
    }
}
