package org.example;

import org.example.model.ConcurrentLinkedList;
import org.example.model.CustomQueue;
import org.example.model.Participant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LinkedListHandler {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);
    private CountDownLatch consumerLatch;
    private int threadsNumber;
    private CustomQueue processedData;
    private ConcurrentLinkedList linkedList;
    private ExecutorService executorService;

    public LinkedListHandler(CustomQueue processedData, ConcurrentLinkedList linkedList, int linkedListHandlerThreads, CountDownLatch consumerLatch) {
        this.consumerLatch = consumerLatch;
        this.processedData = processedData;
        this.linkedList = linkedList;
        this.executorService = Executors.newFixedThreadPool(linkedListHandlerThreads);
        int threadsNumber = linkedListHandlerThreads;

        for (int i = 0; i < threadsNumber; i++) {
            executorService.submit(new ConsumerThread(processedData, linkedList));
        }
    }

    public void handle() {

    }

    class ConsumerThread implements Runnable {
        private final CustomQueue processedData;
        private ConcurrentLinkedList linkedList;

        public ConsumerThread(CustomQueue processedData, ConcurrentLinkedList linkedList) {
            this.processedData = processedData;
            this.linkedList = linkedList;
        }

        @Override
        public void run() {
            Participant participant = processedData.get();
            while (participant != null) {
                logger.info("Adding participant {} to linked list", participant.getID());
                linkedList.insert(participant);
                participant = processedData.get();
            }
            consumerLatch.countDown();
        }
    }
}