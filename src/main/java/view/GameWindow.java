package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.border.EmptyBorder;

import engine.GameEngine;
import engine.GameLoopController;
import engine.PlayerModeController;
import engine.InteractionController;

/**
 * Gameplay shell: dark frame; {@link GamePanel} is the observer and input
 * surface — it delegates
 * keys to {@link GameEngine} without containing rules. A top control strip
 * provides navigation back
 * to the main menu without trapping keyboard focus.
 */
public class GameWindow extends JFrame {

    private static final int WINDOW_W = 920;
    private static final int WINDOW_H = 560;
    private static final int ENGINE_TICK_INTERVAL_MS = 100;

    private final GameLoopController gameLoopController;

    public GameWindow(GameEngine engine) {
        setTitle("Dungeon Krawler — Build Mode");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        RetroTheme.styleFrameDark(this);
        gameLoopController = new GameLoopController(engine, ENGINE_TICK_INTERVAL_MS);

        PlayerModeController playerModeController = new PlayerModeController(engine);
        InteractionController interactionController = new InteractionController(engine);
        GamePanel panel = new GamePanel(engine, playerModeController, interactionController);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controlPanel.setBackground(RetroTheme.BG_DUNGEON);
        controlPanel.setFocusable(false);
        controlPanel.setBorder(new EmptyBorder(8, 10, 8, 10));

        JButton returnToMenu = new JButton("Return to Main Menu");
        RetroTheme.styleRetroButton(returnToMenu, RetroTheme.BTN_SECONDARY);
        // Keeps WASD/arrows on GamePanel: the button still activates on mouse click.
        returnToMenu.setFocusable(false);
        returnToMenu.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new MainMenuWindow().setVisible(true));
        });
        controlPanel.add(returnToMenu);

        // Bottom strip under the map for gameplay actions.
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        bottomPanel.setBackground(RetroTheme.BG_DUNGEON);
        bottomPanel.setFocusable(false);
        bottomPanel.setBorder(new EmptyBorder(8, 10, 8, 10));

        JButton inventoryButton = new JButton("Inventory");
        RetroTheme.styleRetroButton(inventoryButton, RetroTheme.BTN_PRIMARY);
        try (InputStream in = GameWindow.class.getResourceAsStream("/inventorychest.png")) {
            if (in != null) {
                BufferedImage img = ImageIO.read(in);
                java.awt.Image scaled = img.getScaledInstance(48, 48, java.awt.Image.SCALE_SMOOTH);
                inventoryButton.setIcon(new ImageIcon(scaled));
                inventoryButton.setText("");
                inventoryButton.setPreferredSize(new Dimension(48, 48));
                inventoryButton.setBorder(new LineBorder(new Color(255, 255, 255, 90), 1, true));
                inventoryButton.setBorderPainted(true);
                inventoryButton.setContentAreaFilled(false);
                inventoryButton.setOpaque(false);
            }
        } catch (Exception ignored) {
            // Fallback keeps text label if image cannot be loaded.
        }
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

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                gameLoopController.start();
                panel.requestFocusInWindow();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                gameLoopController.stop();
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
    }
}
