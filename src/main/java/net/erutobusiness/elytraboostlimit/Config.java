package net.erutobusiness.elytraboostlimit;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCKED_DIMENSIONS;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_EMPTY_HANDS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TOOLTIP_ITEMS;

    public static final ForgeConfigSpec.BooleanValue VERTICAL_ONLY_BOOST;
    public static final ForgeConfigSpec.DoubleValue VERTICAL_BOOST_PER_FLIGHT_LEVEL;

    public static final ForgeConfigSpec.BooleanValue SPEED_LIMIT_ENABLED;
    public static final ForgeConfigSpec.DoubleValue HORIZONTAL_LIMIT;
    public static final ForgeConfigSpec.DoubleValue DIVING_OR_CLIMBING_LIMIT;
    public static final ForgeConfigSpec.DoubleValue RELIEF_AT_VERTICAL_SPEED;
    public static final ForgeConfigSpec.DoubleValue LIMIT_CATCH_UP_RATE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXEMPT_DIMENSIONS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        BLOCKED_DIMENSIONS = b
                .comment("Dimensions where boosting an elytra is switched off entirely.",
                        "A boost rocket attached to a gliding player is cancelled before it enters",
                        "the world, so no acceleration is applied at all.",
                        "Empty (the default) means boosting works everywhere and the speed limit",
                        "below is what keeps it in check. Gliding itself is never touched.")
                .defineList("blockedDimensions", List.of(), o -> o instanceof String);

        REQUIRE_EMPTY_HANDS = b
                .comment("Only let 'keybind style' boosts through when both hands are empty.",
                        "A keybind boost is recognised as a rocket with no firework data on it:",
                        "crafted fireworks always carry a Fireworks tag, mod-spawned boost rockets",
                        "usually do not. This lets a mod bind its boost to right-click without it",
                        "firing every time you use a spyglass or a bow in mid-air.",
                        "Crafted fireworks are unaffected by this option.")
                .define("requireEmptyHands", true);

        TOOLTIP_ITEMS = b
                .comment("Items that get an extra tooltip line explaining the empty-hands rule.",
                        "Ignored when requireEmptyHands is false, or when the item is not present.")
                .defineList("tooltipItems", List.of("deeperdarker:soul_elytra"),
                        o -> o instanceof String);

        VERTICAL_ONLY_BOOST = b
                .comment("",
                        "--- Fireworks push straight up ---",
                        "A firework boost stops pushing along your line of sight and pushes purely",
                        "upward instead. Horizontal speed is left exactly as it is: not added to, and",
                        "not taken away either.",
                        "This makes a rocket a way to gain height rather than a way to gain ground.",
                        "Aiming up while already fast used to fight the horizontal cap - vanilla trades",
                        "half your horizontal speed for lift every tick, and the cap kept trimming what",
                        "was left, so neither the climb nor the speed came out right.")
                .define("verticalOnlyBoost", true);

        VERTICAL_BOOST_PER_FLIGHT_LEVEL = b
                .comment("How hard a rocket pushes up, per point of its Flight duration (1-3).",
                        "The climb settles at 1.7 x this x Flight blocks per tick, so at 0.5 a plain",
                        "Flight 1 rocket climbs at 0.85 (17 m/s) and a Flight 3 one at 2.55 (51 m/s).",
                        "Vanilla, aimed straight up, settles at 1.7 regardless of Flight.")
                .defineInRange("verticalBoostPerFlightLevel", 0.5D, 0.0D, 10.0D);

        SPEED_LIMIT_ENABLED = b
                .comment("",
                        "--- Horizontal speed limit while gliding ---",
                        "Caps how fast a gliding player travels sideways. Vertical movement is never",
                        "touched: climbing on a rocket and dropping like a stone both stay vanilla.",
                        "This is what makes fireworks a way to gain height rather than a way to cross",
                        "the world in a minute.")
                .define("speedLimitEnabled", true);

        HORIZONTAL_LIMIT = b
                .comment("Horizontal speed cap in blocks per tick while flying level.",
                        "Multiply by 20 for blocks per second. Vanilla firework boosting settles at",
                        "about 1.7 (34 m/s); a horse does 0.7 (14 m/s).",
                        "This is the one that decides how fast a straight line across the map is.")
                .defineInRange("horizontalLimit", 0.75D, 0.0D, 100.0D);

        DIVING_OR_CLIMBING_LIMIT = b
                .comment("Horizontal cap while climbing or diving - the cap is relaxed by how fast you",
                        "are moving vertically, in either direction. Flying is fast as long as you keep",
                        "working the vertical: dive to trade height for ground, burn a rocket to climb",
                        "back. Level cruising is the slow way.",
                        "Set equal to horizontalLimit to remove the relief.")
                .defineInRange("divingOrClimbingLimit", 1.5D, 0.0D, 100.0D);

        RELIEF_AT_VERTICAL_SPEED = b
                .comment("Vertical speed (blocks per tick, up or down) at which the relaxed cap applies",
                        "in full. In between, the cap moves smoothly from horizontalLimit to",
                        "divingOrClimbingLimit, so levelling out slows you down gradually instead of",
                        "hitting a wall.",
                        "The vertical speed used here is smoothed over about half a second, because a",
                        "glide that looks level still swings up and down every tick between lift and",
                        "gravity. Reading the raw per-tick value handed out relief during level flight",
                        "and let it run at 1.22 blocks/tick against a 0.75 cap (measured 2026-08-05).")
                .defineInRange("reliefAtVerticalSpeed", 1.0D, 0.01D, 10.0D);

        LIMIT_CATCH_UP_RATE = b
                .comment("How much of the excess speed is removed each tick. 1.0 keeps you exactly at",
                        "the cap.",
                        "WARNING: lower values do NOT simply feel softer - a firework pulls your speed",
                        "toward its target by half the gap every tick, which is stronger than a gentle",
                        "trim, so a low rate lets boosted flight settle well above the cap.",
                        "At 0.5 a level boost settles around 1.07 blocks/tick even with the cap at 0.75.")
                .defineInRange("limitCatchUpRate", 1.0D, 0.0D, 1.0D);

        EXEMPT_DIMENSIONS = b
                .comment("",
                        "Dimensions left completely alone: no speed limit, and fireworks push along",
                        "your line of sight the way they do in vanilla.")
                .defineList("exemptDimensions", List.of("minecraft:the_end"),
                        o -> o instanceof String);

        SPEC = b.build();
    }

    private Config() {
    }
}
