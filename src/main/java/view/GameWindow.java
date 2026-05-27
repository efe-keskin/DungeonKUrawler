package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import engine.GameEngine;
import engine.MissionListener;
import engine.PlayerModeController;
import engine.InteractionController;
import engine.GameStateListener;
import model.ValuableItem;
import view.assets.AssetId;
import view.assets.AssetManager;

/**
 * Gameplay shell: dark frame; {@link GamePanel} is the observer and input
 * surface — it delegates
 * keys to {@link GameEngine} without containing rules. A top control strip
 * provides navigation back
 * to the main menu without trapping keyboard focus.
 */
public class GameWindow extends JFrame implements GameStateListener {

    private static final int WINDOW_W = 920;
    private static final int WINDOW_H = 560;
    private static final Color CONTROL_BACKGROUND = new Color(18, 17, 22);
    private static final Color CONTROL_BORDER = new Color(103, 91, 75);

    private final GameEngine engine;
    private JButton pauseButton;
    private boolean gameOverDialogShown;

    public GameWindow(GameEngine engine) {
        this.engine = engine;
        setTitle("Dungeon Krawler — Build Mode");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        RetroTheme.styleFrameDark(this);

        PlayerModeController playerModeController = new PlayerModeController(engine);
        InteractionController interactionController = new InteractionController(engine);
        GamePanel panel = new GamePanel(engine, playerModeController, interactionController);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controlPanel.setBackground(CONTROL_BACKGROUND);
        controlPanel.setFocusable(false);
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, CONTROL_BORDER),
                new EmptyBorder(9, 12, 9, 12)));

        JButton returnToMenu = new GameplayButton("RETURN TO MENU", false);
        returnToMenu.setPreferredSize(new Dimension(198, 45));
        // Keeps WASD/arrows on GamePanel: the button still activates on mouse click.
        returnToMenu.setFocusable(false);
        returnToMenu.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new MainMenuWindow().setVisible(true));
        });
        controlPanel.add(returnToMenu);

        pauseButton = new GameplayButton(engine.isPaused() ? "RESUME" : "PAUSE", false);
        pauseButton.setPreferredSize(new Dimension(140, 45));
        pauseButton.setFocusable(false);
        pauseButton.addActionListener(e -> {
            engine.togglePause();
            panel.requestFocusInWindow();
        });
        controlPanel.add(pauseButton);

        // Bottom strip under the map for gameplay actions.
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        bottomPanel.setBackground(CONTROL_BACKGROUND);
        bottomPanel.setFocusable(false);
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, CONTROL_BORDER),
                new EmptyBorder(9, 10, 9, 10)));

        JButton inventoryButton = new GameplayButton("INVENTORY", true);
        ImageIcon chestIcon = AssetManager.get().icon(AssetId.INVENTORY_CHEST_ICON, 44, 44);
        if (chestIcon != null) {
            inventoryButton.setIcon(chestIcon);
            inventoryButton.setText("");
        }
        inventoryButton.setPreferredSize(new Dimension(70, 64));
        inventoryButton.setFocusable(false);
        inventoryButton.addActionListener(e -> {
            InventoryDialog dialog = new InventoryDialog(this, engine);
            dialog.setVisible(true);
            // Keep keyboard movement controls on the map after popup closes.
            panel.requestFocusInWindow();
        });
        bottomPanel.add(inventoryButton);

        // Clicks on the strip background (not consumed by the button) return focus to
        // the game.
        controlPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getSource() == controlPanel) {
                    panel.requestFocusInWindow();
                }
            }
        });

        JPanel wrap = new JPanel(new BorderLayout());
        RetroTheme.stylePanelDark(wrap);
        wrap.add(controlPanel, BorderLayout.NORTH);
        wrap.add(panel, BorderLayout.CENTER);
        wrap.add(bottomPanel, BorderLayout.SOUTH);
        add(wrap);

        // Mission win: when the hero finally picks up the target valuable,
        // surface the victory screen on the EDT (engine fires synchronously
        // from the pickup call site).
        engine.getTargetMission().addListener(new MissionListener() {
            @Override
            public void onMissionWon(ValuableItem target) {
                SwingUtilities.invokeLater(() -> {
                    MissionSplashDialog.showVictory(GameWindow.this, target);
                    panel.requestFocusInWindow();
                });
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                panel.requestFocusInWindow();
                ValuableItem target = engine.getTargetMission().getTarget();
                if (target != null) {
                    // Deferred so the gameplay frame paints once underneath the splash.
                    SwingUtilities.invokeLater(() -> {
                        MissionSplashDialog.showFindThis(GameWindow.this, target);
                        panel.requestFocusInWindow();
                    });
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {
                engine.removeGameStateListener(GameWindow.this);
                engine.shutdown();
            }
        });

        // After Alt+Tab or dialogs, keep movement keys on the game surface.
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                panel.requestFocusInWindow();
            }
        });

        setSize(WINDOW_W, WINDOW_H);
        setLocationRelativeTo(null);

        engine.addGameStateListener(this);
    }

    @Override
    public void onGameStateChanged() {
        if (pauseButton != null) {
            pauseButton.setText(engine.isPaused() ? "RESUME" : "PAUSE");
        }

        if (engine.isGameOver() && !gameOverDialogShown) {
            gameOverDialogShown = true;
            SwingUtilities.invokeLater(() -> {
                Object[] options = new Object[] { "Return to Menu" };
                JOptionPane.showOptionDialog(
                        GameWindow.this,
                        "Your HP reached 0.",
                        "DEFEAT",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        options,
                        options[0]);
                dispose();
                SwingUtilities.invokeLater(() -> new MainMenuWindow().setVisible(true));
            });
        }
    }

    private static Font controlFont(float size) {
        Font base = RetroTheme.UI_MONO == null
                ? new Font(Font.MONOSPACED, Font.BOLD, Math.round(size))
                : RetroTheme.UI_MONO;
        return base.deriveFont(Font.PLAIN, size);
    }

    /**
     * Hard-edged gameplay control styled to match the retro overlay surfaces.
     */
    private static final class GameplayButton extends JButton {
        private static final Color OUTLINE = new Color(5, 5, 9);
        private static final Color STONE = new Color(103, 91, 75);
        private static final Color STONE_HIGHLIGHT = new Color(156, 131, 85);
        private static final Color GOLD = new Color(214, 170, 70);
        private static final Color GOLD_BRIGHT = new Color(244, 205, 103);
        private static final Color TEXT = new Color(240, 222, 180);

        private final boolean primary;
        private boolean hovered;

        GameplayButton(String label, boolean primary) {
            super(label);
            this.primary = primary;
            setFont(controlFont(13f));
            setForeground(TEXT);
            setBorder(new EmptyBorder(13, 18, 13, 18));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                Color background = primary
                        ? (hovered ? new Color(126, 84, 31) : new Color(92, 61, 28))
                        : (hovered ? new Color(65, 55, 47) : new Color(43, 38, 37));
                Color border = primary
                        ? (hovered ? GOLD_BRIGHT : GOLD)
                        : (hovered ? STONE_HIGHLIGHT : STONE);
                g2.setColor(OUTLINE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(background);
                g2.fillRect(3, 3, getWidth() - 6, getHeight() - 6);
                g2.setColor(border);
                g2.drawRect(2, 2, getWidth() - 5, getHeight() - 5);
                g2.setColor(hovered ? GOLD_BRIGHT : new Color(155, 122, 62));
                g2.fillRect(5, 5, getWidth() - 10, 2);
            } finally {
                g2.dispose();
            }
            super.paintComponent(graphics);
        }
    }
}
