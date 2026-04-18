package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import model.Armor;
import model.Inventory;
import model.Item;
import model.Potion;
import model.Weapon;

/**
 * Modal inventory view with fixed 2x4 slot layout (8 total).
 */
public class InventoryDialog extends JDialog {

    private static final Color SLOT_EMPTY_BG = new Color(18, 18, 24);
    private static final Color SLOT_FILLED_BG = new Color(36, 42, 56);
    private static final Color SLOT_BORDER = new Color(78, 82, 98);

    public InventoryDialog(JDialog owner, Inventory inventory) {
        super(owner, "Inventory", Dialog.ModalityType.APPLICATION_MODAL);
        buildUi(inventory);
    }

    public InventoryDialog(java.awt.Frame owner, Inventory inventory) {
        super(owner, "Inventory", true);
        buildUi(inventory);
    }

    private void buildUi(Inventory inventory) {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        root.setBackground(RetroTheme.BG_DUNGEON);

        JLabel title = new JLabel("Inventory " + inventory.size() + "/" + inventory.getCapacity(), SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(RetroTheme.UI_MONO);
        root.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 4, 8, 8));
        grid.setOpaque(false);

        for (int i = 0; i < 8; i++) {
            Item item = i < inventory.size() ? inventory.getItems().get(i) : null;
            grid.add(createSlot(item));
        }

        root.add(grid, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        actions.setOpaque(false);

        JButton close = new JButton("Close");
        RetroTheme.styleRetroButton(close, RetroTheme.BTN_SECONDARY);
        close.setFocusable(false);
        close.addActionListener(e -> dispose());
        actions.add(close);

        root.add(actions, BorderLayout.SOUTH);
        setContentPane(root);
        setSize(540, 330);
        setResizable(false);
        setLocationRelativeTo(getOwner());
    }

    private JPanel createSlot(Item item) {
        JPanel slot = new JPanel(new BorderLayout(0, 8));
        slot.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SLOT_BORDER),
                new EmptyBorder(10, 8, 10, 8)));

        if (item == null) {
            slot.setBackground(SLOT_EMPTY_BG);
            JLabel empty = new JLabel("Empty", SwingConstants.CENTER);
            empty.setFont(RetroTheme.UI_MONO_SMALL);
            empty.setForeground(new Color(140, 140, 155));
            slot.add(empty, BorderLayout.CENTER);
            return slot;
        }

        slot.setBackground(SLOT_FILLED_BG);

        JLabel marker = new JLabel(typeMarker(item), SwingConstants.CENTER);
        marker.setOpaque(true);
        marker.setBackground(typeColor(item));
        marker.setForeground(Color.WHITE);
        marker.setFont(RetroTheme.UI_MONO_SMALL);
        marker.setBorder(new EmptyBorder(2, 4, 2, 4));

        JLabel name = new JLabel(item.getName(), SwingConstants.CENTER);
        name.setForeground(Color.WHITE);
        name.setFont(RetroTheme.UI_MONO_SMALL);

        slot.add(marker, BorderLayout.NORTH);
        slot.add(name, BorderLayout.CENTER);
        return slot;
    }

    private String typeMarker(Item item) {
        if (item instanceof Potion) {
            return "POT";
        }
        if (item instanceof Weapon) {
            return "WPN";
        }
        if (item instanceof Armor) {
            return "ARM";
        }
        return "ITM";
    }

    private Color typeColor(Item item) {
        if (item instanceof Potion) {
            return new Color(70, 120, 70);
        }
        if (item instanceof Weapon) {
            return new Color(120, 85, 40);
        }
        if (item instanceof Armor) {
            return new Color(60, 95, 130);
        }
        return new Color(95, 85, 120);
    }
}
