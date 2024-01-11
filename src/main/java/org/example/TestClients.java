package org.example;

import java.io.IOException;
import java.util.concurrent.*;

public class TestClients {
    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 1; i <= 5; i++) {
            executorService.submit(new Client(i, 100));
        }
        executorService.shutdown();
    }
}
