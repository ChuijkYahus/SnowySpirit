package net.mehvahdjukaar.snowyspirit.common.wreath;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.mehvahdjukaar.moonlight.api.misc.WorldSavedData;
import net.mehvahdjukaar.moonlight.api.misc.WorldSavedDataType;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.snowyspirit.reg.ModRegistry;
import net.mehvahdjukaar.supplementaries.common.items.components.BlackboardData;
import net.minecraft.Util;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class ChunksWithWreaths extends WorldSavedData {
    public static final Codec<ChunksWithWreaths> CODEC = Codec.LONG_STREAM
            .xmap(longs -> {
                ChunksWithWreaths data = new ChunksWithWreaths();
                for (long l : longs.toArray()) {
                    data.chunksWithWreaths.add(new ChunkPos(l));
                }
                return data;
            }, d -> d.chunksWithWreaths.stream().mapToLong(ChunkPos::toLong)).fieldOf("chunks").codec();
    public static final StreamCodec<ByteBuf, ChunksWithWreaths> STREAM_CODEC = ByteBufCodecs.VAR_LONG
            .apply(ByteBufCodecs.list()).map(longs -> {
                ChunksWithWreaths data = new ChunksWithWreaths();
                for (long l : longs) {
                    data.chunksWithWreaths.add(new ChunkPos(l));
                }
                return data;
            }, d -> d.chunksWithWreaths.stream().mapToLong(ChunkPos::toLong).boxed().toList());

    private final Set<ChunkPos> chunksWithWreaths = new HashSet<>();

    @Override
    public WorldSavedDataType<? extends WorldSavedData> getType() {
        return ModRegistry.WREATH_WORLD_DATA;
    }


    public Stream<ChunkAccess> getLoadedChunks(ServerLevel level) {
        return chunksWithWreaths.stream().filter(cp -> level.hasChunk(cp.x, cp.z))
                .map(cp -> level.getChunk(cp.x, cp.z));
    }

    public Stream<ChunkAccess> getLoadedChunksInRange(Level level, Vec3 center, int range) {
        return chunksWithWreaths.stream()
                .filter(cp -> center.distanceTo(cp.getWorldPosition().getCenter()) <= range * range)
                .filter(cp -> level.hasChunk(cp.x, cp.z))
                .map(cp -> level.getChunk(cp.x, cp.z));

    }

    public void updateStatus(ChunkPos chunkPos, boolean empty) {
        if (empty) {
            if (chunksWithWreaths.remove(chunkPos)) {
                this.setDirty();
            }
        } else {
            if (chunksWithWreaths.add(chunkPos)) {
                this.setDirty();
            }
        }
    }

    public boolean isEmpty() {
        return chunksWithWreaths.isEmpty();
    }
}
