package netnotice.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import netnotice.common.Protocol;

/**
 * NetNoticeServer: Multithreaded TCP server for NetNotice.
 * Manages client connections, tracks active users, and broadcasts announcements.
 */
public class NetNoticeServer {

    private static final Map<String, ClientHandler> connectedClients = new HashMap<>();
    private static ServerSocket serverSocket;
    private static volatile boolean isRunning = false;
    private static ServerListener listener;

    /**
     * Interface for GUI or external observers to receive server events.
     */
    public interface ServerListener {
        void onClientListUpdated(List<String> clients);
        void onLogMessage(String message);
    }

    /**
     * Register an event listener (such as ServerGUI).
     */
    public static synchronized void setListener(ServerListener newListener) {
        listener = newListener;
    }

    /**
     * Main entry point. Can run in console mode or launch ServerGUI.
     */
    public static void main(String[] args) {
        // If "--console" argument is provided, run as console-only server
        if (args.length > 0 && args[0].equalsIgnoreCase("--console")) {
            startConsoleServer(Protocol.DEFAULT_PORT);
        } else {
            // By default, launch the Swing Server GUI
            ServerGUI.main(args);
        }
    }

    /**
     * Start the server in a separate background thread (useful for GUI).
     */
    public static synchronized void startServer(int port) throws IOException {
        if (isRunning) {
            return;
        }

        serverSocket = new ServerSocket(port);
        isRunning = true;
        logEvent("NetNotice Server started on port " + port);

        Thread acceptThread = new Thread(() -> {
            try {
                while (isRunning && !serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    logEvent("New TCP connection: " + clientSocket.getRemoteSocketAddress());

                    ClientHandler handler = new ClientHandler(clientSocket);
                    Thread clientThread = new Thread(handler);
                    clientThread.start();
                }
            } catch (IOException e) {
                if (isRunning) {
                    logEvent("Server accept loop error: " + e.getMessage());
                }
            } finally {
                isRunning = false;
            }
        }, "NetNotice-AcceptThread");

        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    /**
     * Start the server directly in console mode with an admin command loop.
     */
    public static void startConsoleServer(int port) {
        try {
            startServer(port);
            System.out.println("Running in console mode. Type an announcement to broadcast, or 'exit' to quit:");

            try (Scanner scanner = new Scanner(System.in)) {
                while (scanner.hasNextLine()) {
                    String input = scanner.nextLine().trim();
                    if (input.equalsIgnoreCase("exit")) {
                        stopServer();
                        break;
                    }
                    if (!input.isEmpty()) {
                        broadcastAnnouncement(input);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server on port " + port + ": " + e.getMessage());
        }
    }

    /**
     * Stop the server and disconnect clients.
     */
    public static synchronized void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            // Disconnect all connected clients
            for (ClientHandler handler : connectedClients.values()) {
                handler.closeConnection();
            }
            connectedClients.clear();
            notifyClientListUpdated();
            logEvent("Server stopped.");
        } catch (IOException e) {
            logEvent("Error stopping server: " + e.getMessage());
        }
    }

    /**
     * Register a new client by display name.
     */
    public static synchronized boolean registerClient(String name, ClientHandler handler) {
        if (connectedClients.containsKey(name)) {
            return false;
        }

        connectedClients.put(name, handler);
        logEvent("Client joined: " + name);
        displayConnectedClients();
        notifyClientListUpdated();
        return true;
    }

    /**
     * Remove a disconnected client.
     */
    public static synchronized void removeClient(String name, ClientHandler handler) {
        if (connectedClients.remove(name, handler)) {
            logEvent("Client left: " + name);
            displayConnectedClients();
            notifyClientListUpdated();
        }
    }

    /**
     * Broadcast an announcement to all currently connected clients.
     */
    public static synchronized void broadcastAnnouncement(String announcementText) {
        String message = Protocol.PREFIX_ANNOUNCEMENT + announcementText;
        logEvent("Broadcasting announcement: " + announcementText);

        for (ClientHandler handler : connectedClients.values()) {
            handler.sendMessage(message);
        }
    }

    /**
     * Returns a sorted list of currently connected client display names.
     */
    public static synchronized List<String> getConnectedClients() {
        List<String> names = new ArrayList<>(connectedClients.keySet());
        Collections.sort(names);
        return names;
    }

    /**
     * Console output of connected clients.
     */
    private static void displayConnectedClients() {
        List<String> names = getConnectedClients();
        System.out.println("Connected clients (" + names.size() + "): "
                + (names.isEmpty() ? "(none)" : String.join(", ", names)));
    }

    /**
     * Log a message to console and GUI listener.
     */
    public static void logEvent(String message) {
        System.out.println("[SERVER] " + message);
        if (listener != null) {
            listener.onLogMessage(message);
        }
    }

    /**
     * Notify registered listener about client list changes.
     */
    private static void notifyClientListUpdated() {
        if (listener != null) {
            listener.onClientListUpdated(getConnectedClients());
        }
    }

    public static boolean isRunning() {
        return isRunning;
    }
}
