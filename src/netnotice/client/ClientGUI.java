package netnotice.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import netnotice.common.FeatureHeader;
import netnotice.common.ActivityType;
import netnotice.common.Protocol;
import netnotice.common.UITheme;

/** Student workspace with responsive pages, session notice history and live activities. */
public class ClientGUI extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String APP_TITLE = "NetNotice - Client Portal";

    // Connection Form Components
    JTextField nameField;
    private JTextField ipField;
    private JTextField portField;
    private UITheme.FlatButton connectButton;
    private UITheme.FlatButton disconnectButton;
    private UITheme.StatusBadge statusBadge;
    private JLabel uptimeLabel;

    // Announcement Components
    private ClassroomWelcome welcome;
    private JPanel notice;
    private JPanel reply;
    private JPanel sessionRow;
    private JTextArea announcementDisplay;
    private JLabel announcementTimeLabel;
    private JLabel announcementCountLabel;
    private int announcementCount = 0;
    private final java.util.List<String> notices = new java.util.ArrayList<>();
    private JButton copyNoticeButton;
    private JButton soundToggleButton;
    private JButton markReadButton;
    private netnotice.common.NotificationCenter notifications;

    // Response / Reply to Server Feature
    private JTextField replyField;
    private UITheme.FlatButton sendReplyButton;

    // Poll & Quiz Components
    private UITheme.StatusBadge pollStatusBadge;
    private JLabel pollTitleLabel;
    private JTextArea pollQuestionLabel;
    private JPanel optionsPanel;
    private UITheme.FlatButton[] pollOptionButtons;
    private JTextArea pollResultsArea;
    private JPanel visualBarsPanel;
    private UITheme.PollProgressBar[] visualBars;
    private String[] currentOptions;
    private javax.swing.JTabbedPane workspaceTabs;
    private int votedOption = -1;
    private FeatureHeader activityHeader;
    private JPanel activityBody;
    private JPanel resultsCard;
    private JLabel resultTitle;
    private boolean isQuizMode = false;

    // Status Bar & Minimalist Event Info
    private JLabel statusBarLabel;
    private JLabel lastEventLabel;
    private JTextArea historyArea = new JTextArea(); // Retained in memory

    // Networking
    private transient volatile Socket socket;
    private transient BufferedReader reader;
    private transient PrintWriter writer;
    private transient Thread listenerThread;
    private volatile boolean isConnected = false;

    // Live Connection Timer
    private Timer uptimeTimer;
    private long connectStartTime;

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    @SuppressWarnings("this-escape")
    public ClientGUI() {
        super(APP_TITLE);
        initUI();
    }

    private void initUI() {
        netnotice.common.WorkspaceUI.setup(this, 1000, 760);
        setTitle("NetNotice • Student");
        notifications=new netnotice.common.NotificationCenter();
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(ClientGUI.class);
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UITheme.BG_APP);
        javax.swing.JTabbedPane tabs = new javax.swing.JTabbedPane();
        workspaceTabs = tabs;
        netnotice.common.WorkspaceUI.tabs(tabs);

        JPanel home = netnotice.common.WorkspaceUI.stack();
        JPanel connection = netnotice.common.WorkspaceUI.card("Join your classroom", "Enter your name and the server address shared by your instructor.", UITheme.Icons.users(22, UITheme.PRIMARY));
        nameField = new UITheme.RoundedTextField(prefs.get("name", "Student"), 10);
        ipField = new UITheme.RoundedTextField(prefs.get("host", Protocol.DEFAULT_HOST), 12);
        portField = new UITheme.RoundedTextField(prefs.get("port", String.valueOf(Protocol.DEFAULT_PORT)), 5);
        JPanel fields = netnotice.common.WorkspaceUI.grid(3); fields.setOpaque(false);
        fields.add(netnotice.common.WorkspaceUI.field("Your name", nameField));
        fields.add(netnotice.common.WorkspaceUI.field("Server address", ipField));
        fields.add(netnotice.common.WorkspaceUI.field("Port", portField));
        connection.add(fields, BorderLayout.CENTER);
        connectButton = UITheme.primaryButton("Join classroom");
        connectButton.setIcon(UITheme.Icons.check(16, Color.WHITE));
        connectButton.addActionListener(e -> startConnect());
        disconnectButton = UITheme.optionButton("Leave"); disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(e -> disconnect());
        statusBadge = new UITheme.StatusBadge("Offline", UITheme.BG_CARD_ALT, UITheme.TEXT_MUTED);
        uptimeLabel = new JLabel(); uptimeLabel.setFont(UITheme.FONT_SMALL);
        connection.add(netnotice.common.WorkspaceUI.row(connectButton, disconnectButton, statusBadge, uptimeLabel), BorderLayout.SOUTH);
        JPanel connectionPage = netnotice.common.WorkspaceUI.stack();
        netnotice.common.WorkspaceUI.add(connectionPage, connection);
        JButton openConnection = UITheme.optionButton("Connection settings");
        openConnection.addActionListener(e -> tabs.setSelectedIndex(2));
        sessionRow = netnotice.common.WorkspaceUI.row(statusBadge, uptimeLabel, openConnection);
        netnotice.common.WorkspaceUI.add(home, sessionRow);
        welcome = new ClassroomWelcome(() -> { tabs.setSelectedIndex(2); nameField.requestFocusInWindow(); });
        netnotice.common.WorkspaceUI.add(home, welcome);

        notice = netnotice.common.WorkspaceUI.card("Latest announcement", "", UITheme.Icons.broadcast(22, UITheme.PRIMARY));
        announcementDisplay = netnotice.common.WorkspaceUI.text("", 3);
        announcementDisplay.setFont(UITheme.FONT_READING);
        announcementDisplay.setBackground(Color.WHITE);
        announcementDisplay.setBorder(BorderFactory.createEmptyBorder(5, 0, 6, 0));
        announcementDisplay.setFocusable(false);
        notice.add(netnotice.common.WorkspaceUI.scroll(announcementDisplay), BorderLayout.CENTER);
        announcementCountLabel = new JLabel("0 received"); announcementTimeLabel = new JLabel("Waiting for a notice");
        copyNoticeButton = UITheme.optionButton("Copy"); copyNoticeButton.setIcon(UITheme.Icons.copy(16, UITheme.PRIMARY));
        copyNoticeButton.setEnabled(false); copyNoticeButton.addActionListener(e -> copyAnnouncementToClipboard());
        soundToggleButton = UITheme.optionButton(UITheme.Sound.isSoundEnabled()?"Sound on":"Muted"); soundToggleButton.setIcon(UITheme.Icons.volume(16, UITheme.SUCCESS));
        soundToggleButton.addActionListener(e -> toggleSound());
        JButton soundSettings = UITheme.optionButton("Sound settings");
        soundSettings.addActionListener(e -> { UITheme.Sound.showSettings(this); refreshSoundButton(); });
        markReadButton = UITheme.optionButton("Mark as read"); markReadButton.setEnabled(false);
        markReadButton.addActionListener(e -> { announcementTimeLabel.setText("Read just now"); markReadButton.setText("Read"); markReadButton.setEnabled(false); });
        JButton history = UITheme.optionButton("History"); history.addActionListener(e -> showNoticeHistory());
        JPanel noticeFooter = new JPanel(new BorderLayout()); noticeFooter.setOpaque(false);
        noticeFooter.add(netnotice.common.WorkspaceUI.row(announcementCountLabel, announcementTimeLabel), BorderLayout.NORTH);
        noticeFooter.add(netnotice.common.WorkspaceUI.row(copyNoticeButton, history, markReadButton), BorderLayout.CENTER);
        notice.add(noticeFooter, BorderLayout.SOUTH);
        netnotice.common.WorkspaceUI.add(home, notice);

        reply = netnotice.common.WorkspaceUI.card(new FeatureHeader(FeatureHeader.Art.MESSAGE, "Ask your instructor", "Write a question and press Enter to send it."));
        replyField = new UITheme.RoundedTextField("", 20); replyField.setEnabled(false);
        replyField.addActionListener(e -> sendClientReply());
        sendReplyButton = UITheme.primaryButton("Send message"); sendReplyButton.setEnabled(false);
        sendReplyButton.addActionListener(e -> sendClientReply());
        JPanel messageRow = new JPanel(new BorderLayout(10,0)); messageRow.setOpaque(false);
        messageRow.add(replyField,BorderLayout.CENTER); messageRow.add(sendReplyButton,BorderLayout.EAST);
        reply.add(messageRow, BorderLayout.CENTER);
        netnotice.common.WorkspaceUI.add(home, reply);
        refreshClassroom();
        tabs.addTab("  Classroom  ", UITheme.Icons.broadcast(18, UITheme.PRIMARY), netnotice.common.WorkspaceUI.page(home));

        JPanel activities = netnotice.common.WorkspaceUI.stack();
        activityHeader = new FeatureHeader(FeatureHeader.Art.QUIZ, "Waiting for an activity", "Your instructor’s next poll or quiz will appear here.");
        JPanel poll = netnotice.common.WorkspaceUI.card(activityHeader);
        pollTitleLabel = new JLabel("Live activity"); pollTitleLabel.setFont(UITheme.FONT_SECTION);
        pollStatusBadge = new UITheme.StatusBadge("No active activity", UITheme.BG_CARD_ALT, UITheme.TEXT_MUTED);
        pollQuestionLabel = netnotice.common.WorkspaceUI.text("Your instructor hasn't started an activity yet.", 2); pollQuestionLabel.setFont(UITheme.FONT_BOLD);
        JPanel pollContent = new JPanel(new BorderLayout(0, 14)); pollContent.setOpaque(false);
        JPanel pollHeading = new JPanel(new BorderLayout(0, 12)); pollHeading.setOpaque(false);
        pollHeading.add(netnotice.common.WorkspaceUI.row(pollTitleLabel, pollStatusBadge), BorderLayout.NORTH);
        pollHeading.add(pollQuestionLabel, BorderLayout.CENTER);
        pollContent.add(pollHeading, BorderLayout.NORTH);
        optionsPanel = new JPanel(new GridLayout(0, 1, 0, 10)); optionsPanel.setOpaque(false);
        pollOptionButtons = new UITheme.FlatButton[4];
        for (int i=0; i<4; i++) {
            final int index=i;
            pollOptionButtons[i] = UITheme.optionButton("Option " + (char)('A'+i));
            pollOptionButtons[i].setHorizontalAlignment(JButton.LEFT);
            pollOptionButtons[i].setEnabled(false);
            pollOptionButtons[i].addActionListener(e -> submitAnswer(index)); optionsPanel.add(pollOptionButtons[i]);
        }
        activityBody = pollContent; activityBody.setVisible(false);
        pollContent.add(optionsPanel, BorderLayout.CENTER); poll.add(pollContent, BorderLayout.CENTER);
        netnotice.common.WorkspaceUI.add(activities, poll);
        resultTitle = new JLabel("Results");
        resultTitle.setFont(UITheme.FONT_SECTION);
        JPanel results = netnotice.common.WorkspaceUI.card(resultTitle);
        resultsCard = results;
        resultsCard.setVisible(false);
        visualBarsPanel = new JPanel(new GridLayout(0, 1, 0, 10)); visualBarsPanel.setOpaque(false);
        visualBars = new UITheme.PollProgressBar[4];
        for(int i=0;i<4;i++) { visualBars[i]=new UITheme.PollProgressBar((char)('A'+i), "Option"); visualBars[i].setVisible(false); visualBarsPanel.add(visualBars[i]); }
        pollResultsArea = netnotice.common.WorkspaceUI.text("Results will appear after an activity starts.", 5);
        pollResultsArea.setBackground(UITheme.BG_CARD_ALT); pollResultsArea.setMargin(new Insets(12,12,12,12));
        JPanel resultBody = new JPanel(new BorderLayout(0, 12)); resultBody.setOpaque(false);
        resultBody.add(visualBarsPanel, BorderLayout.NORTH); resultBody.add(netnotice.common.WorkspaceUI.scroll(pollResultsArea), BorderLayout.CENTER);
        results.add(resultBody, BorderLayout.CENTER); netnotice.common.WorkspaceUI.add(activities, results);
        tabs.addTab("  Activities  ", UITheme.Icons.poll(18, UITheme.SUCCESS), netnotice.common.WorkspaceUI.page(activities));
        JPanel soundCard = netnotice.common.WorkspaceUI.card("Notifications", "A gentle, original bell for new notices. Your volume and mute preferences are remembered.", UITheme.Icons.volume(20, UITheme.TEXT_SECONDARY));
        soundCard.add(netnotice.common.WorkspaceUI.row(soundToggleButton, soundSettings), BorderLayout.CENTER);
        netnotice.common.WorkspaceUI.add(connectionPage, soundCard);
        tabs.addTab("  Connection  ", UITheme.Icons.settings(18, UITheme.PRIMARY), netnotice.common.WorkspaceUI.page(connectionPage));
        root.add(tabs, BorderLayout.CENTER);
        statusBarLabel = new JLabel("  Offline • Join a classroom to get started"); lastEventLabel = new JLabel("Ready  ");
        JPanel footer = new JPanel(new BorderLayout()); footer.setBorder(BorderFactory.createEmptyBorder(9,16,9,16));
        footer.add(statusBarLabel, BorderLayout.CENTER); footer.add(lastEventLabel, BorderLayout.EAST); root.add(footer, BorderLayout.SOUTH);
        setContentPane(new netnotice.common.DesktopShell(root, tabs, "Student", notifications));
        refreshSoundButton();
        netnotice.common.WorkspaceUI.shortcut(getRootPane(), "control L", () -> { tabs.setSelectedIndex(2); ipField.requestFocusInWindow(); });
        portField.addActionListener(e -> { if(connectButton.isEnabled()) startConnect(); });
        uptimeTimer = new Timer(1000, e -> updateUptime());
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { disconnect(); }
            public void windowActivated(WindowEvent e) { setTitle("NetNotice • Student"); }
        });
        tabs.addChangeListener(e -> { home.revalidate(); activities.revalidate(); });
    }

    private void refreshClassroom() {
        boolean hasNotice = announcementCount > 0;
        welcome.connected(isConnected);
        welcome.setVisible(!hasNotice);
        notice.setVisible(hasNotice);
        reply.setVisible(isConnected);
        sessionRow.setVisible(isConnected || hasNotice);
        welcome.getParent().revalidate();
        welcome.getParent().repaint();
    }

    private void copyAnnouncementToClipboard() {
        String text = announcementDisplay.getText().trim();
        if (!text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            copyNoticeButton.setText("Copied!");
            Timer t = new Timer(1500, e -> copyNoticeButton.setText("Copy"));
            t.setRepeats(false);
            t.start();
        }
    }

    private void refreshSoundButton() {
        boolean enabled=UITheme.Sound.isSoundEnabled();
        soundToggleButton.setText(enabled?"Sound on":"Muted");
        soundToggleButton.setIcon(enabled?UITheme.Icons.volume(16,UITheme.TEXT_SECONDARY):UITheme.Icons.volumeMute(16,UITheme.TEXT_MUTED));
    }
    private void toggleSound() {
        UITheme.Sound.setSoundEnabled(!UITheme.Sound.isSoundEnabled()); refreshSoundButton();
    }

    private void sendClientReply() {
        if (!isConnected || writer == null) {
            return;
        }
        String text = replyField.getText().trim();
        if (text.isEmpty() || text.equals("Type a question or message to server...")) {
            return;
        }
        writer.println(Protocol.PREFIX_MESSAGE + Protocol.encode(text));
        lastEventLabel.setText("Sent reply: \"" + (text.length() > 30 ? text.substring(0, 27) + "..." : text) + "\"");
        replyField.setText("");
        replyField.requestFocus();
    }

    private void updateUptime() {
        if (!isConnected) {
            uptimeLabel.setText("");
            return;
        }
        long elapsedSec = (System.currentTimeMillis() - connectStartTime) / 1000;
        long minutes = elapsedSec / 60;
        long seconds = elapsedSec % 60;
        uptimeLabel.setText(String.format("Session %02d:%02d", minutes, seconds));
    }

    // ================================================================
    // Connection Management
    // ================================================================
    private void startConnect() {
        String name = nameField.getText().trim();
        String ip = ipField.getText().trim();
        String portStr = portField.getText().trim();

        if (name.isEmpty() || name.length() > 20 || name.contains("|") || name.contains("\n") || name.contains("\r")) {
            JOptionPane.showMessageDialog(this,
                    "Name must be 1-20 characters without the '|' symbol.",
                    "Invalid Name", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if(ip.isEmpty()) { ipField.requestFocusInWindow(); return; }
        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port < 1024 || port > 65535) {
                JOptionPane.showMessageDialog(this,
                        "Port must be between 1024 and 65535.",
                        "Invalid Port", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Port must be a valid number.",
                    "Invalid Port", JOptionPane.ERROR_MESSAGE);
            return;
        }

        connectButton.setEnabled(false);
        statusBadge.setStatus("● Connecting...", UITheme.AMBER_BG, UITheme.AMBER);
        statusBarLabel.setText("● Connecting to " + ip + ":" + port + "...");

        new Thread(() -> connect(name, ip, port), "NetNotice-ClientConnectThread").start();
    }

    private void connect(String name, String ip, int port) {
        try {
            socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(ip, port), 5000);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            writer = new PrintWriter(socket.getOutputStream(), true, java.nio.charset.StandardCharsets.UTF_8);

            // Read welcome greeting
            socket.setSoTimeout(5000);
            String greeting = reader.readLine();
            if (greeting != null) {
                SwingUtilities.invokeLater(() -> lastEventLabel.setText("[SERVER] " + greeting));
            }

            // Send registration request
            writer.println(Protocol.PREFIX_JOIN + name);

            // Read response
            String response = reader.readLine();
            if (response == null || !response.equals(Protocol.PREFIX_JOINED + name)) {
                String errMsg = (response != null && response.startsWith(Protocol.PREFIX_ERROR))
                        ? response.substring(Protocol.PREFIX_ERROR.length())
                        : "Registration rejected";
                throw new IOException(errMsg);
            }

            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(ClientGUI.class);
            prefs.put("name", name); prefs.put("host", ip); prefs.put("port", String.valueOf(port));
            socket.setSoTimeout(0);
            isConnected = true;
            connectStartTime = System.currentTimeMillis();

            SwingUtilities.invokeLater(() -> {
                statusBadge.setStatus("● Connected as " + name, UITheme.SUCCESS_BG, UITheme.SUCCESS);
                statusBarLabel.setText("Connected to " + ip + ":" + port + " as " + name);
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                nameField.setEnabled(false);
                ipField.setEnabled(false);
                portField.setEnabled(false);
                refreshClassroom();
                replyField.setEnabled(true);
                sendReplyButton.setEnabled(true);
                uptimeTimer.start();
                updateUptime();
                lastEventLabel.setText("Joined successfully as: " + name);
            });

            Socket session = socket;
            BufferedReader sessionReader = reader;
            listenerThread = new Thread(() -> listenForServerMessages(session, sessionReader), "NetNotice-ClientListenerThread");
            listenerThread.setDaemon(true);
            listenerThread.start();

        } catch (Exception e) {
            disconnect();
            SwingUtilities.invokeLater(() -> {
                statusBadge.setStatus("● Connection Failed", UITheme.DANGER_BG, UITheme.DANGER);
                statusBarLabel.setText("● Connection failed");
                connectButton.setEnabled(true);
                JOptionPane.showMessageDialog(this,
                        "Could not connect to " + ip + ":" + port + "\nReason: " + e.getMessage(),
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    private void listenForServerMessages(Socket session, BufferedReader sessionReader) {
        try {
            String line;
            while (isConnected && socket==session && (line = sessionReader.readLine()) != null) {
                final String msg = line;

                // 1. Announcement broadcast received
                if (msg.startsWith(Protocol.PREFIX_ANNOUNCEMENT)) {
                    String announcementText = Protocol.decode(msg.substring(Protocol.PREFIX_ANNOUNCEMENT.length()));
                    SwingUtilities.invokeLater(() -> onAnnouncementReceived(announcementText));
                }

                // 2. Poll/Quiz question received
                else if (msg.startsWith(Protocol.PREFIX_POLL)) {
                    String payload = msg.substring(Protocol.PREFIX_POLL.length());
                    String[] parts = payload.split("\\|", -1);
                    for(int field=0;field<parts.length;field++) parts[field]=Protocol.decode(parts[field]);
                    if (parts.length >= 3) {
                        String question = parts[0];
                        String[] options = new String[parts.length - 1];
                        for (int i = 1; i < parts.length; i++) {
                            options[i - 1] = parts[i];
                        }
                        SwingUtilities.invokeLater(() -> onPollReceived(question, options));
                    }
                }

                // 3. Live poll results
                else if (msg.startsWith(Protocol.PREFIX_POLL_RESULT)) {
                    String payload = msg.substring(Protocol.PREFIX_POLL_RESULT.length());
                    String[] countStrs = payload.split("\\|");
                    int[] counts = new int[countStrs.length];
                    for (int i = 0; i < countStrs.length; i++) {
                        try {
                            counts[i] = Integer.parseInt(countStrs[i].trim());
                        } catch (NumberFormatException e) {
                            counts[i] = 0;
                        }
                    }
                    final int[] finalCounts = counts;
                    SwingUtilities.invokeLater(() -> onPollResultsReceived(finalCounts));
                }

                // 4. Poll/Quiz ended
                else if (msg.startsWith(Protocol.PREFIX_POLL_END)) {
                    String payload = msg.substring(Protocol.PREFIX_POLL_END.length());
                    String[] countStrs = payload.split("\\|");
                    int[] counts = new int[countStrs.length];
                    for (int i = 0; i < countStrs.length; i++) {
                        try {
                            counts[i] = Integer.parseInt(countStrs[i].trim());
                        } catch (NumberFormatException e) {
                            counts[i] = 0;
                        }
                    }
                    final int[] finalCounts = counts;
                    SwingUtilities.invokeLater(() -> onPollEnded(finalCounts));
                }

                // 5. Normal server message received
                else if (msg.startsWith(Protocol.PREFIX_MESSAGE)) {
                    String text = Protocol.decode(msg.substring(Protocol.PREFIX_MESSAGE.length()));
                    SwingUtilities.invokeLater(() -> lastEventLabel.setText("[SERVER] " + text));
                }

                // 6. Error received
                else if (msg.startsWith(Protocol.PREFIX_ERROR)) {
                    String err = Protocol.decode(msg.substring(Protocol.PREFIX_ERROR.length()));
                    SwingUtilities.invokeLater(() -> lastEventLabel.setText("[ERROR] " + err));
                }
            }
        } catch (IOException e) {
            if (isConnected) {
                SwingUtilities.invokeLater(() -> lastEventLabel.setText("Connection lost: " + e.getMessage()));
            }
        } finally {
            synchronized(this) { if(socket==session) disconnect(); }
        }
    }

    private void showNoticeHistory() {
        javax.swing.JDialog dialog = new javax.swing.JDialog(this, "Announcement history", false);
        JPanel body = new JPanel(new BorderLayout(12, 12));
        body.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        body.setBackground(UITheme.BG_APP);
        JTextField search = new UITheme.RoundedTextField("", 24);
        search.setToolTipText("Search announcements");
        JPanel heading = new JPanel(new BorderLayout(8, 8));
        heading.setOpaque(false);
        heading.add(new JLabel("Search announcements"), BorderLayout.NORTH);
        heading.add(search, BorderLayout.CENTER);
        JTextArea entries = new JTextArea();
        entries.setEditable(false); entries.setLineWrap(true); entries.setWrapStyleWord(true);
        entries.setFont(UITheme.FONT_REGULAR); entries.setMargin(new Insets(12, 12, 12, 12));
        Runnable filter = () -> {
            String query = search.getText().trim().toLowerCase(java.util.Locale.ROOT);
            StringBuilder result = new StringBuilder();
            for (String notice : notices) if (notice.toLowerCase(java.util.Locale.ROOT).contains(query)) result.append(notice).append("\n\n");
            entries.setText(result.length() == 0 ? "No announcements found." : result.toString());
            entries.setCaretPosition(0);
        };
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter.run(); }
        });
        JButton export = UITheme.primaryButton("Export history");
        export.addActionListener(e -> UITheme.exportText(dialog, String.join("\n\n", notices), "netnotice-history.txt"));
        body.add(heading, BorderLayout.NORTH); body.add(new JScrollPane(entries), BorderLayout.CENTER);
        body.add(export, BorderLayout.SOUTH); filter.run();
        dialog.setContentPane(body); dialog.setSize(600, 460); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
    }

    private void onAnnouncementReceived(String text) {
        String timestamp = TIME_FORMAT.format(new Date());
        copyNoticeButton.setEnabled(true);
        markReadButton.setText("Mark as read"); markReadButton.setEnabled(true);
        announcementCount++;
        notices.add(0, "[" + timestamp + "] " + text);
        announcementDisplay.setText(text);
        announcementTimeLabel.setText(timestamp);
        announcementCountLabel.setText(announcementCount + (announcementCount == 1 ? " notice" : " notices"));
        refreshClassroom();
        lastEventLabel.setText("Notice at " + timestamp);

        notifications.show("New announcement",text,() -> { workspaceTabs.setSelectedIndex(0); notice.scrollRectToVisible(new java.awt.Rectangle(0,0,1,1)); });
        // Play the original soft notification bell
        UITheme.Sound.playAnnouncementChime();

        if (!isActive()) {
            setTitle("(New Notice!) " + APP_TITLE);
        }
    }

    // ==========================================
    // Poll & Quiz Logic (Conditional Live Results)
    // ==========================================
    private void onPollReceived(String question, String[] options) {
        workspaceTabs.setSelectedIndex(1);
        currentOptions = options;
        votedOption = -1;

        // Detect whether this is a Quiz (blind voting) or a Poll (live results)
        ActivityType type = ActivityType.from(question);
        isQuizMode = type == ActivityType.QUIZ;
        activityHeader.update(isQuizMode ? FeatureHeader.Art.QUIZ : FeatureHeader.Art.POLL,
                isQuizMode ? "Answer the quiz" : "Cast your vote",
                isQuizMode ? "Choose your answer. Quiz results are revealed when the quiz closes." : "Choose an option to vote. Poll results update as votes arrive.");
        for (UITheme.PollProgressBar bar : visualBars) bar.setActivityType(type);
        resultTitle.setText(type.title + " results");
        activityBody.setVisible(true); resultsCard.setVisible(!isQuizMode);

        if (isQuizMode) {
            pollTitleLabel.setText("Quiz question");
            pollStatusBadge.setStatus("● Quiz Active", UITheme.AMBER_BG, UITheme.AMBER);
            pollResultsArea.setText("Quiz Mode: Live results are hidden by the instructor.\nSubmit your answer above. Final results will be revealed when closed.");
        } else {
            pollTitleLabel.setText("Poll question");
            pollStatusBadge.setStatus("● Poll Active", UITheme.SUCCESS_BG, UITheme.SUCCESS);
            pollResultsArea.setText("Waiting for responses...");
        }

        pollQuestionLabel.setText(ActivityType.displayQuestion(question));

        // Update option buttons
        for (int i = 0; i < pollOptionButtons.length; i++) {
            if (i < options.length) {
                String prefix = "(" + (char) ('A' + i) + ") ";
                pollOptionButtons[i].setText(prefix + options[i]);
                pollOptionButtons[i].setEnabled(true);
                pollOptionButtons[i].setVisible(true);
                pollOptionButtons[i].setColors(UITheme.BG_CARD_ALT, UITheme.PRIMARY_BG, UITheme.TEXT_PRIMARY);
                pollOptionButtons[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.PRIMARY, 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
                ));
            } else {
                pollOptionButtons[i].setVisible(false);
            }
        }
        optionsPanel.revalidate();
        optionsPanel.repaint();

        // Initialize visual bars (visible for Poll, hidden for Quiz)
        for (int i = 0; i < visualBars.length; i++) {
            if (i < options.length) {
                visualBars[i].updateData(options[i], 0, 0, false, false);
                visualBars[i].setVisible(!isQuizMode); // Hidden if Quiz!
            } else {
                visualBars[i].setVisible(false);
            }
        }
        visualBarsPanel.revalidate();
        visualBarsPanel.repaint();

        lastEventLabel.setText("New " + (isQuizMode ? "Quiz" : "Poll") + " received");
    }

    private void onPollResultsReceived(int[] counts) {
        if (currentOptions == null) {
            return;
        }

        // IF QUIZ: DO NOT SHOW LIVE RESULTS TO CLIENTS!
        if (isQuizMode) {
            for (UITheme.PollProgressBar bar : visualBars) {
                bar.setVisible(false);
            }
            if (votedOption >= 0) {
                pollResultsArea.setText("✓ Your answer has been submitted!\nLive results are hidden during the quiz. Final results will appear when closed.");
            } else {
                pollResultsArea.setText("Quiz Mode: Live results are hidden.\nPlease select your answer above.");
            }
            return;
        }

        // IF POLL: SHOW LIVE RESULTS TO CLIENTS!
        int total = 0;
        int maxVotes = -1;
        for (int c : counts) {
            total += c;
            if (c > maxVotes) {
                maxVotes = c;
            }
        }

        for (int i = 0; i < visualBars.length; i++) {
            if (i < currentOptions.length) {
                int c = (i < counts.length) ? counts[i] : 0;
                boolean isMy = (i == votedOption);
                boolean isLead = (total > 0 && c == maxVotes);
                visualBars[i].updateData(currentOptions[i], c, total, isMy, isLead);
                visualBars[i].setVisible(true);
            } else {
                visualBars[i].setVisible(false);
            }
        }
        visualBarsPanel.revalidate();
        visualBarsPanel.repaint();

        pollResultsArea.setText(ActivityType.POLL.results(false, currentOptions, counts, votedOption));
    }

    private void onPollEnded(int[] counts) {
        pollStatusBadge.setStatus(isQuizMode ? "● Quiz Closed" : "● Poll Ended", UITheme.DANGER_BG, UITheme.DANGER);

        for (UITheme.FlatButton btn : pollOptionButtons) {
            btn.setEnabled(false);
        }

        if (currentOptions != null) {
            int total = 0;
            int maxVotes = -1;
            for (int c : counts) {
                total += c;
                if (c > maxVotes) {
                    maxVotes = c;
                }
            }

            // UNHIDE AND DISPLAY FINAL RESULTS (For BOTH Quiz and Poll)
            for (int i = 0; i < visualBars.length; i++) {
                if (i < currentOptions.length) {
                    int c = (i < counts.length) ? counts[i] : 0;
                    boolean isMy = (i == votedOption);
                    boolean isLead = (total > 0 && c == maxVotes);
                    visualBars[i].updateData(currentOptions[i], c, total, isMy, isLead);
                    visualBars[i].setVisible(true);
                } else {
                    visualBars[i].setVisible(false);
                }
            }
            visualBarsPanel.revalidate();
            visualBarsPanel.repaint();

            resultsCard.setVisible(true);
            ActivityType type = isQuizMode ? ActivityType.QUIZ : ActivityType.POLL;
            activityHeader.update(isQuizMode ? FeatureHeader.Art.QUIZ : FeatureHeader.Art.POLL,
                    type.title + " complete", isQuizMode ? "Review the class’s answers below." : "Review the final vote totals below.");
            pollResultsArea.setText(type.results(true, currentOptions, counts, votedOption));
        }

        lastEventLabel.setText((isQuizMode ? "Quiz" : "Poll") + " closed - Final standings revealed");
    }

    private void submitAnswer(int optionIndex) {
        if (writer == null || !isConnected || currentOptions == null) {
            return;
        }
        if (optionIndex < 0 || optionIndex >= currentOptions.length) {
            return;
        }

        writer.println(Protocol.PREFIX_ANSWER + optionIndex);
        votedOption = optionIndex;

        // Visual feedback on buttons
        for (int i = 0; i < pollOptionButtons.length; i++) {
            if (i < currentOptions.length) {
                String prefix = "(" + (char) ('A' + i) + ") ";
                if (i == optionIndex) {
                    pollOptionButtons[i].setColors(UITheme.SUCCESS, UITheme.SUCCESS_HOVER, Color.WHITE);
                    pollOptionButtons[i].setText("✓ " + prefix + currentOptions[i]);
                } else {
                    pollOptionButtons[i].setColors(UITheme.BG_CARD_ALT, UITheme.PRIMARY_BG, UITheme.TEXT_PRIMARY);
                    pollOptionButtons[i].setText(prefix + currentOptions[i]);
                }
            }
        }

        if (isQuizMode) {
            activityHeader.update(FeatureHeader.Art.QUIZ, "Answer submitted", "You can change your answer until your instructor closes the quiz.");
            pollResultsArea.setText("✓ Your answer has been submitted!\nLive results are hidden during the quiz. Final results will appear when closed.");
        }
        lastEventLabel.setText((isQuizMode ? "Answer submitted: " : "Vote submitted: ") + currentOptions[optionIndex]);
    }

    private void resetPollUI() {
        activityHeader.update(FeatureHeader.Art.QUIZ, "Waiting for an activity", "Your instructor’s next poll or quiz will appear here.");
        activityBody.setVisible(false); resultsCard.setVisible(false);
        pollStatusBadge.setStatus("No active activity", UITheme.BG_APP, UITheme.TEXT_MUTED);
        pollTitleLabel.setText("Live Poll & Quiz");
        pollQuestionLabel.setText("Waiting for server to launch a live poll or quiz...");
        for (UITheme.FlatButton btn : pollOptionButtons) {
            btn.setText("Option");
            btn.setEnabled(false);
            btn.setVisible(true);
            btn.setColors(UITheme.BG_CARD_ALT, UITheme.PRIMARY_BG, UITheme.TEXT_PRIMARY);
        }
        for (UITheme.PollProgressBar bar : visualBars) {
            bar.setVisible(false);
        }
        pollResultsArea.setText("Standings will appear here when an activity is launched.");
        votedOption = -1;
        currentOptions = null;
        isQuizMode = false;
    }

    private synchronized void disconnect() {
        if (!isConnected && socket == null) {
            return;
        }

        isConnected = false;

        if (uptimeTimer != null && uptimeTimer.isRunning()) {
            uptimeTimer.stop();
        }

        try {
            if (writer != null && nameField != null) {
                writer.println(Protocol.PREFIX_DISCONNECT + "|" + nameField.getText().trim());
            }
        } catch (Exception ignored) {
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }

        socket = null;

        SwingUtilities.invokeLater(() -> {
            statusBadge.setStatus("● Disconnected", UITheme.DANGER_BG, UITheme.DANGER);
            statusBarLabel.setText("● Disconnected from server");
            uptimeLabel.setText("");
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            nameField.setEnabled(true);
            ipField.setEnabled(true);
            portField.setEnabled(true);
            refreshClassroom();
            replyField.setEnabled(false);
            sendReplyButton.setEnabled(false);
            lastEventLabel.setText("Disconnected");
            resetPollUI();
        });
    }

    // Retained for compatibility
    public void appendLog(String message) {
        historyArea.append(message + "\n");
        lastEventLabel.setText(message);
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            ClientGUI clientGUI = new ClientGUI();
            if (args.length > 0 && !args[0].isEmpty() && !args[0].startsWith("-")) {
                clientGUI.nameField.setText(args[0]);
            }
            clientGUI.setVisible(true);
        });
    }
}
