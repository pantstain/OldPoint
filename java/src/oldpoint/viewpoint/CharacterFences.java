package oldpoint.viewpoint;

import java.util.HashMap;
import java.util.Map;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;

/**
 * Fence edges a nearby character stands against. Isometric drawing always
 * draws a character after the fence beside it; in 3D a character leaning over
 * or through a thin fence is cut by it, so those fence pieces are faded.
 */
final class CharacterFences {
    /** Fade amount for a fence piece a character stands against. */
    static final float FADE = 0.5f;
    /** A character this close to a fence line can reach through it. */
    private static final float REACH = 0.8f;
    private static final float RANGE = 14f;

    private CharacterFences() {}

    static long key(int x, int y) {
        return ((long) x << 32) ^ (y & 0xffffffffL);
    }

    /** 1 = fence on the square's north edge, 2 = west edge, 0 = not a fence. */
    static int kind(IsoSprite sprite) {
        if (sprite == null) {
            return 0;
        }
        var p = sprite.getProperties();
        int kind = 0;
        if (p.has(IsoFlagType.HoppableN) || p.has(IsoFlagType.TallHoppableN)) {
            kind |= 1;
        }
        if (p.has(IsoFlagType.HoppableW) || p.has(IsoFlagType.TallHoppableW)) {
            kind |= 2;
        }
        return kind;
    }

    /** Square key to edge mask for every fence edge a visible character on this floor is against. */
    static Map<Long, Integer> capture(IsoGameCharacter player, int floor) {
        HashMap<Long, Integer> result = new HashMap<>();
        var cell = IsoWorld.instance == null ? null : IsoWorld.instance.getCell();
        if (cell == null || player == null) {
            return result;
        }
        int index = player instanceof IsoPlayer local ? local.getIndex() : -1;
        add(result, player, floor);
        var zombies = cell.getZombieList();
        if (zombies == null) {
            return result;
        }
        for (int i = 0; i < zombies.size(); i++) {
            IsoZombie zombie = zombies.get(i);
            if (zombie == null || zombie.isDead() || (int) Math.floor(zombie.getZ()) != floor) {
                continue;
            }
            float dx = zombie.getX() - player.getX(), dy = zombie.getY() - player.getY();
            if (dx * dx + dy * dy > RANGE * RANGE || !seen(zombie, index)) {
                continue;
            }
            add(result, zombie, floor);
        }
        return result;
    }

    private static boolean seen(IsoZombie zombie, int index) {
        if (index < 0 || index >= 4) {
            return false;
        }
        var square = zombie.getCurrentSquare();
        if (square != null && square.isCanSee(index)) {
            return true;
        }
        boolean[] seenBy = zombie.isVisibleToPlayer;
        return seenBy != null && index < seenBy.length && seenBy[index];
    }

    private static void add(Map<Long, Integer> result, IsoGameCharacter character, int floor) {
        if ((int) Math.floor(character.getZ()) != floor) {
            return;
        }
        float px = character.getX(), py = character.getY();
        int cx = (int) Math.floor(px), cy = (int) Math.floor(py);
        for (int sy = cy - 1; sy <= cy + 2; sy++) {
            for (int sx = cx - 1; sx <= cx + 2; sx++) {
                int mask = 0;
                // North edge: the line y = sy from x = sx to sx + 1.
                if (Math.abs(py - sy) < REACH && px > sx - REACH && px < sx + 1 + REACH) {
                    mask |= 1;
                }
                // West edge: the line x = sx from y = sy to sy + 1.
                if (Math.abs(px - sx) < REACH && py > sy - REACH && py < sy + 1 + REACH) {
                    mask |= 2;
                }
                if (mask != 0) {
                    result.merge(key(sx, sy), mask, (a, b) -> a | b);
                }
            }
        }
    }
}
