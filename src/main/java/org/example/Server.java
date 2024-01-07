package org.example;

import org.example.model.ConcurrentLinkedList;
import org.example.model.CustomQueue;
import org.example.model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);

    public static void main(String[] args) throws Exception {
        int clientHandlerThreads = 2;
        int linkedListHandlerThreads = 2;
        CountDownLatch producerLatch = new CountDownLatch(clientHandlerThreads);
        CountDownLatch consumerLatch = new CountDownLatch(linkedListHandlerThreads);
        // I don't think the above is necessary, but I'm not sure
        ExecutorService clientDataExecutorService = Executors.newFixedThreadPool(clientHandlerThreads);
        CustomQueue processedData = new CustomQueue(1000);
        ConsoleRequestHandler requestHandler = new ConsoleRequestHandler(clientDataExecutorService, processedData);

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Map<String, Connection> connections = new ConcurrentHashMap<>();
        BlockingQueue<Connection> pendingConnections = new LinkedBlockingQueue<>(100);
        ConnectionHandler connectionHandler = new ConnectionHandler(pendingConnections, connections, requestHandler, executorService);

        ConcurrentLinkedList linkedList = new ConcurrentLinkedList();
        LinkedListHandler linkedListHandler = new LinkedListHandler(processedData, linkedList, linkedListHandlerThreads, consumerLatch);
        connectionHandler.listen(8000);
        consumerLatch.await();
        logger.info("Sorting linked list");
        linkedList.sort();
        var list = linkedList.getAll();
        for (Node node : list) {
            if (node.getParticipant() != null) {
                logger.info(node.getParticipant().toString());
            }
        }

    }
}