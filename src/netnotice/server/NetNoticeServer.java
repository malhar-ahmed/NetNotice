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
 * Manages client connections, tracks active users, broadcasts announcements,
 * and manages interactive polls with live results.
 */
public class NetNoticeServer {

    private static final Map<String, ClientHandler> connectedClients = new HashMap<>();
    private static ServerSocket serverSocket;
    private static volatile boolean isRunning = false;
    private static ServerListener listener;

    // Poll state (Member 3)
    private static String currentPollQuestion = null;
    private static List<String> currentPollOptions = new ArrayList<>();
    private static final Map<String, Integer> pollVotes = new HashMap<>();

    /**
     * Interface for GUI or external observers to receive server events.
     */
    public interface ServerListener {
        void onClientListUpdated(List<String> clients);
        void onLogMessage(String message);
        void onPollStateChanged(String question, List<String> options,
                                int[] voteCounts, int totalVotes, boolean isActive);
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
            System.out.println("Running in console mode. Commands:");
            System.out.println("  Type text to broadcast an announcement");
            System.out.println("  /poll Question | Option1 | Option2 | ...  - Start a poll");
            System.out.println("  /endpoll  - End the current poll");
            System.out.println("  exit  - Stop the server");

            try (Scanner scanner = new Scanner(System.in)) {
                while (scanner.hasNextLine()) {
                    String input = scanner.nextLine().trim();

                    if (input.equalsIgnoreCase("exit")) {
                        stopServer();
                        break;
                    }

                    // Poll command: /poll Question | Option1 | Option2 | ...
                    if (input.toLowerCase().startsWith("/poll ")) {
                        String pollData = input.substring(6).trim();
                        String[] parts = pollData.split("\\s*\\|\\s*");

                        if (parts.length < 3) {
                            System.out.println("Usage: /poll Question | Option1 | Option2 | ...");
                            continue;
                        }

                        String question = parts[0].trim();
                        List<String> options = new ArrayList<>();

                        for (int i = 1; i < parts.length; i++) {
                            String opt = parts[i].trim();
                            if (!opt.isEmpty()) {
                                options.add(opt);
                            }
                        }

                        if (options.size() < 2) {
                            System.out.println("At least 2 options are required.");
                            continue;
                        }

                        startPoll(question, options);
                        continue;
                    }

                    // End poll command
                    if (input.equalsIgnoreCase("/endpoll")) {
                        if (isPollActive()) {
                            endPoll();
                        } else {
                            System.out.println("No active poll to end.");
                        }
                        continue;
                    }

                    // Default: broadcast as announcement
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

        // Clear poll state
        currentPollQuestion = null;
        currentPollOptions.clear();
        pollVotes.clear();

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

            // Clean up poll vote on disconnect (Member 3)
            removePollVote(name);

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

    // ========================================================
    // Poll System (Member 3)
    // ========================================================

    /**
     * Start a new poll and broadcast to all connected clients.
     * If a poll is already active, it is ended first.
     */
    public static synchronized void startPoll(String question, List<String> options) {
        if (currentPollQuestion != null) {
            endPoll();
        }

        currentPollQuestion = question;
        currentPollOptions = new ArrayList<>(options);
        pollVotes.clear();

        // Build poll message: POLL|question|opt1|opt2|...
        StringBuilder sb = new StringBuilder(Protocol.PREFIX_POLL);
        sb.append(question);
        for (String option : options) {
            sb.append("|").append(option);
        }
        String pollMessage = sb.toString();

        for (ClientHandler handler : connectedClients.values()) {
            handler.sendMessage(pollMessage);
        }

        logEvent("Poll started: " + question + " (" + options.size() + " options)");
        notifyPollStateChanged(true);
    }

    /**
     * Submit a poll answer from a client.
     * Allows changing a previous vote.
     *
     * @return true if accepted, false if no active poll or invalid index
     */
    public static synchronized boolean submitPollAnswer(String clientName, int optionIndex) {
        if (currentPollQuestion == null) {
            return false;
        }
        if (optionIndex < 0 || optionIndex >= currentPollOptions.size()) {
            return false;
        }

        boolean isChange = pollVotes.containsKey(clientName);
        pollVotes.put(clientName, optionIndex);

        if (isChange) {
            logEvent(clientName + " changed vote to: " + currentPollOptions.get(optionIndex));
        } else {
            logEvent(clientName + " voted: " + currentPollOptions.get(optionIndex));
        }

        // Broadcast updated results to all clients
        broadcastPollResults();
        notifyPollStateChanged(true);

        return true;
    }

    /**
     * End the current poll and broadcast final results.
     */
    public static synchronized void endPoll() {
        if (currentPollQuestion == null) {
            return;
        }

        // Broadcast final results with POLL_END prefix
        int[] counts = getVoteCounts();
        StringBuilder sb = new StringBuilder(Protocol.PREFIX_POLL_END);
        for (int i = 0; i < counts.length; i++) {
            if (i > 0) {
                sb.append("|");
            }
            sb.append(counts[i]);
        }
        String endMessage = sb.toString();

        for (ClientHandler handler : connectedClients.values()) {
            handler.sendMessage(endMessage);
        }

        logEvent("Poll ended: " + currentPollQuestion
                + " (Total votes: " + pollVotes.size() + ")");

        // Notify listener before clearing state so it can read final data
        notifyPollStateChanged(false);

        // Clear poll state
        currentPollQuestion = null;
        currentPollOptions.clear();
        pollVotes.clear();
    }

    /**
     * Check if a poll is currently active.
     */
    public static synchronized boolean isPollActive() {
        return currentPollQuestion != null;
    }

    /**
     * Build the POLL| message for the current active poll.
     * Used to send the active poll to newly joined clients.
     *
     * @return the poll message, or null if no poll is active
     */
    public static synchronized String getCurrentPollMessage() {
        if (currentPollQuestion == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(Protocol.PREFIX_POLL);
        sb.append(currentPollQuestion);
        for (String option : currentPollOptions) {
            sb.append("|").append(option);
        }
        return sb.toString();
    }

    /**
     * Build the current POLL_RESULT| message for live results.
     *
     * @return the result message, or null if no poll is active
     */
    public static synchronized String getCurrentPollResultMessage() {
        if (currentPollQuestion == null) {
            return null;
        }
        int[] counts = getVoteCounts();
        StringBuilder sb = new StringBuilder(Protocol.PREFIX_POLL_RESULT);
        for (int i = 0; i < counts.length; i++) {
            if (i > 0) {
                sb.append("|");
            }
            sb.append(counts[i]);
        }
        return sb.toString();
    }

    /**
     * Remove a disconnected client's poll vote and broadcast updated results.
     */
    private static void removePollVote(String clientName) {
        if (currentPollQuestion != null && pollVotes.remove(clientName) != null) {
            broadcastPollResults();
            notifyPollStateChanged(true);
            logEvent("Poll vote removed (client disconnected): " + clientName);
        }
    }

    /**
     * Broadcast current poll results to all connected clients.
     */
    private static void broadcastPollResults() {
        int[] counts = getVoteCounts();
        StringBuilder sb = new StringBuilder(Protocol.PREFIX_POLL_RESULT);
        for (int i = 0; i < counts.length; i++) {
            if (i > 0) {
                sb.append("|");
            }
            sb.append(counts[i]);
        }
        String resultMessage = sb.toString();

        for (ClientHandler handler : connectedClients.values()) {
            handler.sendMessage(resultMessage);
        }
    }

    /**
     * Get vote counts for each option in the current poll.
     */
    private static int[] getVoteCounts() {
        int[] counts = new int[currentPollOptions.size()];
        for (int optionIndex : pollVotes.values()) {
            if (optionIndex >= 0 && optionIndex < counts.length) {
                counts[optionIndex]++;
            }
        }
        return counts;
    }

    /**
     * Notify the registered listener about poll state changes.
     */
    private static void notifyPollStateChanged(boolean isActive) {
        if (listener != null) {
            String question = currentPollQuestion;
            List<String> options = new ArrayList<>(currentPollOptions);
            int[] voteCounts = currentPollOptions.isEmpty() ? new int[0] : getVoteCounts();
            int totalVotes = pollVotes.size();
            listener.onPollStateChanged(question, options, voteCounts, totalVotes, isActive);
        }
    }

    // ========================================================
    // Utility Methods
    // ========================================================

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
