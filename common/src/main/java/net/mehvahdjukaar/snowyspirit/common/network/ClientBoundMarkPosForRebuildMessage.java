package net.mehvahdjukaar.snowyspirit.common.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.snowyspirit.SnowySpirit;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class ClientBoundMarkPosForRebuildMessage implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, ClientBoundMarkPosForRebuildMessage> CODEC =
            Message.makeType(SnowySpirit.res("c2s_rebuild_chunk"), ClientBoundMarkPosForRebuildMessage::new);

    public final BlockPos pos;

    public ClientBoundMarkPosForRebuildMessage(RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
    }

    public ClientBoundMarkPosForRebuildMessage(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
    }

    @Override
    public void handle(Context context) {
        handleClient();
    }

    @Environment(EnvType.CLIENT)
    private void handleClient() {
        Minecraft.getInstance().levelRenderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return CODEC.type();
    }
}