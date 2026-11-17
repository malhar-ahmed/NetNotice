package netnotice.common;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayDeque;
import javax.swing.*;

/** Notifications are Swing children of the workspace toolbar, including when minimized. */
public final class NotificationCenter extends JPanel {
    private record Notice(String title, String message, Runnable open) {}
    private final ArrayDeque<Notice> pending = new ArrayDeque<>();
    private final UITheme.FlatButton bell = UITheme.optionButton("");
    private final JLabel title = new JLabel();
    private final JLabel preview = new JLabel();
    private final JPanel strip = new JPanel(new BorderLayout(14, 0));
    private int delivered;
    private boolean expanded;

    public NotificationCenter() {
        super(new BorderLayout());
        setOpaque(false);
        bell.setIcon(new BellIcon());
        bell.setCompact(true);
        bell.setToolTipText("Show or hide notifications");
        bell.addActionListener(e -> { expanded = !expanded; refresh(); });
        strip.setBackground(new Color(239, 245, 255));
        strip.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(210, 222, 239)),
            BorderFactory.createEmptyBorder(9, 20, 10, 16)));
        strip.add(new JLabel(new BellIcon()), BorderLayout.WEST);
        JPanel words = new JPanel(new GridLayout(2, 1, 0, 2));
        words.setOpaque(false);
        title.putClientProperty("html.disable", true);
        preview.putClientProperty("html.disable", true);
        title.setFont(UITheme.FONT_HEADER);
        preview.setFont(UITheme.FONT_SMALL);
        preview.setForeground(UITheme.TEXT_SECONDARY);
        words.add(title);
        words.add(preview);
        strip.add(words, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        actions.setOpaque(false);
        UITheme.FlatButton view = UITheme.optionButton("View");
        view.setCompact(true);
        view.addActionListener(e -> {
            Notice notice = pending.pollFirst();
            refresh();
            if (notice != null) notice.open().run();
        });
        UITheme.FlatButton dismiss = UITheme.optionButton("Dismiss");
        dismiss.setCompact(true);
        dismiss.addActionListener(e -> { pending.pollFirst(); refresh(); });
        actions.add(view);
        actions.add(dismiss);
        strip.add(actions, BorderLayout.EAST);
        add(strip);
        refresh();
    }

    public JButton toolbarButton() { return bell; }
    public int activeCount() { return pending.size(); }
    public int deliveredCount() { return delivered; }

    public void show(String heading, String message, Runnable open) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> show(heading, message, open));
            return;
        }
        delivered++;
        pending.addFirst(new Notice(heading, message, open));
        if (pending.size() > 32) pending.removeLast();
        expanded = true;
        refresh();
    }

    private void refresh() {
        Notice current = pending.peekFirst();
        if (current == null) expanded = false;
        bell.setText(current == null ? "" : Integer.toString(pending.size()));
        bell.setEnabled(current != null);
        bell.getAccessibleContext().setAccessibleName("Notifications, " + pending.size() + " pending");
        title.setText(current == null ? "" : current.title());
        preview.setText(current == null ? "" : current.message().replace('\n', ' ').replace('\r',' '));
        setVisible(expanded);
        revalidate();
        repaint();
    }

    private static final class BellIcon implements Icon {
        public int getIconWidth() { return 19; }
        public int getIconHeight() { return 19; }
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x,y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(UITheme.TEXT_SECONDARY);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D bell = new Path2D.Double();
            bell.moveTo(3,14); bell.quadTo(5,12,5,9); bell.quadTo(5,4,9.5,3);
            bell.quadTo(14,4,14,9); bell.quadTo(14,12,16,14); bell.closePath();
            g.draw(bell); g.drawArc(7,14,5,3,180,180);
            g.dispose();
        }
    }
}
