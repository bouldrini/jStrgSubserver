package com.company.communication_management.external_communication.answers.application.user_answers;


import com.company.communication_management.core.Answer;

import java.net.Socket;

public class UploadAnswer extends Answer {
    public String m_transaction_id;

    //CONSTRUCTORS
    public UploadAnswer(Socket _socket, Answer.status _status, String _transaction_id) {
        super(_socket);
        m_status = _status;
        m_transaction_id = _transaction_id;
    }

    // HELPER
    public String for_socket_answer() {
        return "status:" + m_status + ";";
    }

    @Override
    public String toString() {
        return "<UploadFileAnswer::{m_status: " + m_status + "}>";
    }
}
