
package netnotice.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private String clientName;

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

                // Client registration
                if (message.startsWith("JOIN|")) {

                    if (clientName != null) {
                        writer.println("ERROR|Already joined");
                        continue;
                    }

                    String name = message.substring(5).trim();

                    if (name.isEmpty() || name.length() > 20
                            || name.contains("|")) {

                        writer.println("ERROR|Invalid display name");
                        continue;
                    }

                    boolean registered =
                            NetNoticeServer.registerClient(name, this);

                    if (registered) {
                        clientName = name;
                        writer.println("JOINED|" + clientName);
                    } else {
                        writer.println("ERROR|Name already in use");
                    }

                }

                // Receive messages
                else if (message.startsWith("MESSAGE|")) {

                    if (clientName == null) {
                        writer.println("ERROR|Please join first");
                        continue;
                    }

                    String text = message.substring(8);

                    System.out.println(
                        "[" + clientName + "] " + text
                    );

                    writer.println("MESSAGE|Server received: " + text);
                }

                // Client disconnect request
                else if (message.equals("DISCONNECT")
                        || (clientName != null &&
                        message.equals("DISCONNECT|" + clientName))) {

                    break;
                }

                // Unknown message type
                else {
                    writer.println("ERROR|Unknown message type");
                }
            }

        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());

        } finally {

            if (clientName != null) {
                NetNoticeServer.removeClient(clientName, this);
            }

            System.out.println("Connection closed: "
                    + socket.getRemoteSocketAddress());
        }
    }
}
