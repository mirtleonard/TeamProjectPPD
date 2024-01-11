package org.example;

import org.example.model.ConcurrentLinkedList;
import org.example.model.Participant;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.example.model.CustomQueue;

import java.util.concurrent.*;

// this handler should be different for client and server
public class RequestHandler implements IRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);
    private final CountDownLatch producerLatch;
    private final CustomQueue<Future> finalRankingResponseTasks;
    private final CustomQueue<Future> responseTasks;
    private final CustomQueue<Participant> processedData;
    private final ConcurrentLinkedList linkedList;


    public RequestHandler(ConcurrentLinkedList linkedList, CustomQueue processedData, CustomQueue responseTasks, CustomQueue finalRankingResponseTasks, CountDownLatch producerLatch) {
        this.processedData = processedData;
        this.producerLatch = producerLatch;
        this.linkedList = linkedList;
        this.responseTasks = responseTasks;
        this.finalRankingResponseTasks = finalRankingResponseTasks;
    }

    @Override
    public synchronized void handle(JSONObject request, Connection connection) {
        logger.info("Handling request {}", request.toString());
        JSONObject header = request.getJSONObject("header");
        String type = header.getString("type");
        if (type.contains("sending-data")) {
            Object body = request.get("body");
            String country = header.get("Country").toString();
            addDataInQueue(country, body);
            connection.terminate();
        } else if (type.contains("get-ranking")) {
            logger.info("Received request for ranking");
            responseTasks.add(Server.getRankingResponseFutureTask(connection));
        } else if (type.contains("get-final-ranking")) {
            finalRankingResponseTasks.add(Server.getFinalRankingResponseFutureTask(connection));
            producerLatch.countDown();
            if (producerLatch.getCount() == 0) {
                processedData.setFinished();
                responseTasks.setFinished();
                finalRankingResponseTasks.setFinished();
            }
        } else if (type.contains("disconnect")) {
            connection.terminate();
        } else {
            logger.error("Unknown request type {}", type);
        }
    }

    public void addDataInQueue(String country, Object data) {
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