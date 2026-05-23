package net.mehvahdjukaar.snowyspirit.platform;

import net.mehvahdjukaar.snowyspirit.SnowySpirit;
import net.mehvahdjukaar.snowyspirit.client.WreathRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;

@EventBusSubscriber(modid = SnowySpirit.MOD_ID, value = Dist.CLIENT)
public class SnowySpiritForgeClient {


    @SubscribeEvent
    public static void addWreathsGeometry(AddSectionGeometryEvent event) {
        Level level = event.getLevel();
        BlockPos origin = new BlockPos(event.getSectionOrigin());
        ChunkAccess chunk = level.getChunk(origin);
        if (WreathRenderer.hasWreathsInChunk(chunk, origin)) {
            event.addRenderer(context -> {
                WreathRenderer.renderWreathsInChunk(chunk,origin, context.getPoseStack(), context.getRegion(),
                        context::getOrCreateChunkBuffer);
            });
        }
    }


}