package org.example;

import org.example.model.ConcurrentLinkedList;
import org.example.model.CustomQueue;
import org.example.model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map;
import java.util.concurrent.*;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);

    public static void main(String[] args) throws Exception {
        int clientHandlerThreads = 2;
        int linkedListHandlerThreads = 2;
        CountDownLatch producerLatch = new CountDownLatch(2); // the number of clients that will send data
        CountDownLatch consumerLatch = new CountDownLatch(linkedListHandlerThreads);
        // I don't think the above is necessary, but I'm not sure
        ExecutorService clientDataExecutorService = Executors.newFixedThreadPool(clientHandlerThreads);
        CustomQueue processedData = new CustomQueue(1000);
        ConsoleRequestHandler requestHandler = new ConsoleRequestHandler(clientDataExecutorService, processedData, producerLatch);

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Map<String, Connection> connections = new ConcurrentHashMap<>();
        BlockingQueue<Connection> pendingConnections = new LinkedBlockingQueue<>(1000);
        ConnectionHandler connectionHandler = new ConnectionHandler(pendingConnections, connections, requestHandler, executorService);

        ConcurrentLinkedList linkedList = new ConcurrentLinkedList();
        LinkedListHandler linkedListHandler = new LinkedListHandler(processedData, linkedList, linkedListHandlerThreads, consumerLatch);
        connectionHandler.listen(8000);
        consumerLatch.await();
        logger.info("Sorting linked list");
        linkedList.sort();

        writeToFile("data/clasament.txt", linkedList);
    }

    static void writeToFile(String file, ConcurrentLinkedList linkedList) {
        try {
            var fileWriter = new FileWriter(file);
            var writer = new BufferedWriter(fileWriter);
            var list = linkedList.getAll();
            for (Node node : list) {
                if (node.getParticipant() != null) {
                    writer.write(node.getParticipant().toString());
                    writer.newLine();
                }
            }
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}