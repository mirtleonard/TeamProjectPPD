package org.example;

import org.example.model.CustomQueue;

import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.concurrent.*;

public class MainServer {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello world!");
        int clientHandlerThreads = 2;

        Map<String, Connection> connections = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        ExecutorService clientDataExecutorService = Executors.newFixedThreadPool(clientHandlerThreads);
        CustomQueue processedData = new CustomQueue(1000);
        ConsoleRequestHandler requestHandler = new ConsoleRequestHandler(clientDataExecutorService, processedData);
        BlockingQueue<Connection> pendingConnections = new LinkedBlockingQueue<>(100);
        ConnectionHandler connectionHandler = new ConnectionHandler(pendingConnections, connections, requestHandler, executorService);
        connectionHandler.listen(8000);
    }
}