package net.erutobusiness.elytraboostlimit;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(ElytraBoostLimit.MODID)
public class ElytraBoostLimit {

    public static final String MODID = "elytra_boost_limit";

    public ElytraBoostLimit() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
