package net.erutobusiness.elytraboostlimit.mixin;

import net.erutobusiness.elytraboostlimit.VerticalBoost;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 花火ブーストの推進を**垂直だけ**にする。
 *
 * <p>バニラ（1.20.1 の {@code FireworkRocketEntity#tick}）は、x・y・z に<b>同じ式</b>を当てて
 * 視線方向へ寄せていく:
 *
 * <pre>delta.axis += look.axis * 0.1 + (look.axis * 1.5 - delta.axis) * 0.5</pre>
 *
 * この式が当たる {@code setDeltaMovement} は tick の中に1か所しかないので、そこだけ差し替える。
 * 上を向いたときの「水平を毎 tick 半分に削る」働きもここに含まれるため、**差し替えると
 * 水平には一切触らなくなる**（今の速度をそのまま保つ）。狙いどおり。
 *
 * <h2>なぜ視線を見ないのか</h2>
 *
 * 「水平に速く飛んでいるとき、上を向いて花火を使っても高度を確保できない」という声から始めた変更
 * （2026-08-05）。視線に沿って押す作りのままだと、水平を向いていれば水平に押され、その水平は
 * 上限で削られる——**押しては削るの綱引き**になり、どちらも中途半端になる。
 * 花火の力を最初から垂直にだけ出せば、この綱引きが起きない。
 */
@Mixin(FireworkRocketEntity.class)
public abstract class FireworkBoostMixin {

    /**
     * ⚠ 差し替え先は <b>{@code LivingEntity} の</b> {@code setDeltaMovement}。
     * 同じ tick の中にロケット自身への {@code setDeltaMovement} もあるが、そちらは受け手の型が
     * {@code FireworkRocketEntity} なので、この指定では捕まらない。
     */
    @Redirect(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;"
                            + "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void elytraBoostLimit$verticalOnly(LivingEntity attached, Vec3 vanillaResult) {
        FireworkRocketEntity rocket = (FireworkRocketEntity) (Object) this;
        // ⚠ ここではまだ書き換わっていないので、getDeltaMovement は**押される前**の速度。
        Vec3 before = attached.getDeltaMovement();
        Vec3 replacement = VerticalBoost.replace(rocket, attached, before, vanillaResult);
        attached.setDeltaMovement(replacement);
    }
}
