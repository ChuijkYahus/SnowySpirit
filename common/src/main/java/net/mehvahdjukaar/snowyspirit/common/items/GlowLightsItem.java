package net.mehvahdjukaar.snowyspirit.common.items;

import net.mehvahdjukaar.snowyspirit.common.block.GlowLightsBlockTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class GlowLightsItem extends BlockItem {

    public GlowLightsItem(Block pBlock) {
        super(pBlock, new Properties());
    }

    public static class SelfPlacementContext extends BlockPlaceContext {
        public SelfPlacementContext(Player player, InteractionHand interactionHand, ItemStack itemStack, BlockHitResult blockHitResult) {
            super(player, interactionHand, itemStack, blockHitResult);
            this.replaceClicked = true;
        }
    }

    @Override
    protected SoundEvent getPlaceSound(BlockState state) {
        return SoundEvents.AMETHYST_CLUSTER_HIT;
    }

    @Override
    public @Nullable BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        Direction face = context.getClickedFace();
        BlockPos targetPos = pos.relative(face.getOpposite());
        BlockState targetState = level.getBlockState(targetPos);
        if (GlowLightsBlockTile.isValidBlock(targetState, targetPos, level)) {
            BlockHitResult hit = new BlockHitResult(context.getClickLocation(), face, targetPos, true);
            return new SelfPlacementContext(context.getPlayer(), context.getHand(), context.getItemInHand(),hit);
        }
        return null;
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState oldState = level.getBlockState(pos);
        boolean b = super.placeBlock(context, state);
        if (level.getBlockEntity(pos) instanceof GlowLightsBlockTile tile) {
            tile.acceptBlock(oldState);
        }
        return b;
    }


}
