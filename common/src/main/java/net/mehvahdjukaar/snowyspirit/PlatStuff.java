package net.mehvahdjukaar.snowyspirit;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.mehvahdjukaar.moonlight.api.MoonlightRegistry;
import net.minecraft.world.item.ItemStack;

public class PlatStuff {
    @ExpectPlatform
    public static boolean isShear(ItemStack stack) {
       throw new AssertionError();
    }
}
