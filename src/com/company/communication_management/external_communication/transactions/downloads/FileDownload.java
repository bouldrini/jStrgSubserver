package com.company.communication_management.external_communication.transactions.downloads;

import com.company.communication_management.core.Download;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

public class FileDownload extends Download {
    public FileDownload(Socket _socket, File _file, String _transaction_id){
        super(_socket, _file, _transaction_id);
    }


    public boolean process() throws IOException {
        System.out.println("Using open transaction_id: " + m_transaction_id);

        OutputStream outToServer = this.m_socket.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToServer);
//        out.writeUTF(Cryptor.encrypt(this.for_server_request()));

        System.out.println("SENDING FILE");
        this.send_file(outToServer);
        System.out.println("TRANSFER DONE.");
        return true;
    }

    public boolean send_file(OutputStream _client_output_stream) throws IOException {
        System.out.println("SENDING FILE TO THE CLIENT");

        Path path = Paths.get("docs/er_model.pdf");
        System.out.println(path.toString());

        FileInputStream fis = new FileInputStream(path.toString());
        // TODO: REPLACE WITH REAL FILE
//        FileInputStream fis = new FileInputStream(m_file.getAbsolutePath().toString());

        byte[] buffer = new byte[4096];

        while (fis.read(buffer) > 0) {
            _client_output_stream.write(buffer);
        }

        fis.close();
        return true;
    }
}
