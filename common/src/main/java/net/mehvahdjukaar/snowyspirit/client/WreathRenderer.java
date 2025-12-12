package net.mehvahdjukaar.snowyspirit.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.client.util.RenderUtil;
import net.mehvahdjukaar.snowyspirit.common.wreath.ChunksWithWreaths;
import net.mehvahdjukaar.snowyspirit.common.wreath.WreathData;
import net.mehvahdjukaar.snowyspirit.reg.ModRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Iterator;
import java.util.function.Function;

public class WreathRenderer {

    //mega slow
    public static void renderAllWreaths(PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.player.level();

        ChunksWithWreaths chunksWithWreaths = ModRegistry.WREATH_WORLD_DATA.getData(level);
        if (chunksWithWreaths == null || chunksWithWreaths.isEmpty()) return;
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        poseStack.pushPose();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        RenderSystem.enableDepthTest();


        Iterator<ChunkAccess> loadedChunksInRange = chunksWithWreaths.getLoadedChunksInRange(level, cameraPos, 64).iterator();
        while (loadedChunksInRange.hasNext()) {
            ChunkAccess chunk = loadedChunksInRange.next();
            WreathData wreathData = ModRegistry.WREATH_CHUNK_DATA.getOrNull(chunk);
            if (wreathData == null) continue;
            BlockState state = ModRegistry.WREATH.get().defaultBlockState();
            BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();

            for (BlockPos pos : wreathData.getWreathBlocks()) {


                poseStack.pushPose();
                poseStack.translate(pos.getX() - cameraPos.x(), pos.getY() - cameraPos.y(), pos.getZ() - cameraPos.z());


                BlockState doorState = level.getBlockState(pos);

                if (doorState.getBlock() instanceof DoorBlock) {
                    Direction dir = doorState.getValue(DoorBlock.FACING);
                    boolean open = doorState.getValue(DoorBlock.OPEN);
                    boolean hinge = doorState.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT;
                    if (open) {
                        if (hinge) {
                            dir = dir.getCounterClockWise();
                        } else {
                            dir = dir.getClockWise();
                        }
                    }
                    Vec2 dim = calculateDoorDimensions(level, pos, doorState, dir.getAxis());

                    dir = dir.getOpposite(); //idk why

                    poseStack.translate(0.5, 0.5, 0.5);

                    poseStack.pushPose();
                    poseStack.mulPose(Axis.YP.rotationDegrees(-dir.toYRot()));
                    poseStack.translate(-0.5, -0.5, -0.5 + dim.x);
                    RenderUtil.renderBlock(0, poseStack, bufferSource, state, level, pos, blockRenderer);
                    poseStack.popPose();

                    poseStack.pushPose();
                    poseStack.mulPose(Axis.YP.rotationDegrees(-dir.getOpposite().toYRot()));
                    poseStack.translate(-0.5, -0.5, -0.5 + dim.y);
                    RenderUtil.renderBlock(0, poseStack, bufferSource, state, level, pos, blockRenderer);
                    poseStack.popPose();
                }

                //render stuff
                poseStack.popPose();
            }
        }
        RenderSystem.disableDepthTest();
        bufferSource.endBatch();
        poseStack.popPose();
    }

    public static boolean hasWreathsInChunk(ChunkAccess chunk, BlockPos origin) {
        int minY = origin.getY();
        int maxY = minY + 16;
        WreathData d = ModRegistry.WREATH_CHUNK_DATA.getOrNull(chunk);
        return d != null && d.hasWreathsInSection(minY, maxY);
    }


    public static void renderWreathsInChunk(ChunkAccess chunk, BlockPos origin, PoseStack poseStack, BlockAndTintGetter level,
                                            Function<RenderType, VertexConsumer> getOrCreateChunkBuffer) {
        int minY = origin.getY();
        int maxY = minY + 16;
        WreathData data = ModRegistry.WREATH_CHUNK_DATA.getOrNull(chunk);
        RandomSource random = RandomSource.create();
        VertexConsumer vc = null;
        BlockState wreath = ModRegistry.WREATH.get().defaultBlockState();
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();

        for (var pos : data.getWreathBlocks()) {
            if (pos.getY() >= minY && pos.getY() < maxY) {
                if (vc == null) {
                    vc = getOrCreateChunkBuffer.apply(RenderType.cutout());
                }
                BlockState doorState = level.getBlockState(pos);
                pos = pos.subtract(origin);
                poseStack.pushPose();
                poseStack.translate(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);


                if (doorState.getBlock() instanceof DoorBlock) {
                    Direction dir = doorState.getValue(DoorBlock.FACING);
                    boolean open = doorState.getValue(DoorBlock.OPEN);
                    boolean hinge = doorState.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT;
                    if (open) {
                        if (hinge) {
                            dir = dir.getCounterClockWise();
                        } else {
                            dir = dir.getClockWise();
                        }
                    }
                    Vec2 dim = calculateDoorDimensions(level, pos, doorState, dir.getAxis());


                    poseStack.pushPose();
                    poseStack.mulPose(Axis.YP.rotationDegrees(-dir.toYRot()));
                    poseStack.translate(-0.5, -0.5, -0.5 + dim.y);
                    blockRenderer.getModelRenderer().tesselateBlock(level, blockRenderer.getBlockModel(wreath), wreath,
                            pos, poseStack, vc, false, random,
                            wreath.getSeed(BlockPos.ZERO), OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();

                    poseStack.pushPose();
                    poseStack.mulPose(Axis.YP.rotationDegrees(-dir.getOpposite().toYRot()));
                    poseStack.translate(-0.5, -0.5, -0.5 + dim.x);
                    blockRenderer.getModelRenderer().tesselateBlock(level, blockRenderer.getBlockModel(wreath), wreath,
                            pos, poseStack, vc, false, random,
                            wreath.getSeed(BlockPos.ZERO), OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                } else {
                    poseStack.translate(-0.5, -0.5, -0.5);
                    blockRenderer.getModelRenderer().tesselateBlock(level, blockRenderer.getBlockModel(wreath), wreath,
                            pos, poseStack, vc, false, random,
                            wreath.getSeed(BlockPos.ZERO), OverlayTexture.NO_OVERLAY);
                }


                poseStack.popPose();
            }

        }
    }

    private static Vec2 calculateDoorDimensions(BlockGetter level, BlockPos pos, BlockState state, Direction.Axis axis) {
        state = state.setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.OPEN, Boolean.FALSE)
                .setValue(DoorBlock.HINGE, DoorHingeSide.RIGHT);
        VoxelShape shape = state.getShape(level, pos);
        AABB bounds = shape.bounds();
        double front = bounds.minZ - 1;
        double back = -bounds.maxZ;
        return new Vec2((float) front, (float) back);
    }


}

