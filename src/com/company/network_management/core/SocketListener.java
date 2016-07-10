package com.company.network_management.core;


import com.company.communication_management.core.Answer;
import com.company.communication_management.core.Request;
import com.company.communication_management.external_communication.requests.application.user_requests.DeleteFileRequest;
import com.company.communication_management.external_communication.requests.application.user_requests.DownloadFileRequest;
import com.company.communication_management.external_communication.requests.application.user_requests.UploadFileRequest;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;

public class SocketListener extends Thread {

    public int m_port = 3001;
    public Thread m_thread;

    public SocketListener(int _port) {
        m_port = _port;
    }

    /**
     * Starting the Socketlistener
     *
     * @throws IOException socket error
     */
    public void listen() throws IOException {
        this.start();
    }


    public void run() {
        ServerSocket server_socket = null;
        try {
            server_socket = new ServerSocket(m_port);
            while (true) {
                try {
                    System.out.println("====WAITING FOR INPUT ON PORT " + m_port + "====");
                    Socket socket = server_socket.accept();

                    System.out.println("");
                    System.out.println("====CLIENT CONNECTED, WAITING FOR REQUEST====");
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    String client_request_string = in.readUTF();
                    Request client_request = new Request(socket, client_request_string);
                    if (client_request.m_status) {
                        if (client_request.m_request_type == Request.type.UPLOAD_FILE_REQUEST) {
                            System.out.println("====PROCESSING CLIENT UPLOAD FILE REQUEST====");
                            UploadFileRequest request = new UploadFileRequest(socket, client_request_string);
                            request.start();
                        } else if (client_request.m_request_type == Request.type.DOWNLOAD_FILE) {
                            System.out.println("====PROCESSING CLIENT DOWNLOAD FILE REQUEST====");
                            DownloadFileRequest request = new DownloadFileRequest(socket, client_request_string);
                            request.start();
                        } else if (client_request.m_request_type == Request.type.DELETE_FILE) {
                            System.out.println("====PROCESSING CLIENT DELETE FILE REQUEST====");
                            DeleteFileRequest request = new DeleteFileRequest(socket, client_request_string);
                            request.start();
                        } else {
                            System.out.println(client_request.m_request_string);
                            System.out.println("====INVALID REQUEST TYPE====");
                            Answer answer = new Answer(socket, Answer.error_code.UNKNOWN_REQUEST_TYPE);
                            answer.send();
                        }
                    } else {
                        System.out.println("====USER AUTHENTICATION FAILED====");
                        Answer answer = new Answer(socket, Answer.error_code.UNAUTHORIZED);
                        answer.send();
                    }
                    System.out.println("");
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        if (m_thread == null) {
            m_thread = new Thread(this);
            m_thread.start();
        }
    }
}