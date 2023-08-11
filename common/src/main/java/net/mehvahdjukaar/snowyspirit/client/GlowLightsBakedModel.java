package net.mehvahdjukaar.snowyspirit.client;

import net.mehvahdjukaar.moonlight.api.client.model.BakedQuadBuilder;
import net.mehvahdjukaar.moonlight.api.client.model.CustomBakedModel;
import net.mehvahdjukaar.moonlight.api.client.model.ExtraModelData;
import net.mehvahdjukaar.snowyspirit.common.block.GlowLightsBlock;
import net.mehvahdjukaar.snowyspirit.common.block.GlowLightsBlockTile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class GlowLightsBakedModel implements CustomBakedModel {
    private final BlockModelShaper blockModelShaper;
    private final BakedModel overlay;

    public GlowLightsBakedModel(BakedModel overlay, ModelState state) {
        this.overlay = overlay;
        this.blockModelShaper = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
    }

    @Override
    public List<BakedQuad> getBlockQuads(BlockState state, Direction side, RandomSource rand,
                                         RenderType renderType, ExtraModelData extraData) {

        List<BakedQuad> quads = new ArrayList<>();
        if (state != null) {
            //always cutout
            try {
                BlockState mimic = extraData.get(GlowLightsBlockTile.MIMIC_KEY);
                if (mimic != null && !mimic.isAir()) {
                    BakedModel model = blockModelShaper.getBlockModel(mimic);
                    var qu = model.getQuads(state, side, rand);
                   // BakedQuadBuilder builder = BakedQuadBuilder.create();
                 //   for(var q : qu){
                    //} //TODO: fabric emissive
                    quads.addAll(qu);
                }
            } catch (Exception ignored) {
            }

            //need to be added later so they go ontop

            var overlay = this.overlay.getQuads(state, side, rand);
            if (GlowLightsBlock.hasSide(state, side)) {
                quads.addAll(overlay);
            }
        }
        return quads;
    }


    @Override
    public TextureAtlasSprite getBlockParticle(ExtraModelData data) {
        BlockState mimic = data.get(GlowLightsBlockTile.MIMIC_KEY);
        if (mimic != null && !mimic.isAir()) {

            BakedModel model = blockModelShaper.getBlockModel(mimic);
            try {
                return model.getParticleIcon();
            } catch (Exception ignored) {
            }
        }
        return blockModelShaper.getBlockModel(Blocks.OAK_LEAVES.defaultBlockState()).getParticleIcon();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Override
    public ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }
}
