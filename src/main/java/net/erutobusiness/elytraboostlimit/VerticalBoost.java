package net.erutobusiness.elytraboostlimit;

import net.erutobusiness.elytraboostlimit.mixin.FireworkRocketEntityAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 「花火の推進を垂直だけにする」の中身。mixin は入口だけにして、判断はこちらに置く。
 *
 * <h2>効き方</h2>
 *
 * バニラと同じ「目標へ寄せる」式を、<b>視線ではなく真上</b>に対して当てる:
 *
 * <pre>delta.y += s * 0.1 + (s * 1.5 - delta.y) * 0.5</pre>
 *
 * これは {@code 1.7 * s}（blocks/tick）へ収束する。{@code s} は花火の Flight（1〜3）に
 * {@link Config#VERTICAL_BOOST_PER_FLIGHT_LEVEL} を掛けた値なので、<b>強い花火ほど高く速く上がる</b>。
 * 既定 0.5 なら Flight1 で 0.85 blocks/tick（秒速17ブロック）、Flight2 で 34、Flight3 で 51。
 *
 * <p><b>水平には一切足さない。</b> 減らしもしない（今の速度をそのまま保つ）。
 * ⚠ バニラの式には「上を向くと水平を毎 tick 半分に削って垂直へ振り替える」働きが含まれているが、
 * それも一緒に無くなる。**水平は水平のまま、垂直だけが伸びる**のが、この形の意図。
 */
public final class VerticalBoost {

    private VerticalBoost() {
    }

    /** Flight が読めなかったときの段階（MOD がキー操作で湧かせる素のロケットなど）。 */
    private static final int DEFAULT_FLIGHT = 1;

    public static Vec3 replace(FireworkRocketEntity rocket, LivingEntity attached,
                               Vec3 before, Vec3 vanillaResult) {
        if (!Config.VERTICAL_ONLY_BOOST.get() || !(attached instanceof Player)) {
            return vanillaResult;
        }
        String dimension = rocket.level().dimension().location().toString();
        if (Config.EXEMPT_DIMENSIONS.get().contains(dimension)) {
            // エンドなど、制限を掛けない次元では従来どおり視線方向へ押す
            return vanillaResult;
        }

        double target = Config.VERTICAL_BOOST_PER_FLIGHT_LEVEL.get() * flightLevel(rocket);
        double y = before.y + target * 0.1D + (target * 1.5D - before.y) * 0.5D;
        return new Vec3(before.x, y, before.z);
    }

    /** 花火の Flight（打ち上げの持続時間 1〜3）。読めなければ 1 とみなす。 */
    private static int flightLevel(FireworkRocketEntity rocket) {
        ItemStack item = rocket.getEntityData()
                .get(FireworkRocketEntityAccessor.elytraBoostLimit$fireworksItem());
        if (item.isEmpty() || !item.hasTag()) {
            return DEFAULT_FLIGHT;
        }
        CompoundTag fireworks = item.getTag().getCompound("Fireworks");
        if (!fireworks.contains("Flight")) {
            return DEFAULT_FLIGHT;
        }
        int flight = fireworks.getByte("Flight");
        return flight <= 0 ? DEFAULT_FLIGHT : flight;
    }
}
