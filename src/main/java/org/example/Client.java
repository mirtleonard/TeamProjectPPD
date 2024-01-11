package org.example;

import org.apache.commons.codec.binary.Base64;
import org.example.utils.JSONBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
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
        ArrayList<String> buffer = new ArrayList<>();
        try {
            for (int i = 1; i <= 10; ++i) {
                readDataFromFile(buffer, i);
                sendRankingRequest("get-ranking");
            }
            sendRankingRequest("get-final-ranking");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readDataFromFile(ArrayList<String> buffer, int fileIndex) throws IOException {
        String file = folder + "/rezultateC" + id + "_" + fileIndex + ".in";
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.add(line);
                if (buffer.size() == 20) {
                    sendData(buffer);
                }
            }
            if (buffer.size() > 0) {
                sendData(buffer);
            }
        }
    }

    private void sendRankingRequest(String requestType) throws IOException {
        JSONObject data = JSONBuilder.create()
                .addHeader("Country", "C" + id)
                .addHeader("type",requestType)
                .build();
        Socket socket = new Socket("localhost", 8000);
        Connection connection = new Connection(socket);
        connection.send(data);
        JSONObject receivedData = connection.read();
        processReceivedData(receivedData);

    }

    private void processReceivedData(JSONObject receivedData) {
        JSONObject header = receivedData.optJSONObject("header");

        if (header != null) {
            String responseType = header.optString("type");

            switch (responseType) {
                case " ranking":
                    System.out.println(receivedData.toString());
                    break;
                case "final-ranking":
                    handleReceivedData(receivedData);
                    break;
                default:
                    System.out.println("Unknown response " + responseType);
                    break;
            }
        } else {
            System.out.println("Invalid response");
        }

        System.out.println(receivedData.toString());
    }

    public void handleReceivedData(JSONObject receivedData) {
        try {
            JSONObject body = receivedData.optJSONObject("body");
            if (body != null) {
                if (body.has("participants")) {
                    String participantsEncoded = body.getString("participants");
                    byte[] participantsContent = Base64.decodeBase64(participantsEncoded);
                    saveFile("participants_ranking.txt", participantsContent);
                }
                if (body.has("countries")) {
                    String countriesEncoded = body.getString("countries");
                    byte[] countriesContent = Base64.decodeBase64(countriesEncoded);
                    saveFile("country_ranking.txt", countriesContent);
                }
            } else {
                System.out.println("Invalid response");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void saveFile(String fileName, byte[] content) throws IOException {
        File directory = new File("data/filesfromserver");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        String baseName = fileName.substring(0, lastDotIndex);
        String extension = fileName.substring(lastDotIndex);
        String newFileName = baseName + id + extension;

        File file = new File(directory, newFileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content);
        }
    }
}


