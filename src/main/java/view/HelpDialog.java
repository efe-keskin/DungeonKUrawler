package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Window;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import engine.audio.AudioManager;
import view.assets.AssetManager;

public final class HelpDialog extends JDialog {

    private static final Color GOLD = new Color(244, 205, 103);
    private static final Color BODY = new Color(220, 220, 225);
    private static final Color BORDER = new Color(103, 91, 75);
    private static final int ICON_SIZE = 28;

    public HelpDialog(Window owner) {
        super(owner, "Help", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel helpPanel = new JPanel();
        helpPanel.setLayout(new BoxLayout(helpPanel, BoxLayout.Y_AXIS));
        helpPanel.setBackground(RetroTheme.BG_PANEL);
        helpPanel.setBorder(new EmptyBorder(18, 20, 18, 20));
        addSectionWithIcons(helpPanel, "Objective",
                "Dungeon KUrawler has two ways to play: Story Mode and Custom Map Mode. In every map, your main goal is to survive, find the required valuable item, and then reach the exit door. If enemies reduce your HP to 0, you die and the map is lost. The exit door cannot be completed until the valuable item has been found.",
                "/items/valuable_items/crystal_shard_64x64.png",
                "/background_floor/assets/doors/15_door_closed_wood.png");
        addSection(helpPanel, "Game Modes",
                "Story Mode contains 10 levels. Levels are unlocked in order, so you must complete the current level before moving to the next one. Finishing level 10 means you win the game. Custom Map Mode lets you play maps created in Build Map. Load Map starts a custom map, while Load Game lets you continue from a saved game.");
        addSection(helpPanel, "Main Menu",
                "Start Game opens Story Mode and lets you choose from the available unlocked levels. Load Map loads a custom map created in Build Map and starts it in Custom Map Mode. Load Game continues from a previously saved game. Build Map opens the map editor where you can create your own dungeon layout. Exit closes the game. The question mark button opens this help page.");
        addSectionWithIcons(helpPanel, "Play Mode Controls",
                "Use these controls while playing a map:\n\n"
                        + "WASD / Arrow Keys ---> move the hero\n"
                        + "T ---> take the first available nearby item\n"
                        + "E ---> equip the nearest item: weapons are equipped, armor is worn, books are read, anything else is taken\n"
                        + "O ---> open a nearby chest, arch, container, or searchable object\n"
                        + "P ---> attack the nearest enemy; if no enemy can be attacked, break a nearby breakable object\n"
                        + "I ---> open the inventory\n"
                        + "R ---> pause or resume the game\n"
                        + "ESC ---> open the in-game menu or close dialogs\n"
                        + "Mouse Click ---> attack or interact with a visible tile",
                "/characters/hero1.png");
        addSection(helpPanel, "Mouse Controls",
                "In Play Mode, clicking a visible map tile first tries to attack an enemy on that tile. If there is no enemy, the game opens the available interaction options for items or objects on that tile. In containers or chests, clicking an item takes that item. In the inventory, clicking an item opens its available actions.");
        addSectionWithIcons(helpPanel, "Inventory and Equipment",
                "The inventory stores weapons, armor, rings, potions, keys, valuable items, and other collected items. Inventory capacity is limited, so choose what to keep carefully. Items can be equipped, used, or dropped/removed depending on their type. Only one weapon can be equipped at a time. Armor can be equipped with a weapon, and one ring can also be worn for a passive bonus.",
                "/inventorychest.png");
        addSectionWithIcons(helpPanel, "Weapons and Items",
                "Melee weapons such as swords are used for close combat and increase melee damage. Bows are ranged physical weapons and use energy when attacking. Magic staffs or wands are ranged magical weapons and use mana when attacking. Armor increases defense and helps reduce incoming damage. Rings give passive bonuses such as extra strength, mana, energy, or defense. Potions can be used for helpful effects. Keys are used when the game requires them. Money can be found as loot, but it is not shown inside the inventory.",
                "/weapons/bows/058_curved_bow.png",
                "/weapons/staves/012_long_wooden_pole.png",
                "/weapons/armor.png");
        addSectionWithIcons(helpPanel, "Objects and Loot",
                "Searchable objects can be opened or searched with O. They may contain weapons, equipment, potions, keys, money, valuable items, or other inventory items. Breakable objects can be destroyed with P. Breaking an object may reveal an item or money. If you are stuck or need better equipment, check containers and break nearby objects when it is safe.",
                "/items/chests/01_chest_closed_blue_trim.png",
                "/items/golds_coins/20_coin_pile_gold.png");
        addSectionWithIcons(helpPanel, "Enemies and Bosses",
                "Regular enemies include melee knights and ranged magic users. Melee enemies are dangerous up close, while ranged enemies can threaten you from a distance. Levels 5 and 10 include stronger melee final bosses. Boss fights are harder than normal encounters, so prepare with better equipment, enough HP, and useful items before fighting them.",
                "/characters/knight1.png",
                "/characters/wizard1.png",
                "/characters/boss1_move_01.png");
        addSection(helpPanel, "Combat",
                "You can attack with P or by clicking a visible enemy tile. The equipped weapon affects how you fight. Melee weapons are best for adjacent enemies. Bows can attack from range but require energy. Magic weapons can attack from range but require mana. If you do not have enough mana or energy, ranged attacks may fail. Watch your HP before fighting, and avoid rushing into enemies without checking your equipment.");
        addSectionWithIcons(helpPanel, "Build Mode",
                "Build Map lets you create a custom dungeon. Choose a tool button, then use these mouse controls:\n\n"
                        + "Left Click / Left Drag ---> place the selected build tool\n"
                        + "Right Click / Right Drag ---> erase content from map cells\n"
                        + "Drag Tool Icon to Map ---> place that tool at the drop location\n\n"
                        + "The game can handle required gameplay elements for custom maps, so focus on designing a playable layout.",
                "/background_floor/assets/breakable assets/vase.png");
        addSection(helpPanel, "Saving and Loading",
                "Press ESC during gameplay to open the in-game menu and save your progress. Both Story Mode and Custom Map Mode can be saved. Multiple save files may exist. Load Game continues from a saved game, while Load Map starts a custom map from the beginning instead of continuing a saved state.");
        addSection(helpPanel, "Tips",
                "Find the valuable item before going to the exit door. Keep your inventory organized because space is limited. Search containers and break objects to find better loot, money, keys, potions, or equipment. Equip the right weapon for the situation: melee weapons for close enemies, bows when you have energy, and magic weapons when you have mana. Save before risky fights, especially before boss levels. If a fight feels too hard, look for better equipment or useful items first.");

        JScrollPane scrollPane = new JScrollPane(helpPanel);
        scrollPane.setPreferredSize(new Dimension(560, 420));
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER, 2));
        scrollPane.getViewport().setBackground(RetroTheme.BG_PANEL);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getVerticalScrollBar().setBlockIncrement(60);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));

        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JButton close = new JButton("Close");
        RetroTheme.styleRetroButton(close, RetroTheme.BTN_ACCENT);
        close.addActionListener(e -> {
            AudioManager.shared().play("button_click");
            dispose();
        });

        JPanel buttonRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        buttonRow.setOpaque(false);
        buttonRow.add(close);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(RetroTheme.BG_DUNGEON);
        content.setBorder(new EmptyBorder(18, 18, 18, 18));
        content.add(scrollPane, BorderLayout.CENTER);
        content.add(buttonRow, BorderLayout.SOUTH);

        setContentPane(content);
        pack();
        setLocationRelativeTo(owner);
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }

    private static void addSection(JPanel parent, String title, String body) {
        addSectionWithIcons(parent, title, body);
    }

    private static void addSectionWithIcons(JPanel parent, String title, String body, String... iconPaths) {
        JPanel headerRow = new JPanel();
        headerRow.setLayout(new BoxLayout(headerRow, BoxLayout.X_AXIS));
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerRow.setOpaque(false);

        JLabel header = new JLabel(title);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setForeground(GOLD);
        header.setFont(font(RetroTheme.UI_MONO, Font.BOLD, 16f));
        headerRow.add(header);

        for (String iconPath : iconPaths) {
            JLabel icon = createIconLabel(iconPath);
            if (icon != null) {
                headerRow.add(Box.createHorizontalStrut(8));
                headerRow.add(icon);
            }
        }
        headerRow.add(Box.createHorizontalGlue());

        JTextArea bodyText = new JTextArea(body);
        bodyText.setAlignmentX(Component.LEFT_ALIGNMENT);
        bodyText.setEditable(false);
        bodyText.setFocusable(false);
        bodyText.setLineWrap(true);
        bodyText.setWrapStyleWord(true);
        bodyText.setOpaque(false);
        bodyText.setForeground(BODY);
        bodyText.setFont(font(RetroTheme.UI_MONO_SMALL, Font.PLAIN, 13f));
        bodyText.setBorder(BorderFactory.createEmptyBorder(6, 0, 14, 0));

        parent.add(headerRow);
        parent.add(Box.createVerticalStrut(2));
        parent.add(bodyText);
    }

    private static JLabel createIconLabel(String resourcePath) {
        BufferedImage image = AssetManager.get().image(resourcePath);
        if (image == null) {
            return null;
        }
        Image scaled = image.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
        JLabel label = new JLabel(new ImageIcon(scaled));
        label.setAlignmentY(Component.CENTER_ALIGNMENT);
        return label;
    }

    private static Font font(Font configured, int style, float size) {
        Font base = configured == null ? new Font(Font.MONOSPACED, style, Math.round(size)) : configured;
        return base.deriveFont(style, size);
    }
}
