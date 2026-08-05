package net.erutobusiness.elytraboostlimit;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 滑空中の**水平速度だけ**に上限をかける。垂直（y）には一切触らない。
 *
 * <p>狙いは「花火で登るのは自由・花火で世界を横断するのは不可」。だから止めるのは
 * 水平成分だけで、上昇も落下もバニラのまま残す。
 *
 * <h2>なぜ「加速に倍率」ではなく「速度に上限」なのか</h2>
 *
 * バニラの花火ブーストは、毎 tick こう当たる（1.20.1 の {@code FireworkRocketEntity#tick}。
 * x・y・z が完全に同じ式）:
 *
 * <pre>delta.axis += look.axis * 0.1 + (look.axis * 1.5 - delta.axis) * 0.5</pre>
 *
 * これは**目標速度へ寄せていく作り**で、現在速度と {@code 1.5 * 視線} の差を毎 tick 半分
 * 埋める。したがって<b>この加速の項に倍率をかけても、寄っていく先は変わらない</b>
 * （到達が遅くなるだけで最高速は同じ）。収束先は {@code (0.2 + 1.5) * 視線 = 1.7 blocks/tick}
 * ＝ 34 m/s で、よく言われる「花火ブーストは約 33.5 m/s」と一致する。
 *
 * <p>速度そのものに上限を置けば、この綱引きに勝てる。⚠ ただし
 * {@link Config#LIMIT_CATCH_UP_RATE} を下げると綱引きに負ける:
 * ブーストは毎 tick 差の 0.5 を埋めてくるので、削る側が 0.5 だと定常速度は
 * 上限 0.75 に対して 1.07 blocks/tick まで浮く。既定を 1.0（＝きっちり頭打ち）に
 * しているのはこのため。
 *
 * <h2>当てる場所</h2>
 *
 * {@code PlayerTickEvent}(END)。Forge の {@code onPlayerPostTick} は
 * {@code Player#tick} の末尾——{@code LivingEntity#tick}（＝ aiStep・travel・move）の
 * <b>後</b>——で発火する（1.20.1 の実 jar で確認）。よって「今 tick の移動は済み、
 * 削った結果は次 tick に効く」。1 tick の遅れは体感に出ない。
 *
 * <p>⚠ <b>クライアントにも入っていないと効かない。</b> プレイヤーの移動を計算しているのは
 * クライアントで、サーバーはその報告を受け取る側だから。当MODは両側に配る前提。
 * 裏を返すと、部員が自分の config を書き換えれば外せる（性善説で運用する）。
 */
public final class SpeedLimiter {

    private SpeedLimiter() {
    }

    /** 速度がこれ未満なら向きが定まらないので触らない。 */
    private static final double EPSILON = 1.0E-6D;

    /**
     * 垂直速度をならす強さ（1 tick ぶんの寄せ率）。0.1 ＝ おおよそ 10 tick（0.5秒）の平均。
     *
     * <p>⚠ **瞬間の垂直速度をそのまま使ってはいけない。** 実測（2026-08-05）で、
     * 水平飛行のつもりの滑空でも <b>1 tick ごとに揚力と重力で上下に振れている</b>ことが分かった。
     * 30 tick の正味の高度差は -1.16（-0.039/tick）＝ほぼ水平なのに、瞬間値は大きく振れており、
     * それを「上下に動いている」と読んで緩和が入り、**水平が 1.22 blocks/tick（24 m/s）まで出ていた**
     * （狙いは 15 m/s）。
     *
     * <p>符号つきの速度をならしてから絶対値を取る。**絶対値をならすのでは駄目**——振動は
     * ならしても大きいままなので、緩和が入りっぱなしになる。
     */
    private static final double VERTICAL_SMOOTHING = 0.1D;

    /** ならした垂直速度をプレイヤーごとに覚えておく。持ち主が消えれば一緒に消える。 */
    private static final Map<Player, Double> SMOOTHED_VERTICAL =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static void apply(Player player) {
        if (!Config.SPEED_LIMIT_ENABLED.get() || !player.isFallFlying()) {
            SMOOTHED_VERTICAL.remove(player);
            return;
        }
        String dimension = player.level().dimension().location().toString();
        if (Config.EXEMPT_DIMENSIONS.get().contains(dimension)) {
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        double smoothedVertical = smooth(player, velocity.y);

        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontal < EPSILON) {
            return;
        }

        double limit = limitFor(smoothedVertical);
        if (horizontal <= limit) {
            return;
        }

        double rate = Config.LIMIT_CATCH_UP_RATE.get();
        double trimmed = limit + (horizontal - limit) * (1.0D - rate);
        double scale = trimmed / horizontal;

        // ⚠ y はそのまま渡す。ここが「垂直は今までどおり」の実体。
        player.setDeltaMovement(velocity.x * scale, velocity.y, velocity.z * scale);
    }

    /**
     * その瞬間の上限。<b>上下どちらであれ</b>垂直に動いているほど緩める。
     *
     * <p>狙いは「**水平にまっすぐ飛び続けるのが遅い**」の一点。地図を横断する飛び方だけを
     * 抑えたいのであって、上下を使った飛び方まで潰したいわけではない。
     *
     * <p>⚠ この対称の緩和には自明な帰結がある: <b>上下に動かし続ければ水平も
     * {@code divingOrClimbingLimit} で進み続けられる</b>（登りの緩和は高度を消費しないので、
     * 「登る→降りる」のジグザグに上限が無い）。つまり実効の巡航速度は、花火を焚き続ける限り
     * 緩和側の値になる。<b>2026-08-05 に、それを承知のうえでこの形を選んだ</b>——
     * 資源（花火）を払い続ける飛び方が速いのは構わない、という判断。
     * 「まっすぐ水平に飛ぶだけで速い」状態にしないことが目的なので、{@code horizontalLimit}
     * が本体で、こちらは操作している人への緩和と位置づける。
     *
     * <p>段差ではなく**線形に**動かす。急降下から水平へ引き起こしたとき、上限が一気に
     * 下がると壁にぶつかったような減速になるため。
     *
     * <p>登りが潰れる心配は元々要らない。垂直には一切触らないので<b>登る速さ自体は
     * バニラのまま</b>で、絞られるのは「登りながら前へ進む距離」だけ。花火の垂直推力は
     * 重力（0.08 blocks/tick）の数倍あるため、水平が遅いせいで揚力を失って落ちることもない。
     */
    private static double limitFor(double verticalSpeed) {
        double level = Config.HORIZONTAL_LIMIT.get();
        double relieved = Config.DIVING_OR_CLIMBING_LIMIT.get();
        double fullReliefAt = Config.RELIEF_AT_VERTICAL_SPEED.get();

        double vertical = Math.abs(verticalSpeed);
        double t = Math.min(1.0D, vertical / fullReliefAt);
        return level + (relieved - level) * t;
    }

    /** 符号つきの垂直速度をならして覚える。返すのはならした後の値。 */
    private static double smooth(Player player, double verticalSpeed) {
        double previous = SMOOTHED_VERTICAL.getOrDefault(player, verticalSpeed);
        double smoothed = previous + (verticalSpeed - previous) * VERTICAL_SMOOTHING;
        SMOOTHED_VERTICAL.put(player, smoothed);
        return smoothed;
    }
}
