
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

            Thread listener = new Thread(() -> {
                try {
                    String response;

                    while ((response = reader.readLine()) != null) {
                        System.out.println("[SERVER] " + response);
                    }

                    System.out.println("Server closed the connection.");

                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        System.out.println("Connection lost: "
                                + e.getMessage());
                    }
                }
            });

            listener.setDaemon(true);
            listener.start();

            System.out.println("Type messages below.");
            System.out.println("Type 'exit' to disconnect.");

            while (scanner.hasNextLine()) {

                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("exit")) {
                    writer.println("DISCONNECT");
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
