package netnotice.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;

/** Shared typography, colour, vector controls and original notification audio. */
public final class UITheme {

    private UITheme() {}

    public static void exportText(Component owner, String text, String filename) {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setSelectedFile(new java.io.File(filename));
        if (chooser.showSaveDialog(owner) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        java.io.File file = chooser.getSelectedFile();
        if (file.exists() && javax.swing.JOptionPane.showConfirmDialog(owner, "Replace " + file.getName() + "?",
                "Replace file", javax.swing.JOptionPane.YES_NO_OPTION) != javax.swing.JOptionPane.YES_OPTION) return;
        try {
            writeText(file.toPath(), text);
        } catch (java.io.IOException ex) {
            javax.swing.JOptionPane.showMessageDialog(owner, "Could not save: " + ex.getMessage(), "Export failed", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void writeText(java.nio.file.Path path, String text) throws java.io.IOException {
        java.nio.file.Files.write(path,text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    // ==========================================
    // 1. Color Palette — Clean Slate & Vibrant Accents
    // ==========================================

    // Backgrounds
    public static final Color BG_APP        = new Color(250, 250, 252);   // Slate 50
    public static final Color BG_CARD       = Color.WHITE;                // Pure White
    public static final Color BG_CARD_ALT   = new Color(245, 245, 247);   // Slate 100
    public static final Color BG_INPUT      = new Color(250, 250, 252);   // Slate 50 input fill

    // Borders
    public static final Color BORDER        = new Color(226, 226, 231);   // Slate 200
    public static final Color BORDER_LIGHT  = new Color(245, 245, 247);   // Slate 100
    public static final Color BORDER_FOCUS  = new Color(99, 102, 241);    // Indigo 500

    // Text (High Contrast & Legibility)
    public static final Color TEXT_PRIMARY   = new Color(29, 29, 31);     // Slate 900
    public static final Color TEXT_SECONDARY = new Color(81, 81, 84);     // Slate 700
    public static final Color TEXT_MUTED     = new Color(110, 110, 115);  // Slate 500

    // Primary — Indigo / Royal Blue
    public static final Color PRIMARY        = new Color(0, 113, 227);    // Indigo 600
    public static final Color PRIMARY_HOVER  = new Color(0, 95, 198);    // Indigo 700
    public static final Color PRIMARY_BG     = new Color(230, 240, 253);  // Indigo 50

    // Success — Emerald
    public static final Color SUCCESS        = new Color(36, 138, 61);   // Emerald 500
    public static final Color SUCCESS_HOVER  = new Color(5, 150, 105);    // Emerald 600
    public static final Color SUCCESS_BG     = new Color(236, 253, 245);  // Emerald 50

    // Danger — Rose Red
    public static final Color DANGER         = new Color(239, 68, 68);    // Red 500
    public static final Color DANGER_HOVER   = new Color(220, 38, 38);    // Red 600
    public static final Color DANGER_BG      = new Color(254, 242, 242);  // Red 50

    // Purple — Violet
    public static final Color PURPLE         = new Color(73, 98, 160);   // Violet 500
    public static final Color PURPLE_HOVER   = new Color(124, 58, 237);   // Violet 600
    public static final Color PURPLE_BG      = new Color(245, 243, 255);  // Violet 50

    // Amber / Warning
    public static final Color AMBER          = new Color(217, 119, 6);    // Amber 600
    public static final Color AMBER_BG       = new Color(254, 243, 199);  // Amber 100
    public static final Color ORANGE         = new Color(249, 115, 22);   // Orange 500
    public static final Color ORANGE_HOVER   = new Color(234, 88, 12);    // Orange 600
    public static final Color ORANGE_BG      = new Color(255, 247, 237);  // Orange 50

    // Sky Blue / Info
    public static final Color BLUE           = new Color(0, 113, 227);    // Blue 600
    public static final Color BLUE_BG        = new Color(245, 247, 250);  // Blue 50

    // ==========================================
    // 2. Typography Scale
    // ==========================================
    private static final boolean MAC = System.getProperty("os.name", "").startsWith("Mac");
    private static Font interfaceFont(String filename, int fallbackStyle) {
        Font system = javax.swing.UIManager.getFont("Label.font");
        if (system == null) system = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
        if (MAC) return system.deriveFont(fallbackStyle);
        try {
            java.nio.file.Path build = java.nio.file.Paths.get(UITheme.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            java.io.File file = build.resolveSibling("assets").resolve("fonts").resolve(filename).toFile();
            return Font.createFont(Font.TRUETYPE_FONT, file);
        } catch (Exception unavailable) {
            return system.deriveFont(fallbackStyle);
        }
    }
    private static final Font TEXT = interfaceFont("InterVariable.ttf", Font.PLAIN);
    private static final Font SEMIBOLD = interfaceFont("Inter-SemiBold.otf", Font.BOLD);
    private static final Font DISPLAY = interfaceFont("InterDisplay-SemiBold.otf", Font.BOLD);
    public static final Font FONT_READING = interfaceFont("InterDisplay-Regular.otf", Font.PLAIN).deriveFont(20f);
    public static final String FONT_FAMILY = TEXT.getFamily();
    public static final Font FONT_TITLE = DISPLAY.deriveFont(31f);
    public static final Font FONT_SECTION = DISPLAY.deriveFont(19f);
    public static final Font FONT_HEADER = SEMIBOLD.deriveFont(14f);
    public static final Font FONT_BOLD = SEMIBOLD.deriveFont(13f);
    public static final Font FONT_REGULAR = TEXT.deriveFont(14f);
    public static final Font FONT_SMALL = TEXT.deriveFont(12f);
    public static final Font FONT_CAPTION = TEXT.deriveFont(11f);
    public static final Font FONT_MONO    = new Font("Monospaced", Font.PLAIN, 12);

    // ==========================================
    // 3. Borders & Utilities
    // ==========================================
    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        );
    }

    public static Border inputBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        );
    }

    // ==========================================
    // 4. Custom Java2D Vector Icons
    // ==========================================
    public static Icon tinted(Icon original,Color color) {
        return new Icon() {
            public int getIconWidth(){return original.getIconWidth();} public int getIconHeight(){return original.getIconHeight();}
            public void paintIcon(Component c,Graphics g,int x,int y) {
                java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(getIconWidth(),getIconHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics=image.createGraphics();original.paintIcon(c,graphics,0,0);
                graphics.setComposite(java.awt.AlphaComposite.SrcIn);graphics.setColor(color);graphics.fillRect(0,0,getIconWidth(),getIconHeight());graphics.dispose();g.drawImage(image,x,y,null);
            }
        };
    }

    public static final class Icons {
        private Icons() {}

        private static Icon lineIcon(int size, Color color, int kind) {
            return new Icon() {
                public int getIconWidth() { return size; }
                public int getIconHeight() { return size; }
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D a = (Graphics2D) g.create();
                    a.translate(x, y);
                    a.scale(size / 24.0, size / 24.0);
                    a.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    a.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    a.setColor(c.isEnabled() ? color : TEXT_MUTED);
                    if (kind == 0) {
                        a.drawPolygon(new int[]{3, 8, 17, 17, 8, 3}, new int[]{9, 9, 4, 18, 14, 14}, 6);
                        a.drawLine(7, 15, 9, 21); a.drawLine(20, 8, 22, 7); a.drawLine(20, 15, 22, 16);
                    } else if (kind == 1) {
                        a.drawRoundRect(4, 12, 3, 8, 2, 2); a.drawRoundRect(10, 4, 3, 16, 2, 2);
                        a.drawRoundRect(16, 8, 3, 12, 2, 2);
                    } else if (kind == 2) {
                        a.drawOval(6, 3, 7, 7); a.drawArc(3, 13, 13, 12, 0, 180);
                        a.drawArc(14, 3, 6, 7, -80, 160); a.drawArc(15, 13, 7, 10, -10, 100);
                    } else if (kind == 4) {
                        a.drawArc(2,4,20,18,35,110); a.drawArc(6,9,12,10,35,110); a.fillOval(10,17,4,4);
                    } else if (kind == 5) {
                        a.drawRoundRect(4,4,16,16,4,4); a.drawLine(8,9,16,9); a.drawLine(8,15,16,15);
                        a.fillOval(10,7,4,4); a.fillOval(6,13,4,4);
                    } else {
                        a.drawPolygon(new int[]{3, 21, 14, 10, 3}, new int[]{4, 3, 21, 13, 4}, 5);
                        a.drawLine(10, 13, 21, 3);
                    }
                    a.dispose();
                }
            };
        }
        public static Icon broadcast(int size, Color color) { return lineIcon(size, color, 0); }
        public static Icon poll(int size, Color color) { return lineIcon(size, color, 1); }
        public static Icon users(int size, Color color) { return lineIcon(size, color, 2); }
        public static Icon send(int size, Color color) { return lineIcon(size, color, 3); }

        public static Icon network(int size, Color color) { return lineIcon(size,color,4); }
        public static Icon settings(int size, Color color) { return lineIcon(size,color,5); }

        public static Icon clock(int size, Color color) {
            return new Icon() {
                @Override public int getIconWidth() { return size; }
                @Override public int getIconHeight() { return size; }
                @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(color);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawOval(x + 1, y + 1, size - 3, size - 3);
                    // Clock hands
                    int cx = x + size / 2;
                    int cy = y + size / 2;
                    g2.drawLine(cx, cy, cx, cy - size / 4);
                    g2.drawLine(cx, cy, cx + size / 4, cy);
                    g2.dispose();
                }
            };
        }

        public static Icon copy(int size, Color color) {
            return new Icon() {
                @Override public int getIconWidth() { return size; }
                @Override public int getIconHeight() { return size; }
                @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(color);
                    g2.setStroke(new BasicStroke(1.2f));
                    // Back sheet
                    g2.drawRoundRect(x + size / 3, y + 1, size / 2, size * 2 / 3, 2, 2);
                    // Front sheet
                    g2.setColor(BG_CARD);
                    g2.fillRoundRect(x + 1, y + size / 4, size / 2, size * 2 / 3, 2, 2);
                    g2.setColor(color);
                    g2.drawRoundRect(x + 1, y + size / 4, size / 2, size * 2 / 3, 2, 2);
                    g2.dispose();
                }
            };
        }

        public static Icon trash(int size, Color color) {
            return new Icon() {
                @Override public int getIconWidth() { return size; }
                @Override public int getIconHeight() { return size; }
                @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(color);
                    g2.setStroke(new BasicStroke(1.3f));
                    // Lid
                    g2.drawLine(x + 2, y + 3, x + size - 2, y + 3);
                    g2.drawLine(x + size / 3, y + 1, x + (size * 2) / 3, y + 1);
                    // Can body
                    g2.drawRoundRect(x + 3, y + 4, size - 7, size - 6, 2, 2);
                    g2.dispose();
                }
            };
        }

        public static Icon check(int size, Color color) {
            return new Icon() {
                @Override public int getIconWidth() { return size; }
                @Override public int getIconHeight() { return size; }
                @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(color);
                    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(x + 2, y + size / 2, x + size / 3 + 1, y + size - 3);
                    g2.drawLine(x + size / 3 + 1, y + size - 3, x + size - 2, y + 2);
                    g2.dispose();
                }
            };
        }

