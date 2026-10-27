
# NetNotice

Real-Time LAN Announcement and Response System

EC5205 - Operating System and Network Programming

## Technology
- Java
- TCP sockets
- Java threads
- Swing GUI

## Features
- Multithreaded TCP server with GUI admin panel
- Client Swing GUI with real-time updates
- Client display names with validation
- Server announcement broadcasting
- Interactive poll/quiz system with live results
- Graceful disconnect handling with poll vote cleanup
- Connected client tracking
- Console mode fallback (--console flag)
- LAN support

## Compile

From the project root:

javac -d out src/netnotice/common/Protocol.java src/netnotice/server/NetNoticeServer.java src/netnotice/server/ClientHandler.java src/netnotice/server/ServerGUI.java src/netnotice/client/NetNoticeClient.java src/netnotice/client/ClientGUI.java

## Run Server (GUI)

java -cp out netnotice.server.NetNoticeServer

## Run Server (Console)

java -cp out netnotice.server.NetNoticeServer --console

## Run Client (GUI)

java -cp out netnotice.client.NetNoticeClient

## Run Client (Console, same computer)

java -cp out netnotice.client.NetNoticeClient --console

## Run Client (Console, LAN)

java -cp out netnotice.client.NetNoticeClient --console SERVER_IP

Replace SERVER_IP with the server laptop's IPv4 address.

## TCP Protocol

JOIN|Name            - Register client
JOINED|Name          - Registration successful
MESSAGE|Text         - Send normal message
DISCONNECT|Name      - Disconnect client
ERROR|Text           - Report an error
ANNOUNCEMENT|Text    - Broadcast announcement to all clients
POLL|Q|Opt1|Opt2|..  - Server sends poll with question and options
ANSWER|Index         - Client submits poll answer (0-based option index)
POLL_RESULT|C1|C2|.. - Server broadcasts live vote counts per option
POLL_END|C1|C2|..    - Server ends poll with final vote counts

## Architecture

NetNoticeServer accepts TCP connections and manages client handlers.

Each client receives its own ClientHandler thread.

A synchronized client registry tracks connected users.

ServerGUI provides admin controls: start/stop, announcements, polls.

ClientGUI provides user interface: connect, view announcements, answer polls.

The poll system tracks votes per client, broadcasts live results,
and cleans up votes when clients disconnect.

## Console Commands

### Server Console Mode
- Type text to broadcast an announcement
- `/poll Question | Option1 | Option2 | ...` to start a poll
- `/endpoll` to end the current poll
- `exit` to stop the server

### Client Console Mode
- Type text to send a message
- `vote <number>` to answer a poll (0-based index)
- `exit` to disconnect
