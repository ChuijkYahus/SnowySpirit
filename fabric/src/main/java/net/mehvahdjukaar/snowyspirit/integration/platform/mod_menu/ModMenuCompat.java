package net.mehvahdjukaar.snowyspirit.integration.platform.mod_menu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.mehvahdjukaar.moonlight.core.client.config.MoonlightConfigSelectScreen;
import net.mehvahdjukaar.snowyspirit.SnowySpirit;

public class ModMenuCompat implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> MoonlightConfigSelectScreen.create(SnowySpirit.MOD_ID, parent,
                SnowySpirit.res("textures/block/gingerbread_frosted_block.png"));
    }
}
