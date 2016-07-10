package com.company.communication_management.external_communication.answers.application.user_answers;

import com.company.communication_management.core.Answer;

import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DownloadFileRequestAnswer extends Answer {
    public String m_transaction_id;

    //CONSTRUCTORS
    public DownloadFileRequestAnswer(Socket _socket, Answer.status _status, String _transaction_id) {
        super(_socket);
        m_status = _status;
        System.out.println(m_status);
        m_transaction_id = _transaction_id;
        // TODO: REPLACE WITH REAL FILE_SIZE
        Path path = Paths.get("docs/er_model.pdf");
        java.io.File file = new java.io.File(path.toString());

        m_file_size = file.length();
    }

    public long m_file_size;

    // HELPER
    public String for_socket_answer() {
        return "status:" + m_status + ";transaction_id:" + m_transaction_id + ";file_size:" + m_file_size+ ";";
    }

    @Override
    public String toString() {
        return "<DownloadFileRequestAnswer::{m_status: " + m_status + ", m_transaction_id: " + m_transaction_id + ", m_file_size:" + m_file_size + "}>";
    }
}
