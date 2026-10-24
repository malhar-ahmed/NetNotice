
package netnotice.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetNoticeServer {

    private static final Map<String, ClientHandler> connectedClients
            = new HashMap<>();

    public static void main(String[] args) {

        int port = 5000;

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("NetNotice Server started!");
            System.out.println("Listening on port " + port);

            while (true) {

                Socket clientSocket = serverSocket.accept();

                System.out.println("New connection: "
                        + clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(clientSocket);

                Thread clientThread = new Thread(handler);
                clientThread.start();
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    // Register a new client
    public static synchronized boolean registerClient(
            String name, ClientHandler handler) {

        if (connectedClients.containsKey(name)) {
            return false;
        }

        connectedClients.put(name, handler);

        System.out.println("Client joined: " + name);
        displayConnectedClients();

        return true;
    }

    // Remove a disconnected client
    public static synchronized void removeClient(
            String name, ClientHandler handler) {

        if (connectedClients.remove(name, handler)) {
            System.out.println("Client left: " + name);
            displayConnectedClients();
        }
    }

    // Display the currently connected clients
    private static void displayConnectedClients() {

        List<String> names = new ArrayList<>(
                connectedClients.keySet());

        Collections.sort(names);

        System.out.println("Connected clients (" + names.size()
                + "): " + (names.isEmpty()
                ? "(none)"
                : String.join(", ", names)));
    }
}
