package netnotice.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import netnotice.common.Protocol;

/**
 * ClientGUI: Java Swing Client Interface for NetNotice.
 * Connects to NetNoticeServer, receives real-time announcements,
 * and maintains GUI responsiveness through background socket threads.
 */
public class ClientGUI extends JFrame {

    private JTextField nameField;
    private JTextField ipField;
    private JTextField portField;
    private JButton connectButton;
    private JButton disconnectButton;
    private JLabel statusLabel;

    private JTextArea announcementDisplay;
    private JLabel announcementTimeLabel;
    private JTextArea historyArea;

    // Networking
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Thread listenerThread;
    private volatile boolean isConnected = false;

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public ClientGUI() {
        super("NetNotice - Client");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(580, 620);
        setMinimumSize(new Dimension(480, 500));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ==========================================
        // 1. TOP HEADER: Title & Connection Form
        // ==========================================
        JPanel topPanel = new JPanel(new BorderLayout(8, 8));

        JLabel titleLabel = new JLabel("NETNOTICE", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setForeground(new Color(30, 41, 59));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        // Connection Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        formPanel.setBackground(new Color(248, 250, 252));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Name:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        nameField = new JTextField("Ahmed", 12);
        nameField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        formPanel.add(nameField, gbc);

        // Row 1: Server IP
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Server IP:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        ipField = new JTextField(Protocol.DEFAULT_HOST, 12);
        ipField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        formPanel.add(ipField, gbc);

        // Row 2: Port
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Port:"), gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT), 12);
        portField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        formPanel.add(portField, gbc);

        // Row 3: Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonPanel.setOpaque(false);

        connectButton = new JButton("Connect");
        connectButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        connectButton.setBackground(new Color(34, 197, 94));
        connectButton.setForeground(Color.WHITE);
        connectButton.setFocusPainted(false);
        connectButton.addActionListener(e -> startConnect());

        disconnectButton = new JButton("Disconnect");
        disconnectButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        disconnectButton.setBackground(new Color(239, 68, 68));
        disconnectButton.setForeground(Color.WHITE);
        disconnectButton.setFocusPainted(false);
        disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(e -> disconnect());

        statusLabel = new JLabel("Status: Disconnected");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        statusLabel.setForeground(new Color(220, 38, 38));

        buttonPanel.add(connectButton);
        buttonPanel.add(disconnectButton);
        buttonPanel.add(statusLabel);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        formPanel.add(buttonPanel, gbc);

        topPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // ==========================================
        // 2. CENTER: Announcement Display Board
        // ==========================================
        JPanel announcementPanel = new JPanel(new BorderLayout(6, 6));
        announcementPanel.setBorder(BorderFactory.createTitledBorder("NEW ANNOUNCEMENT"));

        JPanel annHeader = new JPanel(new BorderLayout());
        JLabel annTitle = new JLabel("📢 Latest Announcement:");
        annTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        annTitle.setForeground(new Color(30, 41, 59));
        announcementTimeLabel = new JLabel("");
        announcementTimeLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        announcementTimeLabel.setForeground(Color.GRAY);
        annHeader.add(annTitle, BorderLayout.WEST);
        annHeader.add(announcementTimeLabel, BorderLayout.EAST);
        announcementPanel.add(annHeader, BorderLayout.NORTH);

        announcementDisplay = new JTextArea(4, 25);
        announcementDisplay.setText("(No announcements received yet)");
        announcementDisplay.setFont(new Font("SansSerif", Font.BOLD, 15));
        announcementDisplay.setForeground(new Color(15, 23, 42));
        announcementDisplay.setBackground(new Color(254, 249, 195)); // Soft highlight
        announcementDisplay.setLineWrap(true);
        announcementDisplay.setWrapStyleWord(true);
        announcementDisplay.setEditable(false);
        announcementDisplay.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane annScrollPane = new JScrollPane(announcementDisplay);
        announcementPanel.add(annScrollPane, BorderLayout.CENTER);

        mainPanel.add(announcementPanel, BorderLayout.CENTER);

        // ==========================================
        // 3. BOTTOM: Message History & Log
        // ==========================================
        JPanel historyPanel = new JPanel(new BorderLayout(4, 4));
        historyPanel.setBorder(BorderFactory.createTitledBorder("Connection & Message Log"));
        historyPanel.setPreferredSize(new Dimension(500, 150));

        historyArea = new JTextArea();
        historyArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        historyArea.setEditable(false);
        historyArea.setBackground(new Color(250, 250, 250));
        JScrollPane historyScrollPane = new JScrollPane(historyArea);
        historyPanel.add(historyScrollPane, BorderLayout.CENTER);

        mainPanel.add(historyPanel, BorderLayout.SOUTH);

        // ==========================================
        // MEMBER 3 HOOK: Poll / Quiz Section
        // (Member 3 will add Question cards & Choice buttons here)
        // ==========================================

        setContentPane(mainPanel);

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
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Port must be a valid number.",
                    "Invalid Port", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Disable button during connection attempt
        connectButton.setEnabled(false);
        statusLabel.setText("Status: Connecting...");
        statusLabel.setForeground(new Color(234, 179, 8));

        // Connect in background thread
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

            // Update UI on Event Dispatch Thread
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Status: Connected");
                statusLabel.setForeground(new Color(22, 163, 74));
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                nameField.setEnabled(false);
                ipField.setEnabled(false);
                portField.setEnabled(false);
                appendLog("Joined successfully as: " + name);
            });

