package net.erutobusiness.elytraboostlimit.client;

import com.kyanite.deeperdarker.client.Keybinds;
import com.kyanite.deeperdarker.network.Messages;
import com.kyanite.deeperdarker.network.SoulElytraBoostPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.fml.ModList;

/**
 * Deeper and Darker の加速キーを読み、押し下げの立ち上がりで加速を要求する。
 *
 * <p>⚠ このクラスに入るのは、呼び出し側が Deeper and Darker が在ることを確かめた後だけ。
 */
final class DeeperDarkerBoost {

    private static final String ELYTRA_SLOT = "elytraslot";

    /** 前の tick で押されていたか。押しっぱなしで連射しないために持つ。 */
    private static boolean wasDown;

    private DeeperDarkerBoost() {
    }

    static void tick(boolean gliding) {
        if (Keybinds.BOOST.getKey().getType() != InputConstants.Type.MOUSE) {
            // ⚠ キーボードに割り当てられている間は何もしない。そちらは Deeper and Darker と
            //    Elytra Slot の InputEvent.Key が正しく拾うので、ここでも送ると二重に飛ぶ。
            wasDown = false;
            return;
        }

        boolean down = Keybinds.BOOST.isDown();
        boolean rising = down && !wasDown;
        wasDown = down;
        if (!rising || !gliding) {
            return;
        }

        // ⚠ 2つ送る。装備の枠で担当が分かれており、どちらも相手の枠を見ないため:
        //    Deeper and Darker 側は胸当ての枠（Inventory.armor の 2 番）だけ、
        //    Elytra Slot 側は Curios の枠だけを見る。
        //    条件に合わないほうは何もせずに終わるので、二重に加速することはない
        //    （仮に両方の枠が埋まっていても、先に通ったほうがアイテムのクールダウンを
        //     立てるので、もう一方はそこで止まる）。
        if (Messages.INSTANCE != null) {
            Messages.INSTANCE.sendToServer(new SoulElytraBoostPacket());
        }
        if (ModList.get().isLoaded(ELYTRA_SLOT)) {
            ElytraSlotBoost.send();
        }
    }
}
