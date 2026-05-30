package view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/** Armor art helpers: PNG for item icons, fitted pixel overlay for equipped hero. */
final class HeroArmorPixelArt {

    static BufferedImage armorImage;

    static {
        try (InputStream in = HeroArmorPixelArt.class.getResourceAsStream("/weapons/armor.png")) {
            if (in != null) {
                armorImage = ImageIO.read(in);
            }
        } catch (IOException ignored) {
            armorImage = null;
        }
    }

    private HeroArmorPixelArt() {
    }

    static void paintEquipped(Graphics2D g2, int originX, int originY, int bodyW, int bodyH,
            boolean facingLeft) {
        int pixel = Math.max(1, Math.round(bodyH / 32f));
        int cx = xAt(originX, bodyW, 8);

        paintHelmet(g2, originX, originY, bodyW, bodyH, pixel);

        int shoulderY = yAt(originY, bodyH, 23);
        int chestTop = yAt(originY, bodyH, 24);
        int chestBottom = yAt(originY, bodyH, 30);
        int chestLeft = xAt(originX, bodyW, 2);
        int chestRight = xAt(originX, bodyW, 14);

        g2.setColor(new Color(20, 25, 36));
        fillRectPositive(g2, xAt(originX, bodyW, 1), shoulderY, xAt(originX, bodyW, 6) - xAt(originX, bodyW, 1),
                yAt(originY, bodyH, 26) - shoulderY);
        fillRectPositive(g2, xAt(originX, bodyW, 10), shoulderY,
                xAt(originX, bodyW, 15) - xAt(originX, bodyW, 10), yAt(originY, bodyH, 26) - shoulderY);
        g2.fillPolygon(new int[] {
                chestLeft, chestRight, chestRight, xAt(originX, bodyW, 11), cx,
                xAt(originX, bodyW, 5), chestLeft
        }, new int[] {
                chestTop, chestTop, yAt(originY, bodyH, 29), chestBottom, yAt(originY, bodyH, 31),
                chestBottom, yAt(originY, bodyH, 29)
        }, 7);

        g2.setColor(new Color(116, 132, 154));
        fillRectPositive(g2, xAt(originX, bodyW, 2), yAt(originY, bodyH, 24),
                xAt(originX, bodyW, 7) - xAt(originX, bodyW, 2), pixel * 3);
        fillRectPositive(g2, xAt(originX, bodyW, 9), yAt(originY, bodyH, 24),
                xAt(originX, bodyW, 14) - xAt(originX, bodyW, 9), pixel * 3);
        g2.fillPolygon(new int[] {
                xAt(originX, bodyW, 3), cx, cx, xAt(originX, bodyW, 5), xAt(originX, bodyW, 3)
        }, new int[] {
                yAt(originY, bodyH, 25), yAt(originY, bodyH, 25), chestBottom,
                yAt(originY, bodyH, 29), yAt(originY, bodyH, 28)
        }, 5);
        g2.fillPolygon(new int[] {
                cx, xAt(originX, bodyW, 13), xAt(originX, bodyW, 13), xAt(originX, bodyW, 11), cx
        }, new int[] {
                yAt(originY, bodyH, 25), yAt(originY, bodyH, 25), yAt(originY, bodyH, 28),
                yAt(originY, bodyH, 29), chestBottom
        }, 5);

        g2.setColor(new Color(202, 218, 218));
        fillRectPositive(g2, xAt(originX, bodyW, 9), yAt(originY, bodyH, 25), pixel * 3, pixel);
        fillRectPositive(g2, xAt(originX, bodyW, 10), yAt(originY, bodyH, 26), pixel, pixel * 3);
        fillRectPositive(g2, xAt(originX, bodyW, 4), yAt(originY, bodyH, 25), pixel, pixel);

        g2.setColor(new Color(31, 37, 55));
        fillRectPositive(g2, cx - pixel / 2, chestTop, pixel, yAt(originY, bodyH, 31) - chestTop);
        fillRectPositive(g2, xAt(originX, bodyW, 3), yAt(originY, bodyH, 30),
                xAt(originX, bodyW, 13) - xAt(originX, bodyW, 3), pixel);

        paintSmallShield(g2, originX, originY, bodyW, bodyH, pixel, facingLeft);
    }

