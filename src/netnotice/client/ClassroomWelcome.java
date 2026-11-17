package netnotice.client;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import netnotice.common.UITheme;

/** A join invitation that becomes the listening state once the student connects. */
final class ClassroomWelcome extends JPanel {
    private final JLabel heading = new JLabel();
    private final JTextArea detail = new JTextArea();
    private final JButton join;

    ClassroomWelcome(Runnable openConnection) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));
        JPanel copy = new JPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.setOpaque(false);
        heading.setFont(UITheme.FONT_TITLE.deriveFont(26f));
        heading.setForeground(new Color(31, 46, 77));
        detail.setFont(UITheme.FONT_REGULAR);
        detail.setForeground(new Color(68, 85, 115));
        detail.setEditable(false);
        detail.setFocusable(false);
        detail.setOpaque(false);
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);
        detail.setBorder(null);
        detail.setRows(2);
        join = UITheme.primaryButton("Join classroom");
        join.addActionListener(e -> openConnection.run());
        for (JComponent item : new JComponent[]{heading, detail, join}) item.setAlignmentX(LEFT_ALIGNMENT);
        copy.add(heading);
        copy.add(Box.createVerticalStrut(8));
        copy.add(detail);
        copy.add(Box.createVerticalStrut(14));
        copy.add(join);
        add(copy, BorderLayout.CENTER);
        add(new NoticeArtwork(), BorderLayout.EAST);
        connected(false);
    }

    void connected(boolean online) {
        heading.setText(online ? "Waiting for the first notice" : "Join your classroom");
        detail.setText(online ? "Your instructor’s announcements will appear here." :
                "Use your instructor’s server address to receive notices and ask questions.");
        join.setVisible(!online);
        revalidate();
    }

    @Override public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(227, 239, 255), getWidth(), getHeight(), new Color(238, 232, 252)));
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
        g.dispose();
    }

    private static final class NoticeArtwork extends JComponent {
        NoticeArtwork() { setPreferredSize(new Dimension(126, 96)); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(12, getHeight()/2 - 38);
            g.rotate(0.12, 65, 35);
            g.setColor(new Color(176, 179, 231));
            g.fillRoundRect(20, 0, 84, 60, 15, 15);
            g.rotate(-0.21, 55, 40);
            g.setColor(new Color(120, 134, 192, 45));
            g.fillRoundRect(5, 19, 86, 58, 14, 14);
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Float(3, 13, 86, 58, 14, 14));
            g.setColor(new Color(81, 104, 212));
            g.fillOval(14, 24, 10, 10);
            g.fillRoundRect(30, 27, 37, 4, 4, 4);
            g.setColor(new Color(208, 215, 232));
            g.fillRoundRect(14, 42, 59, 4, 4, 4);
            g.fillRoundRect(14, 53, 39, 4, 4, 4);
            g.dispose();
        }
    }
}
