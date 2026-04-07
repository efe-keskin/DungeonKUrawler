package view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Login screen with retro styling; delegates navigation only (no game rules).
 */
public class LoginWindow extends JFrame {

    private static final int PADDING = 12;

    public LoginWindow() {
        setTitle("Dungeon Krawler — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        RetroTheme.styleFrameDark(this);

        JTextField usernameField = new JTextField(16);
        RetroTheme.styleTextField(usernameField);
        JPasswordField passwordField = new JPasswordField(16);
        RetroTheme.styleTextField(passwordField);

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        RetroTheme.stylePanelDark(form);
        form.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
        JLabel u = new JLabel("Username:");
        JLabel p = new JLabel("Password:");
        RetroTheme.styleLabel(u);
        RetroTheme.styleLabel(p);
        form.add(u);
        form.add(usernameField);
        form.add(p);
        form.add(passwordField);

        JButton loginButton = new JButton("Login");
        RetroTheme.styleRetroButton(loginButton, RetroTheme.BTN_PRIMARY);
        loginButton.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new MainMenuWindow().setVisible(true));
        });

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        RetroTheme.stylePanelDark(south);
        south.setBorder(new EmptyBorder(0, PADDING, PADDING, PADDING));
        south.add(loginButton);

        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }
}