        public static Icon volume(int size, Color color) {
            return new Icon() {
                @Override public int getIconWidth() { return size; }
                @Override public int getIconHeight() { return size; }
                @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(color);
                    // Speaker body
                    int sw = size / 3;
                    g2.fillRect(x + 1, y + size / 3, sw, size / 3);
                    int[] xs = {x + 1 + sw, x + (size * 2) / 3, x + (size * 2) / 3, x + 1 + sw};
                    int[] ys = {y + size / 3, y + 2, y + size - 2, y + (size * 2) / 3};
                    g2.fillPolygon(xs, ys, 4);
                    // Sound waves
                    g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawArc(x + (size * 2) / 3 - 2, y + size / 4, size / 2, size / 2, -45, 90);
                    g2.dispose();
                }
            };
        }

        public static Icon volumeMute(int size, Color color) {
            return new Icon() {
                @Override public int getIconWidth() { return size; }
                @Override public int getIconHeight() { return size; }
                @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(color);
                    int sw = size / 3;
                    g2.fillRect(x + 1, y + size / 3, sw, size / 3);
                    int[] xs = {x + 1 + sw, x + (size * 2) / 3, x + (size * 2) / 3, x + 1 + sw};
                    int[] ys = {y + size / 3, y + 2, y + size - 2, y + (size * 2) / 3};
                    g2.fillPolygon(xs, ys, 4);
                    // Mute 'X'
                    g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int mx = x + (size * 2) / 3 + 2;
                    int my = y + size / 3 + 1;
                    g2.drawLine(mx, my, mx + 4, my + 4);
                    g2.drawLine(mx + 4, my, mx, my + 4);
                    g2.dispose();
                }
            };
        }
    }

    // ==========================================
    // 5. Synthesized Notification Sound Player
    // ==========================================
    public static final class Sound {
        private Sound() {}
        private static final java.util.prefs.Preferences PREFS = java.util.prefs.Preferences.userNodeForPackage(Sound.class);
        private static volatile boolean soundEnabled = PREFS.getBoolean("soundEnabled", true);
        private static volatile int volume = PREFS.getInt("soundVolume", 35);
        private static final java.util.concurrent.atomic.AtomicBoolean playing = new java.util.concurrent.atomic.AtomicBoolean();
        public static boolean isSoundEnabled() { return soundEnabled; }
        public static void setSoundEnabled(boolean enabled) { soundEnabled=enabled; PREFS.putBoolean("soundEnabled",enabled); }
        public static int getVolume() { return volume; }
        public static void setVolume(int value) { volume=Math.max(0,Math.min(100,value)); PREFS.putInt("soundVolume",volume); }
        /** Original soft two-tone bell; 650 ms, with a smooth attack and decay. */
        public static byte[] samples(int level) {
            int rate=44100; byte[] pcm=new byte[(int)(rate*0.65)*2];
            double gain=Math.max(0,Math.min(100,level))/100.0;
            for(int i=0;i<pcm.length/2;i++) {
                double t=i/(double)rate, value=0;
                for(int note=0;note<2;note++) {
                    double dt=t-note*0.12;
                    if(dt>=0) {
                        double frequency=note==0?880:1174.66;
                        double envelope=Math.min(1,dt/0.015)*Math.exp(-dt*9)*Math.min(1,(0.65-t)/0.05);
                        value+=Math.sin(2*Math.PI*frequency*dt)*envelope*0.24*gain;
                    }
                }
                short sample=(short)(value*32767); pcm[i*2]=(byte)sample; pcm[i*2+1]=(byte)(sample>>8);
            }
            return pcm;
        }
        public static void playAnnouncementChime() {
            if(!soundEnabled || volume==0 || !playing.compareAndSet(false,true)) return;
            Thread worker=new Thread(() -> {
                try {
                    AudioFormat format=new AudioFormat(44100,16,1,true,false);
                    try(SourceDataLine line=AudioSystem.getSourceDataLine(format)) {
                        line.open(format); line.start(); byte[] pcm=samples(volume); line.write(pcm,0,pcm.length); line.drain();
                    }
                } catch(Exception ignored) { /* Silent fallback when no audio device is available. */ }
                finally { playing.set(false); }
            },"NetNotice-SoftBell");
            worker.setDaemon(true); worker.start();
        }
        public static void showSettings(Component owner) {
            javax.swing.JCheckBox enabled=new javax.swing.JCheckBox("Play a sound for new announcements",soundEnabled);
            enabled.setOpaque(false); enabled.setFont(FONT_REGULAR);
            javax.swing.JSlider slider=new javax.swing.JSlider(0,100,volume); slider.setOpaque(false);
            JLabel level=new JLabel("Volume  " + volume + "%"); level.setFont(FONT_SMALL);
            enabled.addActionListener(e -> setSoundEnabled(enabled.isSelected()));
            slider.addChangeListener(e -> { setVolume(slider.getValue()); level.setText("Volume  " + volume + "%"); });
            JButton preview=optionButton("Preview soft bell"); preview.addActionListener(e -> playAnnouncementChime());
            JPanel body=new JPanel(new java.awt.GridLayout(0,1,0,12)); body.setOpaque(false);
            body.add(enabled); body.add(level); body.add(slider); body.add(preview);
            javax.swing.JOptionPane.showMessageDialog(owner,body,"Notification sound",javax.swing.JOptionPane.PLAIN_MESSAGE);
        }
    }

    // ==========================================
    // 5. RoundedCard Component
    // ==========================================
    public static class RoundedCard extends JPanel {
        private int cornerRadius = 12;
        private Color borderColor = BORDER;

        public RoundedCard() {
            this(12, BORDER);
        }

        public RoundedCard(int cornerRadius, Color borderColor) {
            this.cornerRadius = cornerRadius;
            this.borderColor = borderColor;
            setOpaque(false);
            setBackground(BG_CARD);
            setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        }

        public void setBorderColor(Color c) {
            this.borderColor = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Subtle drop shadow
            g2.setColor(new Color(15, 23, 42, 8));
            g2.fillRoundRect(1, 2, w - 2, h - 2, cornerRadius, cornerRadius);

            // Card background
            g2.setColor(getBackground() != null ? getBackground() : BG_CARD);
            g2.fillRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);

            // Border
            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Keep RoundedPanel alias for backward compatibility
    public static class RoundedPanel extends RoundedCard {
        public RoundedPanel() { super(12, BORDER); }
        public RoundedPanel(int radius) { super(radius, BORDER); }
        public RoundedPanel(int radius, Color border) { super(radius, border); }
    }

    // ==========================================
    // 6. RoundedTextField Component
    // ==========================================
    public static class RoundedTextField extends JTextField {
        private static final int RADIUS = 8;
        private boolean focused = false;

        public RoundedTextField() { this(0); }
        public RoundedTextField(int columns) {
            super(columns);
            init();
        }
        public RoundedTextField(String text, int columns) {
            super(text, columns);
            init();
        }

        private void init() {
            setOpaque(false);
            setFont(FONT_REGULAR);
            setForeground(TEXT_PRIMARY);
            setCaretColor(TEXT_PRIMARY);
            setDisabledTextColor(TEXT_SECONDARY);
            setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
            setBackground(BG_CARD);

            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(FocusEvent e) { focused = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Input background
            g2.setColor(isEnabled() ? (getBackground() != null ? getBackground() : BG_INPUT) : BG_CARD_ALT);
            g2.fillRoundRect(0, 0, w - 1, h - 1, RADIUS, RADIUS);

            // Border
            g2.setColor(focused ? BORDER_FOCUS : (isEnabled() ? BORDER : BORDER_LIGHT));
            g2.setStroke(new BasicStroke(focused ? 1.5f : 1.0f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, RADIUS, RADIUS);

            g2.dispose();
            super.paintComponent(g);
        }

        @Override protected void paintBorder(Graphics g) {}
    }

    // ==========================================
    // 7. FlatButton Component
    // ==========================================
    public static class FlatButton extends JButton {
        private Color normalBg;
        private Color hoverBg;
        private Color normalFg;
        private boolean isHovered = false;
        private boolean isPressed = false;
        private int cornerRadius = 13;

        public FlatButton(String text, Color bg, Color hoverBg, Color fg) {
            super(text);
            this.normalBg = bg;
            this.hoverBg = hoverBg;
            this.normalFg = fg;

            setFont(FONT_REGULAR);
            setForeground(fg);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(7, 13, 7, 13));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { if (isEnabled()) { isHovered = true; repaint(); } }
                @Override public void mouseExited(MouseEvent e) { isHovered = false; isPressed = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { if (isEnabled()) { isPressed = true; repaint(); } }
                @Override public void mouseReleased(MouseEvent e) { isPressed = false; repaint(); }
            });
        }

        public void setColors(Color bg, Color hoverBg, Color fg) {
            this.normalBg = bg;
            this.hoverBg = hoverBg;
            this.normalFg = fg;
            setForeground(fg);
            repaint();
        }

        public void setCornerRadius(int r) {
            this.cornerRadius = r;
            repaint();
        }

        public void setCompact(boolean compact) {
            if (compact) {
                setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                setFont(FONT_SMALL);
            } else {
                setBorder(BorderFactory.createEmptyBorder(7, 13, 7, 13));
                setFont(FONT_BOLD);
            }
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (!isEnabled()) {
                g2.setColor(new Color(245, 245, 247));
                g2.fillRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);
                super.setForeground(TEXT_MUTED);
            } else {
                Color currentBg = isPressed ? hoverBg.darker() : (isHovered ? hoverBg : normalBg);

                // Draw button body
                g2.setColor(currentBg);
                g2.fillRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);

                // Subtle outline if light button
                if (normalBg.equals(BG_CARD) || normalBg.equals(BG_CARD_ALT)) {
                    g2.setColor(isHovered ? PRIMARY : BORDER);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);
                }

                super.setForeground(normalFg);
            }

            if (hasFocus()) {
                g2.setColor(PRIMARY);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(2, 2, w - 5, h - 5, cornerRadius, cornerRadius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Button factories
    public static FlatButton primaryButton(String text) {
        return new FlatButton(text, PRIMARY, PRIMARY_HOVER, Color.WHITE);
    }

    public static FlatButton successButton(String text) {
        return new FlatButton(text, SUCCESS, SUCCESS_HOVER, Color.WHITE);
    }

    public static FlatButton dangerButton(String text) {
        return new FlatButton(text, DANGER, DANGER_HOVER, Color.WHITE);
    }

    public static FlatButton purpleButton(String text) {
        return new FlatButton(text, PURPLE, PURPLE_HOVER, Color.WHITE);
    }

    public static FlatButton optionButton(String text) {
        FlatButton btn = new FlatButton(text, BG_CARD_ALT, new Color(232,232,237), TEXT_PRIMARY);
        btn.setFont(FONT_BOLD);
        btn.setCornerRadius(10);
        return btn;
    }

    // ==========================================
    // 8. StatusBadge Component
    // ==========================================
    public static class StatusBadge extends JLabel {
        private Color bgColor;
        private Color fgColor;

        public StatusBadge(String text, Color bg, Color fg) {
            super(text);
            this.bgColor = bg;
            this.fgColor = fg;
            setFont(FONT_REGULAR);
            setForeground(fg);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        }

        public void setStatus(String text, Color bg, Color fg) {
            setText(text);
            this.bgColor = bg;
            this.fgColor = fg;
            setForeground(fg);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ==========================================
    // 9. Interactive Visual Poll Progress Bar
    // ==========================================
    public static class PollProgressBar extends JPanel {
        private ActivityType activityType = ActivityType.POLL;
        public void setActivityType(ActivityType type) { activityType = type; }
        private char optionLetter = 'A';
        private String optionText = "";
        private int votes = 0;
        private int totalVotes = 0;
        private boolean isMyVote = false;
        private boolean isLeader = false;

        public PollProgressBar(char letter, String text) {
            this.optionLetter = letter;
            this.optionText = text;
            setOpaque(false);
            setPreferredSize(new Dimension(280, 42));
            setMinimumSize(new Dimension(180, 36));
        }

        public void updateData(String text, int votes, int totalVotes, boolean isMyVote, boolean isLeader) {
            this.optionText = text;
            this.votes = votes;
            this.totalVotes = totalVotes;
            this.isMyVote = isMyVote;
            this.isLeader = isLeader;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int pad = 4;

            // Background card
            g2.setColor(isMyVote ? SUCCESS_BG : BG_CARD);
            g2.fillRoundRect(pad, pad, w - pad * 2, h - pad * 2, 8, 8);
            g2.setColor(isMyVote ? SUCCESS : (isLeader && totalVotes > 0 ? PRIMARY : BORDER));
            g2.setStroke(new BasicStroke(isMyVote ? 1.5f : 1.0f));
            g2.drawRoundRect(pad, pad, w - pad * 2, h - pad * 2, 8, 8);

            // Progress bar track
            double pct = totalVotes > 0 ? ((double) votes / totalVotes) : 0.0;
            int barY = h - pad - 6;
            int barW = w - pad * 2 - 8;
            int fillW = (int) Math.round(barW * pct);

            // Fill background track
            g2.setColor(BG_CARD_ALT);
            g2.fillRoundRect(pad + 4, barY, barW, 4, 3, 3);

            // Filled bar
            if (fillW > 0) {
                g2.setColor(isMyVote ? SUCCESS : (isLeader ? PRIMARY : PURPLE));
                g2.fillRoundRect(pad + 4, barY, Math.max(fillW, 4), 4, 3, 3);
            }

            // Letter circle badge
            int circleD = 18;
            int circleX = pad + 6;
            int circleY = pad + 4;
            g2.setColor(isMyVote ? SUCCESS : PRIMARY);
            g2.fillOval(circleX, circleY, circleD, circleD);
            g2.setColor(Color.WHITE);
            g2.setFont(FONT_BOLD);
            FontMetrics fm = g2.getFontMetrics();
            String lStr = String.valueOf(optionLetter);
            g2.drawString(lStr, circleX + (circleD - fm.stringWidth(lStr)) / 2, circleY + 13);

            // Option text
            g2.setColor(TEXT_PRIMARY);
            g2.setFont(FONT_BOLD);
            int textX = circleX + circleD + 8;
            int maxTextW = w - textX - g2.getFontMetrics(FONT_SMALL).stringWidth(activityType.count(votes) + " (100%)") - 20;
            String displayTxt = optionText;
            if (fm.stringWidth(displayTxt) > maxTextW && maxTextW > 20) {
                while (displayTxt.length() > 3 && fm.stringWidth(displayTxt + "...") > maxTextW) {
                    displayTxt = displayTxt.substring(0, displayTxt.length() - 1);
                }
                displayTxt += "...";
            }
            g2.drawString(displayTxt, textX, pad + 17);

            // Vote & percentage badge
            int pctInt = (int) Math.round(pct * 100.0);
            String statStr = activityType.count(votes) + " (" + pctInt + "%)";
            g2.setFont(FONT_SMALL);
            g2.setColor(isMyVote ? SUCCESS : TEXT_SECONDARY);
            FontMetrics fmStat = g2.getFontMetrics();
            g2.drawString(statStr, w - pad - 6 - fmStat.stringWidth(statStr), pad + 17);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ==========================================
    // 10. Avatar / Client Initial Badge
    // ==========================================
    public static class AvatarBadge extends JLabel {
        private static final Color[] AVATAR_COLORS = {
            new Color(0, 113, 227),  // Indigo
            new Color(36, 138, 61), // Emerald
            new Color(245, 158, 11), // Amber
            new Color(239, 68, 68),  // Rose
            new Color(73, 98, 160), // Purple
            new Color(14, 165, 233), // Sky
            new Color(236, 72, 153)  // Pink
        };

        private String name;
        private Color avatarColor;

        public AvatarBadge(String name) {
            this.name = name;
            int hash = Math.abs(name != null ? name.hashCode() : 0);
            this.avatarColor = AVATAR_COLORS[hash % AVATAR_COLORS.length];
            setOpaque(false);
            setPreferredSize(new Dimension(24, 24));
        }

        public void setName(String name) {
            this.name = name;
            int hash = Math.abs(name != null ? name.hashCode() : 0);
            this.avatarColor = AVATAR_COLORS[hash % AVATAR_COLORS.length];
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int d = Math.min(getWidth(), getHeight()) - 2;
            g2.setColor(avatarColor);
            g2.fillOval(1, 1, d, d);

            g2.setColor(Color.WHITE);
            g2.setFont(FONT_BOLD);
            String initial = (name != null && !name.isEmpty()) ? name.substring(0, 1).toUpperCase() : "?";
            FontMetrics fm = g2.getFontMetrics();
            int tx = 1 + (d - fm.stringWidth(initial)) / 2;
            int ty = 1 + ((d - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(initial, tx, ty);

            g2.dispose();
        }
    }

    // ==========================================
    // 11. Helper Factories
    // ==========================================
    public static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SECTION);
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    public static JLabel captionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_CAPTION);
        label.setForeground(TEXT_MUTED);
        return label;
    }

    public static Component createGap(int height) {
        return Box.createRigidArea(new Dimension(0, height));
    }

    public static JScrollPane scrollPane(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(BG_APP);
        return sp;
    }
}
