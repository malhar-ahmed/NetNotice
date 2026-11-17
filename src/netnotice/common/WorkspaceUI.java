package netnotice.common;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Layout primitives shared by both desktop workspaces. */
public final class WorkspaceUI {
    private WorkspaceUI() {}
    private static boolean configured;
    public static void setup(JFrame frame, int width, int height) {
        if (!configured) {
            try { UIManager.setLookAndFeel(System.getProperty("os.name", "").startsWith("Mac")
                    ? UIManager.getSystemLookAndFeelClassName() : UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) { }
            for(Object key : java.util.Collections.list(UIManager.getDefaults().keys()))
                if(key.toString().endsWith(".font")) UIManager.put(key,new javax.swing.plaf.FontUIResource(UITheme.FONT_REGULAR));
            UIManager.put("TextArea.background", Color.WHITE);
            UIManager.put("TextField.background", Color.WHITE);
            UIManager.put("ComboBox.background", Color.WHITE);
            UIManager.put("ScrollBar.width", 10);
            configured = true;
        }
        UIManager.put("TabbedPane.font", UITheme.FONT_BOLD);
        UIManager.put("TabbedPane.selected", UITheme.PRIMARY_BG);
        UIManager.put("TabbedPane.contentAreaColor", UITheme.BG_APP);
        UIManager.put("Panel.background", UITheme.BG_APP);
        java.awt.image.BufferedImage appIcon=new java.awt.image.BufferedImage(64,64,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D iconGraphics=appIcon.createGraphics();
        iconGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        iconGraphics.setColor(UITheme.PRIMARY);iconGraphics.fillRoundRect(0,0,64,64,18,18);
        UITheme.Icons.broadcast(42,Color.WHITE).paintIcon(frame,iconGraphics,11,11);iconGraphics.dispose();
        frame.setIconImage(appIcon);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setSize(Math.min(width, screen.width), Math.min(height, screen.height));
        frame.setMinimumSize(new Dimension(Math.min(600, screen.width), Math.min(480, screen.height)));
        frame.setLocationRelativeTo(null);
    }
    public static JPanel stack() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.BG_APP);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 26, 22, 26));
        return panel;
    }
    public static void add(JPanel stack, JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);

        stack.add(component); stack.add(Box.createVerticalStrut(16));
    }
    public static JPanel card(String title, String description, Icon icon) {
        JPanel heading = new JPanel(new BorderLayout(0, 7)); heading.setOpaque(false);
        JLabel label = new JLabel(title, icon, JLabel.LEFT); label.setFont(UITheme.FONT_SECTION);
        label.setForeground(UITheme.TEXT_PRIMARY); label.setIconTextGap(10);
        heading.add(label, BorderLayout.NORTH);
        JTextArea sub = text(description, 1); sub.setOpaque(false); sub.setForeground(UITheme.TEXT_MUTED);
        if (!description.isBlank()) heading.add(sub, BorderLayout.CENTER);
        return card(heading);
    }

    public static JPanel card(JComponent heading) {
        JPanel card = new UITheme.RoundedCard(18, UITheme.BORDER) {
            @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
        };
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(17, 20, 17, 20));
        card.add(heading, BorderLayout.NORTH);
        return card;
    }
    public static JPanel row(Component... components) {
        JPanel panel = new JPanel(new WrapLayout()); panel.setOpaque(false);
        for (Component c : components) panel.add(c); return panel;
    }
    /** FlowLayout with a height that follows its available width. */
    private static final class WrapLayout extends FlowLayout {
        WrapLayout() { super(FlowLayout.LEFT, 8, 4); }
        @Override public Dimension preferredLayoutSize(Container target) {
            synchronized(target.getTreeLock()) {
                int width=target.getWidth();
                if(width<=0) return super.preferredLayoutSize(target);
                Insets in=target.getInsets(); int available=width-in.left-in.right-16;
                int rowWidth=0,rowHeight=0,totalHeight=8,maxWidth=0;
                for(Component c:target.getComponents()) if(c.isVisible()) {
                    Dimension d=c.getPreferredSize();
                    if(rowWidth>0 && rowWidth+8+d.width>available) {
                        totalHeight+=rowHeight+4; maxWidth=Math.max(maxWidth,rowWidth); rowWidth=0; rowHeight=0;
                    }
                    rowWidth+=(rowWidth==0?0:8)+d.width; rowHeight=Math.max(rowHeight,d.height);
                }
                return new Dimension(Math.max(maxWidth,rowWidth)+16+in.left+in.right,totalHeight+rowHeight+in.top+in.bottom);
            }
        }
    }

    public static JPanel field(String title, JComponent input) {
        JPanel panel = new JPanel(new BorderLayout(0, 7)); panel.setOpaque(false);
        JLabel label = new JLabel(title); label.setFont(UITheme.FONT_BOLD); label.setForeground(UITheme.TEXT_SECONDARY);
        label.setLabelFor(input); panel.add(label, BorderLayout.NORTH); panel.add(input, BorderLayout.CENTER); return panel;
    }
    public static JTextArea text(String value, int rows) {
        JTextArea area = new JTextArea(value, rows, 20); area.setFont(UITheme.FONT_REGULAR);
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setForeground(UITheme.TEXT_PRIMARY); area.setLineWrap(true); area.setWrapStyleWord(true); area.setEditable(false);
        return area;
    }
    public static JScrollPane scroll(Component component) {
        JScrollPane scroll = new JScrollPane(component); scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(UITheme.BG_APP);
        scroll.getVerticalScrollBar().setUnitIncrement(18); return scroll;
    }
    public static JScrollPane page(JPanel panel) {
        JPanel viewport = new ScrollPage(); viewport.setBackground(UITheme.BG_APP);
        viewport.add(panel, BorderLayout.NORTH);
        JScrollPane scroll = scroll(viewport);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }
    private static final class ScrollPage extends JPanel implements Scrollable {
        ScrollPage() { super(new BorderLayout()); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction) { return 18; }
        public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction) { return Math.max(18,r.height-36); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }
    public static JPanel grid(int maximumColumns) {
        JPanel panel = new JPanel(new GridLayout(0, maximumColumns, 12, 12) {
            private void adapt(Container c) { int width=c.getWidth(); setColumns(width>0 ? Math.max(1,Math.min(maximumColumns,width/210)) : maximumColumns); }
            public Dimension preferredLayoutSize(Container c) { adapt(c); return super.preferredLayoutSize(c); }
            public void layoutContainer(Container c) { adapt(c); super.layoutContainer(c); }
        });
        panel.setOpaque(false); return panel;
    }

    public static void tabs(JTabbedPane tabs) {
        tabs.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            protected int calculateTabAreaHeight(int placement,int runs,int maxHeight) { return 0; }
            protected int calculateTabAreaWidth(int placement,int runs,int maxWidth) { return 0; }
            protected void paintTabArea(Graphics g,int placement,int selected) { }
            protected void paintContentBorder(Graphics g,int placement,int selected) { }
            protected void installDefaults() { super.installDefaults(); contentBorderInsets=new Insets(0,0,0,0); }
        });
        tabs.setBorder(BorderFactory.createEmptyBorder());
    }

    public static void polish(Component component) {
        if(component instanceof JComboBox) {
            JComboBox<?> combo=(JComboBox<?>)component;
            combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
                protected JButton createArrowButton() {
                    JButton arrow=new JButton() {
                        protected void paintComponent(Graphics graphics) {
                            Graphics2D g=(Graphics2D)graphics.create();
                            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                            g.setColor(UITheme.TEXT_MUTED); g.setStroke(new BasicStroke(1.5f));
                            int x=getWidth()/2,y=getHeight()/2; g.drawLine(x-4,y-2,x,y+2); g.drawLine(x,y+2,x+4,y-2); g.dispose();
                        }
                    };
                    arrow.setPreferredSize(new Dimension(28,28)); arrow.setBorder(BorderFactory.createEmptyBorder());
                    arrow.setContentAreaFilled(false); return arrow;
                }
            });
            combo.setBackground(Color.WHITE); combo.setForeground(UITheme.TEXT_PRIMARY);
            combo.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UITheme.BORDER),BorderFactory.createEmptyBorder(4,8,4,0)));
        }
        if(component instanceof JScrollBar) {
            JScrollBar bar=(JScrollBar)component;
            bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
                protected JButton createDecreaseButton(int orientation) { return hidden(); }
                protected JButton createIncreaseButton(int orientation) { return hidden(); }
                private JButton hidden() { JButton b=new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
                protected void paintTrack(Graphics g,JComponent c,Rectangle bounds) { g.setColor(UITheme.BG_APP); g.fillRect(bounds.x,bounds.y,bounds.width,bounds.height); }
                protected void paintThumb(Graphics graphics,JComponent c,Rectangle r) {
                    Graphics2D g=(Graphics2D)graphics.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(new Color(195,195,201)); g.fillRoundRect(r.x+2,r.y+2,Math.max(4,r.width-4),Math.max(4,r.height-4),8,8); g.dispose();
                }
            });
            bar.setPreferredSize(new Dimension(10,10));
        }
        if(component instanceof Container) for(Component child:((Container)component).getComponents()) polish(child);
    }

    public static void shortcut(JComponent root, String key, Runnable action) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        root.getActionMap().put(key, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }
}
