package org.example;

import org.example.model.ConcurrentLinkedList;
import org.example.model.Participant;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.example.model.CustomQueue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

// this handler should be different for client and server
public class ConsoleRequestHandler implements IRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleRequestHandler.class);
    private final ExecutorService executorService;
    private final CountDownLatch producerLatch;
    private final CustomQueue<CompletableFuture<Void>> responseQueue;
    private final CustomQueue<Participant> processedData;
    private final ConcurrentLinkedList linkedList;

    public ConsoleRequestHandler(ConcurrentLinkedList linkedList, ExecutorService clientDataExecutorService, CustomQueue processedData, CustomQueue responseQueue, CountDownLatch producerLatch) {
        this.executorService = clientDataExecutorService;
        this.processedData = processedData;
        this.producerLatch = producerLatch;
        this.linkedList = linkedList;
        this.responseQueue = responseQueue;
    }

    @Override
    public synchronized void handle(JSONObject request, Connection connection) {
        logger.info("Handling request {}", request.toString());
        JSONObject header = request.getJSONObject("header");
        String type = header.getString("type");
        if (type.contains("sending-data")) {
            Object body = request.get("body");
            String country = header.get("Country").toString();
            executorService.submit(new ProducerThread(processedData, country, body));
            connection.terminate();
        } else if (type.contains("get-ranking")) {
            logger.info("Received request for ranking");
            responseQueue.add(Server.getResponseFutureTask(connection));
        } else if (type.contains("get-final-ranking")) {
            producerLatch.countDown();
            if (producerLatch.getCount() == 0) {
                processedData.setFinished();
                responseQueue.setFinished();
            }
        } else if (type.contains("disconnect")) {
            connection.terminate();
        } else {
            logger.error("Unknown request type {}", type);
        }
    }

    class ProducerThread implements Runnable {
        private final CustomQueue processedData;
        private String country;
        private Object data;

        public ProducerThread(CustomQueue processedData, String country , Object data) {
            this.processedData = processedData;
            this.country = country;
            this.data = data;
        }

        @Override
        public void run() {
            logger.info("Processing data for country {}", country);

            JSONArray rawParticipants = (JSONArray) data;

            for (Object participant : rawParticipants) {
                String[] participantData = participant.toString().split(" ");
                int id = Integer.parseInt(participantData[0]);
                int score = Integer.parseInt(participantData[1]);
                processedData.add(new Participant(id, score, country));
            }
        }
    }

}