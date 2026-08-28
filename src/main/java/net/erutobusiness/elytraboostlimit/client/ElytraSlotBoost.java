package net.erutobusiness.elytraboostlimit.client;

import com.illusivesoulworks.elytraslot.common.integration.deeperdarker.DeeperDarkerPlugin;
import com.illusivesoulworks.elytraslot.common.integration.deeperdarker.SoulElytraBoostPayload;

/**
 * Elytra Slot の Deeper and Darker 連携へ加速を要求する。
 *
 * <p>Elytra Slot 側は装備を {@code Services.ELYTRA.getEquipped}（Curios の枠を引く）で見るので、
 * <b>胸当てとは別の枠に着けているとき</b>はこちらだけが通る。
 *
 * <p>⚠ このクラスに入るのは、呼び出し側が Elytra Slot が在ることを確かめた後だけ。
 */
final class ElytraSlotBoost {

    private ElytraSlotBoost() {
    }

    static void send() {
        if (DeeperDarkerPlugin.NETWORK == null) {
            return;
        }
        DeeperDarkerPlugin.NETWORK.sendToServer(new SoulElytraBoostPayload());
    }
}
