package netnotice.common;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

/**
 * UITheme: Centralized modern design tokens, high-contrast palette,
 * typography, and custom styled components for NetNotice.
 */
public final class UITheme {

    private UITheme() {}

    // ==========================================
    // High-Contrast Color Palette
    // ==========================================
    public static final Color BG_APP = new Color(241, 245, 249);       // Slate 100
    public static final Color BG_CARD = Color.WHITE;                    // Pure White
    public static final Color BG_CARD_ALT = new Color(248, 250, 252);   // Slate 50
    public static final Color BORDER = new Color(203, 213, 225);        // Slate 300
    public static final Color BORDER_LIGHT = new Color(226, 232, 240);  // Slate 200

    // Text Colors (High Contrast - WCAG AAA compliant)
    public static final Color TEXT_PRIMARY = new Color(15, 23, 42);     // Slate 900 (Deep, sharp)
    public static final Color TEXT_SECONDARY = new Color(51, 65, 85);   // Slate 700 (Very readable)
    public static final Color TEXT_MUTED = new Color(100, 116, 139);    // Slate 500

    // Action & Status Colors
    public static final Color PRIMARY = new Color(37, 99, 235);         // Royal Blue 600
    public static final Color PRIMARY_HOVER = new Color(29, 78, 216);   // Blue 700
    public static final Color PRIMARY_BG = new Color(239, 246, 255);    // Blue 50

    public static final Color SUCCESS = new Color(22, 163, 74);         // Emerald Green 600
    public static final Color SUCCESS_HOVER = new Color(21, 128, 61);   // Green 700
    public static final Color SUCCESS_BG = new Color(240, 253, 244);    // Green 50

    public static final Color DANGER = new Color(220, 38, 38);          // Red 600
    public static final Color DANGER_HOVER = new Color(185, 28, 28);    // Red 700
    public static final Color DANGER_BG = new Color(254, 242, 242);     // Red 50

    public static final Color PURPLE = new Color(124, 58, 237);         // Violet 600
    public static final Color PURPLE_HOVER = new Color(109, 40, 217);   // Violet 700
    public static final Color PURPLE_BG = new Color(245, 243, 255);     // Violet 50

    public static final Color AMBER = new Color(180, 83, 9);            // Amber 700 (High contrast)
    public static final Color AMBER_BG = new Color(254, 252, 232);      // Yellow 50

    // ==========================================
    // Typography
    // ==========================================
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);

    // ==========================================
    // Borders
    // ==========================================
    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)
        );
    }

    public static Border inputBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        );
    }

    // ==========================================
    // Modern Flat Button Component
    // ==========================================
    public static class FlatButton extends JButton {
        private Color normalBg;
        private Color hoverBg;
        private Color normalFg;
        private boolean isHovered = false;

        public FlatButton(String text, Color bg, Color hoverBg, Color fg) {
            super(text);
            this.normalBg = bg;
            this.hoverBg = hoverBg;
            this.normalFg = fg;

            setFont(FONT_BOLD);
            setForeground(fg);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) {
                        isHovered = true;
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }

        public void setColors(Color bg, Color hoverBg, Color fg) {
            this.normalBg = bg;
            this.hoverBg = hoverBg;
            this.normalFg = fg;
            setForeground(fg);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (!isEnabled()) {
                g2.setColor(new Color(226, 232, 240)); // Disabled light gray
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(148, 163, 184)); // Disabled text
                super.setForeground(new Color(148, 163, 184));
            } else {
                g2.setColor(isHovered ? hoverBg : normalBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.setForeground(normalFg);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Factory methods for standard buttons
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
        FlatButton btn = new FlatButton(text, BG_CARD, PRIMARY_BG, TEXT_PRIMARY);
        btn.setFont(FONT_BOLD);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        return btn;
    }

    // ==========================================
    // Status Pill Component
    // ==========================================
    public static class StatusBadge extends JLabel {
        private Color bgColor;
        private Color fgColor;

        public StatusBadge(String text, Color bg, Color fg) {
            super(text);
            this.bgColor = bg;
            this.fgColor = fg;
            setFont(FONT_BOLD);
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
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
