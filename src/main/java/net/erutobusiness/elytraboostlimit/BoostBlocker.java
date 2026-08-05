package net.erutobusiness.elytraboostlimit;

import java.util.OptionalInt;

import net.erutobusiness.elytraboostlimit.mixin.FireworkRocketEntityAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 「このロケットは、いま消すべきか」だけを決める。
 *
 * <p>判定は湧いた瞬間（{@code EntityJoinLevelEvent}）に行う。湧いてから消す方式だと
 * 最初の1tick ぶんの加速が乗ってしまうため。
 */
public final class BoostBlocker {

    private BoostBlocker() {
    }

    public static boolean shouldBlock(Entity entity, Level level) {
        // ⚠ instanceof で見る。FireworkRocketEntity を**継承した別の entity type** を
        //    足すMODがあり（当部では Alex's Mobs のエンデリオファージロケットと
        //    Alex's Caves のソーダボトルロケット）、id を並べる方式だと取りこぼす。
        //    継承しているものはここで自動的に対象になる。
        if (!(entity instanceof FireworkRocketEntity rocket)) {
            return false;
        }

        Player rider = attachedPlayer(rocket, level);
        if (rider == null || !rider.isFallFlying()) {
            // 誰にも取り付いていない＝ただの打ち上げ花火。滑空していない＝加速に使っていない
            return false;
        }

        String dimension = level.dimension().location().toString();
        if (Config.BLOCKED_DIMENSIONS.get().contains(dimension)) {
            return true;
        }

        // ここから先は「ブーストが通る次元の中」の話。
        // 速度の制限は SpeedLimiter が別に受け持つので、ここでは通す／通さないだけを見る。
        if (!Config.REQUIRE_EMPTY_HANDS.get()) {
            return false;
        }
        if (!isKeybindStyleBoost(rocket)) {
            // クラフトした花火は手に持って使うものなので、この規則の対象外
            return false;
        }
        if (holdsFireworkRocket(rider)) {
            // ⚠ **手に花火を持っている＝アイテムを使って出した加速**。キー操作由来ではない。
            //    この手当てが無いと、NBT を持たない素のロケット（`/give @s firework_rocket` や
            //    クリエイティブで配ったもの）を**手に持って使った瞬間に消される**。
            //    2026-08-05 に、この規則が全次元へ広がったことで踏みうる罠になった
            //    （それまでは「エンドの中だけ」の規則だったので実害が出にくかった）。
            return false;
        }
        return !bothHandsEmpty(rider);
    }

    /** 取り付き先がプレイヤーならそれを返す。取り付いていなければ null。 */
    private static Player attachedPlayer(FireworkRocketEntity rocket, Level level) {
        OptionalInt id = rocket.getEntityData()
                .get(FireworkRocketEntityAccessor.elytraBoostLimit$attachedToTarget());
        if (id.isEmpty()) {
            return null;
        }
        Entity target = level.getEntity(id.getAsInt());
        return target instanceof Player player ? player : null;
    }

    /**
     * キー1つで出た加速か。
     *
     * <p>見分け方は「**素のロケットかどうか**」。クラフトした花火は必ず {@code Fireworks} タグ
     * （Flight や Explosions）を持つが、MOD がキー操作で湧かせるものは
     * {@code new ItemStack(Items.FIREWORK_ROCKET)} で NBT が空のことが多い。
     *
     * <p>⚠ これは**推定に基づく見分け方**であって保証ではない。NBT を持たせて湧かせる
     * MOD があれば素通りする。逆に、クリエイティブで配った NBT 無しの花火は
     * キー由来と見なされる（サバイバルでは作れないので実害は無い）。
     */
    private static boolean isKeybindStyleBoost(FireworkRocketEntity rocket) {
        ItemStack item = rocket.getEntityData()
                .get(FireworkRocketEntityAccessor.elytraBoostLimit$fireworksItem());
        return item.isEmpty() || !item.hasTag() || !item.getTag().contains("Fireworks");
    }

    private static boolean bothHandsEmpty(Player player) {
        return player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty();
    }

    /** どちらかの手にロケット花火を持っているか（＝アイテムを使って出した加速）。 */
    private static boolean holdsFireworkRocket(Player player) {
        return player.getMainHandItem().is(Items.FIREWORK_ROCKET)
                || player.getOffhandItem().is(Items.FIREWORK_ROCKET);
    }
}
