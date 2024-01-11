package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ConnectionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);
    private final ExecutorService clientService;
    private volatile boolean terminated;
    private volatile boolean running = true;
    private Listener listener;
    private Thread listenerThread;
    private final IRequestHandler requestHandler;

    public ConnectionHandler(IRequestHandler requestHandler, ExecutorService executorService) {
        this.requestHandler = requestHandler;
        this.clientService = executorService;
    }

    public void listen(int port) throws Exception {
        if (port <= 0) {
            throw new Exception("Invalid port");
        }
        if (listener != null) {
            throw new Exception("There is a listener running on port "
                    + listener.getPort()
                    + ". Stop current listener and try again");
        }
        listener = new Listener(port, clientService, requestHandler);
        listenerThread = new Thread(listener);
        listenerThread.start();
        logger.info("Listening on port {}", port);
    }

    public void shutdown() throws IOException, InterruptedException {
        logger.info("shutdown");
        clientService.shutdownNow();
        if (listener != null) {
            listener.terminate();
        }
        running = false;
        while (!clientService.awaitTermination(1, TimeUnit.SECONDS)) {
            logger.info("Still waiting to shutdown...");
        }
    }

}
