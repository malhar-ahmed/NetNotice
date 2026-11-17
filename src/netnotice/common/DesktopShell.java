package netnotice.common;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/** Quiet split-view navigation with a compact selector on small windows. */
public final class DesktopShell extends JPanel {
    private final JPanel sidebar = new JPanel(new BorderLayout()) {
        protected void paintComponent(Graphics g) {
            Graphics2D a=(Graphics2D)g.create();a.setPaint(new GradientPaint(0,0,new Color(244,245,248),getWidth(),getHeight(),new Color(229,233,241)));
            a.fillRect(0,0,getWidth(),getHeight());a.dispose();
        }
    };
    private final JPanel compact = new JPanel(new GridLayout(1, 0, 4, 0));
    private final List<JToggleButton> segments = new ArrayList<>();
    private final List<UITheme.FlatButton> navigation = new ArrayList<>();
    private final JTabbedPane pages;

    public DesktopShell(JPanel oldRoot, JTabbedPane pages, String role, NotificationCenter notifications) {
        super(new BorderLayout());
        this.pages=pages;
        setBackground(UITheme.BG_APP);
        sidebar.setBackground(new Color(240,240,244));
        sidebar.setPreferredSize(new Dimension(208,500));
        sidebar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,0,1,UITheme.BORDER),BorderFactory.createEmptyBorder(24,14,22,14)));
        JLabel roleLabel = new JLabel(role + " workspace");
        roleLabel.setFont(UITheme.FONT_HEADER);
        roleLabel.setForeground(UITheme.TEXT_SECONDARY);
        roleLabel.setBorder(BorderFactory.createEmptyBorder(0,8,16,0));
        JPanel list=new JPanel(); list.setLayout(new BoxLayout(list,BoxLayout.Y_AXIS)); list.setOpaque(false);
        compact.setBackground(new Color(238, 239, 245));
        compact.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
        ButtonGroup group = new ButtonGroup();
        for(int i=0;i<pages.getTabCount();i++) {
            final int index=i; String label=pages.getTitleAt(i).trim(); String shortLabel = label.equals("Polls & Quizzes") ? "Activities" : label.equals("Activity log") ? "Log" : label;
            JToggleButton segment = new JToggleButton(shortLabel) {
                @Override protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (isSelected() || getModel().isRollover()) {
                        g.setColor(isSelected() ? Color.WHITE : new Color(227, 231, 240));
                        g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                    }
                    if (hasFocus()) {
                        g.setColor(UITheme.PRIMARY);
                        g.drawRoundRect(1,1,getWidth()-3,getHeight()-3,12,12);
                    }
                    g.dispose(); super.paintComponent(graphics);
                }
            };
            segment.setFont(UITheme.FONT_BOLD);
            segment.setOpaque(false); segment.setContentAreaFilled(false);
            segment.setBorderPainted(false); segment.setFocusPainted(false);
            segment.setBorder(BorderFactory.createEmptyBorder(7,3,7,3));
            segment.setToolTipText(label);
            segment.addActionListener(e -> pages.setSelectedIndex(index));
            group.add(segment); segments.add(segment); compact.add(segment);
            UITheme.FlatButton button=new UITheme.FlatButton(label,new Color(240,240,244),new Color(228,228,233),UITheme.TEXT_PRIMARY);
            button.setIcon(pages.getIconAt(i)); button.setIconTextGap(11); button.setHorizontalAlignment(JButton.LEFT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE,36)); button.setPreferredSize(new Dimension(184,36));
            button.addActionListener(e -> pages.setSelectedIndex(index)); list.add(button); list.add(Box.createVerticalStrut(5)); navigation.add(button);
        }
        JPanel upper=new JPanel(new BorderLayout()); upper.setOpaque(false); upper.add(roleLabel,BorderLayout.NORTH); upper.add(list,BorderLayout.CENTER);
        sidebar.add(upper,BorderLayout.NORTH);
        add(sidebar,BorderLayout.WEST);
        JPanel detailPanel=new JPanel(new BorderLayout()); detailPanel.setOpaque(false);
        JPanel chrome = new JPanel(new BorderLayout());
        JPanel toolbar = new JPanel(new BorderLayout(12, 0)) {
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setPaint(new GradientPaint(0,0,new Color(254,254,255),0,getHeight(),new Color(239,239,243)));
                g.fillRect(0,0,getWidth(),getHeight()); g.dispose();
            }
        };
        toolbar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,UITheme.BORDER),
                BorderFactory.createEmptyBorder(9,18,9,15)));
        JLabel name = new JLabel("NetNotice",UITheme.Icons.broadcast(22,UITheme.PRIMARY),JLabel.LEFT);
        name.setFont(UITheme.FONT_SECTION); name.setIconTextGap(10);
        toolbar.add(name,BorderLayout.WEST);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); tools.setOpaque(false);
        tools.add(notifications.toolbarButton());
        toolbar.add(tools,BorderLayout.EAST);
        JPanel top = new JPanel(new BorderLayout());
        top.add(toolbar, BorderLayout.NORTH);
        top.add(compact, BorderLayout.SOUTH);
        chrome.add(top,BorderLayout.NORTH);
        chrome.add(notifications,BorderLayout.SOUTH); add(chrome,BorderLayout.NORTH);
        JLabel heading = new JLabel(); heading.setFont(UITheme.FONT_TITLE);
        heading.setBorder(BorderFactory.createEmptyBorder(22,27,0,16));
        detailPanel.add(heading,BorderLayout.NORTH); detailPanel.add(pages,BorderLayout.CENTER);
        BorderLayout previous=(BorderLayout)oldRoot.getLayout(); Component footer=previous.getLayoutComponent(BorderLayout.SOUTH);
        if(footer!=null && role.equals("Student")) {
            footer.setFont(UITheme.FONT_SMALL); footer.setBackground(Color.WHITE);
            detailPanel.add(footer,BorderLayout.SOUTH);
        }
        add(detailPanel,BorderLayout.CENTER);
        Runnable select=() -> {
            heading.setText(pages.getTitleAt(pages.getSelectedIndex()).trim());

            for(int i=0;i<navigation.size();i++) {
                boolean selected=i==pages.getSelectedIndex();
                segments.get(i).setSelected(selected);
                segments.get(i).setForeground(selected ? UITheme.PRIMARY : UITheme.TEXT_SECONDARY);
                navigation.get(i).setColors(selected?new Color(214,223,239):new Color(0,0,0,0),
                    new Color(208,217,234),UITheme.TEXT_PRIMARY);
                navigation.get(i).setIcon(UITheme.tinted(pages.getIconAt(i),selected?UITheme.PRIMARY:UITheme.TEXT_SECONDARY));
            }
        };
        pages.addChangeListener(e -> select.run()); select.run();
        pages.addPropertyChangeListener("indexForTitle",e -> {

            for(int i=0;i<navigation.size();i++) {
                String title=pages.getTitleAt(i).trim(); navigation.get(i).setText(title);
                if (i == 0) segments.get(i).setText(title);
            }
            select.run();
        });
        WorkspaceUI.polish(this);
    }
    @Override public void doLayout() {
        boolean wide=getWidth()>=820;
        sidebar.setVisible(wide); compact.setVisible(!wide);
        super.doLayout();
    }
}
