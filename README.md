
# NetNotice

Real-Time LAN Announcement and Response System

EC5205 - Operating System and Network Programming

## Technology
- Java
- TCP sockets
- Java threads
- Console-based networking

## Current Features
- Multiple TCP clients
- Client display names
- Client-to-server messages
- Server-to-client replies
- Connected client tracking
- Client disconnect handling

## Compile

From the project root:

javac -d out src/netnotice/server/NetNoticeServer.java src/netnotice/server/ClientHandler.java src/netnotice/client/NetNoticeClient.java

## Run Server

java -cp out netnotice.server.NetNoticeServer

## Run Client (same computer)

java -cp out netnotice.client.NetNoticeClient

## Run Client (LAN)

java -cp out netnotice.client.NetNoticeClient SERVER_IP

Replace SERVER_IP with the server laptop's IPv4 address.

## TCP Protocol

JOIN|Name           - Register client
JOINED|Name         - Registration successful
MESSAGE|Text        - Send normal message
DISCONNECT|Name     - Disconnect client
ERROR|Text          - Report an error

## Architecture

NetNoticeServer accepts TCP connections.

Each client receives its own ClientHandler thread.

A synchronized client registry tracks connected users.

## Next Development

Member 2:
- Announcement broadcasting
- Server Swing GUI
- Client Swing GUI

Member 3:
- Polls and answers
- Live results
- Final integration
