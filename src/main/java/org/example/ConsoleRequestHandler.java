package org.example;

import org.example.model.Participant;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.example.model.CustomQueue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

public class ConsoleRequestHandler implements IRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleRequestHandler.class);
    private final ExecutorService executorService;
    private final CountDownLatch producerLatch;

    CustomQueue processedData;

    public ConsoleRequestHandler(ExecutorService clientDataExecutorService, CustomQueue processedData, CountDownLatch producerLatch) {
        this.executorService = clientDataExecutorService;
        this.processedData = processedData;
        this.producerLatch = producerLatch;
    }

    @Override
    public synchronized void handle(JSONObject request, Connection connection) {
        //logger.info("Handling request {}", request.toString());
        JSONObject header = request.getJSONObject("header");
        String type = header.getString("type");
        if (type.contains("sending-data")) {
            Object body = request.get("body");
            String country = header.get("Country").toString();
            executorService.submit(new ProducerThread(processedData, country, body));
        } else if (type.contains("get-ranking")) {
            logger.info("Received request for ranking");
            producerLatch.countDown();
            if (producerLatch.getCount() == 0) {
                processedData.setFinished();
            }
        } else if (type.contains("local_disconnect")) {
            //logger.info("Received disconnect request");
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