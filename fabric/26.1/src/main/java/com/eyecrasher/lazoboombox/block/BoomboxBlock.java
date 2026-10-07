package com.eyecrasher.lazoboombox.block;

import com.eyecrasher.lazoboombox.config.BoomboxConfig;
import com.eyecrasher.lazoboombox.data.BoomboxData;
import com.eyecrasher.lazoboombox.data.DiscNameUtil;
import com.eyecrasher.lazoboombox.server.BoomboxPlaybackManager;
import com.eyecrasher.lazodiscs.data.CustomDiscData;
import com.eyecrasher.lazodiscs.data.DiscDataUtil;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.UUID;

public class BoomboxBlock extends BaseEntityBlock {
    public static final MapCodec<BoomboxBlock> CODEC = simpleCodec(BoomboxBlock::new);

    @Override
    public MapCodec<BoomboxBlock> codec() {
        return CODEC;
    }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    // Relative quarter-turn steps preserve old saves: a missing rotation defaults to zero.
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty ROTATION =
            net.minecraft.world.level.block.state.properties.IntegerProperty.create(
                    "rotation", 0, 3);

    public BoomboxBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(
            net.minecraft.world.item.context.BlockPlaceContext context) {
        return BoomboxOrientation.withStep(
                defaultBlockState(), BoomboxOrientation.stepForYaw(context.getRotation()));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BoomboxOrientation.outline(state);
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BoomboxOrientation.shape(state);
    }

    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return BoomboxOrientation.withStep(
                state, rotation.rotate(BoomboxOrientation.step(state), 16));
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        int yawStep = (BoomboxOrientation.step(state) + 8) & 15;
        return BoomboxOrientation.withStep(state, (mirror.mirror(yawStep, 16) + 8) & 15);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BoomboxBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlocks.BOOMBOX_ENTITY) return null;
        return (l, p, s, be) -> ((BoomboxBlockEntity) be).serverTick();
    }

    @Override
    public ItemStack getCloneItemStack(
            net.minecraft.world.level.LevelReader level,
            BlockPos pos,
            BlockState state,
            boolean includeData) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
        net.minecraft.world.level.block.entity.BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof BoomboxBlockEntity boombox && !stack.isEmpty())
            BoomboxData.writeColor(stack, boombox.getColor());
        return stack;
    }

    @Override
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player) {
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
                                    (ServerLevel) level, pos, data);
                    }
                }
                be.setChanged();
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    // Right click manages discs; Shift + right click picks up the boombox.
    // Vanilla suppresses Block.useItemOn when the player is sneaking with an item that
    // doesn't bypass sneaks (music discs don't). So disc insert/swap must happen on plain
    // RMB, not shift+RMB. Shift+RMB is reserved for picking up the boombox.
    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        BoomboxBlockEntity be = getEntity(sl, pos);
        if (be == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            pickup(sl, pos, player);
            return InteractionResult.SUCCESS;
        }
        smartInteract(sl, pos, player, hand, be);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        BoomboxBlockEntity be = getEntity(sl, pos);
        if (be == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            pickup(sl, pos, player);
            return InteractionResult.SUCCESS;
        }
        // Empty hand + plain RMB: extract disc if present, else show state.
        if (be.hasDisc()) {
            smartInteract(sl, pos, player, InteractionHand.MAIN_HAND, be);
            return InteractionResult.SUCCESS;
        }
        showState(sl, player, be);
        return InteractionResult.SUCCESS;
    }

    private void showState(ServerLevel sl, Player player, BoomboxBlockEntity be) {
        if (be.hasDisc()) {
            Component title = DiscNameUtil.get(be.getDisc(), sl.registryAccess());
            showMessage(
                    player,
                    Component.translatable("lazoboombox.message.now_playing", title)
                            .withStyle(ChatFormatting.AQUA),
                    true);
        } else {
            showMessage(
                    player,
                    Component.translatable("lazoboombox.tooltip.empty")
                            .withStyle(ChatFormatting.YELLOW),
                    true);
        }
    }

    private void smartInteract(
            ServerLevel sl,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BoomboxBlockEntity be) {
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
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundSource.RECORDS,
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
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundSource.RECORDS,
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
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundSource.RECORDS,
                    0.5F,
                    1.2F);
        } else {
            showState(sl, player, be);
        }
    }

    public void pickup(ServerLevel level, BlockPos pos, Player player) {
        BoomboxBlockEntity be = getEntity(level, pos);
        if (be == null) return;
        if (BoomboxConfig.FEATURE_OWNERSHIP.get()
                && BoomboxConfig.OWNERSHIP_ONLY_OWNER_PICKUP.get()) {
            UUID owner = be.getOwner();
            if (owner != null && !owner.equals(player.getUUID())) {
                showMessage(
                        player,
                        Component.translatable("lazoboombox.message.not_owner")
                                .withStyle(ChatFormatting.RED),
                        true);
                return;
            }
        }
        ItemStack boombox = new ItemStack(ModBlocks.BOOMBOX_ITEM);
        if (be.hasDisc()) BoomboxData.writeDisc(boombox, be.getDisc(), level.registryAccess());
        if (be.getOwner() != null) BoomboxData.writeOwner(boombox, be.getOwner());
        BoomboxData.writeColor(boombox, be.getColor());
        BoomboxPlaybackManager.INSTANCE.stopPlaced(level, pos, "picked-up");
        level.removeBlock(pos, false);
        giveToPlayer(player, boombox);
        showMessage(
                player,
                Component.translatable("lazoboombox.message.picked_up")
                        .withStyle(ChatFormatting.AQUA),
                true);
        level.playSound(
                null,
                pos,
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                SoundSource.RECORDS,
                0.5F,
                1.0F);
    }

    private static void showMessage(Player player, Component message, boolean actionBar) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message, actionBar);
        }
    }

    private void giveToPlayer(Player player, ItemStack stack) {
        if (player.getMainHandItem().isEmpty())
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        else if (player.getOffhandItem().isEmpty())
            player.setItemInHand(InteractionHand.OFF_HAND, stack);
        else if (!player.getInventory().add(stack))
            Block.popResource((ServerLevel) player.level(), player.blockPosition(), stack);
    }

    private BoomboxBlockEntity getEntity(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof BoomboxBlockEntity b ? b : null;
    }
}
