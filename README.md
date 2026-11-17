# NetNotice

Real-time LAN announcements, replies and live polls for EC5205 - Operating System and Network Programming. Built with Java 21, TCP sockets, Java threads and Swing.

## Run NetNotice on Windows

Use JDK 21 on each computer running NetNotice. In PowerShell, check that both commands report version 21:

```powershell
java -version
javac -version
```

The commands below assume the project is in `D:\Projects\NetNotice`. If your copy is elsewhere, use its folder in each `cd` command.

### Desktop GUI

1. **Instructor.** Double-click `Start-Server.cmd` in File Explorer, or run it from PowerShell:

   ```powershell
   cd 'D:\Projects\NetNotice'
   .\Start-Server.cmd
   ```

   In the instructor window, open **Connection**, leave **Port** at `5000`, and click **Start server**. Keep this window open.

2. **Student.** Double-click `Start-Client.cmd`, or run it in a second PowerShell window:

   ```powershell
   cd 'D:\Projects\NetNotice'
   .\Start-Client.cmd
   ```

   Open **Connection**, enter a unique name under **Your name**, set **Server address** to `127.0.0.1` and **Port** to `5000`, then click **Join classroom**.

   If the student is on another computer on the same LAN, use **Copy LAN address** in the instructor’s **Connection** page. For example, if it copies `192.168.1.25:5000`, enter `192.168.1.25` under **Server address** and `5000` under **Port**.

Both GUI launchers compile the current source into `out` automatically. To check the connection, send an announcement from the instructor’s **Overview** page and look for it on the student’s **Classroom** page.

### Terminal only

1. **Compile** in PowerShell. Do this once per computer and again after changing the source:

   ```powershell
   cd 'D:\Projects\NetNotice'
   $sources = (Get-ChildItem .\src -Recurse -Filter '*.java').FullName
   javac -encoding UTF-8 -d out @sources
   ```

2. **Start the instructor server** in the same terminal:

   ```powershell
   java -cp out netnotice.server.NetNoticeServer --console
   ```

   Wait for `NetNotice Server started on port 5000`. Leave this terminal running.

3. **Join as a student** from a second PowerShell terminal:

   ```powershell
   cd 'D:\Projects\NetNotice'
   java -cp out netnotice.client.NetNoticeClient --console 127.0.0.1
   ```

   Enter a unique display name when prompted. For a student on another computer, replace `127.0.0.1` with the instructor’s LAN IPv4 address. Both console apps use port `5000`.

4. **Try an activity.** Type these commands in the running NetNotice consoles:

   | Action | Instructor terminal | Student terminal |
   | --- | --- | --- |
   | Send an announcement and reply | Type `Class starts now` | Type `Ready` |
   | Start a poll and vote | `/poll What should we review? \| Threads \| Sockets` | `vote 0` selects Threads |
   | Close the poll | `/endpoll` | Read the final totals |
   | Start a quiz and answer | `/poll [Quiz] Which protocol uses a connection? \| TCP \| UDP` | `answer 0` selects TCP |
   | Reveal quiz results | `/endpoll` | Read the final answer counts |

   Choices are numbered from `0`. To finish, type `exit` in the student terminal, then `exit` in the instructor terminal.

## Desktop interface

The redesigned interface has a pale sidebar, white toolbar, simple outline icons and restrained blue accents. Compact windows replace the sidebar with a row of directly selectable pages. Pages scroll vertically and fields adapt to the available width.

Instructor pages: Overview, Polls & Quizzes, Students, Activity log, Connection.
Student pages: Classroom, Activities, Connection.

## Features

- Start/stop a multithreaded TCP server and copy its LAN address.
- Register a unique display name; remember successful connection details locally.
- Broadcast announcements, including multiline and Unicode text.
- Recover announcement drafts (up to 8,000 characters), choose quick templates, preview or clear a draft, and send with Ctrl+Enter.
- Receive notices, copy text, mark the latest notice as read locally, and search/export session history.
- Student questions appear in the instructor Overview inbox, with sender, time, unread counts, mark-all-read and message export; they also remain in the activity log.
- New notices and student messages appear inside the NetNotice toolbar. The bell collapses or reopens pending alerts. View opens the relevant workspace and Dismiss removes the alert.
- Launch polls or blind quizzes with two to four choices; change a vote and view final counts.
- Quiz vote counts stay off the client network until the instructor closes the quiz. Quizzes display response counts, not graded scores.
- Track/search connected students; clean up votes and the roster on disconnect.
- Export activity logs and poll results as UTF-8 text; pause following the latest log entry.
- Original 650 ms soft notification bell with saved mute/volume controls and preview in Connection > Notifications.
- Student Ctrl+L opens connection settings. Connection attempts time out after five seconds when the server cannot be reached.
- Console mode remains available.

## Design and fonts

The interface follows Apple's [sidebar](https://developer.apple.com/design/human-interface-guidelines/sidebars), [typography](https://developer.apple.com/design/human-interface-guidelines/typography) and [colour](https://developer.apple.com/design/human-interface-guidelines/color) guidance: neutral surfaces, one blue action accent, clear hierarchy and a separate navigation layer. Icons and the notification bell are original; no Apple assets are copied.

macOS uses its system look and feel and interface font. Windows uses bundled Inter for controls and Inter Display for titles and reading text. The fonts ship under the SIL Open Font License in `assets/fonts/LICENSE.txt`.

## Verify

Run `powershell -NoProfile -ExecutionPolicy Bypass -File .\Test-NetNotice.ps1` to compile and run all three suites. Desktop tests briefly open and close dialogs and restore saved connection, draft, mute and volume preferences. See [TESTING.md](TESTING.md) for coverage and limits. Screenshots and the generated sound sample are saved in `out/previews`.

## TCP protocol

| Message | Purpose |
| --- | --- |
| JOIN\|Name | Register client |
| JOINED\|Name | Confirm registration |
| MESSAGE\|Text | Send/acknowledge a reply |
| DISCONNECT\|Name | Leave server |
| ERROR\|Text | Report an error |
| ANNOUNCEMENT\|Text | Broadcast notice |
| POLL\|Question\|Option1\|Option2 | Start activity |
| ANSWER\|Index | Submit zero-based answer |
| POLL_RESULT\|Count1\|Count2 | Live poll counts |
| POLL_END\|Count1\|Count2 | Final activity counts |

Fields containing newlines, pipes or the reserved `~b64~` marker use that marker followed by UTF-8 Base64. Simple fields keep the original wire format. Update server and clients together. Questions prefixed with `[Quiz]` withhold live counts.

## Architecture

NetNoticeServer manages a synchronized client registry and poll votes. Each connection receives a ClientHandler thread. ServerGUI provides instructor actions and receives server callbacks; ClientGUI listens for broadcasts and submits replies/votes. WorkspaceUI, DesktopShell and UITheme provide shared responsive layouts, navigation, typography, icons and audio.
