package net.erutobusiness.elytraboostlimit.mixin;

import java.util.OptionalInt;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * バニラが private で持っている2つを読めるようにする。
 *
 * <ul>
 *   <li>{@code DATA_ATTACHED_TO_TARGET} … このロケットが誰に取り付いているか。
 *       近くのプレイヤーを探す方式だと、たまたま近くを滑空していた他人の花火まで
 *       巻き込むので、**取り付き先を正面から読む**</li>
 *   <li>{@code DATA_ID_FIREWORKS_ITEM} … 元になったアイテム。
 *       クラフト品か、MOD が湧かせた素のロケットかの見分けに使う</li>
 * </ul>
 */
@Mixin(FireworkRocketEntity.class)
public interface FireworkRocketEntityAccessor {

    @Accessor("DATA_ATTACHED_TO_TARGET")
    static EntityDataAccessor<OptionalInt> elytraBoostLimit$attachedToTarget() {
        throw new AssertionError();
    }

    @Accessor("DATA_ID_FIREWORKS_ITEM")
    static EntityDataAccessor<ItemStack> elytraBoostLimit$fireworksItem() {
        throw new AssertionError();
    }
}
