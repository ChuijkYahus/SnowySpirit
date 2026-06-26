package net.mehvahdjukaar.snowyspirit.platform;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.snowyspirit.SnowySpirit;
import net.mehvahdjukaar.snowyspirit.client.blood.BloodStainHandler;
import net.mehvahdjukaar.snowyspirit.client.WreathRenderer;
import net.mehvahdjukaar.snowyspirit.common.wreath.WreathHandler;
import net.mehvahdjukaar.snowyspirit.reg.ModTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.phys.Vec3;

public class SnowySpiritFabric implements ModInitializer {

    @Override
    public void onInitialize() {

        SnowySpirit.commonInit();

        PlatHelper.addCommonSetup(SnowySpiritFabric::commonSetup);

        if (PlatHelper.getPhysicalSide().isClient()) {
            WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
                WreathRenderer.renderAllWreaths(context.matrixStack());
            });

            // render blood decals last, in their own buffer pass (never baked into chunk meshes)
            WorldRenderEvents.LAST.register(context ->
                    BloodStainHandler.render(context.matrixStack()));

            // ghast tear: fire a blood splatter along the player's look direction
            UseItemCallback.EVENT.register((player, world, hand) -> {
                ItemStack stack = player.getItemInHand(hand);
                if (world.isClientSide && stack.is(Items.GHAST_TEAR)) {
                    Vec3 origin = player.getEyePosition();
                    Vec3 dir = player.getViewVector(1.0f);
                    BloodStainHandler.addSplatter(world, origin, dir, 0.75f, 5.0f);
                    return InteractionResultHolder.success(stack);
                }
                return InteractionResultHolder.pass(stack);
            });
        }

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
                WreathHandler.onRightClickBlock(player, world, player.getItemInHand(hand), hitResult.getBlockPos()));
    }


    private static void commonSetup() {
        BiomeModifications.addFeature(BiomeSelectors.tag(ModTags.HAS_GINGER),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                ResourceKey.create(Registries.PLACED_FEATURE, SnowySpirit.res("wild_ginger")));

        BiomeModifications.addFeature(BiomeSelectors.tag(ModTags.HAS_GINGER_DENSE),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                ResourceKey.create(Registries.PLACED_FEATURE, SnowySpirit.res("wild_ginger_dense")));
    }


}
