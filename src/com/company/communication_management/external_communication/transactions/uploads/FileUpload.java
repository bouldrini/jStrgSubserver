package com.company.communication_management.external_communication.transactions.uploads;

import com.company.communication_management.core.Answer;
import com.company.communication_management.core.Upload;
import com.company.communication_management.external_communication.answers.application.user_answers.FileUploadAnswer;

import java.io.*;
import java.io.File;
import java.net.Socket;
import java.security.GeneralSecurityException;

public class FileUpload extends Upload {

    public FileUpload(Socket _socket, String _file_name, String _file_path, long _file_size, String _transaction_id) throws GeneralSecurityException, IOException {
        super(_socket, _file_name, _file_path, _file_size, _transaction_id);
    }


    public Answer process() {
        try {
            File floating_file = new File("/tmp/floating_file.txt");

            floating_file.getParentFile().mkdirs();
            floating_file.createNewFile();

            System.out.println("EXPECT THE CLIENT TO START SENDING");

            try {
                DataInputStream dis = new DataInputStream(m_socket.getInputStream());
                try {
                    FileOutputStream fos = new FileOutputStream(floating_file.getAbsolutePath());
                    byte[] buffer = new byte[4096];

                    int read = 0;
                    int totalRead = 0;
                    long remaining = m_file_size;

                    while((read = dis.read(buffer, 0, Math.min(buffer.length, (int)remaining))) > 0) {
                        totalRead += read;
                        remaining -= read;
                        fos.write(buffer);
                    }

                    fos.close();

                    Answer answer = new FileUploadAnswer(this.m_socket, Answer.status.DONE);
                    return answer;
                } catch (FileNotFoundException ex) {
                    System.out.println("File not found. ");
                    Answer answer = new Answer(m_socket,  Answer.error_code.INTERNAL_ERROR, "There was an internal Problem with saving the file temporarily to the master. The reason for this may be having not enough storage space left on the master");
                    return answer;
                }
            } catch (IOException ex) {
                System.out.println("Can't get socket input stream. ");
                Answer answer = new Answer(m_socket, Answer.error_code.INTERNAL_ERROR, "There was an internal Problem with listening to the input stream. Try again or contact your Administrator");
                return answer;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Answer answer = new Answer(m_socket, Answer.error_code.INTERNAL_ERROR, "There was an internal Problem with saving the file temporarily to the master. The reason for this may be having not enough storage space left on the master");
            return answer;
        }
    }
}
