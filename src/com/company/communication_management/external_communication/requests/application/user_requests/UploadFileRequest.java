package com.company.communication_management.external_communication.requests.application.user_requests;


import com.company.communication_management.core.Answer;
import com.company.communication_management.core.Request;

import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;

public class UploadFileRequest extends Request {
    // CONSTRUCTORS
    public UploadFileRequest(Socket _socket, String _client_request_string) throws GeneralSecurityException, IOException {
        super(_socket, _client_request_string);
        String key = "";
        String value = "";
        for (String line : m_request_string.split(";")) {
            key = line.split(":")[0];
            value = line.split(":")[1];
            if (key.equals("file_size")) {
                m_file_size = Long.parseLong(value);
            } else if (key.equals("file_path")) {
                m_file_path = value;
            } else if (key.equals("file_name")) {
                m_file_name = value;
            }
        }
        ;
    }

    // ATTRIBUTES
    public long m_file_size;
    public String m_file_path = "";
    public String m_file_name = "";

    // HANDLE THE REQUEST
    public void run() {

    }

    public Answer start_upload(){
        return null;
    }


    // HELPER
    @Override
    public String toString() {
        return "<UploadRequest::{m_status: " + m_status + ", m_request_type: " + m_request_type + ", m_file_path: " + m_file_path + ", m_file_size: " + m_file_size + "}>";
    }
}
