package com.company.communication_management.external_communication.answers.application.user_answers;

import com.company.communication_management.core.Answer;

import java.net.Socket;

public class FileUploadAnswer extends Answer {
    //CONSTRUCTORS
    public FileUploadAnswer(Socket _socket, Answer.status _status) {
        super(_socket);
        m_status = _status;
    }

    // HELPER
    public String for_socket_answer() {
        return "status:" + m_status + ";";
    }

    @Override
    public String toString() {
        return "<FileUploadAnswer::{m_status: " + m_status + "}>";
    }
}
