package net.mehvahdjukaar.snowyspirit.client.block_model;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.geometry.IModelGeometry;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

public class GlowLightsModelGeometry implements IModelGeometry<GlowLightsModelGeometry> {

    private final BlockModel overlay;

    protected GlowLightsModelGeometry(BlockModel overlay) {
        this.overlay = overlay;
    }

    @Override
    public BakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation) {
        BakedModel bakedOverlay = this.overlay.bake(bakery, overlay, spriteGetter, modelTransform, modelLocation, true);
        return new GlowLightsBakedModel(bakedOverlay);
    }

    @Override
    public Collection<Material> getTextures(IModelConfiguration owner, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
        return overlay.getMaterials(modelGetter, missingTextureErrors);
    }
}