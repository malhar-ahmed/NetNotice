
package netnotice.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class NetNoticeServer {

    public static void main(String[] args) {

        int port = 5000;

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("NetNotice Server started!");
            System.out.println("Listening on port " + port);

            while (true) {

                Socket clientSocket = serverSocket.accept();

                System.out.println("New client connected: "
                        + clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(clientSocket);

                Thread clientThread = new Thread(handler);
                clientThread.start();
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}
