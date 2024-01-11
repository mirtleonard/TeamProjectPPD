package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class Listener implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Listener.class);
    private final int port;
    private volatile boolean terminated;

    private ExecutorService clientService;
    private IRequestHandler handler;
    private ServerSocket socket;


    public int getPort() {
        return port;
    }


    public Listener(int port, ExecutorService clientService, IRequestHandler handler) {
        this.port = port;
        this.clientService = clientService;
        this.handler = handler;
    }

    public void terminate() {
        logger.info("Listener terminating");
        terminated = true;

        try {
            socket.close();
        } catch (IOException e) {
            logger.error("exception {} {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    @Override
    public void run() {
        logger.info("start listening");
        try {
            socket = new ServerSocket(port);
        } catch (Exception e) {
            logger.error("socket server error {} {}", e.getClass().getSimpleName(), e.getMessage());
            System.exit(-1);
        }

        while (!terminated) {
            try {
                Socket s = socket.accept();
                logger.info("new connection {}", s.getInetAddress().toString());
                try {
                    Connection client = new Connection(s);
                    client.setHandler(handler);
                    clientService.submit(client);
                } catch (Exception ignore) {
                }
            } catch (Exception exception) {
                logger.error("exception {} {}", exception.getClass().getSimpleName(), exception.getMessage());
                terminate();
            }
        }
    }
}