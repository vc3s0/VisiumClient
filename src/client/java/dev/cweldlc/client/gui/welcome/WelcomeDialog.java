package dev.cweldlc.client.gui.welcome;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

public class WelcomeDialog {

    private static boolean shown = false;

    public static synchronized void showWelcome() {
        if (shown) return;
        shown = true;

        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        try {
            JDialog dialog = new JDialog((Frame) null, "VisiumClient - Witaj", true);
            dialog.setUndecorated(true);
            int width = 450;
            int height = 270;
            dialog.setSize(width, height);
            dialog.setLocationRelativeTo(null);
            dialog.setAlwaysOnTop(true);
            dialog.setShape(new RoundRectangle2D.Double(0, 0, width, height, 24, 24));
            dialog.setBackground(new Color(0, 0, 0, 0));

            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                    // Deep obsidian solid background
                    g2.setColor(new Color(14, 14, 14));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);

                    // Subtle luxury rim
                    g2.setColor(new Color(36, 36, 36));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);

                    // Pill badge at top
                    int pillW = 126;
                    int pillH = 24;
                    int pillX = (getWidth() - pillW) / 2;
                    int pillY = 24;
                    g2.setColor(new Color(26, 26, 26));
                    g2.fillRoundRect(pillX, pillY, pillW, pillH, 12, 12);

                    g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                    g2.setColor(new Color(210, 210, 210));
                    FontMetrics fmPill = g2.getFontMetrics();
                    String pillText = "VISIUM CLIENT";
                    g2.drawString(pillText, pillX + (pillW - fmPill.stringWidth(pillText)) / 2, pillY + 16);

                    // Header Title
                    g2.setFont(new Font("SansSerif", Font.BOLD, 22));
                    g2.setColor(Color.WHITE);
                    FontMetrics fmTitle = g2.getFontMetrics();
                    String title = "Witaj w VisiumClient!";
                    g2.drawString(title, (getWidth() - fmTitle.stringWidth(title)) / 2, 88);

                    // Subtitle
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    g2.setColor(new Color(156, 163, 175));
                    FontMetrics fmSub = g2.getFontMetrics();
                    String sub = "Wersja 1.0.0 • Fabric 1.21.4 • LiquidGlass Engine";
                    g2.drawString(sub, (getWidth() - fmSub.stringWidth(sub)) / 2, 114);

                    // User greeting
                    String username = System.getProperty("user.name", "Gracz");
                    String user = "Zalogowano jako: " + username;
                    g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                    g2.setColor(new Color(229, 231, 235));
                    FontMetrics fmUser = g2.getFontMetrics();
                    g2.drawString(user, (getWidth() - fmUser.stringWidth(user)) / 2, 148);

                    g2.dispose();
                }
            };
            panel.setLayout(null);

            // Close button (X) top right
            JButton closeBtn = new JButton("✕") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (getModel().isRollover()) {
                        g2.setColor(new Color(239, 68, 68));
                    } else {
                        g2.setColor(new Color(36, 36, 36));
                    }
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                    g2.setColor(getModel().isRollover() ? Color.WHITE : new Color(160, 160, 160));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("✕", (getWidth() - fm.stringWidth("✕")) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2.dispose();
                }
            };
            closeBtn.setBounds(width - 34, 14, 20, 20);
            closeBtn.setBorderPainted(false);
            closeBtn.setContentAreaFilled(false);
            closeBtn.setFocusPainted(false);
            closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeBtn.addActionListener(e -> {
                dialog.dispose();
            });
            panel.add(closeBtn);

            // Primary Launch Button
            JButton launchBtn = new JButton("Uruchom klienta (4s)") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (getModel().isPressed()) {
                        g2.setColor(new Color(210, 210, 210));
                    } else if (getModel().isRollover()) {
                        g2.setColor(new Color(255, 255, 255));
                    } else {
                        g2.setColor(new Color(240, 240, 240));
                    }
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                    g2.setColor(new Color(15, 15, 15));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2.dispose();
                }
            };
            launchBtn.setBounds((width - 210) / 2, 192, 210, 38);
            launchBtn.setBorderPainted(false);
            launchBtn.setContentAreaFilled(false);
            launchBtn.setFocusPainted(false);
            launchBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            launchBtn.addActionListener(e -> dialog.dispose());
            panel.add(launchBtn);

            // Enter / Space key closes dialog and proceeds
            dialog.getRootPane().setDefaultButton(launchBtn);
            dialog.getRootPane().registerKeyboardAction(
                    e -> dialog.dispose(),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW
            );

            // Window drag listener
            final Point[] dragPoint = new Point[1];
            panel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    dragPoint[0] = e.getPoint();
                }
            });
            panel.addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragPoint[0] != null) {
                        Point curr = dialog.getLocation();
                        dialog.setLocation(curr.x + e.getX() - dragPoint[0].x, curr.y + e.getY() - dragPoint[0].y);
                    }
                }
            });

            dialog.add(panel);

            // Auto-launch countdown timer (4 seconds)
            final int[] countdown = {4};
            Timer timer = new Timer(1000, null);
            timer.addActionListener(e -> {
                countdown[0]--;
                if (countdown[0] <= 0) {
                    timer.stop();
                    dialog.dispose();
                } else {
                    launchBtn.setText("Uruchom klienta (" + countdown[0] + "s)");
                    launchBtn.repaint();
                }
            });
            timer.start();

            // Display dialog modally; blocks until user clicks or timer expires
            dialog.setVisible(true);
            timer.stop();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
