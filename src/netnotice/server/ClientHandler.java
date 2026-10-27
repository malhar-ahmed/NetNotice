package netnotice.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import netnotice.common.Protocol;

/**
 * Handles communication for an individual connected client in a dedicated thread.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private String clientName;
    private PrintWriter writer;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public String getClientName() {
        return clientName;
    }

    /**
     * Send a message to this specific client.
     * Synchronized to prevent interleaving of lines from multiple threads.
     */
    public synchronized void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    /**
     * Close socket and associated streams.
     */
    public void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }

    @Override
    public void run() {

        try (
            Socket client = socket;
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(client.getInputStream())
            )
        ) {
            // Initialize writer for this client socket
            this.writer = new PrintWriter(client.getOutputStream(), true);

            // Send initial welcome message
            sendMessage(Protocol.PREFIX_MESSAGE + "Connected to NetNotice Server!");

            String message;

            while ((message = reader.readLine()) != null) {

                // Client registration
                if (message.startsWith(Protocol.PREFIX_JOIN)) {

                    if (clientName != null) {
                        sendMessage(Protocol.PREFIX_ERROR + "Already joined");
                        continue;
                    }

                    String name = message.substring(Protocol.PREFIX_JOIN.length()).trim();

                    if (name.isEmpty() || name.length() > 20 || name.contains("|")) {
                        sendMessage(Protocol.PREFIX_ERROR + "Invalid display name");
                        continue;
                    }

                    boolean registered = NetNoticeServer.registerClient(name, this);

                    if (registered) {
                        clientName = name;
                        sendMessage(Protocol.PREFIX_JOINED + clientName);

                        // Send current active poll to newly joined client (Member 3)
                        String pollMsg = NetNoticeServer.getCurrentPollMessage();
                        if (pollMsg != null) {
                            sendMessage(pollMsg);
                            String resultMsg = NetNoticeServer.getCurrentPollResultMessage();
                            if (resultMsg != null) {
                                sendMessage(resultMsg);
                            }
                        }
                    } else {
                        sendMessage(Protocol.PREFIX_ERROR + "Name already in use");
                    }

                }

                // Normal message
                else if (message.startsWith(Protocol.PREFIX_MESSAGE)) {

                    if (clientName == null) {
                        sendMessage(Protocol.PREFIX_ERROR + "Please join first");
                        continue;
                    }

                    String text = message.substring(Protocol.PREFIX_MESSAGE.length());
                    System.out.println("[" + clientName + "] " + text);
                    NetNoticeServer.logEvent("[" + clientName + "] " + text);

                    sendMessage(Protocol.PREFIX_MESSAGE + "Server received: " + text);
                }

                // Client disconnect request
                else if (message.equals(Protocol.PREFIX_DISCONNECT)
                        || (clientName != null && message.equals(Protocol.PREFIX_DISCONNECT + "|" + clientName))) {
                    break;
                }

                // Poll answer from client (Member 3)
                else if (message.startsWith(Protocol.PREFIX_ANSWER)) {

                    if (clientName == null) {
                        sendMessage(Protocol.PREFIX_ERROR + "Please join first");
                        continue;
                    }

                    try {
                        int optionIndex = Integer.parseInt(
                                message.substring(Protocol.PREFIX_ANSWER.length()).trim());

                        boolean accepted = NetNoticeServer.submitPollAnswer(clientName, optionIndex);

                        if (!accepted) {
                            sendMessage(Protocol.PREFIX_ERROR + "No active poll or invalid option");
                        }
                    } catch (NumberFormatException e) {
                        sendMessage(Protocol.PREFIX_ERROR + "Invalid answer format");
                    }
                }

                // Unknown message type
                else {
                    sendMessage(Protocol.PREFIX_ERROR + "Unknown message type");
                }
            }

        } catch (IOException e) {
            System.out.println("Connection error for "
                    + (clientName != null ? clientName : socket.getRemoteSocketAddress())
                    + ": " + e.getMessage());

        } finally {
            if (clientName != null) {
                NetNoticeServer.removeClient(clientName, this);
            }

            System.out.println("Connection closed: " + socket.getRemoteSocketAddress());
        }
    }
}
