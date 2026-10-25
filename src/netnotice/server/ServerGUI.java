package netnotice.server;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.BorderFactory;
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

/**
 * ServerGUI: Java Swing Administration Interface for NetNotice.
 * Displays real-time server status, connected client roster,
 * announcement broadcast controls, and server activity log.
 */
public class ServerGUI extends JFrame implements NetNoticeServer.ServerListener {

    private static final long serialVersionUID = 1L;

    private JLabel statusLabel;
    private JTextField portField;
    private JButton startStopButton;
    private DefaultListModel<String> clientListModel;
    private JList<String> clientList;
    private JLabel clientCountLabel;
    private JTextArea announcementArea;
    private JButton sendAnnouncementButton;
    private JTextArea logArea;

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    @SuppressWarnings("this-escape")
    public ServerGUI() {
        super("NetNotice - Server Admin");
        initUI();
        NetNoticeServer.setListener(this);
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 600);
        setMinimumSize(new Dimension(550, 450));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ==========================================
        // 1. TOP HEADER: Title & Server Status
        // ==========================================
        JPanel headerPanel = new JPanel(new BorderLayout(8, 8));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        headerPanel.setBackground(new Color(245, 247, 250));

        JLabel titleLabel = new JLabel("NETNOTICE ADMIN", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setForeground(new Color(30, 41, 59));

        JPanel controlBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        controlBar.setOpaque(false);

        JLabel portLabel = new JLabel("Port:");
        portLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT), 5);
        portField.setFont(new Font("SansSerif", Font.PLAIN, 13));

        startStopButton = new JButton("Start Server");
        startStopButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        startStopButton.setBackground(new Color(34, 197, 94));
        startStopButton.setForeground(Color.WHITE);
        startStopButton.setFocusPainted(false);
        startStopButton.addActionListener(e -> toggleServer());

        statusLabel = new JLabel("Status: Stopped");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        statusLabel.setForeground(new Color(220, 38, 38));

        controlBar.add(portLabel);
        controlBar.add(portField);
        controlBar.add(startStopButton);
        controlBar.add(new JLabel(" | "));
        controlBar.add(statusLabel);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(controlBar, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ==========================================
        // 2. CENTER PANEL: Clients (Left) & Announcements (Right)
        // ==========================================
        // Left: Connected Clients List
        JPanel clientsPanel = new JPanel(new BorderLayout(6, 6));
        clientsPanel.setBorder(BorderFactory.createTitledBorder("Connected Clients"));

        clientCountLabel = new JLabel("Active Users: 0");
        clientCountLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        clientsPanel.add(clientCountLabel, BorderLayout.NORTH);

        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        clientList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        clientList.setFixedCellHeight(26);
        JScrollPane clientScrollPane = new JScrollPane(clientList);
        clientsPanel.add(clientScrollPane, BorderLayout.CENTER);
        clientsPanel.setPreferredSize(new Dimension(200, 200));

        // Right: Announcement Composer
        JPanel announcementPanel = new JPanel(new BorderLayout(8, 8));
        announcementPanel.setBorder(BorderFactory.createTitledBorder("Announcement"));

        JLabel promptLabel = new JLabel("Type announcement to broadcast to all clients:");
        promptLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        announcementArea = new JTextArea(4, 25);
        announcementArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        announcementArea.setLineWrap(true);
        announcementArea.setWrapStyleWord(true);
        JScrollPane announcementScrollPane = new JScrollPane(announcementArea);

        sendAnnouncementButton = new JButton("SEND ANNOUNCEMENT");
        sendAnnouncementButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        sendAnnouncementButton.setBackground(new Color(37, 99, 235));
        sendAnnouncementButton.setForeground(Color.WHITE);
        sendAnnouncementButton.setFocusPainted(false);
        sendAnnouncementButton.setEnabled(false);
        sendAnnouncementButton.addActionListener(e -> sendAnnouncement());

        announcementPanel.add(promptLabel, BorderLayout.NORTH);
        announcementPanel.add(announcementScrollPane, BorderLayout.CENTER);
        announcementPanel.add(sendAnnouncementButton, BorderLayout.SOUTH);

        // Split pane for responsive layout
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, clientsPanel, announcementPanel);
        splitPane.setResizeWeight(0.35);
        splitPane.setContinuousLayout(true);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // ==========================================
        // 3. BOTTOM PANEL: Server Activity Log
        // ==========================================
        JPanel logPanel = new JPanel(new BorderLayout(4, 4));
        logPanel.setBorder(BorderFactory.createTitledBorder("Server Event Log"));
        logPanel.setPreferredSize(new Dimension(600, 150));

        logArea = new JTextArea();
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setEditable(false);
        logArea.setBackground(new Color(250, 250, 250));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logPanel.add(logScrollPane, BorderLayout.CENTER);

        mainPanel.add(logPanel, BorderLayout.SOUTH);

        // ==========================================
        // MEMBER 3 HOOK: Live Quiz / Poll Results Panel
        // (Member 3 will attach Poll controls & real-time counts here)
        // ==========================================

        setContentPane(mainPanel);
    }

    /**
     * Start or stop the server.
     */
    private void toggleServer() {
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
                statusLabel.setText("Status: Running on Port " + port);
                statusLabel.setForeground(new Color(22, 163, 74));
                startStopButton.setText("Stop Server");
                startStopButton.setBackground(new Color(220, 38, 38));
                portField.setEnabled(false);
                sendAnnouncementButton.setEnabled(true);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Failed to start server: " + e.getMessage(),
                        "Server Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            NetNoticeServer.stopServer();
            statusLabel.setText("Status: Stopped");
            statusLabel.setForeground(new Color(220, 38, 38));
            startStopButton.setText("Start Server");
            startStopButton.setBackground(new Color(34, 197, 94));
            portField.setEnabled(true);
            sendAnnouncementButton.setEnabled(false);
            clientListModel.clear();
            clientCountLabel.setText("Active Users: 0");
        }
    }

    /**
     * Broadcast the typed announcement to all clients.
     */
    private void sendAnnouncement() {
        String text = announcementArea.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter an announcement before sending.",
                    "Empty Announcement", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Send through NetNoticeServer
        NetNoticeServer.broadcastAnnouncement(text);
        announcementArea.setText("");
        announcementArea.requestFocus();
    }

    /**
     * Callback from NetNoticeServer when a client joins or leaves.
     * Uses SwingUtilities.invokeLater() because this event comes from
     * a background network thread.
     */
    @Override
    public void onClientListUpdated(List<String> clients) {
        SwingUtilities.invokeLater(() -> {
            clientListModel.clear();
            for (String client : clients) {
                clientListModel.addElement(client);
            }
            clientCountLabel.setText("Active Users: " + clients.size());
        });
    }

    /**
     * Callback from NetNoticeServer when a log message is produced.
     * Uses SwingUtilities.invokeLater() to safely append to the JTextArea.
     */
    @Override
    public void onLogMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = TIME_FORMAT.format(new Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        // Set native look and feel for a clean appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            ServerGUI gui = new ServerGUI();
            gui.setVisible(true);
            // Automatically start server on launch for convenience
            gui.toggleServer();
        });
    }
}
