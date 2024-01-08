package org.example;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestClients {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 1; i <= 2; i++) {
            executorService.submit(new Client(i, 100));
        }
        executorService.shutdown();
    }
}
