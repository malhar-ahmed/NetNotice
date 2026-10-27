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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import netnotice.common.Protocol;
import netnotice.common.UITheme;

/**
 * ClientGUI: Modern, user-friendly Swing interface for NetNotice clients.
 * Provides crystal-clear typography, high-contrast visibility, and an intuitive
 * card-based layout for announcements, polls, and connection management.
 */
public class ClientGUI extends JFrame {

    private static final long serialVersionUID = 1L;

    // Connection Form Components
    JTextField nameField;
    private JTextField ipField;
    private JTextField portField;
    private UITheme.FlatButton connectButton;
    private UITheme.FlatButton disconnectButton;
    private UITheme.StatusBadge statusBadge;

    // Announcement Components
    private JTextArea announcementDisplay;
    private JLabel announcementTimeLabel;

    // Poll Components (Member 3)
    private UITheme.StatusBadge pollStatusBadge;
    private JLabel pollQuestionLabel;
    private JPanel optionsPanel;
    private UITheme.FlatButton[] pollOptionButtons;
    private JTextArea pollResultsArea;
    private String[] currentOptions;
    private int votedOption = -1;

    // History / Activity Log
    private JTextArea historyArea;

    // Networking
    private transient Socket socket;
    private transient BufferedReader reader;
    private transient PrintWriter writer;
    private transient Thread listenerThread;
    private volatile boolean isConnected = false;

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    @SuppressWarnings("this-escape")
    public ClientGUI() {
        super("NetNotice - Client Portal");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(680, 840);
        setMinimumSize(new Dimension(560, 680));
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel(new BorderLayout(0, 10));
        contentPane.setBackground(UITheme.BG_APP);
        contentPane.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        // ==========================================
        // 1. TOP HEADER & CONNECTION CARD
        // ==========================================
        JPanel topCard = new JPanel(new BorderLayout(8, 10));
        topCard.setBackground(UITheme.BG_CARD);
        topCard.setBorder(UITheme.cardBorder());

        // Header Title Bar
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);

        JLabel appTitle = new JLabel("NetNotice Client Portal");
        appTitle.setFont(UITheme.FONT_TITLE);
        appTitle.setForeground(UITheme.TEXT_PRIMARY);

        statusBadge = new UITheme.StatusBadge("● Disconnected", UITheme.DANGER_BG, UITheme.DANGER);

        titleBar.add(appTitle, BorderLayout.WEST);
        titleBar.add(statusBadge, BorderLayout.EAST);
        topCard.add(titleBar, BorderLayout.NORTH);

        // Connection Form Row
        JPanel formRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        formRow.setOpaque(false);

        JLabel nameLbl = new JLabel("Name:");
        nameLbl.setFont(UITheme.FONT_BOLD);
        nameLbl.setForeground(UITheme.TEXT_SECONDARY);
        nameField = new JTextField("Student", 9);
        nameField.setFont(UITheme.FONT_REGULAR);
        nameField.setForeground(UITheme.TEXT_PRIMARY);
        nameField.setCaretColor(UITheme.TEXT_PRIMARY);
        nameField.setBorder(UITheme.inputBorder());

        JLabel ipLbl = new JLabel("Server IP:");
        ipLbl.setFont(UITheme.FONT_BOLD);
        ipLbl.setForeground(UITheme.TEXT_SECONDARY);
        ipField = new JTextField(Protocol.DEFAULT_HOST, 9);
        ipField.setFont(UITheme.FONT_REGULAR);
        ipField.setForeground(UITheme.TEXT_PRIMARY);
        ipField.setCaretColor(UITheme.TEXT_PRIMARY);
        ipField.setBorder(UITheme.inputBorder());

