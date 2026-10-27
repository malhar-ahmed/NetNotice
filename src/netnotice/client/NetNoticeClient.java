package netnotice.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import netnotice.common.Protocol;

/**
 * NetNoticeClient: Console client for NetNotice.
 * Supports LAN/localhost connections, display names, receiving broadcasts,
 * and answering polls. Can also launch ClientGUI if requested.
 */
public class NetNoticeClient {

    public static void main(String[] args) {

        // Check if user specifically requested console mode
        boolean forceConsole = false;
        String serverIP = Protocol.DEFAULT_HOST;
        int port = Protocol.DEFAULT_PORT;

        for (String arg : args) {
            if (arg.equalsIgnoreCase("--console")) {
                forceConsole = true;
            } else if (!arg.startsWith("-")) {
                serverIP = arg;
            }
        }

        // If not forced to console, launch ClientGUI for standard user experience
        if (!forceConsole) {
            ClientGUI.main(args);
            return;
        }

        // Console client execution:
        try (
            Socket socket = new Socket(serverIP, port);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            PrintWriter writer = new PrintWriter(
                socket.getOutputStream(), true
            );
            Scanner scanner = new Scanner(System.in)
        ) {

            System.out.println("Connected to NetNotice Server at " + serverIP + ":" + port);

            String greeting = reader.readLine();
            if (greeting == null) {
                System.out.println("Server disconnected.");
                return;
            }

            System.out.println("[SERVER] " + greeting);

            String clientName;

            // Join the server using a display name
            while (true) {
                System.out.print("Enter display name: ");
                if (!scanner.hasNextLine()) {
                    return;
                }

                clientName = scanner.nextLine().trim();

                if (clientName.isEmpty() || clientName.length() > 20 || clientName.contains("|")) {
                    System.out.println("Invalid name. Must be 1-20 characters without '|'. Try again.");
                    continue;
                }

                writer.println(Protocol.PREFIX_JOIN + clientName);

                String response = reader.readLine();
                if (response == null) {
                    System.out.println("Server disconnected.");
                    return;
                }

                System.out.println("[SERVER] " + response);

                if (response.equals(Protocol.PREFIX_JOINED + clientName)) {
                    break;
                }
            }

            // Background thread to listen for server broadcasts
            Thread listener = new Thread(() -> {
                try {
                    String response;
                    while ((response = reader.readLine()) != null) {

                        // Announcement
                        if (response.startsWith(Protocol.PREFIX_ANNOUNCEMENT)) {
                            String announcement = response.substring(Protocol.PREFIX_ANNOUNCEMENT.length());
                            System.out.println("\n========================================");
                            System.out.println("  NEW ANNOUNCEMENT:");
                            System.out.println("  " + announcement);
                            System.out.println("========================================\n");
                        }

                        // Poll question received (Member 3)
                        else if (response.startsWith(Protocol.PREFIX_POLL)) {
                            String payload = response.substring(Protocol.PREFIX_POLL.length());
                            String[] parts = payload.split("\\|");
                            if (parts.length >= 3) {
                                System.out.println("\n========================================");
                                System.out.println("  NEW POLL: " + parts[0]);
                                for (int i = 1; i < parts.length; i++) {
                                    System.out.println("    " + (i - 1) + ") " + parts[i]);
                                }
                                System.out.println("  Type 'vote <number>' to answer");
                                System.out.println("========================================\n");
                            }
                        }

                        // Live poll results (Member 3)
                        else if (response.startsWith(Protocol.PREFIX_POLL_RESULT)) {
                            String payload = response.substring(Protocol.PREFIX_POLL_RESULT.length());
                            String[] counts = payload.split("\\|");
                            StringBuilder sb = new StringBuilder("[POLL RESULTS] ");
                            for (int i = 0; i < counts.length; i++) {
                                if (i > 0) {
                                    sb.append(", ");
                                }
                                sb.append("Option ").append(i).append(": ").append(counts[i]);
                            }
                            sb.append(" votes");
                            System.out.println(sb.toString());
                        }

                        // Poll ended (Member 3)
                        else if (response.startsWith(Protocol.PREFIX_POLL_END)) {
                            String payload = response.substring(Protocol.PREFIX_POLL_END.length());
                            String[] counts = payload.split("\\|");
                            System.out.println("\n========================================");
                            StringBuilder sb = new StringBuilder("  POLL ENDED - Final: ");
                            for (int i = 0; i < counts.length; i++) {
                                if (i > 0) {
                                    sb.append(", ");
                                }
                                sb.append("Option ").append(i).append(": ").append(counts[i]);
                            }
                            sb.append(" votes");
                            System.out.println(sb.toString());
                            System.out.println("========================================\n");
                        }

                        // Other server messages
                        else {
                            System.out.println("[SERVER] " + response);
                        }
                    }
                    System.out.println("Server disconnected.");
                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        System.out.println("Connection lost: " + e.getMessage());
                    }
                }
            });

            listener.setDaemon(true);
            listener.start();

            System.out.println("Joined successfully. You can now send messages or wait for announcements.");
            System.out.println("Type 'vote <number>' to answer polls.");
            System.out.println("Type 'exit' to disconnect.");

            // Send messages to server
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("exit")) {
                    writer.println(Protocol.PREFIX_DISCONNECT + "|" + clientName);
                    break;
                }

                // Vote command for polls (Member 3)
                if (input.toLowerCase().startsWith("vote ")) {
                    try {
                        int idx = Integer.parseInt(input.substring(5).trim());
                        writer.println(Protocol.PREFIX_ANSWER + idx);
                    } catch (NumberFormatException e) {
                        System.out.println("Usage: vote <number>");
                    }
                    continue;
                }

                if (!input.trim().isEmpty()) {
                    writer.println(Protocol.PREFIX_MESSAGE + input);
                }
            }

            System.out.println("Client stopped.");

        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}
