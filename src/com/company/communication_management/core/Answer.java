package com.company.communication_management.core;

import com.company.Main;
import com.company.network_management.core.Cryptor;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.security.GeneralSecurityException;

public class Answer {

    // CONSTRUCTORS
    public Answer() {
        m_status = status.ERROR;
        m_error_code = error_code.INVALID_QUERY;
        m_error_message = "QUERY COULDNT BE PERFORMED ! CLIENT SIDE PROBLEM !";
    }

    public Answer(Socket _socket, Answer.error_code _error_code) {
        m_error_code = _error_code;
        m_error_message = Answer.get_failure_message(this.m_error_code);
        m_status = status.ERROR;
        m_socket = _socket;
    }
    public Answer(Socket _socket, Answer.error_code _error_code, String _error_message) {
        m_error_code = _error_code;
        m_error_message = _error_message;
        m_status = status.ERROR;
        m_socket = _socket;
    }

    public Answer(Socket _socket){
        m_socket = _socket;
    }

    public static String get_failure_message(Answer.error_code _error_code) {
        String error_message = "";
        if (_error_code == error_code.UNAUTHORIZED) {
            error_message = "Invalid Userdata";
        } else if (_error_code == error_code.FILE_NOT_FOUND) {
            error_message = "File couldn't be found in the storage system";
        } else if (_error_code == error_code.INVALID_QUERY) {
            error_message = "invalid query string";
        } else if (_error_code == error_code.INSUFFITIANT_PERMISSIONS_DELETE_FILE) {
            error_message = "No permissions to delete this file";
        } else if (_error_code == error_code.INSUFFITIANT_PERMISSIONS_DELETE_FOLDER) {
            error_message = "No permissions to delete this folder";
        } else if (_error_code == error_code.INSUFFITIANT_PERMISSIONS_CREATE_USER) {
            error_message = "No permissions to create new users";
        }
        return error_message;
    }

    // CONSTANTS
    public enum status {
        DONE, READY_FOR_FILEUPLOAD, ERROR, READY_FOR_FILEDOWNLOAD
    }

    public enum error_code {
        UNAUTHORIZED, FILE_NOT_FOUND, FOLDER_NOT_FOUND, INVALID_QUERY, INVALID_FILE_FORMAT, LOCATION_NOT_FOUND, INSUFFITIANT_PERMISSIONS_DELETE_FILE, INSUFFITIANT_PERMISSIONS_DELETE_FOLDER, INSUFFITIANT_PERMISSIONS_CREATE_USER, INSUFFITIANT_PERMISSION_UPLOAD, INSUFFITIANT_PERMISSIONS_TO_UPLOAD, NOT_ENOUGH_SPACE, INTERNAL_ERROR, UNKNOWN_REQUEST_TYPE, USER_ALREADY_EXISTS
    }

    // ATTRIBUTES
    public Answer.status m_status;
    public Answer.error_code m_error_code;
    public String m_error_message;
    public Socket m_socket;

    // SUBMIT ANSWER
    public boolean send() throws IOException, GeneralSecurityException {
        if (m_socket != null) {
            System.out.println("====SENDING ANSWER TO CLIENT====");
            if(m_socket.isClosed()){
                System.out.println("connection lost. couldnt send a client answer");
                return false;
            } else {
                OutputStream outToClient = m_socket.getOutputStream();
                DataOutputStream out = new DataOutputStream(outToClient);
                String encrypted_answer = Cryptor.encrypt(Main.m_settings.m_network_communication_secret1, Main.m_settings.m_network_communication_secret2, this.for_socket_answer());
                out.writeUTF(encrypted_answer);
                return true;
            }
        } else {
            return false;
        }
    }

    // HELPER
    public String for_socket_answer() {
        return "status:" + m_status + ";error_code:" + m_error_code + ";error_message:" + m_error_message + ";";
    }

    @Override
    public String toString() {
        return "<Answer::{m_status: " + m_status + ", m_error_code: " + m_error_code + ", m_error_message: " + m_error_message + "}>";
    }
}
