package com.dreamgyf.mqtt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class MqttConnect {

    private Socket socket;

    private String broker;

    private int port;

    private byte[] message;

    protected MqttConnect(String broker, int port, byte[] message){
        this.broker = broker;
        this.port = port;
        this.message = message;
    }

    public void connect() throws IOException {
        socket = new Socket(broker,port);
        OutputStream os = socket.getOutputStream();
        os.write(message);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        InputStream is = socket.getInputStream();
        int size = is.available();
        byte[] read = new byte[size];
        is.read(read);
        String response = new String(read, "utf-8");
    }
}
