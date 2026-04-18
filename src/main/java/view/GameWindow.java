package view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import engine.GameEngine;
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

    public GameWindow(GameEngine engine) {
        setTitle("Dungeon Krawler — Build Mode");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        RetroTheme.styleFrameDark(this);

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
        add(wrap);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                panel.requestFocusInWindow();
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
