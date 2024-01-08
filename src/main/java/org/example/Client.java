package org.example;

import org.example.utils.JSONBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;

public class Client implements Runnable{
    private int id;
    private String folder;

    private long interval = 100;
    private long lastTimeSent = 0;

    public Client(int id, long interval) {
        this.id = id;
        this.folder = "data/C" + id;
        this.interval = interval;

    }

    public void sendData(List<String> buffer) {
        try {
            Thread.sleep(max(0, interval - (System.currentTimeMillis() - lastTimeSent)));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        JSONObject data = JSONBuilder.create()
                .addHeader("Country", "C" + id)
                .addHeader("type", "sending-data")
                .setBody(new JSONArray(buffer))
                .build();
        try {
            Socket socket = new Socket("localhost", 8000);
            Connection connection = new Connection(socket);
            System.out.println("Sending request " + data.toString());
            connection.send(data);
            connection.terminate();
            buffer.clear();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        lastTimeSent = System.currentTimeMillis();
    }

    @Override
    public void run() {
        // read from file
        ArrayList<String> buffer = new ArrayList<>();
        try {
            for (int i = 1; i <= 10; ++i) {
                String file = folder + "/rezultateC" + id + "_" + i + ".in";
                FileReader fileReader = new FileReader(file);
                BufferedReader reader = new BufferedReader(fileReader);
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.add(line);
                    if (buffer.size() == 20) {
                        sendData(buffer);
                    }
                }
            }
            if (buffer.size() > 0) {
                sendData(buffer);
            }
            JSONObject data = JSONBuilder.create()
                    .addHeader("Country", "C" + id)
                    .addHeader("type", "get-ranking")
                    .build();
            Socket socket = new Socket("localhost", 8000);
            Connection connection = new Connection(socket);
            connection.send(data);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
