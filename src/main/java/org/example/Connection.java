package org.example;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Connection implements Runnable {
    Logger logger = LoggerFactory.getLogger(Connection.class);
    private volatile boolean terminated;
    private final Socket socket;

    private ObjectInputStream inputStream;
    private final ObjectOutputStream outputStream;
    private IRequestHandler handler;

    public Connection(Socket socket) throws IOException {
        //logger.info("creating connection for {}", socket.getInetAddress().getHostAddress());
        this.socket = socket;
        outputStream = new ObjectOutputStream(this.socket.getOutputStream());
        outputStream.flush();
        inputStream = new ObjectInputStream(this.socket.getInputStream());
        //logger.info("connection created for {}", socket.getInetAddress().getHostAddress());
    }


    public void setHandler(IRequestHandler handler) {
        this.handler = handler;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void terminate() {
        if(terminated){
            return;
        }
        terminated = true;
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            socket.close();
        } catch (IOException e) {
            logger.error("while closing error {} {}", e.getClass().getSimpleName(), e.getMessage());
        }
        //logger.info("connection {} closed", socket.getInetAddress().toString());
    }

    public void send(JSONObject jsonObject) throws IOException {
        synchronized (outputStream) {
            logger.info("sending To: {} JsonObject: {}", socket.getInetAddress().toString(), jsonObject.toString());
            outputStream.writeObject(jsonObject.toString());
            outputStream.flush();
        }
    }

    public JSONObject read() {
        try {
            return new JSONObject((String) inputStream.readObject());
        } catch (Exception e) {
            logger.error("from: {} error {} {}", socket.getInetAddress().toString(), e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    @Override
    public void run() {
        try {
            JSONObject tmp = new JSONObject((String) inputStream.readObject());
            logger.info("getting From: {} JsonObject: {}", socket.getInetAddress().toString(), tmp);
            handler.handle(tmp, this); // this handler should be different for client and server
        } catch (IOException e) {
            terminate();
            logger.error("from: {} error {} {}", socket.getInetAddress().toString(), e.getClass().getSimpleName(), e.getMessage());
        } catch (Exception ignore) {
        }
}
                }