            // Start message listener thread
            listenerThread = new Thread(this::listenForServerMessages, "NetNotice-ClientListenerThread");
            listenerThread.setDaemon(true);
            listenerThread.start();

        } catch (Exception e) {
            disconnect();
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Status: Connection Failed");
                statusLabel.setForeground(new Color(220, 38, 38));
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

                // 2. Normal server message received
                else if (msg.startsWith(Protocol.PREFIX_MESSAGE)) {
                    String text = msg.substring(Protocol.PREFIX_MESSAGE.length());
                    SwingUtilities.invokeLater(() -> appendLog("[SERVER] " + text));
                }

                // 3. Error received
                else if (msg.startsWith(Protocol.PREFIX_ERROR)) {
                    String err = msg.substring(Protocol.PREFIX_ERROR.length());
                    SwingUtilities.invokeLater(() -> appendLog("[ERROR] " + err));
                }

                // ==========================================
                // MEMBER 3 HOOK: Quiz / Poll Question received
                // e.g., else if (msg.startsWith("QUESTION|")) { ... }
                // ==========================================
            }
        } catch (IOException e) {
            if (isConnected) {
                SwingUtilities.invokeLater(() -> appendLog("Connection lost: " + e.getMessage()));
            }
        } finally {
            disconnect();
        }
    }

    /**
     * Updates the announcement display safely on the Swing EDT.
     */
    private void onAnnouncementReceived(String text) {
        String timestamp = TIME_FORMAT.format(new Date());
        announcementDisplay.setText(text);
        announcementTimeLabel.setText("Received at: " + timestamp);
        appendLog("📢 NEW ANNOUNCEMENT: " + text);
    }

    /**
     * Safely disconnect from the server and reset UI components.
     */
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

        // Reset UI on EDT
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Status: Disconnected");
            statusLabel.setForeground(new Color(220, 38, 38));
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            nameField.setEnabled(true);
            ipField.setEnabled(true);
            portField.setEnabled(true);
            appendLog("Disconnected from server.");
        });
    }

    private void appendLog(String message) {
        String timestamp = TIME_FORMAT.format(new Date());
        historyArea.append("[" + timestamp + "] " + message + "\n");
        historyArea.setCaretPosition(historyArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            ClientGUI clientGUI = new ClientGUI();
            // If user supplied name as command line argument (e.g. for testing multiple clients)
            if (args.length > 0 && !args[0].isEmpty()) {
                clientGUI.nameField.setText(args[0]);
            }
            clientGUI.setVisible(true);
        });
    }
}