    private static void paintHelmet(Graphics2D g2, int originX, int originY, int bodyW, int bodyH, int pixel) {
        int left = xAt(originX, bodyW, 4);
        int right = xAt(originX, bodyW, 12);
        int top = yAt(originY, bodyH, 14);
        int brow = yAt(originY, bodyH, 17);
        int bottom = yAt(originY, bodyH, 19);
        int cx = xAt(originX, bodyW, 8);

        g2.setColor(new Color(22, 27, 40));
        fillRectPositive(g2, left, top, right - left, pixel);
        fillRectPositive(g2, left - pixel, top + pixel, right - left + pixel * 2, brow - top);
        fillRectPositive(g2, left, brow, right - left, bottom - brow);

        g2.setColor(new Color(134, 150, 170));
        fillRectPositive(g2, left + pixel, top + pixel, right - left - pixel * 2, brow - top);
        fillRectPositive(g2, left + pixel * 2, brow, right - left - pixel * 4, pixel);

        g2.setColor(new Color(214, 226, 226));
        fillRectPositive(g2, xAt(originX, bodyW, 9), top + pixel, pixel * 2, pixel);
        g2.setColor(new Color(31, 37, 55));
        fillRectPositive(g2, cx - pixel / 2, top + pixel, pixel, bottom - top);
        fillRectPositive(g2, left, bottom - pixel, right - left, pixel);
    }

    private static void paintSmallShield(Graphics2D g2, int originX, int originY, int bodyW, int bodyH, int pixel,
            boolean facingLeft) {
        int cx = xAt(originX, bodyW, 8);
        int dir = facingLeft ? 1 : -1;
        int shieldCx = cx + dir * Math.round(bodyW * 5.5f / 16f);
        int shieldTop = yAt(originY, bodyH, 24);
        int shieldMid = yAt(originY, bodyH, 27);
        int shieldBottom = yAt(originY, bodyH, 30);
        int shieldHalfW = Math.max(pixel, Math.round(bodyW * 1.3f / 16f));

        g2.setColor(new Color(22, 27, 40));
        g2.fillPolygon(new int[] {
                shieldCx - shieldHalfW, shieldCx + shieldHalfW, shieldCx + shieldHalfW,
                shieldCx + pixel, shieldCx, shieldCx - pixel, shieldCx - shieldHalfW
        }, new int[] {
                shieldTop + pixel, shieldTop + pixel, shieldMid,
                shieldBottom - pixel, shieldBottom, shieldBottom - pixel, shieldMid
        }, 7);
        g2.setColor(new Color(80, 100, 136));
        g2.fillPolygon(new int[] {
                shieldCx - pixel, shieldCx + pixel, shieldCx + pixel, shieldCx, shieldCx - pixel
        }, new int[] {
                shieldTop + pixel * 2, shieldTop + pixel * 2, shieldMid,
                shieldBottom - pixel, shieldMid
        }, 5);
        g2.setColor(new Color(165, 195, 205));
        fillRectPositive(g2, shieldCx, shieldTop + pixel * 2, pixel, pixel * 2);
    }

    private static int xAt(int originX, int bodyW, int spriteX) {
        return originX + Math.round(bodyW * spriteX / 16f);
    }

    private static int yAt(int originY, int bodyH, int spriteY) {
        return originY + Math.round(bodyH * spriteY / 32f);
    }

    private static void fillRectPositive(Graphics2D g2, int x, int y, int width, int height) {
        if (width < 0) {
            x += width;
            width = -width;
        }
        if (height < 0) {
            y += height;
            height = -height;
        }
        if (width > 0 && height > 0) {
            g2.fillRect(x, y, width, height);
        }
    }
}
