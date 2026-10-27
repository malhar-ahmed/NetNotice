package netnotice.server;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
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
 * ServerGUI: Modern, user-friendly Administration Interface for NetNotice.
 * Features high-contrast typography, crisp visual cards, live client roster,
 * announcement broadcaster, and interactive live poll manager.
 */
public class ServerGUI extends JFrame implements NetNoticeServer.ServerListener {

    private static final long serialVersionUID = 1L;

    // Header & Server Controls
    private UITheme.StatusBadge serverStatusBadge;
    private JTextField portField;
    private UITheme.FlatButton startStopButton;

    // Client Roster
    private DefaultListModel<String> clientListModel;
    private JList<String> clientList;
    private JLabel clientCountLabel;

    // Announcement Controls
    private JTextArea announcementArea;
    private UITheme.FlatButton sendAnnouncementButton;

    // Poll Controls (Member 3)
    private JTextField pollQuestionField;
    private JTextField[] pollOptionFields;
    private UITheme.FlatButton sendPollButton;
    private UITheme.FlatButton endPollButton;
    private JTextArea pollResultsArea;
    private UITheme.StatusBadge pollStatusBadge;

    // Activity Log
    private JTextArea logArea;

    @SuppressWarnings("this-escape")
    public ServerGUI() {
        super("NetNotice - Server Administration Hub");
        initUI();
        NetNoticeServer.setListener(this);
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(860, 880);
        setMinimumSize(new Dimension(720, 700));
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel(new BorderLayout(0, 10));
        contentPane.setBackground(UITheme.BG_APP);
        contentPane.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        // ==========================================
        // 1. TOP HEADER & SERVER CONTROLS CARD
        // ==========================================
        JPanel topCard = new JPanel(new BorderLayout(10, 10));
        topCard.setBackground(UITheme.BG_CARD);
        topCard.setBorder(UITheme.cardBorder());

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);

        JLabel appTitle = new JLabel("NetNotice Server Admin");
        appTitle.setFont(UITheme.FONT_TITLE);
        appTitle.setForeground(UITheme.TEXT_PRIMARY);

        serverStatusBadge = new UITheme.StatusBadge("● Server Stopped", UITheme.DANGER_BG, UITheme.DANGER);

        titleBar.add(appTitle, BorderLayout.WEST);
        titleBar.add(serverStatusBadge, BorderLayout.EAST);
        topCard.add(titleBar, BorderLayout.NORTH);

        JPanel serverControlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        serverControlRow.setOpaque(false);

        JLabel portLabel = new JLabel("Listening Port:");
        portLabel.setFont(UITheme.FONT_BOLD);
        portLabel.setForeground(UITheme.TEXT_SECONDARY);

        portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT), 6);
        portField.setFont(UITheme.FONT_REGULAR);
        portField.setForeground(UITheme.TEXT_PRIMARY);
        portField.setCaretColor(UITheme.TEXT_PRIMARY);
        portField.setBorder(UITheme.inputBorder());

        startStopButton = UITheme.successButton("Start Server");
        startStopButton.addActionListener(e -> toggleServer());

        JLabel tipLabel = new JLabel("Clients can connect using this computer's IP and port.");
        tipLabel.setFont(UITheme.FONT_SMALL);
        tipLabel.setForeground(UITheme.TEXT_MUTED);

        serverControlRow.add(portLabel);
        serverControlRow.add(portField);
        serverControlRow.add(startStopButton);
        serverControlRow.add(new JLabel("  "));
        serverControlRow.add(tipLabel);

        topCard.add(serverControlRow, BorderLayout.CENTER);
        contentPane.add(topCard, BorderLayout.NORTH);

        // ==========================================
        // 2. CENTER DASHBOARD: CLIENTS (Left) & ACTIONS (Right)
        // ==========================================
        JPanel dashboard = new JPanel(new BorderLayout(10, 0));
        dashboard.setOpaque(false);

        // --- Left Column: Connected Clients ---
        JPanel clientsCard = new JPanel(new BorderLayout(8, 8));
        clientsCard.setBackground(UITheme.BG_CARD);
        clientsCard.setBorder(UITheme.cardBorder());
        clientsCard.setPreferredSize(new Dimension(230, 400));

        JPanel clientHeader = new JPanel(new BorderLayout());
        clientHeader.setOpaque(false);

        JLabel clientTitle = new JLabel("👥 Active Clients");
        clientTitle.setFont(UITheme.FONT_HEADER);
        clientTitle.setForeground(UITheme.TEXT_PRIMARY);

        clientCountLabel = new JLabel("0 Online");
        clientCountLabel.setFont(UITheme.FONT_BOLD);
        clientCountLabel.setForeground(UITheme.PRIMARY);

        clientHeader.add(clientTitle, BorderLayout.WEST);
        clientHeader.add(clientCountLabel, BorderLayout.EAST);
        clientsCard.add(clientHeader, BorderLayout.NORTH);

        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        clientList.setFont(UITheme.FONT_REGULAR);
        clientList.setForeground(UITheme.TEXT_PRIMARY);
        clientList.setBackground(UITheme.BG_CARD_ALT);
        clientList.setFixedCellHeight(30);
        clientList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setText("  👤  " + value);
                lbl.setFont(UITheme.FONT_BOLD);
                if (isSelected) {
                    lbl.setBackground(UITheme.PRIMARY_BG);
                    lbl.setForeground(UITheme.PRIMARY);
                } else {
                    lbl.setBackground(index % 2 == 0 ? UITheme.BG_CARD : UITheme.BG_CARD_ALT);
                    lbl.setForeground(UITheme.TEXT_PRIMARY);
                }
                lbl.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                return lbl;
            }
        });

        JScrollPane clientScroll = new JScrollPane(clientList);
        clientScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT, 1));
        clientsCard.add(clientScroll, BorderLayout.CENTER);

        dashboard.add(clientsCard, BorderLayout.WEST);

        // --- Right Column: Announcements + Poll Manager ---
        JPanel rightColumn = new JPanel(new BorderLayout(0, 10));
        rightColumn.setOpaque(false);

        // Card 1: Announcement Broadcaster
        JPanel annCard = new JPanel(new BorderLayout(8, 8));
        annCard.setBackground(UITheme.BG_CARD);
        annCard.setBorder(UITheme.cardBorder());

        JLabel annTitle = new JLabel("📢 Broadcast Announcement");
        annTitle.setFont(UITheme.FONT_HEADER);
        annTitle.setForeground(UITheme.TEXT_PRIMARY);
        annCard.add(annTitle, BorderLayout.NORTH);

        announcementArea = new JTextArea(3, 25);
        announcementArea.setFont(UITheme.FONT_REGULAR);
        announcementArea.setForeground(UITheme.TEXT_PRIMARY);
        announcementArea.setCaretColor(UITheme.TEXT_PRIMARY);
        announcementArea.setLineWrap(true);
        announcementArea.setWrapStyleWord(true);
        announcementArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JScrollPane annScroll = new JScrollPane(announcementArea);
        annScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT, 1));
        annCard.add(annScroll, BorderLayout.CENTER);

        JPanel annBtnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        annBtnRow.setOpaque(false);
        sendAnnouncementButton = UITheme.primaryButton("📢 Broadcast Announcement to All Users");
        sendAnnouncementButton.setEnabled(false);
        sendAnnouncementButton.addActionListener(e -> sendAnnouncement());
        annBtnRow.add(sendAnnouncementButton);
        annCard.add(annBtnRow, BorderLayout.SOUTH);

        rightColumn.add(annCard, BorderLayout.NORTH);

        // Card 2: Live Poll & Quiz Manager (Member 3)
        JPanel pollCard = new JPanel(new BorderLayout(8, 8));
        pollCard.setBackground(UITheme.BG_CARD);
        pollCard.setBorder(UITheme.cardBorder());

        JPanel pollHeader = new JPanel(new BorderLayout());
        pollHeader.setOpaque(false);

        JLabel pollTitle = new JLabel("📊 Live Poll & Quiz Manager");
        pollTitle.setFont(UITheme.FONT_HEADER);
        pollTitle.setForeground(UITheme.TEXT_PRIMARY);

        pollStatusBadge = new UITheme.StatusBadge("No Active Poll", UITheme.BG_APP, UITheme.TEXT_MUTED);

        pollHeader.add(pollTitle, BorderLayout.WEST);
        pollHeader.add(pollStatusBadge, BorderLayout.EAST);
        pollCard.add(pollHeader, BorderLayout.NORTH);

        // Poll Form (GridBag)
        JPanel pollForm = new JPanel(new GridBagLayout());
        pollForm.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Question
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        JLabel qLbl = new JLabel("Question:");
        qLbl.setFont(UITheme.FONT_BOLD);
        qLbl.setForeground(UITheme.TEXT_SECONDARY);
        pollForm.add(qLbl, gbc);

        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.0;
        pollQuestionField = new JTextField(25);
        pollQuestionField.setFont(UITheme.FONT_REGULAR);
        pollQuestionField.setForeground(UITheme.TEXT_PRIMARY);
        pollQuestionField.setCaretColor(UITheme.TEXT_PRIMARY);
        pollQuestionField.setBorder(UITheme.inputBorder());
        pollForm.add(pollQuestionField, gbc);

        // Option Fields (A, B, C, D)
        pollOptionFields = new JTextField[4];
        gbc.gridwidth = 1;

        // Row 1: Option A and Option B
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        JLabel optALbl = new JLabel("Option A:");
        optALbl.setFont(UITheme.FONT_BOLD);
        optALbl.setForeground(UITheme.TEXT_SECONDARY);
        pollForm.add(optALbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.5;
        pollOptionFields[0] = new JTextField();
        pollOptionFields[0].setFont(UITheme.FONT_REGULAR);
        pollOptionFields[0].setForeground(UITheme.TEXT_PRIMARY);
        pollOptionFields[0].setCaretColor(UITheme.TEXT_PRIMARY);
        pollOptionFields[0].setBorder(UITheme.inputBorder());
        pollForm.add(pollOptionFields[0], gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        JLabel optBLbl = new JLabel("Option B:");
        optBLbl.setFont(UITheme.FONT_BOLD);
        optBLbl.setForeground(UITheme.TEXT_SECONDARY);
        pollForm.add(optBLbl, gbc);

        gbc.gridx = 3; gbc.weightx = 0.5;
        pollOptionFields[1] = new JTextField();
        pollOptionFields[1].setFont(UITheme.FONT_REGULAR);
        pollOptionFields[1].setForeground(UITheme.TEXT_PRIMARY);
        pollOptionFields[1].setCaretColor(UITheme.TEXT_PRIMARY);
        pollOptionFields[1].setBorder(UITheme.inputBorder());
        pollForm.add(pollOptionFields[1], gbc);

        // Row 2: Option C and Option D
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        JLabel optCLbl = new JLabel("Option C:");
        optCLbl.setFont(UITheme.FONT_BOLD);
        optCLbl.setForeground(UITheme.TEXT_SECONDARY);
        pollForm.add(optCLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.5;
        pollOptionFields[2] = new JTextField();
        pollOptionFields[2].setFont(UITheme.FONT_REGULAR);
        pollOptionFields[2].setForeground(UITheme.TEXT_PRIMARY);
        pollOptionFields[2].setCaretColor(UITheme.TEXT_PRIMARY);
        pollOptionFields[2].setBorder(UITheme.inputBorder());
        pollForm.add(pollOptionFields[2], gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        JLabel optDLbl = new JLabel("Option D:");
        optDLbl.setFont(UITheme.FONT_BOLD);
        optDLbl.setForeground(UITheme.TEXT_SECONDARY);
        pollForm.add(optDLbl, gbc);

        gbc.gridx = 3; gbc.weightx = 0.5;
        pollOptionFields[3] = new JTextField();
        pollOptionFields[3].setFont(UITheme.FONT_REGULAR);
        pollOptionFields[3].setForeground(UITheme.TEXT_PRIMARY);
        pollOptionFields[3].setCaretColor(UITheme.TEXT_PRIMARY);
        pollOptionFields[3].setBorder(UITheme.inputBorder());
        pollForm.add(pollOptionFields[3], gbc);

        // Row 3: Action Buttons
        JPanel pollActionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        pollActionRow.setOpaque(false);

        sendPollButton = UITheme.purpleButton("🚀 Launch Poll");
        sendPollButton.setEnabled(false);
        sendPollButton.addActionListener(e -> sendPoll());

        endPollButton = UITheme.dangerButton("⏹️ End Poll");
        endPollButton.setEnabled(false);
        endPollButton.addActionListener(e -> endPoll());

        pollActionRow.add(sendPollButton);
        pollActionRow.add(endPollButton);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4; gbc.weightx = 1.0;
        pollForm.add(pollActionRow, gbc);

        pollCard.add(pollForm, BorderLayout.NORTH);

        // Live Poll Results Area
        JPanel pollResultsPanel = new JPanel(new BorderLayout(4, 4));
        pollResultsPanel.setOpaque(false);

        JLabel resLbl = new JLabel("Live Poll Standings:");
        resLbl.setFont(UITheme.FONT_BOLD);
        resLbl.setForeground(UITheme.TEXT_SECONDARY);

        pollResultsArea = new JTextArea(4, 25);
        pollResultsArea.setFont(UITheme.FONT_MONO);
        pollResultsArea.setForeground(UITheme.TEXT_PRIMARY);
        pollResultsArea.setBackground(UITheme.BG_CARD_ALT);
        pollResultsArea.setEditable(false);
        pollResultsArea.setText("No active poll. Launch a poll to view live incoming votes.");
        pollResultsArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_LIGHT, 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        JScrollPane resultsScroll = new JScrollPane(pollResultsArea);
        resultsScroll.setBorder(BorderFactory.createEmptyBorder());

        pollResultsPanel.add(resLbl, BorderLayout.NORTH);
        pollResultsPanel.add(resultsScroll, BorderLayout.CENTER);

        pollCard.add(pollResultsPanel, BorderLayout.CENTER);
        rightColumn.add(pollCard, BorderLayout.CENTER);

        dashboard.add(rightColumn, BorderLayout.CENTER);
        contentPane.add(dashboard, BorderLayout.CENTER);

        // ==========================================
        // 3. BOTTOM: SERVER ACTIVITY LOG CARD
        // ==========================================
        JPanel logCard = new JPanel(new BorderLayout(6, 6));
        logCard.setBackground(UITheme.BG_CARD);
        logCard.setBorder(UITheme.cardBorder());
        logCard.setPreferredSize(new Dimension(700, 150));

        JPanel logHeader = new JPanel(new BorderLayout());
        logHeader.setOpaque(false);

        JLabel logTitle = new JLabel("📋 Server Event Log");
        logTitle.setFont(UITheme.FONT_BOLD);
        logTitle.setForeground(UITheme.TEXT_SECONDARY);

        JButton clearLogBtn = new JButton("Clear Log");
        clearLogBtn.setFont(UITheme.FONT_SMALL);
        clearLogBtn.setForeground(UITheme.TEXT_MUTED);
        clearLogBtn.setFocusPainted(false);
        clearLogBtn.setContentAreaFilled(false);
        clearLogBtn.setBorder(BorderFactory.createEmptyBorder());
        clearLogBtn.addActionListener(e -> logArea.setText(""));

        logHeader.add(logTitle, BorderLayout.WEST);
        logHeader.add(clearLogBtn, BorderLayout.EAST);
        logCard.add(logHeader, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setFont(UITheme.FONT_MONO);
        logArea.setForeground(UITheme.TEXT_PRIMARY);
        logArea.setBackground(UITheme.BG_CARD_ALT);
        logArea.setEditable(false);
        logArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT, 1));
        logCard.add(logScroll, BorderLayout.CENTER);

        contentPane.add(logCard, BorderLayout.SOUTH);

        setContentPane(contentPane);
    }

    /**
     * Start or stop the server.
     */
    void toggleServer() {
        if (!NetNoticeServer.isRunning()) {
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
                if (port < 1024 || port > 65535) {
                    JOptionPane.showMessageDialog(this,
                            "Port must be between 1024 and 65535.",
                            "Invalid Port", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a valid numeric port.",
                        "Invalid Port", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                NetNoticeServer.startServer(port);
                serverStatusBadge.setStatus("● Running on Port " + port, UITheme.SUCCESS_BG, UITheme.SUCCESS);
                startStopButton.setText("Stop Server");
                startStopButton.setColors(UITheme.DANGER, UITheme.DANGER_HOVER, Color.WHITE);
                portField.setEnabled(false);
                sendAnnouncementButton.setEnabled(true);
                sendPollButton.setEnabled(true);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Failed to start server: " + e.getMessage(),
                        "Server Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            NetNoticeServer.stopServer();
            serverStatusBadge.setStatus("● Server Stopped", UITheme.DANGER_BG, UITheme.DANGER);
            startStopButton.setText("Start Server");
            startStopButton.setColors(UITheme.SUCCESS, UITheme.SUCCESS_HOVER, Color.WHITE);
            portField.setEnabled(true);
            sendAnnouncementButton.setEnabled(false);
            clientListModel.clear();
            clientCountLabel.setText("0 Online");
            resetPollUI();
        }
    }

    private void sendAnnouncement() {
        String text = announcementArea.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter announcement text to broadcast.",
                    "Empty Announcement", JOptionPane.WARNING_MESSAGE);
            return;
        }

        NetNoticeServer.broadcastAnnouncement(text);
        announcementArea.setText("");
        announcementArea.requestFocus();
    }

    private void sendPoll() {
        String question = pollQuestionField.getText().trim();
        if (question.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a poll question.",
                    "Empty Question", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> options = new ArrayList<>();
        for (JTextField field : pollOptionFields) {
            String opt = field.getText().trim();
            if (!opt.isEmpty()) {
                options.add(opt);
            }
        }

        if (options.size() < 2) {
            JOptionPane.showMessageDialog(this,
                    "Please enter at least 2 options for the poll.",
                    "Insufficient Options", JOptionPane.WARNING_MESSAGE);
            return;
        }

        NetNoticeServer.startPoll(question, options);

        sendPollButton.setEnabled(false);
        endPollButton.setEnabled(true);
        pollQuestionField.setEnabled(false);
        for (JTextField f : pollOptionFields) {
            f.setEnabled(false);
        }
    }

    private void endPoll() {
        NetNoticeServer.endPoll();

        sendPollButton.setEnabled(true);
        endPollButton.setEnabled(false);
        pollQuestionField.setEnabled(true);
        pollQuestionField.setText("");
        for (JTextField f : pollOptionFields) {
            f.setEnabled(true);
            f.setText("");
        }
    }

    private void resetPollUI() {
        sendPollButton.setEnabled(false);
        endPollButton.setEnabled(false);
        pollQuestionField.setEnabled(true);
        pollQuestionField.setText("");
        for (JTextField f : pollOptionFields) {
            f.setEnabled(true);
            f.setText("");
        }
        pollResultsArea.setText("No active poll.");
        pollStatusBadge.setStatus("No Active Poll", UITheme.BG_APP, UITheme.TEXT_MUTED);
    }

    @Override
    public void onClientListUpdated(List<String> clients) {
        SwingUtilities.invokeLater(() -> {
            clientListModel.clear();
            for (String client : clients) {
                clientListModel.addElement(client);
            }
            clientCountLabel.setText(clients.size() + " Online");
        });
    }

    @Override
    public void onLogMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @Override
    public void onPollStateChanged(String question, List<String> options,
                                    int[] voteCounts, int totalVotes, boolean isActive) {
        SwingUtilities.invokeLater(() -> {
            if (isActive && question != null) {
                pollStatusBadge.setStatus("● Poll Active", UITheme.SUCCESS_BG, UITheme.SUCCESS);

                StringBuilder sb = new StringBuilder();
                sb.append("Poll Question: ").append(question).append("\n");
                sb.append("Total Responses: ").append(totalVotes).append(" votes\n\n");
                for (int i = 0; i < options.size(); i++) {
                    int count = (i < voteCounts.length) ? voteCounts[i] : 0;
                    double pct = totalVotes > 0 ? (count * 100.0 / totalVotes) : 0;
                    String prefix = "(" + (char)('A' + i) + ") ";

                    int barLen = (int) Math.round(pct / 5.0);
                    StringBuilder bar = new StringBuilder("[");
                    for (int b = 0; b < 20; b++) {
                        bar.append(b < barLen ? "■" : " ");
                    }
                    bar.append("]");

                    sb.append(String.format("  %-16s %s %3.0f%% (%d votes)\n",
                            prefix + options.get(i), bar.toString(), pct, count));
                }
                pollResultsArea.setText(sb.toString());

            } else if (question != null) {
                pollStatusBadge.setStatus("● Poll Closed", UITheme.DANGER_BG, UITheme.DANGER);

                StringBuilder sb = new StringBuilder();
                sb.append("FINAL POLL STANDINGS: ").append(question).append("\n");
                sb.append("Total Recorded Votes: ").append(totalVotes).append("\n\n");
                for (int i = 0; i < options.size(); i++) {
                    int count = (i < voteCounts.length) ? voteCounts[i] : 0;
                    double pct = totalVotes > 0 ? (count * 100.0 / totalVotes) : 0;
                    String prefix = "(" + (char)('A' + i) + ") ";

                    int barLen = (int) Math.round(pct / 5.0);
                    StringBuilder bar = new StringBuilder("[");
                    for (int b = 0; b < 20; b++) {
                        bar.append(b < barLen ? "■" : " ");
                    }
                    bar.append("]");

                    sb.append(String.format("  %-16s %s %3.0f%% (%d votes)\n",
                            prefix + options.get(i), bar.toString(), pct, count));
                }
                pollResultsArea.setText(sb.toString());

            } else {
                pollStatusBadge.setStatus("No Active Poll", UITheme.BG_APP, UITheme.TEXT_MUTED);
                pollResultsArea.setText("No active poll.");
            }
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            ServerGUI gui = new ServerGUI();
            gui.setVisible(true);
            gui.toggleServer();
        });
    }
}
