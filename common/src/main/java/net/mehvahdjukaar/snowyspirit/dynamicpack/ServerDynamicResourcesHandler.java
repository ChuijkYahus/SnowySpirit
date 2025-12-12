package net.mehvahdjukaar.snowyspirit.dynamicpack;

import net.mehvahdjukaar.moonlight.api.resources.RPUtils;
import net.mehvahdjukaar.moonlight.api.resources.RecipeTemplate;
import net.mehvahdjukaar.moonlight.api.resources.SimpleTagBuilder;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynamicServerResourceProvider;
import net.mehvahdjukaar.moonlight.api.resources.pack.PackGenerationStrategy;
import net.mehvahdjukaar.moonlight.api.resources.pack.ResourceGenTask;
import net.mehvahdjukaar.moonlight.api.resources.pack.ResourceSink;
import net.mehvahdjukaar.moonlight.api.set.wood.VanillaWoodTypes;
import net.mehvahdjukaar.snowyspirit.SnowySpirit;
import net.mehvahdjukaar.snowyspirit.reg.ModRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.crafting.Recipe;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class ServerDynamicResourcesHandler extends DynamicServerResourceProvider {

    public static final ServerDynamicResourcesHandler INSTANCE = new ServerDynamicResourcesHandler();

    public ServerDynamicResourcesHandler() {
        super(SnowySpirit.res("generated_pack"), PackGenerationStrategy.CACHED);
    }

    @Override
    protected Collection<String> gatherSupportedNamespaces() {
        return List.of(SnowySpirit.MOD_ID);
    }

    @Override
    protected void regenerateDynamicAssets(Consumer<ResourceGenTask> consumer) {
        consumer.accept(this::regenerateDynamicAssets);
    }

    private void regenerateDynamicAssets(ResourceManager resourceManager, ResourceSink sink) {

        SimpleTagBuilder builder = SimpleTagBuilder.of(SnowySpirit.res("sleds"));
        builder.addEntries(ModRegistry.SLED_ITEMS.values());
        sink.addTag(builder, Registries.ITEM);

        ResourceLocation id = SnowySpirit.res("sled_oak");
        Recipe<?> recipeTemplate = RPUtils.readRecipe(resourceManager, id);

        ModRegistry.SLED_ITEMS.forEach((w, b) -> {
            if (w != VanillaWoodTypes.OAK) {
                var newR = RecipeTemplate.makeSimilarRecipe(recipeTemplate, VanillaWoodTypes.OAK, w, id);
                sink.addRecipe(newR);
            }
        });
    }

}
