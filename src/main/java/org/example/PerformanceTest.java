package org.example;

public class PerformanceTest {
    public static void main(String[] args) {
        final int [] delta_x = {1, 2};
        final int [] delta_t = {1, 2, 4};
        final int[][] p_r = {{2}, {4}};
        final int[][] p_w = {{2}, {2, 4, 8}};
        for (int i = 0; i < delta_x.length; i++) {
            for (int j = 0; j < delta_t.length; j++) {
               for (int x = 0; x < 2; ++x) {
                   for (int k = 0; k < p_r[x].length; k++) {
                       for (int l = 0; l < p_w[x].length; l++) {
                           long startTime = System.currentTimeMillis();
                           String[] main_args = {String.valueOf(p_r[x][k]), String.valueOf(p_w[x][l]), String.valueOf(delta_t[j])};
                           Thread server = new Thread(() -> Server.main(main_args));
                           server.start();
                           final int finalI = i;
                           String[] clients_args = new String[]{String.valueOf(delta_x[finalI])};
                           Thread clients = new Thread(() -> ClientsSimulator.main(clients_args));
                            try {
                                 Thread.sleep(100);
                            } catch (InterruptedException e) {
                                 e.printStackTrace();
                            }
                           clients.start();
                           try {
                               clients.join();
                               server.join();
                           } catch (InterruptedException e) {
                               e.printStackTrace();
                           }
                           long endTime = System.currentTimeMillis();
                            System.out.println("Total execution time: " + (endTime - startTime) + "ms" + " for delta_x = " + delta_x[finalI] + " delta_t = " + delta_t[j] + " p_r = " + p_r[x][k] + " p_w = " + p_w[x][l]);
                       }
                   }
               }
            }
        }
    }
}
