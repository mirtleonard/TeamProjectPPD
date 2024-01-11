package org.example;

import org.example.model.ConcurrentLinkedList;
import org.example.model.CustomQueue;
import org.example.model.Node;
import org.example.model.Participant;
import org.example.utils.JSONBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);
    private static AtomicLong lastCalculatedRanking = new AtomicLong(0);
    private static ConcurrentLinkedList linkedList = new ConcurrentLinkedList();
    private static HashMap<String, Integer> contryRanking = new HashMap<>();
    private static long deltaT = 100;

    public static void main(String[] args) throws Exception {
        //listen();
        int clientHandlerThreads = 2;
        int linkedListHandlerThreads = 2;
        CountDownLatch producerLatch = new CountDownLatch(5); // the number of clients that will send data
        CountDownLatch consumerLatch = new CountDownLatch(linkedListHandlerThreads);

        ExecutorService clientDataExecutorService = Executors.newFixedThreadPool(clientHandlerThreads);
        CustomQueue<Participant> processedData = new CustomQueue<>(1000);
        CustomQueue<FutureTask<Void>> responseQueue = new CustomQueue<>(100);
        ConsoleRequestHandler requestHandler = new ConsoleRequestHandler(linkedList, clientDataExecutorService, processedData, responseQueue, producerLatch);

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Map<String, Connection> connections = new ConcurrentHashMap<>();
        BlockingQueue<Connection> pendingConnections = new LinkedBlockingQueue<>(1000);
        ConnectionHandler connectionHandler = new ConnectionHandler(pendingConnections, connections, requestHandler, executorService);

        LinkedListHandler linkedListHandler = new LinkedListHandler(processedData, linkedList, linkedListHandlerThreads, consumerLatch);
        connectionHandler.listen(8000);

        respondToClients(responseQueue);

        consumerLatch.await();
        logger.info("Sorting linked list");
        linkedList.sort();
        writeToFile("data/clasament.txt", linkedList);
    }

    private static void respondToClients(CustomQueue<FutureTask<Void>> responseQueue) {
        Future<Void> future = responseQueue.get();
        while (future != null) {
            try {
                future.get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            future = responseQueue.get();
        }
    }
    private static void updateRanking() {
        logger.info("Updating ranking");
        HashMap<String, Integer> newRanking = new HashMap<>();
        for (Node node : linkedList.getAll()) {
            if (node.getParticipant() != null) {
                String country = node.getParticipant().getCountry();
                if (newRanking.containsKey(country)) {
                    newRanking.put(country, newRanking.get(country) + node.getParticipant().getScore());
                } else {
                    newRanking.put(country, node.getParticipant().getScore());
                }
            }
        }
        contryRanking = newRanking;
        lastCalculatedRanking.set(System.currentTimeMillis());
    }

    public static CompletableFuture<Void> getResponseFutureTask(Connection connection) {
        return CompletableFuture.runAsync(() -> {
            if (System.currentTimeMillis() - lastCalculatedRanking.get() > deltaT) {
                updateRanking();
            }
            List<String> result = new ArrayList<>();
            contryRanking.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).forEach(entry -> {
                result.add(entry.getKey() + " " + entry.getValue());
            });
            JSONObject response = JSONBuilder.create().addHeader("type", "ranking").setBody(new JSONArray(result)).build();
            try {
                connection.send(response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    static void writeToFile(String file, ConcurrentLinkedList linkedList) {
        logger.info("Writing to file");
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