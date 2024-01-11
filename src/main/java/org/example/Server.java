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

import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private static HashMap<String, Integer> countryRanking = new HashMap<>();
    private static long deltaT;

    //args = [p_r, p_w, wt]
    public static void main(String[] args) {
        try {
            int clientHandlerThreads = Integer.parseInt(args[0]);
            int workerThreads = Integer.parseInt(args[1]);
            deltaT = Integer.parseInt(args[2]);
            CountDownLatch producerLatch = new CountDownLatch(5); // the number of clients that will send data
            CountDownLatch consumerLatch = new CountDownLatch(workerThreads);

            ExecutorService clientService = Executors.newFixedThreadPool(clientHandlerThreads);
            CustomQueue<Participant> processedData = new CustomQueue<>(1000);
            CustomQueue<FutureTask<Void>> responseTasks = new CustomQueue<>(100);
            CustomQueue<FutureTask<Void>> finalResponseTasks = new CustomQueue<>(10);
            RequestHandler requestHandler = new RequestHandler(linkedList, processedData, responseTasks, finalResponseTasks, producerLatch);

            ConnectionHandler connectionHandler = new ConnectionHandler(requestHandler, clientService);
            connectionHandler.listen(8000);
            LinkedListHandler linkedListHandler = new LinkedListHandler(processedData, linkedList, workerThreads, consumerLatch);

            respondToRequests(responseTasks);

            consumerLatch.await();

            updateRanking();
            logger.info("Sorting linked list");
            linkedList.sort();
            writeToFile("data/");
            respondToRequests(finalResponseTasks);
            connectionHandler.shutdown();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private static void respondToRequests(CustomQueue<FutureTask<Void>> responseTasks) {
        FutureTask<Void> future = responseTasks.get();
        while (future != null) {
            future.run();
            future = responseTasks.get();
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
        countryRanking = newRanking;
        lastCalculatedRanking.set(System.currentTimeMillis());
    }

    private static List<String> readFromFile(String file) {
        List<String> result = new ArrayList<>();
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader reader = new BufferedReader(fileReader);
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static Future getFinalRankingResponseFutureTask(Connection connection) {
        return new FutureTask(() -> {
            String participantRankingEncoded = encodeFileToBase64("data/participants_ranking.txt");
            String countryRankingEncoded = encodeFileToBase64("data/country_ranking.txt");

            JSONObject body = new JSONObject()
                    .put("participants", participantRankingEncoded)
                    .put("countries", countryRankingEncoded);

            JSONObject response = JSONBuilder.create().addHeader("type", "final-ranking").setBody(body).build();

            try {
                connection.send(response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    private static String encodeFileToBase64(String filePath) throws IOException {
        byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
        return Base64.encodeBase64String(fileContent);
    }

    public static Future getRankingResponseFutureTask(Connection connection) {
        return new FutureTask(() -> {
            if (System.currentTimeMillis() - lastCalculatedRanking.get() > deltaT) {
                updateRanking();
            }
            List<String> result = new ArrayList<>();
            countryRanking.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).forEach(entry -> {
                result.add(entry.getKey() + " " + entry.getValue());
            });
            JSONObject response = JSONBuilder.create().addHeader("type", "ranking").setBody(new JSONArray(result)).build();
            try {
                connection.send(response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    static void writeToFile(String directory) {
        logger.info("Writing to file");
        File participantsFile = new File(directory + "participants_ranking.txt");
        try {
            var fileWriter = new FileWriter(participantsFile);
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

        File countryFile = new File(directory + "country_ranking.txt");
        try {
            var fileWriter = new FileWriter(countryFile);
            var writer = new BufferedWriter(fileWriter);
            countryRanking.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).forEach(entry -> {
                try {
                    writer.write(entry.getKey() + " " + entry.getValue());
                    writer.newLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}