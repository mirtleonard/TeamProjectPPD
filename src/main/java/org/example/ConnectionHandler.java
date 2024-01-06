package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ConnectionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);
    private final ExecutorService executorService;
    private final BlockingQueue<Connection> pendingConnections;
    private final Map<String, Connection> connections;
    private Listener listener;

    private volatile boolean running = true;
    private final IRequestHandler requestHandler;

    public ConnectionHandler(BlockingQueue<Connection> pendingConnections,
                             Map<String, Connection> connections,
                             IRequestHandler requestHandler, ExecutorService executorService) {
        this.pendingConnections = pendingConnections;
        this.connections = connections;
        this.requestHandler = requestHandler;
        this.executorService = executorService;
    }

    private void submitPendingConnections() {
        while (running) {
            try {
                Connection connection = pendingConnections.take();
                if (connections.putIfAbsent(connection.getSocket().getInetAddress().toString()+":"+connection.getSocket().getPort(), connection) == null) {
                    logger.info("Connection {} added", connection.getSocket().getInetAddress().toString());
                    connection.setHandler(requestHandler);
                    executorService.submit(connection);
                }
            } catch (Exception exception) {
                logger.debug("Exception {} {}", exception.getClass().getSimpleName(), exception.getMessage());
            }
        }
    }

    private void clearClosedConnections() {
        while (running) {
            synchronized (connections) {
                int before = connections.size();
                connections.values().removeIf(Connection::isTerminated);
                if (before - connections.size() > 0) {
                    logger.info("Cleanup deleted {} connections", before - connections.size());
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
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
        listener = new Listener(port, pendingConnections);
        executorService.submit(listener);
        executorService.submit(this::submitPendingConnections);
        executorService.submit(this::clearClosedConnections);
        logger.info("Listening on port {}", port);
    }

    public void stopListening(int port) throws Exception {
        if (listener == null) {
            throw new Exception("There is no listener running");
        }
        listener.terminate();
        listener = null;
    }

    public void shutdown() throws IOException, InterruptedException {
        logger.info("shutdown");
        executorService.shutdownNow();
        if (listener != null) {
            listener.terminate();
        }
        running = false;
        closeAllConnections();
        while (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
            logger.info("Still waiting to shutdown...");
        }
    }

    private void closeAllConnections() {
        for (Connection connection : connections.values()) {
            connection.terminate();
        }
    }
}
