package net.mehvahdjukaar.snowyspirit.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.snowyspirit.SnowySpirit;
import net.mehvahdjukaar.snowyspirit.reg.ClientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientBoundMarkPosForRebuildMessage(BlockPos pos) implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, ClientBoundMarkPosForRebuildMessage> CODEC =
            Message.makeType(SnowySpirit.res("c2s_rebuild_chunk"), ClientBoundMarkPosForRebuildMessage::new);

    public ClientBoundMarkPosForRebuildMessage(RegistryFriendlyByteBuf buffer) {
        this(buffer.readBlockPos());
    }

    @Override
    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
    }

    @Override
    public void handle(Context context) {
        ClientRegistry.markBlocksForReRender(pos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return CODEC.type();
    }
}