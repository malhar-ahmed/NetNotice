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
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import netnotice.common.FeatureHeader;
import netnotice.common.ActivityType;
import netnotice.common.Protocol;
import netnotice.common.UITheme;

/** Instructor workspace for announcements, live activities, students and classroom events. */
public class ServerGUI extends JFrame implements NetNoticeServer.ServerListener {

    private static final long serialVersionUID = 1L;
    private static final String APP_TITLE = "NetNotice - Server Administration Hub";

    // Header & Server Controls
    private UITheme.StatusBadge serverStatusBadge;
    private JTextField portField;
    private UITheme.FlatButton startStopButton;

    // Client Roster
    private DefaultListModel<String> clientListModel;
    private JList<String> clientList;
    private JLabel clientCountLabel;
    private JTextField searchClientField;
    private final List<String> allConnectedClients = new ArrayList<>();

    // Announcement Controls
    private JTextArea announcementArea;
    private UITheme.FlatButton sendAnnouncementButton;
    private JLabel announcementCountLabel;
    private int announcementCount = 0;
    private netnotice.common.NotificationCenter notifications;
    private record StudentMessage(String sender, String time, String text) {
        String transcript() { return sender + "  ·  " + time + "\n" + text; }
    }
    private final DefaultListModel<StudentMessage> messages = new DefaultListModel<>();
    private final java.awt.CardLayout inboxLayout = new java.awt.CardLayout();
    private final JPanel inbox = new JPanel(inboxLayout);
    private JList<StudentMessage> messageList;
    private JLabel unreadMessagesLabel;
    private JButton readMessagesButton;
    private int unreadMessageCount;
    private javax.swing.JTabbedPane workspaceTabs;
    private JPanel messageCard;


    // Poll Controls
    private JTextField pollQuestionField;
    private JTextField[] pollOptionFields;
    private UITheme.FlatButton sendPollButton;
    private UITheme.FlatButton endPollButton;
    private JCheckBox quizModeCheckbox;
    private JTextArea pollResultsArea;
    private UITheme.StatusBadge pollStatusBadge;
    private JPanel visualBarsPanel;
    private UITheme.PollProgressBar[] visualBars;

    // Activity Log
    private JTextArea logArea;
    private JLabel logCountLabel;
    private int logMessageCount = 0;

    @SuppressWarnings("this-escape")
    public ServerGUI() {
        super(APP_TITLE);
        initUI();
        NetNoticeServer.setListener(this);
    }

