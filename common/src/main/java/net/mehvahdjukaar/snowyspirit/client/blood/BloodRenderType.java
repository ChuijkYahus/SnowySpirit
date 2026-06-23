package net.mehvahdjukaar.snowyspirit.client.blood;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.snowyspirit.SnowySpirit;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Render type for blood decals: translucent, no backface cull, depth-tested but not depth-written,
 * with a polygon offset so the decal sits just above the surface it was projected onto.
 *
 * <p>Extends {@link RenderType} purely to reach the protected {@code RenderStateShard} constants and
 * {@link #create} factory. Uses {@code POSITION_COLOR_TEX} with the projector's per-vertex UVs; the
 * vertex color tints the texture and carries the per-vertex distance falloff alpha.
 */
public class BloodRenderType extends RenderType {

    // debug texture: 4 colored quadrants (TL red, TR green, BL blue, BR yellow) to read UV orientation
    private static final ResourceLocation TEXTURE = SnowySpirit.res("textures/effect/blood_debug.png");

    private BloodRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                            boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static final RenderType BLOOD = create(
            "snowyspirit_blood",
            DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.TRIANGLES,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                    .setTextureState(new TextureStateShard(TEXTURE, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setLayeringState(POLYGON_OFFSET_LAYERING)
                    .createCompositeState(false));
}
