package netnotice.test;

import netnotice.common.Protocol;
import netnotice.server.NetNoticeServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Member3IntegrationTest {

    static class TestClient {
        final String name;
        final Socket socket;
        final PrintWriter writer;
        final BufferedReader reader;
        final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        final Thread listenerThread;

        TestClient(String name, int port) throws Exception {
            this.name = name;
            this.socket = new Socket("127.0.0.1", port);
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            this.listenerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        messages.add(line);
                    }
                } catch (Exception ignored) {
                }
            });
            this.listenerThread.setDaemon(true);
            this.listenerThread.start();
        }

        void send(String msg) {
            writer.println(msg);
        }

        String waitForMessage(long timeoutMs) throws InterruptedException {
            return messages.poll(timeoutMs, TimeUnit.MILLISECONDS);
        }

        void close() {
            try {
                send(Protocol.PREFIX_DISCONNECT);
                Thread.sleep(100);
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    public static void main(String[] args) {
        int testPort = 19584;
        System.out.println("=== Starting Member 3 Integration Tests ===");

        try {
            // 1. Start Server
            System.out.println("[TEST] 1. Starting NetNoticeServer on port " + testPort);
            NetNoticeServer.startServer(testPort);
            Thread.sleep(300);

            // 2. Connect Alice, Bob, Charlie
            System.out.println("[TEST] 2. Connecting clients Alice, Bob, Charlie");
            TestClient alice = new TestClient("Alice", testPort);
            TestClient bob = new TestClient("Bob", testPort);
            TestClient charlie = new TestClient("Charlie", testPort);

            assertMsg("Alice welcome", alice.waitForMessage(2000), Protocol.PREFIX_MESSAGE + "Connected to NetNotice Server!");
            assertMsg("Bob welcome", bob.waitForMessage(2000), Protocol.PREFIX_MESSAGE + "Connected to NetNotice Server!");
            assertMsg("Charlie welcome", charlie.waitForMessage(2000), Protocol.PREFIX_MESSAGE + "Connected to NetNotice Server!");

            alice.send(Protocol.PREFIX_JOIN + "Alice");
            bob.send(Protocol.PREFIX_JOIN + "Bob");
            charlie.send(Protocol.PREFIX_JOIN + "Charlie");

            assertMsg("Alice joined", alice.waitForMessage(2000), Protocol.PREFIX_JOINED + "Alice");
            assertMsg("Bob joined", bob.waitForMessage(2000), Protocol.PREFIX_JOINED + "Bob");
            assertMsg("Charlie joined", charlie.waitForMessage(2000), Protocol.PREFIX_JOINED + "Charlie");

            // 3. Test Announcement Broadcast
            System.out.println("[TEST] 3. Testing announcement broadcast");
            NetNoticeServer.broadcastAnnouncement("Welcome to CS301");
            assertMsg("Alice got announcement", alice.waitForMessage(2000), Protocol.PREFIX_ANNOUNCEMENT + "Welcome to CS301");
            assertMsg("Bob got announcement", bob.waitForMessage(2000), Protocol.PREFIX_ANNOUNCEMENT + "Welcome to CS301");
            assertMsg("Charlie got announcement", charlie.waitForMessage(2000), Protocol.PREFIX_ANNOUNCEMENT + "Welcome to CS301");

            // 4. Test Poll Creation
            System.out.println("[TEST] 4. Testing poll creation and broadcast");
            List<String> options = Arrays.asList("IntelliJ", "VS Code", "Eclipse");
            NetNoticeServer.startPoll("Preferred IDE?", options);

            String expectedPoll = Protocol.PREFIX_POLL + "Preferred IDE?|IntelliJ|VS Code|Eclipse";
            assertMsg("Alice got poll", alice.waitForMessage(2000), expectedPoll);
            assertMsg("Bob got poll", bob.waitForMessage(2000), expectedPoll);
            assertMsg("Charlie got poll", charlie.waitForMessage(2000), expectedPoll);

            // 5. Test Voting & Live Results
            System.out.println("[TEST] 5. Testing votes and live tally updates");
            // Alice votes IntelliJ (0)
            alice.send(Protocol.PREFIX_ANSWER + "0");
            assertMsg("Alice got result", alice.waitForMessage(2000), Protocol.PREFIX_POLL_RESULT + "1|0|0");
            assertMsg("Bob got result", bob.waitForMessage(2000), Protocol.PREFIX_POLL_RESULT + "1|0|0");
            assertMsg("Charlie got result", charlie.waitForMessage(2000), Protocol.PREFIX_POLL_RESULT + "1|0|0");

            // Bob votes VS Code (1)
            bob.send(Protocol.PREFIX_ANSWER + "1");
            assertMsg("Alice got result", alice.waitForMessage(2000), Protocol.PREFIX_POLL_RESULT + "1|1|0");
            assertMsg("Bob got result", bob.waitForMessage(2000), Protocol.PREFIX_POLL_RESULT + "1|1|0");
            assertMsg("Charlie got result", charlie.waitForMessage(2000), Protocol.PREFIX_POLL_RESULT + "1|1|0");

            // Charlie votes IntelliJ (0)
            charlie.send(Protocol.PREFIX_ANSWER + "0");
            assertMsg("Alice got result", alice.waitForMessage(2000), Protocol.PREFIX_POLL_RESULT + "2|1|0");
            assertMsg("Bob got result", bob.waitForMessage(2000), Protocol.PREFIX_POLL_RESULT + "2|1|0");
            assertMsg("Charlie got result", charlie.waitForMessage(2000), Protocol.PREFIX_POLL_RESULT + "2|1|0");

            // 6. Test Vote Changing
            System.out.println("[TEST] 6. Testing vote change (Alice switches from 0 to 1)");
            alice.send(Protocol.PREFIX_ANSWER + "1");
            assertMsg("Alice changed vote result", alice.waitForMessage(2000), Protocol.PREFIX_POLL_RESULT + "1|2|0");
            assertMsg("Bob got updated result", bob.waitForMessage(2000), Protocol.PREFIX_POLL_RESULT + "1|2|0");
            assertMsg("Charlie got updated result", charlie.waitForMessage(2000), Protocol.PREFIX_POLL_RESULT + "1|2|0");

            // 7. Test Disconnect Vote Cleanup
            System.out.println("[TEST] 7. Testing disconnect vote cleanup (Bob voted option 1, now disconnects)");
            bob.close();
            // Remaining clients should receive updated poll result with Bob's vote removed (1|1|0)
            assertMsg("Alice sees Bob's vote removed", alice.waitForMessage(2000), Protocol.PREFIX_POLL_RESULT + "1|1|0");
            assertMsg("Charlie sees Bob's vote removed", charlie.waitForMessage(2000), Protocol.PREFIX_POLL_RESULT + "1|1|0");

            // 8. Test Late-Joining Client Receives Active Poll & Results
            System.out.println("[TEST] 8. Testing late-joining client David receiving active poll");
            TestClient david = new TestClient("David", testPort);
            assertMsg("David welcome", david.waitForMessage(2000), Protocol.PREFIX_MESSAGE + "Connected to NetNotice Server!");
            david.send(Protocol.PREFIX_JOIN + "David");
            assertMsg("David joined", david.waitForMessage(2000), Protocol.PREFIX_JOINED + "David");
            assertMsg("David receives active poll", david.waitForMessage(2000), expectedPoll);
            assertMsg("David receives current results", david.waitForMessage(2000), Protocol.PREFIX_POLL_RESULT + "1|1|0");

            // 9. Test Ending Poll
            System.out.println("[TEST] 9. Testing poll ending and final results");
            NetNoticeServer.endPoll();
            String expectedEnd = Protocol.PREFIX_POLL_END + "1|1|0";
            assertMsg("Alice got poll end", alice.waitForMessage(2000), expectedEnd);
            assertMsg("Charlie got poll end", charlie.waitForMessage(2000), expectedEnd);
            assertMsg("David got poll end", david.waitForMessage(2000), expectedEnd);

            // 10. Clean up
            alice.close();
            charlie.close();
            david.close();
            NetNoticeServer.stopServer();

            System.out.println("\n✅ ALL MEMBER 3 INTEGRATION TESTS PASSED SUCCESSFULLY! ✅\n");
            System.exit(0);

        } catch (Throwable t) {
            System.err.println("\n❌ TEST FAILED: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void assertMsg(String stepName, String actual, String expected) {
        if (expected.equals(actual)) {
            System.out.println("  [PASS] " + stepName + ": " + actual);
        } else {
            throw new AssertionError(stepName + " FAILED! Expected: <" + expected + "> but got: <" + actual + ">");
        }
    }
}
