package org.example;

import java.util.concurrent.*;

public class ClientsSimulator {
    public static void main(String[] args) {
        int deltaX = Integer.parseInt(args[0]);
        System.out.println(deltaX);
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 1; i <= 5; i++) {
            executorService.submit(new Client(i,  deltaX));
        }
        executorService.shutdown();
    }
}
