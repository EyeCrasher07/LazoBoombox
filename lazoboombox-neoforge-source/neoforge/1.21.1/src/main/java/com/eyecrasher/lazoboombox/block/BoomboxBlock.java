package com.eyecrasher.lazoboombox.block;
import com.eyecrasher.lazoboombox.config.BoomboxConfig; import com.eyecrasher.lazoboombox.data.BoomboxData;
import com.eyecrasher.lazoboombox.data.DiscNameUtil; import com.eyecrasher.lazoboombox.server.BoomboxPlaybackManager;
import com.eyecrasher.lazodiscs.data.CustomDiscData; import com.eyecrasher.lazodiscs.data.DiscDataUtil;
import com.mojang.serialization.MapCodec; import net.minecraft.ChatFormatting; import net.minecraft.core.BlockPos; import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component; import net.minecraft.server.level.ServerLevel; import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource; import net.minecraft.world.InteractionHand; import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity; import net.minecraft.world.entity.player.Player; import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter; import net.minecraft.world.level.Level; import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block; import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity; import net.minecraft.world.level.block.entity.BlockEntityTicker; import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour; import net.minecraft.world.level.block.state.BlockState; import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties; import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult; import net.minecraft.world.phys.shapes.CollisionContext; import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.UUID;
public class BoomboxBlock extends BaseEntityBlock {
    public static final MapCodec<BoomboxBlock> CODEC = simpleCodec(BoomboxBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    protected static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 5, 16, 10, 11);
    protected static final VoxelShape SHAPE_EAST = Block.box(5, 0, 0, 11, 10, 16);
    public BoomboxBlock(BlockBehaviour.Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)); }
    @Override public MapCodec<BoomboxBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shapeForFacing(state.getValue(FACING)); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shapeForFacing(state.getValue(FACING)); }
    private static VoxelShape shapeForFacing(Direction facing) { return switch (facing) { case NORTH, SOUTH -> SHAPE_NORTH; case EAST, WEST -> SHAPE_EAST; default -> SHAPE_NORTH; }; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BoomboxBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { if (level.isClientSide() || type != ModBlocks.BOOMBOX_ENTITY) return null; return (l, p, s, be) -> ((BoomboxBlockEntity) be).serverTick(); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player) {
            BoomboxBlockEntity be = getEntity(level, pos);
            if (be != null) { BoomboxData.readOwner(stack).ifPresent(be::setOwner); if (BoomboxData.hasDisc(stack)) { ItemStack disc = BoomboxData.readDisc(stack, level.registryAccess()); if (!disc.isEmpty()) { be.setDisc(disc); CustomDiscData data = DiscDataUtil.read(disc).orElse(null); if (data != null) BoomboxPlaybackManager.INSTANCE.startPlaced((ServerLevel) level, pos, data); } } be.setChanged(); }
        }
    }
    // === NEW UX (plain RMB for disc ops, Shift+RMB only for pickup) ===
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level instanceof ServerLevel sl)) return net.minecraft.world.ItemInteractionResult.SUCCESS;
        BoomboxBlockEntity be = getEntity(sl, pos); if (be == null) return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (player.isShiftKeyDown()) { pickup(sl, pos, player); return net.minecraft.world.ItemInteractionResult.SUCCESS; }
        smartInteract(sl, pos, player, hand, be);
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        BoomboxBlockEntity be = getEntity(sl, pos); if (be == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) { pickup(sl, pos, player); return InteractionResult.SUCCESS; }
        if (be.hasDisc()) { smartInteract(sl, pos, player, InteractionHand.MAIN_HAND, be); return InteractionResult.SUCCESS; }
        showState(sl, player, be); return InteractionResult.SUCCESS;
    }
    private void showState(ServerLevel sl, Player player, BoomboxBlockEntity be) {
        if (be.hasDisc()) { Component title = DiscNameUtil.get(be.getDisc(), sl.registryAccess()); player.displayClientMessage(Component.translatable("lazoboombox.message.now_playing", title).withStyle(ChatFormatting.AQUA), true); }
        else { player.displayClientMessage(Component.translatable("lazoboombox.tooltip.empty").withStyle(ChatFormatting.YELLOW), true); }
    }
    private void smartInteract(ServerLevel sl, BlockPos pos, Player player, InteractionHand hand, BoomboxBlockEntity be) {
        ItemStack inHand = player.getItemInHand(hand);
        boolean handHasDisc = DiscDataUtil.isMusicDisc(inHand);
        boolean boxHasDisc = be.hasDisc();
        if (handHasDisc && boxHasDisc) {
            ItemStack oldDisc = be.getDisc(); ItemStack newDisc = inHand.copy(); newDisc.setCount(1);
            be.setDisc(newDisc); be.setChanged(); inHand.shrink(1);
            if (inHand.isEmpty()) player.setItemInHand(hand, oldDisc); else giveToPlayer(player, oldDisc);
            BoomboxPlaybackManager.INSTANCE.stopPlaced(sl, pos, "disc-swapped");
            CustomDiscData data = DiscDataUtil.read(newDisc).orElse(null);
            if (data != null) BoomboxPlaybackManager.INSTANCE.startPlaced(sl, pos, data);
            sl.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.RECORDS, 0.5F, 1.0F);
        } else if (handHasDisc) {
            ItemStack dts = inHand.copy(); dts.setCount(1); be.setDisc(dts); be.setChanged(); inHand.shrink(1);
            if (inHand.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
            CustomDiscData data = DiscDataUtil.read(dts).orElse(null);
            if (data != null) BoomboxPlaybackManager.INSTANCE.startPlaced(sl, pos, data);
            sl.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.RECORDS, 0.5F, 0.8F);
        } else if (boxHasDisc) {
            ItemStack disc = be.getDisc(); be.setDisc(ItemStack.EMPTY); be.setChanged();
            BoomboxPlaybackManager.INSTANCE.stopPlaced(sl, pos, "disc-extracted");
            if (inHand.isEmpty()) player.setItemInHand(hand, disc); else giveToPlayer(player, disc);
            sl.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.RECORDS, 0.5F, 1.2F);
        } else { showState(sl, player, be); }
    }
    private void pickup(ServerLevel level, BlockPos pos, Player player) {
        BoomboxBlockEntity be = getEntity(level, pos); if (be == null) return;
        if (BoomboxConfig.FEATURE_OWNERSHIP.get() && BoomboxConfig.OWNERSHIP_ONLY_OWNER_PICKUP.get()) { UUID owner = be.getOwner(); if (owner != null && !owner.equals(player.getUUID())) { player.displayClientMessage(Component.translatable("lazoboombox.message.not_owner").withStyle(ChatFormatting.RED), true); return; } }
        ItemStack boombox = new ItemStack(ModBlocks.BOOMBOX_ITEM.get());
        if (be.hasDisc()) BoomboxData.writeDisc(boombox, be.getDisc(), level.registryAccess());
        if (be.getOwner() != null) BoomboxData.writeOwner(boombox, be.getOwner());
        BoomboxPlaybackManager.INSTANCE.stopPlaced(level, pos, "picked-up"); level.removeBlock(pos, false); giveToPlayer(player, boombox);
        player.displayClientMessage(Component.translatable("lazoboombox.message.picked_up").withStyle(ChatFormatting.AQUA), true);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.RECORDS, 0.5F, 1.0F);
    }
    private void giveToPlayer(Player player, ItemStack stack) { if (player.getMainHandItem().isEmpty()) player.setItemInHand(InteractionHand.MAIN_HAND, stack); else if (player.getOffhandItem().isEmpty()) player.setItemInHand(InteractionHand.OFF_HAND, stack); else if (!player.getInventory().add(stack)) Block.popResource((ServerLevel) player.level(), player.blockPosition(), stack); }
    private BoomboxBlockEntity getEntity(Level level, BlockPos pos) { BlockEntity be = level.getBlockEntity(pos); return be instanceof BoomboxBlockEntity b ? b : null; }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) { if (!level.isClientSide() && level instanceof ServerLevel sl) { BoomboxBlockEntity be = getEntity(sl, pos); if (be != null) { ItemStack boombox = new ItemStack(ModBlocks.BOOMBOX_ITEM.get()); if (be.hasDisc()) BoomboxData.writeDisc(boombox, be.getDisc(), sl.registryAccess()); if (be.getOwner() != null) BoomboxData.writeOwner(boombox, be.getOwner()); BoomboxPlaybackManager.INSTANCE.stopPlaced(sl, pos, "block-broken"); if (!player.isCreative()) Block.popResource(sl, pos, boombox); } sl.removeBlock(pos, false); } return super.playerWillDestroy(level, pos, state, player); }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) { BoomboxPlaybackManager.INSTANCE.stopPlaced((ServerLevel) level, pos, "block-removed"); super.onRemove(state, level, pos, newState, moved); }
}
