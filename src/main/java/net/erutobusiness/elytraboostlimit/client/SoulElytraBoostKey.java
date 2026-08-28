package net.erutobusiness.elytraboostlimit.client;

import net.erutobusiness.elytraboostlimit.Config;
import net.erutobusiness.elytraboostlimit.ElytraBoostLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * マウスのボタンに割り当てた「ソウルエリトラの加速」キーを、毎 tick 見て発火させる。
 *
 * <p>Deeper and Darker は加速の要求を {@code InputEvent.Key} の中でしか出しておらず、
 * Forge がこのイベントを流すのは<b>キーボードのときだけ</b>（マウスは
 * {@code InputEvent.MouseButton} という別のイベントを通る）。だから加速キーを
 * マウスへ割り当てると一度も発火しない。Elytra Slot 側の Deeper and Darker 連携も
 * 同じイベントを見ているので、同時に黙る。
 *
 * <p>ここでは押下の状態を毎 tick 読む。読み方をイベントの種類に依存させないので、
 * キーボードでもマウスでも同じように拾える。
 *
 * <p>⚠ このクラスは Deeper and Darker のクラスに<b>触らない</b>。触るのは
 * {@link DeeperDarkerBoost} で、そちらへ入るのは {@code ModList} で在ることを
 * 確かめた後だけ（居ない構成でクラスが読み込まれて落ちるのを避ける）。
 */
@Mod.EventBusSubscriber(modid = ElytraBoostLimit.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SoulElytraBoostKey {

    private static final String DEEPER_DARKER = "deeperdarker";

    private SoulElytraBoostKey() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!Config.FIX_MOUSE_BOUND_BOOST_KEY.get() || !ModList.get().isLoaded(DEEPER_DARKER)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.getConnection() == null) {
            return;
        }

        // ⚠ 滑空中だけに絞る。マウス右は「アイテムを使う」と同じ指で、遊んでいる間ずっと
        //    押されるので、絞らないと押すたびにサーバーへ便りを出すことになる。
        //    サーバー側も同じ条件を見ているので、ここで落としても取りこぼしにはならない。
        DeeperDarkerBoost.tick(player.isFallFlying());
    }
}
