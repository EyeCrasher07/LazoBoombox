package com.eyecrasher.lazoboombox.block;

import com.eyecrasher.lazoboombox.config.BoomboxConfig;
import com.eyecrasher.lazoboombox.data.BoomboxData;
import com.eyecrasher.lazoboombox.data.DiscNameUtil;
import com.eyecrasher.lazoboombox.server.BoomboxPlaybackManager;
import com.eyecrasher.lazodiscs.data.CustomDiscData;
import com.eyecrasher.lazodiscs.data.DiscDataUtil;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.UUID;

public class BoomboxBlock extends ContainerBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    // Relative quarter-turn steps preserve old saves: a missing rotation defaults to zero.
    public static final net.minecraft.state.IntegerProperty ROTATION =
            net.minecraft.state.IntegerProperty.create("rotation", 0, 3);

    public BoomboxBlock(AbstractBlock.Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.item.BlockItemUseContext context) {
        return BoomboxOrientation.withStep(
                defaultBlockState(), BoomboxOrientation.stepForYaw(context.getRotation()));
    }

    @Override
    public BlockRenderType getRenderShape(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public VoxelShape getShape(
            BlockState state, IBlockReader level, BlockPos pos, ISelectionContext context) {
        return BoomboxOrientation.outline(state);
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state, IBlockReader level, BlockPos pos, ISelectionContext context) {
        return BoomboxOrientation.shape(state);
    }

    @Override
    public BlockState rotate(BlockState state, net.minecraft.util.Rotation rotation) {
        return BoomboxOrientation.withStep(
                state, rotation.rotate(BoomboxOrientation.step(state), 16));
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.util.Mirror mirror) {
        int yawStep = (BoomboxOrientation.step(state) + 8) & 15;
        return BoomboxOrientation.withStep(state, (mirror.mirror(yawStep, 16) + 8) & 15);
    }

    @Override
    public TileEntity newBlockEntity(IBlockReader level) {
        return new BoomboxBlockEntity();
    }

    @Override
    public ItemStack getCloneItemStack(
            net.minecraft.world.IBlockReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        net.minecraft.tileentity.TileEntity entity = level.getBlockEntity(pos);
        if (entity instanceof BoomboxBlockEntity boombox && !stack.isEmpty())
            BoomboxData.writeColor(stack, boombox.getColor());
        return stack;
    }

    @Override
    public void setPlacedBy(
            World level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof PlayerEntity player) {
            BoomboxBlockEntity be = getEntity(level, pos);
            if (be != null) {
                be.setColor(BoomboxData.readColor(stack));
                be.setOwner(
                        BoomboxData.readOwner(stack)
                                .orElse(
                                        BoomboxConfig.FEATURE_OWNERSHIP.get()
                                                ? player.getUUID()
                                                : null));
                if (BoomboxData.hasDisc(stack)) {
                    ItemStack disc = BoomboxData.readDisc(stack, level.registryAccess());
                    if (!disc.isEmpty()) {
                        be.setDisc(disc);
                        CustomDiscData data = DiscDataUtil.read(disc).orElse(null);
                        if (data != null)
                            BoomboxPlaybackManager.INSTANCE.startPlaced(
                                    (ServerWorld) level, pos, data);
                    }
                }
                be.setChanged();
                level.sendBlockUpdated(pos, state, state, 2);
            }
        }
    }

    // Right click manages discs; Shift + right click picks up the boombox.
    @Override
    public ActionResultType use(
            BlockState state,
            World level,
            BlockPos pos,
            PlayerEntity player,
            Hand hand,
            BlockRayTraceResult hit) {
        if (!(level instanceof ServerWorld sl)) return ActionResultType.SUCCESS;
        BoomboxBlockEntity be = getEntity(sl, pos);
        if (be == null) return ActionResultType.PASS;
        if (player.isShiftKeyDown()) {
            pickup(sl, pos, player);
            return ActionResultType.CONSUME;
        }
        smartInteract(sl, pos, player, hand, be);
        return ActionResultType.CONSUME;
    }

    private void showState(ServerWorld sl, PlayerEntity player, BoomboxBlockEntity be) {
        if (be.hasDisc()) {
            ITextComponent title = DiscNameUtil.get(be.getDisc(), sl.registryAccess());
            player.displayClientMessage(
                    new net.minecraft.util.text.TranslationTextComponent(
                                    "lazoboombox.message.now_playing", title)
                            .withStyle(TextFormatting.AQUA),
                    true);
        } else {
            player.displayClientMessage(
                    new net.minecraft.util.text.TranslationTextComponent(
                                    "lazoboombox.tooltip.empty")
                            .withStyle(TextFormatting.YELLOW),
                    true);
        }
    }

    private void smartInteract(
            ServerWorld sl, BlockPos pos, PlayerEntity player, Hand hand, BoomboxBlockEntity be) {
        ItemStack inHand = player.getItemInHand(hand);
        boolean handHasDisc = DiscDataUtil.isMusicDisc(inHand);
        boolean boxHasDisc = be.hasDisc();
        if (handHasDisc && boxHasDisc) {
            ItemStack oldDisc = be.getDisc();
            ItemStack newDisc = inHand.copy();
            newDisc.setCount(1);
            be.setDisc(newDisc);
            be.setChanged();
            inHand.shrink(1);
            if (inHand.isEmpty()) player.setItemInHand(hand, oldDisc);
            else giveToPlayer(player, oldDisc);
            BoomboxPlaybackManager.INSTANCE.stopPlaced(sl, pos, "disc-swapped");
            CustomDiscData data = DiscDataUtil.read(newDisc).orElse(null);
            if (data != null) BoomboxPlaybackManager.INSTANCE.startPlaced(sl, pos, data);
            sl.playSound(
                    null,
                    pos,
                    net.minecraft.util.SoundEvents.UI_BUTTON_CLICK,
                    SoundCategory.RECORDS,
                    0.5F,
                    1.0F);
        } else if (handHasDisc) {
            ItemStack dts = inHand.copy();
            dts.setCount(1);
            be.setDisc(dts);
            be.setChanged();
            inHand.shrink(1);
            if (inHand.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
            CustomDiscData data = DiscDataUtil.read(dts).orElse(null);
            if (data != null) BoomboxPlaybackManager.INSTANCE.startPlaced(sl, pos, data);
            sl.playSound(
                    null,
                    pos,
                    net.minecraft.util.SoundEvents.UI_BUTTON_CLICK,
                    SoundCategory.RECORDS,
                    0.5F,
                    0.8F);
        } else if (boxHasDisc) {
            ItemStack disc = be.getDisc();
            be.setDisc(ItemStack.EMPTY);
            be.setChanged();
            BoomboxPlaybackManager.INSTANCE.stopPlaced(sl, pos, "disc-extracted");
            if (inHand.isEmpty()) player.setItemInHand(hand, disc);
            else giveToPlayer(player, disc);
            sl.playSound(
                    null,
                    pos,
                    net.minecraft.util.SoundEvents.UI_BUTTON_CLICK,
                    SoundCategory.RECORDS,
                    0.5F,
                    1.2F);
        } else {
            showState(sl, player, be);
        }
    }

    public void pickup(ServerWorld level, BlockPos pos, PlayerEntity player) {
        BoomboxBlockEntity be = getEntity(level, pos);
        if (be == null) return;
        if (BoomboxConfig.FEATURE_OWNERSHIP.get()
                && BoomboxConfig.OWNERSHIP_ONLY_OWNER_PICKUP.get()) {
            UUID owner = be.getOwner();
            if (owner != null && !owner.equals(player.getUUID())) {
                player.displayClientMessage(
                        new net.minecraft.util.text.TranslationTextComponent(
                                        "lazoboombox.message.not_owner")
                                .withStyle(TextFormatting.RED),
                        true);
                return;
            }
        }
        ItemStack boombox = new ItemStack(ModBlocks.BOOMBOX_ITEM.get());
        if (be.hasDisc()) BoomboxData.writeDisc(boombox, be.getDisc(), level.registryAccess());
        if (be.getOwner() != null) BoomboxData.writeOwner(boombox, be.getOwner());
        BoomboxData.writeColor(boombox, be.getColor());
        BoomboxPlaybackManager.INSTANCE.stopPlaced(level, pos, "picked-up");
        level.removeBlock(pos, false);
        giveToPlayer(player, boombox);
        player.displayClientMessage(
                new net.minecraft.util.text.TranslationTextComponent(
                                "lazoboombox.message.picked_up")
                        .withStyle(TextFormatting.AQUA),
                true);
        level.playSound(
                null,
                pos,
                net.minecraft.util.SoundEvents.UI_BUTTON_CLICK,
                SoundCategory.RECORDS,
                0.5F,
                1.0F);
    }

    private void giveToPlayer(PlayerEntity player, ItemStack stack) {
        if (player.getMainHandItem().isEmpty()) player.setItemInHand(Hand.MAIN_HAND, stack);
        else if (player.getOffhandItem().isEmpty()) player.setItemInHand(Hand.OFF_HAND, stack);
        else if (!player.inventory.add(stack))
            Block.popResource((ServerWorld) player.level, player.blockPosition(), stack);
    }

    private BoomboxBlockEntity getEntity(World level, BlockPos pos) {
        TileEntity be = level.getBlockEntity(pos);
        return be instanceof BoomboxBlockEntity b ? b : null;
    }
}
