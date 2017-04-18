package me.ezeh.nftp.protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        try (ServerSocket testServer = new ServerSocket(8888)) {
            System.out.println("Network Test: " + (testSocket(testServer) ? "Successful" : "Unsuccessful"));
            System.out.println("Protocol Test: " + (testProtocol(testServer) ? "Successful" : "Unsuccessful"));
            System.out.println("File Request Test: " + (testFileRequest(testServer) ? "Successful" : "Unsuccessful"));
            System.out.println("Starting Server...");
            NFTPServer main = new NFTPServer(testServer);
            main.start();
        } catch (IOException e) {
            System.out.println("Unable to start test server");
            e.printStackTrace();
        }

    }

    public static boolean testSocket(ServerSocket server) {
        try {
            Socket client = new Socket("0.0.0.0", 8888);
            Socket s = server.accept();
            s.getOutputStream().write(Bytes.TEST);
            s.close();
            int r = client.getInputStream().read();
            return r == Bytes.TEST;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean testProtocol(ServerSocket server) {
        try {
            Connector client = new Connector("0.0.0.0", 8888);
            Connector conn = new Connector(server.accept());
            new Thread(() -> System.out.println("Client Simulation: " + (client.sayHello() ? "Successful" : "Unsuccessful"))).start();
            return conn.respondHello();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean testFileRequest(ServerSocket server) {
        try {
            Connector client = new Connector("0.0.0.0", 8888);
            Connector conn = new Connector(server.accept());
            new Thread(() -> client.requestFile("samuel")).start();
            int type = conn.readByte();
            if (type != Bytes.REQUEST) return false;
            boolean worked = conn.getString().equals("samuel");
            return worked;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


}
