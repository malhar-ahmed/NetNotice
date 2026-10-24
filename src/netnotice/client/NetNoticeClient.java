
package netnotice.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class NetNoticeClient {

    public static void main(String[] args) {

        String serverIP = "127.0.0.1";
        int port = 5000;

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

            System.out.println("Connected to NetNotice Server!");

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

                if (clientName.isEmpty()
                        || clientName.length() > 20
                        || clientName.contains("|")) {

                    System.out.println("Invalid name. Try again.");
                    continue;
                }

                writer.println("JOIN|" + clientName);

                String response = reader.readLine();

                if (response == null) {
                    System.out.println("Server disconnected.");
                    return;
                }

                System.out.println("[SERVER] " + response);

                if (response.equals("JOINED|" + clientName)) {
                    break;
                }
            }

            // Listen for server messages
            Thread listener = new Thread(() -> {

                try {
                    String response;

                    while ((response = reader.readLine()) != null) {
                        System.out.println("[SERVER] " + response);
                    }

                    System.out.println("Server disconnected.");

                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        System.out.println("Connection lost: "
                                + e.getMessage());
                    }
                }
            });

            listener.setDaemon(true);
            listener.start();

            System.out.println("You can now send messages.");
            System.out.println("Type 'exit' to disconnect.");

            // Send messages to server
            while (scanner.hasNextLine()) {

                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("exit")) {

                    writer.println("DISCONNECT|" + clientName);
                    break;
                }

                if (!input.trim().isEmpty()) {
                    writer.println("MESSAGE|" + input);
                }
            }

            System.out.println("Client stopped.");

        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}
