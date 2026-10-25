# NetNotice

Real-Time LAN Announcement and Response System

EC5205 - Operating System and Network Programming
Faculty of Engineering, University of Ruhuna

---

## Technology Stack
- **Language**: Java (JDK 21+)
- **Networking**: TCP Sockets (`java.net.ServerSocket`, `java.net.Socket`)
- **Concurrency**: Multithreading (`java.lang.Thread`, `Runnable`, synchronized data structures)
- **User Interface**: Java Swing (`JFrame`, `JList`, `JTextArea`, `SwingUtilities.invokeLater`)

---

## Current Features

### Completed by Member 1 (Core Networking Foundation)
- Multithreaded TCP Server accepting concurrent client connections
- Client display name registration (`JOIN|Name` -> `JOINED|Name`)
- Synchronized connected client tracking registry
- Bidirectional messaging (`MESSAGE|Text` -> reply)
- Graceful disconnect handling (`DISCONNECT|Name`)

### Completed by Member 2 (GUI & Announcement Broadcasting)
- **Announcement Broadcasting**: Server broadcasts `ANNOUNCEMENT|<Text>` to all active clients simultaneously.
- **Server Swing GUI (`ServerGUI`)**:
  - Live server status and port configuration.
  - Real-time connected client roster (automatically refreshed when clients join/leave).
  - Announcement composer and instant broadcast button.
  - Event log displaying server socket events.
- **Client Swing GUI (`ClientGUI`)**:
  - Intuitive connection panel (Name, Server IP, Port, Connect/Disconnect).
  - Background socket connection and non-blocking message listening.
  - Prominent announcement display card.
  - Full message and activity history log.
- **Shared Protocol Module (`Protocol`)**: Standardized command headers with clean hooks for Member 3.

---

## Compilation

From the project root:

```bash
javac -d out src/netnotice/common/*.java src/netnotice/server/*.java src/netnotice/client/*.java
```

---

## How to Run

### Option 1: Swing GUI Mode (Recommended)

1. **Start Server GUI**:
   ```bash
   java -cp out netnotice.server.ServerGUI
   ```
   *(or run `netnotice.server.NetNoticeServer`)*

2. **Start Client 1 GUI (e.g. Ahmed)**:
   ```bash
   java -cp out netnotice.client.ClientGUI Ahmed
   ```

3. **Start Client 2 GUI (e.g. Sajath)**:
   ```bash
   java -cp out netnotice.client.ClientGUI Sajath
   ```

*(In Eclipse: Right-click `ServerGUI.java` -> Run As -> Java Application. Then right-click `ClientGUI.java` -> Run As -> Java Application twice).*

### Option 2: Console Mode

1. **Run Server Console**:
   ```bash
   java -cp out netnotice.server.NetNoticeServer --console
   ```

2. **Run Client Console**:
   ```bash
   java -cp out netnotice.client.NetNoticeClient --console
   ```
   For LAN connections:
   ```bash
   java -cp out netnotice.client.NetNoticeClient SERVER_IP --console
   ```

---

## TCP Protocol Specification

| Message | Direction | Description |
|---|---|---|
| `JOIN\|<Name>` | Client -> Server | Register client display name |
| `JOINED\|<Name>` | Server -> Client | Confirmation of successful registration |
| `MESSAGE\|<Text>` | Client <-> Server | Client-to-server or reply text |
| `ANNOUNCEMENT\|<Text>` | Server -> All Clients | Broadcast announcement to all connected clients |
| `DISCONNECT\|<Name>` | Client -> Server | Client graceful disconnect request |
| `ERROR\|<Reason>` | Server -> Client | Error notice (e.g. Name already in use) |

---

## Threading & Swing Architecture

1. **Non-blocking Network Threading**:
   - `ClientHandler` runs inside a dedicated thread for each connected socket.
   - `ClientGUI` initiates socket connections and listens for server packets on background daemon threads, keeping the Swing Event Dispatch Thread (EDT) completely responsive and preventing UI freezes.
2. **Safe GUI Updates (`SwingUtilities.invokeLater`)**:
   - Background network events (incoming messages, client connections) trigger UI updates via `SwingUtilities.invokeLater()`, adhering strictly to Java Swing thread-safety rules.

---

## Next Steps for Member 3

- **Stage 11**: Question / Poll broadcast protocol (`QUESTION|<QuestionText>|<OptionA>|<OptionB>|...`).
- **Stage 12**: Client response buttons (A, B, C, D) and answer transmission (`ANSWER|<Option>`).
- **Stage 13**: Server-side answer collection, tallying, and live results chart/display (`RESULTS|<Counts>`).
- **Stage 14**: Poll reset, final integration, and network resilience.
