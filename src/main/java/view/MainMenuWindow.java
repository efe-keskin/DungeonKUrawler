package view;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import engine.GameEngine;

/**
 * Main menu hub with a fantasy dungeon backdrop: loads {@code /image_781c2e.jpg} when present, or
 * paints a high-detail procedural scene (corridor, torches, knight + sorcerer, title). UC-1 flows
 * unchanged (Start Game → engine + {@link GameWindow}, etc.).
 */
public class MainMenuWindow extends JFrame {

    private static final int PREF_W = 920;
    private static final int PREF_H = 560;

    public MainMenuWindow() {
        setTitle("Dungeon Krawler — Main Menu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        FantasyMenuBackgroundPanel background = new FantasyMenuBackgroundPanel();
        background.setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        buttonPanel.setOpaque(false);
        // Lower half: side margins keep center characters visible; bottom weight for composition.
        buttonPanel.setBorder(new EmptyBorder(0, 120, 32, 120));

        JButton start = new JButton("START GAME");
        RetroTheme.styleRetroButton(start, RetroTheme.BTN_PRIMARY);
        start.addActionListener(e -> {
            System.out.println("Game Started");
            GameEngine engine = new GameEngine();
            dispose();
            new GameWindow(engine).setVisible(true);
        });

        JButton load = new JButton("LOAD MAP");
        RetroTheme.styleRetroButton(load, RetroTheme.BTN_SECONDARY);
        load.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Load Map is not implemented yet.",
                "Load Map",
                JOptionPane.INFORMATION_MESSAGE));

