
package netnotice.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        try (
            Socket client = socket;

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(client.getInputStream())
            );

            PrintWriter writer = new PrintWriter(
                client.getOutputStream(), true
            )
        ) {

            writer.println("MESSAGE|Connected to NetNotice Server!");

            String message;

            while ((message = reader.readLine()) != null) {

                if (message.equals("DISCONNECT")) {
                    System.out.println("Client requested disconnect.");
                    break;
                }

                if (message.startsWith("MESSAGE|")) {

                    String text = message.substring(8);

                    System.out.println(
                        "Client " + socket.getRemoteSocketAddress()
                        + " says: " + text
                    );

                    writer.println("MESSAGE|Server received: " + text);

                } else {
                    writer.println("ERROR|Unknown message type");
                }
            }

        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }

        System.out.println("Client disconnected: "
                + socket.getRemoteSocketAddress());
    }
}
