package com.eyecrasher.lazoboombox.item;
import com.eyecrasher.lazoboombox.data.BoomboxData; import com.eyecrasher.lazoboombox.server.BoomboxPlaybackManager;
import com.eyecrasher.lazoboombox.block.ModBlocks; import com.eyecrasher.lazodiscs.data.CustomDiscData; import com.eyecrasher.lazodiscs.data.DiscDataUtil;
import net.minecraft.ChatFormatting; import net.minecraft.network.chat.Component; import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource; import net.minecraft.world.InteractionHand; import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player; import net.minecraft.world.item.BlockItem; import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack; import net.minecraft.world.item.Item.TooltipContext; import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level; import net.minecraft.world.level.block.Block;
public class BoomboxItem extends BlockItem {
    public BoomboxItem(Block block, Item.Properties properties) { super(block, properties); }
    // === NEW UX: Shift+RMB does everything; no-shift RMB places the block (vanilla) ===
    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Player player = context.getPlayer(); Level level = context.getLevel();
        if (level != null) { var state = level.getBlockState(context.getClickedPos()); if (state.getBlock() instanceof com.eyecrasher.lazoboombox.block.BoomboxBlock) return InteractionResult.PASS; }
        if (player != null && player.isShiftKeyDown()) { InteractionResult r = smartInteractHeld(context.getLevel(), player, context.getHand()); return r; }
        return super.useOn(context);
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        if (player.isShiftKeyDown()) return smartInteractHeld(level, player, usedHand);
        return super.use(level, player, usedHand);
    }
    private InteractionResult smartInteractHeld(Level level, Player player, InteractionHand usedHand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ItemStack boombox = player.getItemInHand(usedHand);
        if (boombox.getItem() != ModBlocks.BOOMBOX_ITEM.get()) return InteractionResult.PASS;
        InteractionHand otherHand = (usedHand == InteractionHand.MAIN_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherStack = player.getItemInHand(otherHand);
        boolean boxHasDisc = BoomboxData.hasDisc(boombox);
        boolean otherHasDisc = DiscDataUtil.isMusicDisc(otherStack);
        if (otherHasDisc && boxHasDisc) {
            ItemStack oldDisc = BoomboxData.readDisc(boombox, player.registryAccess());
            ItemStack newDisc = otherStack.copy(); newDisc.setCount(1);
            BoomboxData.writeDisc(boombox, newDisc, player.registryAccess());
            otherStack.shrink(1);
            if (otherStack.isEmpty()) player.setItemInHand(otherHand, oldDisc);
            else if (!player.getInventory().add(oldDisc)) Block.popResource((net.minecraft.server.level.ServerLevel) level, player.blockPosition(), oldDisc);
            BoomboxPlaybackManager.INSTANCE.stopHeld(player.getUUID(), "disc-swapped");
            CustomDiscData data = DiscDataUtil.read(newDisc).orElse(null);
            if (data != null) BoomboxPlaybackManager.INSTANCE.tickPlayer((ServerPlayer) player);
            level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.RECORDS, 0.4F, 1.0F);
            return InteractionResult.CONSUME;
        } else if (otherHasDisc) {
            ItemStack discToStore = otherStack.copy(); discToStore.setCount(1);
            BoomboxData.writeDisc(boombox, discToStore, player.registryAccess());
            otherStack.shrink(1); if (otherStack.isEmpty()) player.setItemInHand(otherHand, ItemStack.EMPTY);
            CustomDiscData data = DiscDataUtil.read(discToStore).orElse(null);
            if (data != null) BoomboxPlaybackManager.INSTANCE.tickPlayer((ServerPlayer) player);
            level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.RECORDS, 0.4F, 0.8F);
            return InteractionResult.CONSUME;
        } else if (boxHasDisc) {
            if (!otherStack.isEmpty()) return InteractionResult.CONSUME;
            ItemStack stored = BoomboxData.readDisc(boombox, player.registryAccess());
            BoomboxData.clearDisc(boombox);
            player.setItemInHand(otherHand, stored.isEmpty() ? ItemStack.EMPTY : stored);
            BoomboxPlaybackManager.INSTANCE.stopHeld(player.getUUID(), "disc-extracted");
            level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.RECORDS, 0.4F, 1.2F);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        if (BoomboxData.hasDisc(stack)) {
            Component discName;
            try { ItemStack disc = BoomboxData.readDisc(stack, context.registries()); discName = com.eyecrasher.lazoboombox.data.DiscNameUtil.get(disc, context.registries()); }
            catch (Throwable t) { discName = Component.literal("?"); }
            tooltip.add(Component.translatable("lazoboombox.tooltip.contains_disc", discName).withStyle(ChatFormatting.AQUA));
        } else { tooltip.add(Component.translatable("lazoboombox.tooltip.empty").withStyle(ChatFormatting.DARK_GRAY)); }
        BoomboxData.readOwner(stack).ifPresent(owner -> tooltip.add(Component.translatable("lazoboombox.tooltip.owner", owner.toString()).withStyle(ChatFormatting.DARK_GRAY)));
    }
}