        JLabel portLbl = new JLabel("Port:");
        portLbl.setFont(UITheme.FONT_BOLD);
        portLbl.setForeground(UITheme.TEXT_SECONDARY);
        portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT), 5);
        portField.setFont(UITheme.FONT_REGULAR);
        portField.setForeground(UITheme.TEXT_PRIMARY);
        portField.setCaretColor(UITheme.TEXT_PRIMARY);
        portField.setBorder(UITheme.inputBorder());

        connectButton = UITheme.successButton("Connect");
        connectButton.addActionListener(e -> startConnect());

        disconnectButton = UITheme.dangerButton("Disconnect");
        disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(e -> disconnect());

        formRow.add(nameLbl);
        formRow.add(nameField);
        formRow.add(ipLbl);
        formRow.add(ipField);
        formRow.add(portLbl);
        formRow.add(portField);
        formRow.add(connectButton);
        formRow.add(disconnectButton);

        topCard.add(formRow, BorderLayout.CENTER);
        contentPane.add(topCard, BorderLayout.NORTH);

        // ==========================================
        // 2. CENTER: ANNOUNCEMENTS & POLLS
        // ==========================================
        JPanel centerContainer = new JPanel(new BorderLayout(0, 10));
        centerContainer.setOpaque(false);

        // --- Card 1: Announcements ---
        JPanel annCard = new JPanel(new BorderLayout(6, 8));
        annCard.setBackground(UITheme.BG_CARD);
        annCard.setBorder(UITheme.cardBorder());

        JPanel annHeader = new JPanel(new BorderLayout());
        annHeader.setOpaque(false);
        JLabel annTitle = new JLabel("📢 Latest Announcement");
        annTitle.setFont(UITheme.FONT_HEADER);
        annTitle.setForeground(UITheme.TEXT_PRIMARY);

        announcementTimeLabel = new JLabel("Listening for broadcasts...");
        announcementTimeLabel.setFont(UITheme.FONT_SMALL);
        announcementTimeLabel.setForeground(UITheme.TEXT_MUTED);

        annHeader.add(annTitle, BorderLayout.WEST);
        annHeader.add(announcementTimeLabel, BorderLayout.EAST);
        annCard.add(annHeader, BorderLayout.NORTH);

        announcementDisplay = new JTextArea(3, 25);
        announcementDisplay.setText("No announcements received yet. Server notices will appear here.");
        announcementDisplay.setFont(new Font("Segoe UI", Font.BOLD, 14));
        announcementDisplay.setForeground(new Color(30, 58, 138)); // Deep legible navy
        announcementDisplay.setBackground(new Color(239, 246, 255)); // Soft blue card
        announcementDisplay.setLineWrap(true);
        announcementDisplay.setWrapStyleWord(true);
        announcementDisplay.setEditable(false);
        announcementDisplay.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(191, 219, 254), 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JScrollPane annScroll = new JScrollPane(announcementDisplay);
        annScroll.setBorder(BorderFactory.createEmptyBorder());
        annCard.add(annScroll, BorderLayout.CENTER);

        // --- Card 2: Live Poll / Quiz (Member 3) ---
        JPanel pollCard = new JPanel(new BorderLayout(8, 8));
        pollCard.setBackground(UITheme.BG_CARD);
        pollCard.setBorder(UITheme.cardBorder());

        JPanel pollHeader = new JPanel(new BorderLayout());
        pollHeader.setOpaque(false);

        JLabel pollTitle = new JLabel("📊 Live Poll & Quiz");
        pollTitle.setFont(UITheme.FONT_HEADER);
        pollTitle.setForeground(UITheme.TEXT_PRIMARY);

        pollStatusBadge = new UITheme.StatusBadge("No Active Poll", UITheme.BG_APP, UITheme.TEXT_MUTED);

        pollHeader.add(pollTitle, BorderLayout.WEST);
        pollHeader.add(pollStatusBadge, BorderLayout.EAST);
        pollCard.add(pollHeader, BorderLayout.NORTH);

        // Poll Body
        JPanel pollBody = new JPanel(new BorderLayout(6, 8));
        pollBody.setOpaque(false);

        pollQuestionLabel = new JLabel("Waiting for server to launch a live poll...");
        pollQuestionLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        pollQuestionLabel.setForeground(UITheme.TEXT_SECONDARY);
        pollQuestionLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));
        pollBody.add(pollQuestionLabel, BorderLayout.NORTH);

        // Options Panel: 2x2 grid for clean, spacious buttons
        optionsPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        optionsPanel.setOpaque(false);
        optionsPanel.setPreferredSize(new Dimension(500, 90));

        pollOptionButtons = new UITheme.FlatButton[4];
        for (int i = 0; i < 4; i++) {
            final int optionIndex = i;
            pollOptionButtons[i] = new UITheme.FlatButton("Option " + (i + 1),
                UITheme.BG_CARD_ALT, UITheme.PRIMARY_BG, UITheme.TEXT_PRIMARY);
            pollOptionButtons[i].setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));
            pollOptionButtons[i].setEnabled(false);
            pollOptionButtons[i].addActionListener(e -> submitAnswer(optionIndex));
            optionsPanel.add(pollOptionButtons[i]);
        }
        pollBody.add(optionsPanel, BorderLayout.CENTER);

        // Live Poll Results Area
        JPanel resultsPanel = new JPanel(new BorderLayout(4, 4));
        resultsPanel.setOpaque(false);

        JLabel resultsTitle = new JLabel("Live Poll Results:");
        resultsTitle.setFont(UITheme.FONT_BOLD);
        resultsTitle.setForeground(UITheme.TEXT_SECONDARY);

        pollResultsArea = new JTextArea(4, 25);
        pollResultsArea.setFont(UITheme.FONT_MONO);
        pollResultsArea.setForeground(UITheme.TEXT_PRIMARY);
        pollResultsArea.setBackground(UITheme.BG_CARD_ALT);
        pollResultsArea.setEditable(false);
        pollResultsArea.setText("Poll results will display here in real time.");
        pollResultsArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_LIGHT, 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JScrollPane resultsScroll = new JScrollPane(pollResultsArea);
        resultsScroll.setBorder(BorderFactory.createEmptyBorder());

        resultsPanel.add(resultsTitle, BorderLayout.NORTH);
        resultsPanel.add(resultsScroll, BorderLayout.CENTER);

        pollBody.add(resultsPanel, BorderLayout.SOUTH);
        pollCard.add(pollBody, BorderLayout.CENTER);

        // Vertical Split between Announcements and Poll Card
        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, annCard, pollCard);
        centerSplit.setResizeWeight(0.32);
        centerSplit.setDividerSize(4);
        centerSplit.setOpaque(false);
        centerSplit.setBorder(BorderFactory.createEmptyBorder());

        centerContainer.add(centerSplit, BorderLayout.CENTER);
        contentPane.add(centerContainer, BorderLayout.CENTER);

        // ==========================================
        // 3. BOTTOM: EVENT & MESSAGE LOG CARD
        // ==========================================
        JPanel logCard = new JPanel(new BorderLayout(6, 6));
        logCard.setBackground(UITheme.BG_CARD);
        logCard.setBorder(UITheme.cardBorder());
        logCard.setPreferredSize(new Dimension(500, 150));

        JPanel logHeader = new JPanel(new BorderLayout());
        logHeader.setOpaque(false);

        JLabel logTitle = new JLabel("📋 Activity & Connection Log");
        logTitle.setFont(UITheme.FONT_BOLD);
        logTitle.setForeground(UITheme.TEXT_SECONDARY);

        JButton clearLogBtn = new JButton("Clear Log");
        clearLogBtn.setFont(UITheme.FONT_SMALL);
        clearLogBtn.setForeground(UITheme.TEXT_MUTED);
        clearLogBtn.setFocusPainted(false);
        clearLogBtn.setContentAreaFilled(false);
        clearLogBtn.setBorder(BorderFactory.createEmptyBorder());
        clearLogBtn.addActionListener(e -> historyArea.setText(""));

        logHeader.add(logTitle, BorderLayout.WEST);
        logHeader.add(clearLogBtn, BorderLayout.EAST);
        logCard.add(logHeader, BorderLayout.NORTH);

        historyArea = new JTextArea();
        historyArea.setFont(UITheme.FONT_MONO);
        historyArea.setForeground(UITheme.TEXT_PRIMARY);
        historyArea.setBackground(UITheme.BG_CARD_ALT);
        historyArea.setEditable(false);
        historyArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JScrollPane logScroll = new JScrollPane(historyArea);
        logScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT, 1));
        logCard.add(logScroll, BorderLayout.CENTER);

        contentPane.add(logCard, BorderLayout.SOUTH);

        setContentPane(contentPane);

        // Clean disconnect on window close
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                disconnect();
            }
        });
    }

    /**
     * Initiates connection in a background thread to prevent freezing the GUI.
     */
    private void startConnect() {
        String name = nameField.getText().trim();
        String ip = ipField.getText().trim();
        String portStr = portField.getText().trim();

        if (name.isEmpty() || name.length() > 20 || name.contains("|")) {
            JOptionPane.showMessageDialog(this,
                    "Name must be 1-20 characters without the '|' symbol.",
                    "Invalid Name", JOptionPane.WARNING_MESSAGE);
            return;
        }

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

        new Thread(() -> connect(name, ip, port), "NetNotice-ClientConnectThread").start();
    }

    private void connect(String name, String ip, int port) {
        try {
            socket = new Socket(ip, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            // Read welcome greeting
            String greeting = reader.readLine();
            if (greeting != null) {
                appendLog("[SERVER] " + greeting);
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

            isConnected = true;

            SwingUtilities.invokeLater(() -> {
                statusBadge.setStatus("● Connected as " + name, UITheme.SUCCESS_BG, UITheme.SUCCESS);
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                nameField.setEnabled(false);
                ipField.setEnabled(false);
                portField.setEnabled(false);
                appendLog("Joined successfully as: " + name);
            });

            listenerThread = new Thread(this::listenForServerMessages, "NetNotice-ClientListenerThread");
            listenerThread.setDaemon(true);
            listenerThread.start();

        } catch (Exception e) {
            disconnect();
            SwingUtilities.invokeLater(() -> {
                statusBadge.setStatus("● Connection Failed", UITheme.DANGER_BG, UITheme.DANGER);
                connectButton.setEnabled(true);
                JOptionPane.showMessageDialog(this,
                        "Could not connect to " + ip + ":" + port + "\nReason: " + e.getMessage(),
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    /**
     * Background message listener loop.
     */
    private void listenForServerMessages() {
        try {
            String line;
            while (isConnected && (line = reader.readLine()) != null) {
                final String msg = line;

                // 1. Announcement broadcast received
                if (msg.startsWith(Protocol.PREFIX_ANNOUNCEMENT)) {
                    String announcementText = msg.substring(Protocol.PREFIX_ANNOUNCEMENT.length());
                    SwingUtilities.invokeLater(() -> onAnnouncementReceived(announcementText));
                }

                // 2. Poll question received (Member 3)
                else if (msg.startsWith(Protocol.PREFIX_POLL)) {
                    String payload = msg.substring(Protocol.PREFIX_POLL.length());
                    String[] parts = payload.split("\\|");
                    if (parts.length >= 3) {
                        String question = parts[0];
                        String[] options = new String[parts.length - 1];
                        for (int i = 1; i < parts.length; i++) {
                            options[i - 1] = parts[i];
                        }
                        SwingUtilities.invokeLater(() -> onPollReceived(question, options));
                    }
                }

                // 3. Live poll results (Member 3)
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

                // 4. Poll ended (Member 3)
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
                    String text = msg.substring(Protocol.PREFIX_MESSAGE.length());
                    SwingUtilities.invokeLater(() -> appendLog("[SERVER] " + text));
                }

                // 6. Error received
                else if (msg.startsWith(Protocol.PREFIX_ERROR)) {
                    String err = msg.substring(Protocol.PREFIX_ERROR.length());
                    SwingUtilities.invokeLater(() -> appendLog("[ERROR] " + err));
                }
            }
        } catch (IOException e) {
            if (isConnected) {
                SwingUtilities.invokeLater(() -> appendLog("Connection lost: " + e.getMessage()));
            }
        } finally {
            disconnect();
        }
    }

    private void onAnnouncementReceived(String text) {
        String timestamp = TIME_FORMAT.format(new Date());
        announcementDisplay.setText(text);
        announcementTimeLabel.setText("Received at: " + timestamp);
        appendLog("NEW ANNOUNCEMENT: " + text);
    }

    // ==========================================
    // Poll Methods (Member 3)
    // ==========================================

    private void onPollReceived(String question, String[] options) {
        currentOptions = options;
        votedOption = -1;

        pollStatusBadge.setStatus("● Poll Active", UITheme.SUCCESS_BG, UITheme.SUCCESS);
        pollQuestionLabel.setText("Q: " + question);

        // Update option buttons
        for (int i = 0; i < pollOptionButtons.length; i++) {
            if (i < options.length) {
                String prefix = "(" + (char)('A' + i) + ") ";
                pollOptionButtons[i].setText(prefix + options[i]);
                pollOptionButtons[i].setEnabled(true);
                pollOptionButtons[i].setVisible(true);
                pollOptionButtons[i].setColors(UITheme.BG_CARD_ALT, UITheme.PRIMARY_BG, UITheme.TEXT_PRIMARY);
                pollOptionButtons[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.PRIMARY, 1),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)
                ));
            } else {
                pollOptionButtons[i].setVisible(false);
            }
        }
        optionsPanel.revalidate();
        optionsPanel.repaint();

        pollResultsArea.setText("Waiting for responses...");
        appendLog("NEW POLL: " + question);
    }

    private void onPollResultsReceived(int[] counts) {
        if (currentOptions == null) {
            return;
        }

        int total = 0;
        for (int c : counts) {
            total += c;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Live Results (Total: ").append(total).append(" votes):\n\n");
        for (int i = 0; i < counts.length && i < currentOptions.length; i++) {
            double pct = total > 0 ? (counts[i] * 100.0 / total) : 0;
            String prefix = "(" + (char)('A' + i) + ") ";

            // Clean visual bar (20 blocks wide)
            int barLen = (int) Math.round(pct / 5.0);
            StringBuilder bar = new StringBuilder("[");
            for (int b = 0; b < 20; b++) {
                bar.append(b < barLen ? "■" : " ");
            }
            bar.append("]");

            sb.append(String.format("  %-16s %s %3.0f%% (%d votes)\n",
                    prefix + currentOptions[i], bar.toString(), pct, counts[i]));
        }
        pollResultsArea.setText(sb.toString());
    }

    private void onPollEnded(int[] counts) {
        pollStatusBadge.setStatus("● Poll Ended", UITheme.DANGER_BG, UITheme.DANGER);

        for (UITheme.FlatButton btn : pollOptionButtons) {
            btn.setEnabled(false);
        }

        if (currentOptions != null) {
            int total = 0;
            for (int c : counts) {
                total += c;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Final Results (Total: ").append(total).append(" votes):\n\n");
            for (int i = 0; i < counts.length && i < currentOptions.length; i++) {
                double pct = total > 0 ? (counts[i] * 100.0 / total) : 0;
                String prefix = "(" + (char)('A' + i) + ") ";
                int barLen = (int) Math.round(pct / 5.0);
                StringBuilder bar = new StringBuilder("[");
                for (int b = 0; b < 20; b++) {
                    bar.append(b < barLen ? "■" : " ");
                }
                bar.append("]");

                sb.append(String.format("  %-16s %s %3.0f%% (%d votes)\n",
                        prefix + currentOptions[i], bar.toString(), pct, counts[i]));
            }
            pollResultsArea.setText(sb.toString());
        }

        appendLog("POLL CLOSED - Final results locked.");
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

        // Visual feedback: Highlight selected button in green with checkmark
        for (int i = 0; i < pollOptionButtons.length; i++) {
            if (i < currentOptions.length) {
                String prefix = "(" + (char)('A' + i) + ") ";
                if (i == optionIndex) {
                    pollOptionButtons[i].setColors(UITheme.SUCCESS, UITheme.SUCCESS_HOVER, Color.WHITE);
                    pollOptionButtons[i].setText("✓ " + prefix + currentOptions[i]);
                } else {
                    pollOptionButtons[i].setColors(UITheme.BG_CARD_ALT, UITheme.PRIMARY_BG, UITheme.TEXT_PRIMARY);
                    pollOptionButtons[i].setText(prefix + currentOptions[i]);
                }
            }
        }

        appendLog("Your Vote: " + currentOptions[optionIndex]);
    }

    private void resetPollUI() {
        pollStatusBadge.setStatus("No Active Poll", UITheme.BG_APP, UITheme.TEXT_MUTED);
        pollQuestionLabel.setText("Waiting for server to launch a live poll...");
        for (UITheme.FlatButton btn : pollOptionButtons) {
            btn.setText("Option");
            btn.setEnabled(false);
            btn.setVisible(true);
            btn.setColors(UITheme.BG_CARD_ALT, UITheme.PRIMARY_BG, UITheme.TEXT_PRIMARY);
        }
        pollResultsArea.setText("Poll results will display here in real time.");
        votedOption = -1;
        currentOptions = null;
    }

    private synchronized void disconnect() {
        if (!isConnected && socket == null) {
            return;
        }

        isConnected = false;

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
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            nameField.setEnabled(true);
            ipField.setEnabled(true);
            portField.setEnabled(true);
            appendLog("Disconnected from server.");
            resetPollUI();
        });
    }

    private void appendLog(String message) {
        String timestamp = TIME_FORMAT.format(new Date());
        historyArea.append("[" + timestamp + "] " + message + "\n");
        historyArea.setCaretPosition(historyArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
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
