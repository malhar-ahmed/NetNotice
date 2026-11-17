package netnotice.common;

import java.awt.*;
import javax.swing.*;

/** Illustrated section header matching the Classroom join invitation. */
public final class FeatureHeader extends JPanel {
    public enum Art {
        NOTICE(0xe3efff,0xeee8fc,0x697bd2), POLL(0xe1f6ed,0xe2eefb,0x318e80),
        MESSAGE(0xe1f4ff,0xe7f3eb,0x518a9d), QUIZ(0xf0e7ff,0xffeef1,0x9464c1), PEOPLE(0xffefdf,0xfce8ed,0xbf7959);
        final Color start, end, ink;
        Art(int a,int b,int c) { start=new Color(a); end=new Color(b); ink=new Color(c); }
    }
    private Art art;
    private final JLabel heading = new JLabel();
    private final JTextArea caption = WorkspaceUI.text("", 2);

    public FeatureHeader(Art art, String title, String detail) {
        super(new BorderLayout(16,0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(17,20,15,16));
        JPanel words = new JPanel(new BorderLayout(0,5)); words.setOpaque(false);
        heading.setFont(UITheme.FONT_TITLE.deriveFont(22f));
        caption.setFocusable(false); caption.setOpaque(false); caption.setForeground(UITheme.TEXT_SECONDARY);
        words.add(heading,BorderLayout.NORTH); words.add(caption,BorderLayout.CENTER);
        add(words,BorderLayout.CENTER);
        add(Box.createHorizontalStrut(83),BorderLayout.EAST);
        update(art,title,detail);
    }

    public void update(Art art,String title,String detail) {
        this.art=art; heading.setText(title); caption.setText(detail); repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g=(Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0,0,art.start,getWidth(),getHeight(),art.end));
        g.fillRoundRect(0,0,getWidth(),getHeight(),22,22);
        g.translate(getWidth()-97,getHeight()/2-27);
        g.rotate(.13,45,26);
        g.setColor(new Color(art.ink.getRed(),art.ink.getGreen(),art.ink.getBlue(),85));
        g.fillRoundRect(13,-4,66,49,12,12);
        g.rotate(-.19,39,30);
        g.setColor(Color.WHITE); g.fillRoundRect(2,7,69,48,12,12);
        g.setColor(art.ink); g.setStroke(new BasicStroke(3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        switch(art) {
            case POLL -> { for(int i=0;i<3;i++) g.fillRoundRect(14+i*14,34-i*7,9,11+i*7,4,4); }
            case QUIZ -> { for(int i=0;i<2;i++){int y=21+i*14; g.drawLine(13,y,16,y+3);g.drawLine(16,y+3,22,y-4);g.drawLine(29,y,56,y);} }
            case PEOPLE -> { for(int i=0;i<2;i++){ int x=14+i*22;g.fillOval(x+2,17,13,13);g.fillRoundRect(x-1,33,19,11,9,9);} }
            case MESSAGE -> {
                g.drawRoundRect(14,17,40,21,10,10);
                g.drawLine(23,39,20,44);
                g.drawLine(20,44,31,39);
                for(int x=24; x<=42; x+=9) g.fillOval(x,26,4,4);
            }
            case NOTICE -> { g.fillOval(13,18,9,9);g.drawLine(29,22,53,22);g.drawLine(14,36,54,36); }
        }
        g.dispose();
    }
}
