package net.erutobusiness.elytraboostlimit;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.TickEvent;
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
         * <p>Phase.END は {@code Player#tick} の末尾＝移動計算の後なので、削った結果は
         * 次の tick の移動に効く。⚠ <b>サーバー側だけでは効かない</b>——プレイヤーの移動を
         * 計算しているのはクライアントなので、両側で走らせる（この購読に側の条件は付けない）。
         */
        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
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
