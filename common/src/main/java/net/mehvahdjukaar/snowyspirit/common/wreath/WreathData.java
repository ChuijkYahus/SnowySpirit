package net.mehvahdjukaar.snowyspirit.common.wreath;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.mehvahdjukaar.moonlight.api.misc.WorldSavedData;
import net.mehvahdjukaar.moonlight.api.misc.WorldSavedDataType;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.snowyspirit.common.network.ClientBoundMarkPosForRebuildMessage;
import net.mehvahdjukaar.snowyspirit.reg.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class WreathData {

    public static final Codec<WreathData> CODEC = BlockPos.CODEC.listOf().xmap(list -> {
        WreathData data = new WreathData();
        data.wreathBlocks.addAll(list);
        return data;
    }, d -> d.wreathBlocks.stream().toList());

    public static final StreamCodec<ByteBuf, WreathData> STREAM_CODEC =
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()).map(l -> {
                WreathData data = new WreathData();
                data.wreathBlocks.addAll(l);
                return data;
            }, w -> w.wreathBlocks.stream().toList());

    private final Set<BlockPos> wreathBlocks = new HashSet<>();

    public static WreathData getAt(Level level, BlockPos pos) {
        return ModRegistry.WREATH_CHUNK_DATA.getOrCreate(level.getChunkAt(pos));
    }


    public void setWreath(BlockPos pos, Level level) {
        wreathBlocks.add(pos);
    }

    public void removeWreath(BlockPos p, Level level, boolean animationAndDrop) {
        wreathBlocks.remove(p);
        if (animationAndDrop) {
            ItemEntity itementity = new ItemEntity(level, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                    ModRegistry.WREATH.get().asItem().getDefaultInstance());
            itementity.setDefaultPickUpDelay();
            level.addFreshEntity(itementity);
            level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, p, Block.getId(ModRegistry.WREATH.get().defaultBlockState()));
        }
    }

    public Set<BlockPos> getWreathBlocks() {
        return wreathBlocks;
    }

    public boolean isEmpty() {
        return this.wreathBlocks.isEmpty();
    }

    public boolean hasWreathAt(BlockPos p) {
        return this.wreathBlocks.contains(p);
    }

    public void markDirty(BlockPos pos, ServerLevel level) {
        //save chunk
        level.getChunkAt(pos).setUnsaved(true);
        //sync to clients
        ModRegistry.WREATH_CHUNK_DATA.sync(level.getChunkAt(pos));
        //tell clients to rebuild chunk
        NetworkHelper.sendToAllClientPlayersTrackingChunk(level, new ChunkPos(pos),
                new ClientBoundMarkPosForRebuildMessage(pos));
        //mark as a chunk that has wreath on server
        ModRegistry.WREATH_WORLD_DATA.getData(level).updateStatus(new ChunkPos(pos), wreathBlocks.isEmpty());
    }

    public boolean hasWreathsInSection(int minY, int maxY) {
        for (BlockPos pos : wreathBlocks) {
            if (pos.getY() >= minY && pos.getY() < maxY) {
                return true;
            }
        }
        return false;
    }

}