    private javax.swing.JCheckBox followLog;
    private void initUI() {
        netnotice.common.WorkspaceUI.setup(this, 1040, 780);
        setTitle("NetNotice • Instructor");
        JPanel root = new JPanel(new BorderLayout());
        notifications=new netnotice.common.NotificationCenter();
        javax.swing.JTabbedPane tabs = new javax.swing.JTabbedPane(); workspaceTabs=tabs; netnotice.common.WorkspaceUI.tabs(tabs);
        JPanel broadcast = netnotice.common.WorkspaceUI.stack();
        messageCard=netnotice.common.WorkspaceUI.card("Student messages", "", UITheme.Icons.users(22,UITheme.PRIMARY));
        JLabel empty = new JLabel("No student messages yet", JLabel.CENTER);
        empty.setFont(UITheme.FONT_REGULAR);
        empty.setForeground(UITheme.TEXT_MUTED);
        inbox.setOpaque(false);
        inbox.add(empty, "empty");
        messageList = new JList<>(messages) {
            @Override public boolean getScrollableTracksViewportWidth() { return true; }
        };
        messageList.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent event) {
                if (messageList.getWidth() > 0) messageList.setFixedCellWidth(messageList.getWidth());
            }
        });
        messageList.setCellRenderer((list, message, index, selected, focus) -> {
            JPanel row = new JPanel(new BorderLayout(12, 4));
            row.setBackground(selected ? UITheme.PRIMARY_BG : Color.WHITE);
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(8, 4, 9, 8)));
            row.add(new UITheme.AvatarBadge(message.sender()), BorderLayout.WEST);
            JPanel content = new JPanel(new BorderLayout(8, 4));
            content.setOpaque(false);
            JLabel sender = new JLabel(message.sender());
            sender.putClientProperty("html.disable", true);
            sender.setFont(UITheme.FONT_HEADER);
            JLabel time = new JLabel(message.time());
            time.setFont(UITheme.FONT_SMALL); time.setForeground(UITheme.TEXT_MUTED);
            JPanel meta = new JPanel(new BorderLayout()); meta.setOpaque(false);
            meta.add(sender, BorderLayout.CENTER); meta.add(time, BorderLayout.EAST);
            content.add(meta, BorderLayout.NORTH);
            JTextArea body = netnotice.common.WorkspaceUI.text(message.text(), 0);
            body.setOpaque(false);
            body.setSize(Math.max(140, list.getWidth()-90), Short.MAX_VALUE);
            content.add(body, BorderLayout.CENTER);
            row.add(content, BorderLayout.CENTER);
            return row;
        });
        inbox.add(netnotice.common.WorkspaceUI.scroll(messageList), "messages");
        inbox.setPreferredSize(new Dimension(200, 134));
        messageCard.add(inbox, BorderLayout.CENTER);
        unreadMessagesLabel=new JLabel("All caught up");unreadMessagesLabel.setFont(UITheme.FONT_SMALL);
        readMessagesButton=UITheme.optionButton("Mark all read");readMessagesButton.setEnabled(false);
        readMessagesButton.addActionListener(e -> markMessagesRead());
        JButton exportMessages=UITheme.optionButton("Export messages");exportMessages.addActionListener(e -> UITheme.exportText(this,java.util.Collections.list(messages.elements()).stream().map(StudentMessage::transcript).collect(java.util.stream.Collectors.joining("\n\n")),"netnotice-student-messages.txt"));
        messageCard.add(netnotice.common.WorkspaceUI.row(unreadMessagesLabel,readMessagesButton,exportMessages),BorderLayout.SOUTH);
        netnotice.common.WorkspaceUI.add(broadcast,messageCard);
        JPanel connection = netnotice.common.WorkspaceUI.card("Classroom connection", "Start your server, then share this computer's LAN address and port with students.", UITheme.Icons.users(22, UITheme.PRIMARY));
        portField = new UITheme.RoundedTextField(String.valueOf(Protocol.DEFAULT_PORT), 6);
        startStopButton = UITheme.primaryButton("Start server"); startStopButton.addActionListener(e -> toggleServer());
        serverStatusBadge = new UITheme.StatusBadge("Server offline", UITheme.BG_CARD_ALT, UITheme.TEXT_MUTED);
        JButton copyAddress = UITheme.optionButton("Copy LAN address");
        copyAddress.setIcon(UITheme.Icons.copy(16, UITheme.PRIMARY));
        copyAddress.addActionListener(e -> {
            String address = lanAddress() + ":" + portField.getText().trim();
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(address), null);
            copyAddress.setText("Copied " + address);
            javax.swing.Timer timer = new javax.swing.Timer(2500, event -> copyAddress.setText("Copy LAN address")); timer.setRepeats(false); timer.start();
        });
        connection.add(netnotice.common.WorkspaceUI.row(new JLabel("Port"), portField, startStopButton, serverStatusBadge), BorderLayout.CENTER);
        JTextField addressDisplay = new UITheme.RoundedTextField(lanAddress(), 14);
        addressDisplay.setEditable(false); addressDisplay.setToolTipText("This computer's LAN address");
        connection.add(netnotice.common.WorkspaceUI.row(addressDisplay, copyAddress), BorderLayout.SOUTH);
        JPanel connectionPage = netnotice.common.WorkspaceUI.stack();
        netnotice.common.WorkspaceUI.add(connectionPage, connection);
        JButton openConnection = UITheme.optionButton("Server settings"); openConnection.addActionListener(e -> tabs.setSelectedIndex(4));
        netnotice.common.WorkspaceUI.add(broadcast, netnotice.common.WorkspaceUI.row(openConnection, serverStatusBadge));
        JPanel compose = netnotice.common.WorkspaceUI.card(new FeatureHeader(FeatureHeader.Art.NOTICE, "Share an announcement", "Write a notice for everyone in your classroom."));
        javax.swing.JComboBox<String> templates = new javax.swing.JComboBox<>(new String[]{"Choose a quick template…", "Class is starting now", "5 minutes remaining", "10-minute break", "Submit your assignment"});
        templates.setFont(UITheme.FONT_REGULAR);
        templates.addActionListener(e -> { if(templates.getSelectedIndex()>0) announcementArea.setText((String)templates.getSelectedItem()); });
        announcementArea = new JTextArea(4, 30); announcementArea.setFont(UITheme.FONT_REGULAR.deriveFont(16f));
        announcementArea.setLineWrap(true); announcementArea.setWrapStyleWord(true); announcementArea.setMargin(new Insets(14,14,14,14));
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(ServerGUI.class);
        announcementArea.setText(prefs.get("draft", ""));
        JLabel characters = new JLabel(announcementArea.getText().length() + " characters • Draft saved locally"); characters.setFont(UITheme.FONT_SMALL);
        announcementArea.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                String text = announcementArea.getText(); characters.setText(text.length()+" characters • Draft saved locally");
                prefs.put("draft", text.substring(0, Math.min(text.length(), 8000)));
            }
            public void insertUpdate(DocumentEvent e) { update(); } public void removeUpdate(DocumentEvent e) { update(); } public void changedUpdate(DocumentEvent e) { update(); }
        });
        JPanel editor = new JPanel(new BorderLayout(0,12)); editor.setOpaque(false);
        editor.add(templates, BorderLayout.NORTH); JScrollPane editScroll = new JScrollPane(announcementArea);
        editScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER)); editor.add(editScroll, BorderLayout.CENTER);
        editor.add(characters, BorderLayout.SOUTH); compose.add(editor, BorderLayout.CENTER);
        sendAnnouncementButton = UITheme.primaryButton("Send announcement"); sendAnnouncementButton.setIcon(UITheme.Icons.send(18,Color.WHITE));
        sendAnnouncementButton.setEnabled(false); sendAnnouncementButton.addActionListener(e -> sendAnnouncement());
        announcementCountLabel = new JLabel("0 sent this session");
        JButton preview = UITheme.optionButton("Preview");
        preview.addActionListener(e -> {
            JTextArea text = netnotice.common.WorkspaceUI.text(announcementArea.getText().trim().isEmpty()?"Your announcement preview will appear here.":announcementArea.getText(),8);
            text.setFont(UITheme.FONT_REGULAR.deriveFont(18f));
            JScrollPane scroll=new JScrollPane(text); scroll.setPreferredSize(new Dimension(480,240));
            JOptionPane.showMessageDialog(this,scroll,"Announcement preview",JOptionPane.PLAIN_MESSAGE);
        });
        JButton clearDraft=UITheme.optionButton("Clear draft"); clearDraft.addActionListener(e -> announcementArea.setText(""));
        sendAnnouncementButton.setToolTipText("Send to every connected student • Ctrl+Enter");
        compose.add(netnotice.common.WorkspaceUI.row(sendAnnouncementButton, preview, clearDraft, announcementCountLabel), BorderLayout.SOUTH);
        netnotice.common.WorkspaceUI.add(broadcast, compose);
        tabs.addTab("  Overview  ", UITheme.Icons.broadcast(18, UITheme.PRIMARY), netnotice.common.WorkspaceUI.page(broadcast));

        JPanel activities = netnotice.common.WorkspaceUI.stack();
        FeatureHeader creator = new FeatureHeader(FeatureHeader.Art.POLL, "Create a poll", "Ask a question with two to four choices. Students can see votes as they arrive.");
        JPanel poll = netnotice.common.WorkspaceUI.card(creator);
        JPanel form = new JPanel(new GridLayout(0,1,0,12)); form.setOpaque(false);
        pollQuestionField = new UITheme.RoundedTextField("", 25); form.add(netnotice.common.WorkspaceUI.field("Question",pollQuestionField));
        pollOptionFields = new JTextField[4];
        JPanel choices = netnotice.common.WorkspaceUI.grid(2); choices.setOpaque(false);
        for(int i=0;i<4;i++) { pollOptionFields[i] = new UITheme.RoundedTextField("",12); choices.add(netnotice.common.WorkspaceUI.field("Choice " + (char)('A'+i)+(i<2?" • required":" • optional"),pollOptionFields[i])); }
        JPanel questionBody = new JPanel(new BorderLayout(0,14)); questionBody.setOpaque(false);
        questionBody.add(form,BorderLayout.NORTH); questionBody.add(choices,BorderLayout.CENTER);
        quizModeCheckbox = new JCheckBox("Quiz mode • Hide live results from students"); quizModeCheckbox.setOpaque(false); quizModeCheckbox.setFont(UITheme.FONT_REGULAR);
        quizModeCheckbox.setToolTipText("Students see the answer totals after you close the quiz.");
        questionBody.add(quizModeCheckbox,BorderLayout.SOUTH); poll.add(questionBody,BorderLayout.CENTER);
        sendPollButton = UITheme.primaryButton("Launch activity"); sendPollButton.setEnabled(false); sendPollButton.addActionListener(e -> sendPoll());
        endPollButton = UITheme.optionButton("Close activity"); endPollButton.setEnabled(false); endPollButton.addActionListener(e -> endPoll());
        Runnable updateCreator = () -> {
            boolean quiz = quizModeCheckbox.isSelected();
            creator.update(quiz ? FeatureHeader.Art.QUIZ : FeatureHeader.Art.POLL, quiz ? "Create a quiz" : "Create a poll",
                    quiz ? "Write a question and its choices. Students see quiz results after you close it." : "Ask a question with two to four choices. Students can see votes as they arrive.");
            sendPollButton.setText(quiz ? "Launch quiz" : "Launch poll");
            endPollButton.setText(quiz ? "Close quiz" : "Close poll");
        };
        quizModeCheckbox.addItemListener(e -> updateCreator.run());
        updateCreator.run();
        pollStatusBadge = new UITheme.StatusBadge("No active activity",UITheme.BG_CARD_ALT,UITheme.TEXT_MUTED);
        poll.add(netnotice.common.WorkspaceUI.row(sendPollButton,endPollButton,pollStatusBadge), BorderLayout.SOUTH);
        netnotice.common.WorkspaceUI.add(activities,poll);
        JPanel results = netnotice.common.WorkspaceUI.card("Response overview", "Follow responses as they arrive. Final results remain available after closing.",UITheme.Icons.poll(22,UITheme.PRIMARY));
        visualBarsPanel = new JPanel(new GridLayout(0,1,0,10)); visualBarsPanel.setOpaque(false); visualBars = new UITheme.PollProgressBar[4];
        for(int i=0;i<4;i++) { visualBars[i]=new UITheme.PollProgressBar((char)('A'+i),"Option"); visualBars[i].setVisible(false); visualBarsPanel.add(visualBars[i]); }
        pollResultsArea = netnotice.common.WorkspaceUI.text("No responses yet. Launch an activity to get started.",5);
        pollResultsArea.setBackground(UITheme.BG_CARD_ALT); pollResultsArea.setMargin(new Insets(12,12,12,12));
        JPanel resultBody = new JPanel(new BorderLayout(0,12)); resultBody.setOpaque(false); resultBody.add(visualBarsPanel,BorderLayout.NORTH); resultBody.add(netnotice.common.WorkspaceUI.scroll(pollResultsArea),BorderLayout.CENTER);
        results.add(resultBody,BorderLayout.CENTER);
        JButton exportResults = UITheme.optionButton("Export results"); exportResults.addActionListener(e -> UITheme.exportText(this,pollResultsArea.getText(),"netnotice-results.txt"));
        results.add(netnotice.common.WorkspaceUI.row(exportResults),BorderLayout.SOUTH); netnotice.common.WorkspaceUI.add(activities,results);
        tabs.addTab("  Polls & Quizzes  ",UITheme.Icons.poll(18,UITheme.SUCCESS),netnotice.common.WorkspaceUI.page(activities));

        JPanel people = netnotice.common.WorkspaceUI.stack();
        JPanel roster = netnotice.common.WorkspaceUI.card(new FeatureHeader(FeatureHeader.Art.PEOPLE, "Class roster", "Find students currently connected to your classroom."));
        clientCountLabel = new JLabel("0 Online"); clientCountLabel.setFont(UITheme.FONT_SECTION);
        searchClientField = new UITheme.RoundedTextField("",20); searchClientField.setToolTipText("Search connected students by name");
        searchClientField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e){applyClientFilter();} public void removeUpdate(DocumentEvent e){applyClientFilter();} public void changedUpdate(DocumentEvent e){applyClientFilter();}
        });
        clientListModel = new DefaultListModel<>(); clientList = new JList<>(clientListModel);
        clientList.setFont(UITheme.FONT_REGULAR); clientList.setFixedCellHeight(54); clientList.setVisibleRowCount(6);
        clientList.setSelectionBackground(UITheme.PRIMARY_BG); clientList.setSelectionForeground(UITheme.PRIMARY);
        clientList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) {
                JLabel label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focus);
                label.setBorder(BorderFactory.createEmptyBorder(8,12,8,12)); label.setIcon(UITheme.Icons.users(22,UITheme.SUCCESS)); label.setIconTextGap(14); return label;
            }
        });
        JPanel rosterBody = new JPanel(new BorderLayout(0,12)); rosterBody.setOpaque(false);
        rosterBody.add(netnotice.common.WorkspaceUI.field("Search students",searchClientField),BorderLayout.NORTH);
        rosterBody.add(new JScrollPane(clientList),BorderLayout.CENTER);
        rosterBody.add(clientCountLabel,BorderLayout.SOUTH); roster.add(rosterBody,BorderLayout.CENTER);
        netnotice.common.WorkspaceUI.add(people,roster);
        tabs.addTab("  Students  ",UITheme.Icons.users(18,UITheme.PRIMARY),netnotice.common.WorkspaceUI.page(people));

        JPanel activity = netnotice.common.WorkspaceUI.stack();
        JPanel log = netnotice.common.WorkspaceUI.card("Classroom activity", "Connection events, student replies and server updates, all in one place.",UITheme.Icons.clock(22,UITheme.PRIMARY));
        logArea = netnotice.common.WorkspaceUI.text("",14); logArea.setFont(UITheme.FONT_MONO); logArea.setMargin(new Insets(14,14,14,14)); logArea.setBackground(UITheme.BG_CARD_ALT);
        log.add(netnotice.common.WorkspaceUI.scroll(logArea),BorderLayout.CENTER); logCountLabel = new JLabel("0 events");
        JButton export = UITheme.optionButton("Export log"); export.addActionListener(e -> UITheme.exportText(this,logArea.getText(),"netnotice-activity.txt"));
        JButton clear = UITheme.optionButton("Clear"); clear.addActionListener(e -> { logArea.setText(""); logMessageCount=0; logCountLabel.setText("0 events"); });
        followLog = new JCheckBox("Follow latest",true); followLog.setOpaque(false);
        log.add(netnotice.common.WorkspaceUI.row(export,clear,followLog,logCountLabel),BorderLayout.SOUTH); netnotice.common.WorkspaceUI.add(activity,log);
        tabs.addTab("  Activity log  ",UITheme.Icons.clock(18,UITheme.PRIMARY),netnotice.common.WorkspaceUI.page(activity));
        tabs.addTab("  Connection  ",UITheme.Icons.settings(18,UITheme.PRIMARY),netnotice.common.WorkspaceUI.page(connectionPage));
        root.add(tabs,BorderLayout.CENTER);
        JLabel footer = new JLabel("NetNotice 2  •  Local network workspace                        Ctrl + Enter: send announcement");
        footer.setFont(UITheme.FONT_SMALL); footer.setForeground(UITheme.TEXT_MUTED); footer.setBorder(BorderFactory.createEmptyBorder(12,22,12,22)); root.add(footer,BorderLayout.SOUTH);
        setContentPane(new netnotice.common.DesktopShell(root, tabs, "Instructor", notifications));
        netnotice.common.WorkspaceUI.shortcut(getRootPane(),"control ENTER",() -> { if(tabs.getSelectedIndex()==0 && sendAnnouncementButton.isEnabled()) sendAnnouncement(); });
        addWindowListener(new java.awt.event.WindowAdapter() { public void windowClosing(java.awt.event.WindowEvent e) { NetNoticeServer.stopServer(); } });
    }

    private void markMessagesRead() {
        unreadMessageCount=0;unreadMessagesLabel.setText("All caught up");readMessagesButton.setEnabled(false);
        workspaceTabs.setTitleAt(0,"Overview");
    }
    private void openMessages() {
        workspaceTabs.setSelectedIndex(0);
        messageCard.scrollRectToVisible(new java.awt.Rectangle(0,0,messageCard.getWidth(),messageCard.getHeight()));
        markMessagesRead();
    }
    @Override public void onStudentMessage(String sender,String text) {
        SwingUtilities.invokeLater(() -> {
            String time=java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            messages.add(0, new StudentMessage(sender, time, text));
            inboxLayout.show(inbox, "messages");
            messageList.ensureIndexIsVisible(0);
            unreadMessageCount++;unreadMessagesLabel.setText(unreadMessageCount+" unread");readMessagesButton.setEnabled(true);
            workspaceTabs.setTitleAt(0,"Overview ("+unreadMessageCount+")");
            notifications.show("Message from "+sender,text,this::openMessages);
            UITheme.Sound.playAnnouncementChime();
        });
    }

    private static String lanAddress() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces=java.net.NetworkInterface.getNetworkInterfaces();
            while(interfaces.hasMoreElements()) {
                java.net.NetworkInterface network=interfaces.nextElement();
                if(!network.isUp() || network.isLoopback() || network.isVirtual()) continue;
                java.util.Enumeration<java.net.InetAddress> addresses=network.getInetAddresses();
                while(addresses.hasMoreElements()) { java.net.InetAddress address=addresses.nextElement();
                    if(address instanceof java.net.Inet4Address && !address.isLoopbackAddress()) return address.getHostAddress(); }
            }
        } catch(java.net.SocketException ignored) {}
        return "127.0.0.1";
    }

    private void applyClientFilter() {
        String filter = searchClientField.getText().trim().toLowerCase();
        if (filter.equals("filter users...")) {
            filter = "";
        }
        clientListModel.clear();
        for (String c : allConnectedClients) {
            if (filter.isEmpty() || c.toLowerCase().contains(filter)) {
                clientListModel.addElement(c);
            }
        }
    }

    // ================================================================
    // Server Toggle
    // ================================================================
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
            allConnectedClients.clear();
            clientListModel.clear();
            clientCountLabel.setText("0 Online");
            resetPollUI();
        }
    }

    // ================================================================
    // Announcement
    // ================================================================
    private void sendAnnouncement() {
        String text = announcementArea.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter announcement text to broadcast.",
                    "Empty Announcement", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!NetNoticeServer.isRunning()) return;
        NetNoticeServer.broadcastAnnouncement(text);
        announcementCount++;
        announcementCountLabel.setText(announcementCount + " sent");
        announcementArea.setText("");
        announcementArea.requestFocus();
    }

    // ================================================================
    // Poll Actions
    // ================================================================
    private void sendPoll() {
        String question = pollQuestionField.getText().trim();
        if (question.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Enter a question before launching.",
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
                    "Enter at least two choices.",
                    "Insufficient Options", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // If Quiz Mode is checked, prepend [Quiz] tag so clients know it is a blind quiz
        if (quizModeCheckbox != null && quizModeCheckbox.isSelected() && !question.toLowerCase(java.util.Locale.ROOT).startsWith("[quiz]")) {
            question = "[Quiz] " + question;
        }

        NetNoticeServer.startPoll(question, options);

        sendPollButton.setEnabled(false);
        endPollButton.setEnabled(true);
        pollQuestionField.setEnabled(false);
        if (quizModeCheckbox != null) {
            quizModeCheckbox.setEnabled(false);
        }
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
        if (quizModeCheckbox != null) {
            quizModeCheckbox.setEnabled(true);
        }
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
        if (quizModeCheckbox != null) {
            quizModeCheckbox.setEnabled(true);
        }
        for (JTextField f : pollOptionFields) {
            f.setEnabled(true);
            f.setText("");
        }
        for (UITheme.PollProgressBar bar : visualBars) {
            bar.setVisible(false);
        }
        pollResultsArea.setText("No responses yet. Launch an activity to get started.");
        pollStatusBadge.setStatus("No active activity", UITheme.BG_APP, UITheme.TEXT_MUTED);
    }

    // ================================================================
    // Listener Callbacks
    // ================================================================
    @Override
    public void onClientListUpdated(List<String> clients) {
        SwingUtilities.invokeLater(() -> {
            allConnectedClients.clear();
            allConnectedClients.addAll(clients);
            applyClientFilter();
            clientCountLabel.setText(clients.size() + " Online");
        });
    }

    @Override
    public void onLogMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logMessageCount++;
            logCountLabel.setText(logMessageCount + " events");
            logArea.append(message + "\n");
            if (followLog.isSelected()) logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @Override
    public void onPollStateChanged(String question, List<String> options,
                                    int[] voteCounts, int totalVotes, boolean isActive) {
        SwingUtilities.invokeLater(() -> {
            if (question == null) { resetPollUI(); return; }
            ActivityType type = ActivityType.from(question);
            pollStatusBadge.setStatus(type.title + (isActive ? " active" : " closed"),
                    isActive ? UITheme.SUCCESS_BG : UITheme.BG_CARD_ALT, isActive ? UITheme.SUCCESS : UITheme.TEXT_SECONDARY);
            int highest = java.util.Arrays.stream(voteCounts).max().orElse(0);
            for (int i = 0; i < visualBars.length; i++) {
                boolean shown = i < options.size();
                visualBars[i].setVisible(shown);
                if (shown) {
                    int count = i < voteCounts.length ? voteCounts[i] : 0;
                    visualBars[i].setActivityType(type);
                    visualBars[i].updateData(options.get(i), count, totalVotes, false, count > 0 && count == highest);
                }
            }
            visualBarsPanel.revalidate();
            visualBarsPanel.repaint();
            pollResultsArea.setText(ActivityType.displayQuestion(question) + "\n" +
                    type.results(!isActive, options.toArray(new String[0]), voteCounts, -1));
        });
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            ServerGUI gui = new ServerGUI();
            gui.setVisible(true);
            gui.toggleServer();
        });
    }
}