        JButton help = new JButton("VIEW HELP");
        RetroTheme.styleRetroButton(help, RetroTheme.BTN_ACCENT);
        help.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Build Mode — Arrow keys or WASD to move.\n"
                        + "The engine handles all rules; UI only forwards input.",
                "Help",
                JOptionPane.INFORMATION_MESSAGE));

        JButton exit = new JButton("EXIT");
        RetroTheme.styleRetroButton(exit, RetroTheme.BTN_DANGER);
        exit.addActionListener(e -> System.exit(0));

        buttonPanel.add(start);
        buttonPanel.add(load);
        buttonPanel.add(help);
        buttonPanel.add(exit);

        background.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(background);
        pack();
        setSize(PREF_W, PREF_H);
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    /**
     * Full-bleed background: optional asset from classpath, else procedural fantasy art with title
     * overlay. Characters face the viewer; corridor, torches, rim light, floor grid.
     */
    private static final class FantasyMenuBackgroundPanel extends JPanel {

        private static final String ASSET_PATH = "/image_781c2e.jpg";
        private BufferedImage loadedArt;

        FantasyMenuBackgroundPanel() {
            setOpaque(true);
            try (InputStream in = FantasyMenuBackgroundPanel.class.getResourceAsStream(ASSET_PATH)) {
                if (in != null) {
                    loadedArt = ImageIO.read(in);
                }
            } catch (Exception ignored) {
                loadedArt = null;
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(PREF_W, PREF_H);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth();
            int h = getHeight();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                if (loadedArt != null) {
                    g2.drawImage(loadedArt, 0, 0, w, h, null);
                    // Slight vignette so UI text stays readable over any photograph.
                    paintVignette(g2, w, h);
                } else {
                    paintProceduralDungeonScene(g2, w, h);
                }
                drawTitleOverlay(g2, w, h);
            } finally {
                g2.dispose();
            }
        }

        private static void paintVignette(Graphics2D g2, int w, int h) {
            RadialGradientPaint vignette = new RadialGradientPaint(
                    new Point2D.Float(w * 0.5f, h * 0.45f),
                    Math.max(w, h) * 0.65f,
                    new float[] { 0.35f, 1f },
                    new Color[] { new Color(0, 0, 0, 0), new Color(0, 0, 0, 120) });
            g2.setPaint(vignette);
            g2.fillRect(0, 0, w, h);
        }

        /**
         * Stylized dungeon corridor: stone, torches, chiaroscuro, two heroes, etched floor grid.
         */
        private static void paintProceduralDungeonScene(Graphics2D g2, int w, int h) {
            // Deep atmospheric base
            GradientPaint sky = new GradientPaint(
                    0, 0, new Color(8, 10, 22),
                    0, h, new Color(18, 12, 28));
            g2.setPaint(sky);
            g2.fillRect(0, 0, w, h);

            // Corridor floor (perspective)
            int horizon = h / 3;
            Polygon floor = new Polygon();
            floor.addPoint(0, h);
            floor.addPoint(w, h);
            floor.addPoint(w - w / 5, horizon);
            floor.addPoint(w / 5, horizon);
            GradientPaint floorGrad = new GradientPaint(
                    0, horizon, new Color(35, 32, 40),
                    0, h, new Color(12, 10, 14));
            g2.setPaint(floorGrad);
            g2.fill(floor);

            // Etched grid on stone tiles (subtle mechanics hint)
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(0, 0, 0, 50));
            for (int i = 0; i < 14; i++) {
                float t = i / 14f;
                int y = horizon + (int) ((h - horizon) * (0.15 + t * 0.85));
                int x1 = (int) (w * 0.5 * (1 - t) * 0.35);
                int x2 = w - x1;
                g2.drawLine(x1, y, x2, y);
            }
            for (int j = 0; j < 10; j++) {
                float t = j / 10f;
                int x = (int) (w * 0.5 * t * 0.9 + w * 0.05);
                g2.drawLine(x, h, w / 2, horizon);
                g2.drawLine(w - x, h, w / 2, horizon);
            }

            // Side walls (carved stone)
            Path2D leftWall = new Path2D.Double();
            leftWall.moveTo(0, 0);
            leftWall.lineTo(w * 0.22, horizon);
            leftWall.lineTo(w * 0.28, h);
            leftWall.lineTo(0, h);
            leftWall.closePath();
            g2.setColor(new Color(28, 26, 34));
            g2.fill(leftWall);
            g2.setColor(new Color(45, 42, 52));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(leftWall);

            Path2D rightWall = new Path2D.Double();
            rightWall.moveTo(w, 0);
            rightWall.lineTo(w - w * 0.22, horizon);
            rightWall.lineTo(w - w * 0.28, h);
            rightWall.lineTo(w, h);
            rightWall.closePath();
            g2.setColor(new Color(26, 24, 32));
            g2.fill(rightWall);
            g2.draw(rightWall);

            // Wall relief / bricks
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(0, 0, 0, 35));
            for (int r = 0; r < 8; r++) {
                int y = 40 + r * (horizon - 40) / 8;
                g2.drawLine(0, y, (int) (w * 0.2), y + 10);
                g2.drawLine(w, y, (int) (w * 0.8), y + 10);
            }

            // Distant arch / corridor depth
            g2.setColor(new Color(6, 5, 10));
            g2.fillRoundRect(w / 2 - 90, horizon - 40, 180, 70, 24, 24);

            // Torches (warm pools + rim)
            drawTorch(g2, (int) (w * 0.12), horizon + 30, 90);
            drawTorch(g2, (int) (w * 0.88), horizon + 30, 90);
            drawTorch(g2, w / 2, horizon - 10, 70);

            // --- Knight (left): plate, forward gaze, glowing blade ---
            int kx = (int) (w * 0.18);
            int ky = horizon + 50;
            // Rim light from torch (right side of figure)
            RadialGradientPaint knightRim = new RadialGradientPaint(
                    kx + 40, ky, 120,
                    new float[] { 0f, 1f },
                    new Color[] { new Color(255, 200, 140, 90), new Color(255, 200, 140, 0) });
            g2.setPaint(knightRim);
            g2.fill(new Ellipse2D.Double(kx - 20, ky - 80, 160, 220));

            g2.setColor(new Color(18, 18, 24));
            g2.fillRoundRect(kx, ky - 100, 85, 210, 20, 20);
            // Pauldrons / helmet
            g2.fillOval(kx + 10, ky - 115, 55, 55);
            g2.setColor(new Color(40, 42, 52));
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(kx, ky - 100, 85, 210, 20, 20);
            // Glowing sword (vertical, cool light — contrasts torch)
            GradientPaint blade = new GradientPaint(
                    kx + 95, ky - 120, new Color(200, 230, 255, 240),
                    kx + 105, ky + 40, new Color(80, 140, 255, 200));
            g2.setPaint(blade);
            g2.fillRoundRect(kx + 92, ky - 125, 14, 160, 4, 4);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
            g2.setColor(new Color(180, 220, 255));
            g2.fillOval(kx + 70, ky - 130, 60, 40);
            g2.setComposite(AlphaComposite.SrcOver);

            // --- Sorcerer (right): hood, purple energy swirls (reference: violet glow clusters) ---
            int sx = (int) (w * 0.62);
            int sy = horizon + 35;
            RadialGradientPaint sorRim = new RadialGradientPaint(
                    sx - 30, sy + 40, 130,
                    new float[] { 0f, 1f },
                    new Color[] { new Color(160, 80, 255, 85), new Color(160, 80, 255, 0) });
            g2.setPaint(sorRim);
            g2.fill(new Ellipse2D.Double(sx - 40, sy - 60, 200, 240));

            g2.setColor(new Color(22, 18, 32));
            Path2D hood = new Path2D.Double();
            hood.moveTo(sx + 60, sy - 90);
            hood.curveTo(sx + 20, sy - 110, sx - 10, sy - 50, sx, sy + 30);
            hood.lineTo(sx + 130, sy + 120);
            hood.lineTo(sx + 160, sy + 20);
            hood.closePath();
            g2.fill(hood);

            // Hands + swirling purple magic
            for (int n = 0; n < 5; n++) {
                float a = (float) (n * Math.PI / 2.5);
                int cx = sx + 25 + (int) (28 * Math.cos(a));
                int cy = sy + 30 + (int) (18 * Math.sin(a * 2));
                RadialGradientPaint magic = new RadialGradientPaint(
                        cx, cy, 35f + n * 6,
                        new float[] { 0f, 0.6f, 1f },
                        new Color[] {
                            new Color(255, 200, 255, 220),
                            new Color(160, 60, 255, 140),
                            new Color(80, 0, 160, 0)
                        });
                g2.setPaint(magic);
                g2.fill(new Ellipse2D.Double(cx - 30, cy - 30, 60, 60));
            }
            g2.setStroke(new BasicStroke(2.5f));
            g2.setColor(new Color(200, 150, 255, 180));
            for (int s = 0; s < 3; s++) {
                g2.drawArc(sx - 10 + s * 15, sy + 10, 80 + s * 20, 60, 200, 140);
            }

            // Forward-facing emphasis (eyes toward viewer — highlights)
            g2.setColor(new Color(255, 240, 220, 80));
            g2.fillOval(kx + 28, ky - 95, 8, 6);
            g2.fillOval(kx + 48, ky - 95, 8, 6);
            g2.setColor(new Color(220, 200, 255, 100));
            g2.fillOval(sx + 55, sy - 72, 10, 6);
        }

        private static void drawTorch(Graphics2D g2, int x, int y, int radius) {
            RadialGradientPaint fire = new RadialGradientPaint(
                    x, y, radius,
                    new float[] { 0f, 0.4f, 1f },
                    new Color[] {
                            new Color(255, 250, 200, 240),
                            new Color(255, 140, 60, 120),
                            new Color(255, 80, 20, 0)
                    });
            g2.setPaint(fire);
            g2.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
            g2.setColor(new Color(60, 45, 35));
            g2.fillRect(x - 6, y + 10, 12, 40);
        }

        private static void drawTitleOverlay(Graphics2D g2, int w, int h) {
            String title = "DUNGEON KUrAWLER";
            String sub = "by Stack Overglow";

            Font titleFont = new Font(Font.SERIF, Font.BOLD, 44);
            Font subFont = new Font(Font.MONOSPACED, Font.ITALIC, 16);

            g2.setFont(titleFont);
            int tw = g2.getFontMetrics().stringWidth(title);
            int tx = (w - tw) / 2;
            int ty = 72;

            // Textured / chiseled look: dark outline + warm highlight
            g2.setColor(new Color(0, 0, 0, 160));
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    if (dx != 0 || dy != 0) {
                        g2.drawString(title, tx + dx, ty + dy);
                    }
                }
            }
            GradientPaint titlePaint = new GradientPaint(
                    tx, ty - 30, new Color(255, 230, 180),
                    tx, ty + 10, new Color(200, 160, 90));
            g2.setPaint(titlePaint);
            g2.drawString(title, tx, ty);

            g2.setFont(subFont);
            int sw = g2.getFontMetrics().stringWidth(sub);
            int sx = (w - sw) / 2;
            g2.setColor(new Color(0, 0, 0, 140));
            g2.drawString(sub, sx + 1, 102);
            g2.setColor(new Color(200, 190, 220));
            g2.drawString(sub, sx, 101);
        }
    }
}
