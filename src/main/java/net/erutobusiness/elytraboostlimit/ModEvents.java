package net.erutobusiness.elytraboostlimit;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModEvents {

    private ModEvents() {
    }

    @Mod.EventBusSubscriber(modid = ElytraBoostLimit.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Common {

        /**
         * 湧いた瞬間に止める。
         *
         * <p>湧かせてから消す方式（datapack でやっていたこと）だと、消えるまでの1tick ぶんの
         * 加速が乗る。ここで {@code setCanceled} すれば、そもそも世界に出ない。
         */
        @SubscribeEvent
        public static void onEntityJoin(EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide()) {
                return;
            }
            if (BoostBlocker.shouldBlock(event.getEntity(), event.getLevel())) {
                event.setCanceled(true);
            }
        }

        /**
         * 滑空中の水平速度を毎 tick 見て、上限を超えていたら水平成分だけ縮める。
         *
         * <p>⚠⚠ <b>Phase.START の最後尾で削る。</b> 1 tick の中の順序が要点:
         *
         * <pre>
         *   START  … 他の MOD が速度を足す（推進をつける翼など）
         *   START  … ここで削る（LOWEST なので足す側より後に走る）
         *   途中   … Player#tick が travel/move を回す ＝ 削った速度で動く
         *   END    … 移動は済んでいる
         * </pre>
         *
         * <p>⚠ <b>END で削ると、毎 tick の移動には一度も効かない。</b> START で速度を足す MOD が
         * 居ると、END で削った値は次の tick の START で上書きされ、移動はいつも足された側の
         * 速度で行われる。実測（2026-08-28）: Icarus の翼で水平 <b>2.45 blocks/tick</b>
         * ＝ 49 m/s が出ていた（上限は 0.75〜1.5）。同じ手順で素のエリトラは 0.87 で、
         * こちらは計算どおり上限に収まっていた——<b>START で押す者が居ないから</b>。
         *
         * <p>⚠ {@code EventPriority.LOWEST} は「同じ phase の中で最後に走る」ための指定。
         * Forge は優先度の高いものから呼ぶので、既定（NORMAL）で足す側より後になる。
         *
         * <p>⚠ <b>サーバー側だけでは効かない</b>——プレイヤーの移動を計算しているのは
         * クライアントなので、両側で走らせる（この購読に側の条件は付けない）。
         */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.START) {
                return;
            }
            SpeedLimiter.apply(event.player);
        }
    }

    @Mod.EventBusSubscriber(modid = ElytraBoostLimit.MODID, value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Client {

        /**
         * 設定で指定したアイテムに、手ぶら条件の説明を1行足す。
         *
         * <p>⚠ **キー名は書かない**。加速のキーを持っているのは相手のMODで、
         * プレイヤーが割り当てを変えれば食い違う。相手のMODが
         * 「Press %s to boost」のような案内を自分で出すので、こちらは条件だけを足す。
         */
        @SubscribeEvent
        public static void onItemTooltip(ItemTooltipEvent event) {
            if (!Config.REQUIRE_EMPTY_HANDS.get()) {
                return;
            }
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
            if (id == null || !Config.TOOLTIP_ITEMS.get().contains(id.toString())) {
                return;
            }
            event.getToolTip().add(
                    Component.translatable("tooltip.elytra_boost_limit.empty_hands")
                            .withStyle(ChatFormatting.GRAY));
        }
    }
}
