package com.eyecrasher.lazoboombox.item;

import com.eyecrasher.lazoboombox.data.BoomboxData;
import com.eyecrasher.lazoboombox.server.BoomboxPlaybackManager;
import com.eyecrasher.lazodiscs.data.CustomDiscData;
import com.eyecrasher.lazodiscs.data.DiscDataUtil;

import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public class BoomboxItem extends BlockItem {
    public BoomboxItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    // Shift + right click manages the stored disc; ordinary use places the block.
    @Override
    public ActionResultType useOn(net.minecraft.item.ItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        World level = context.getLevel();
        if (level != null) {
            var state = level.getBlockState(context.getClickedPos());
            if (state.getBlock() instanceof com.eyecrasher.lazoboombox.block.BoomboxBlock)
                return ActionResultType.PASS;
        }
        if (player != null && player.isShiftKeyDown()) {
            ActionResultType r = smartInteractHeld(context.getLevel(), player, context.getHand());
            return r;
        }
        if (!level.isClientSide()
                && !com.eyecrasher.lazoboombox.config.BoomboxConfig.FEATURE_PLACED_BOOMBOX.get())
            return ActionResultType.FAIL;
        return super.useOn(context);
    }

    @Override
    public net.minecraft.util.ActionResult<ItemStack> use(
            World level, PlayerEntity player, Hand usedHand) {
        if (player.isShiftKeyDown()) {
            ActionResultType r = smartInteractHeld(level, player, usedHand);
            return new net.minecraft.util.ActionResult<>(r, player.getItemInHand(usedHand));
        }
        return super.use(level, player, usedHand);
    }

    private ActionResultType smartInteractHeld(World level, PlayerEntity player, Hand usedHand) {
        if (level.isClientSide()) return ActionResultType.SUCCESS;
        ItemStack boombox = player.getItemInHand(usedHand);
        if (!(boombox.getItem() instanceof BoomboxItem)) return ActionResultType.PASS;
        Hand otherHand = (usedHand == Hand.MAIN_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND;
        ItemStack otherStack = player.getItemInHand(otherHand);
        boolean boxHasDisc = BoomboxData.hasDisc(boombox);
        boolean otherHasDisc = DiscDataUtil.isMusicDisc(otherStack);
        if (otherHasDisc && boxHasDisc) {
            ItemStack oldDisc = BoomboxData.readDisc(boombox, player.level.registryAccess());
            ItemStack newDisc = otherStack.copy();
            newDisc.setCount(1);
            BoomboxData.writeDisc(boombox, newDisc, player.level.registryAccess());
            otherStack.shrink(1);
            if (otherStack.isEmpty()) player.setItemInHand(otherHand, oldDisc);
            else if (!player.inventory.add(oldDisc))
                Block.popResource(
                        (net.minecraft.world.server.ServerWorld) level,
                        player.blockPosition(),
                        oldDisc);
            BoomboxPlaybackManager.INSTANCE.stopHeld(player.getUUID(), "disc-swapped");
            CustomDiscData data = DiscDataUtil.read(newDisc).orElse(null);
            if (data != null)
                BoomboxPlaybackManager.INSTANCE.tickPlayer((ServerPlayerEntity) player);
            level.playSound(
                    null,
                    player.blockPosition(),
                    net.minecraft.util.SoundEvents.UI_BUTTON_CLICK,
                    SoundCategory.RECORDS,
                    0.4F,
                    1.0F);
            return ActionResultType.CONSUME;
        } else if (otherHasDisc) {
            ItemStack discToStore = otherStack.copy();
            discToStore.setCount(1);
            BoomboxData.writeDisc(boombox, discToStore, player.level.registryAccess());
            otherStack.shrink(1);
            if (otherStack.isEmpty()) player.setItemInHand(otherHand, ItemStack.EMPTY);
            CustomDiscData data = DiscDataUtil.read(discToStore).orElse(null);
            if (data != null)
                BoomboxPlaybackManager.INSTANCE.tickPlayer((ServerPlayerEntity) player);
            level.playSound(
                    null,
                    player.blockPosition(),
                    net.minecraft.util.SoundEvents.UI_BUTTON_CLICK,
                    SoundCategory.RECORDS,
                    0.4F,
                    0.8F);
            return ActionResultType.CONSUME;
        } else if (boxHasDisc) {
            if (!otherStack.isEmpty()) return ActionResultType.CONSUME;
            ItemStack stored = BoomboxData.readDisc(boombox, player.level.registryAccess());
            BoomboxData.clearDisc(boombox);
            player.setItemInHand(otherHand, stored.isEmpty() ? ItemStack.EMPTY : stored);
            BoomboxPlaybackManager.INSTANCE.stopHeld(player.getUUID(), "disc-extracted");
            level.playSound(
                    null,
                    player.blockPosition(),
                    net.minecraft.util.SoundEvents.UI_BUTTON_CLICK,
                    SoundCategory.RECORDS,
                    0.4F,
                    1.2F);
            return ActionResultType.CONSUME;
        }
        return ActionResultType.PASS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            World context,
            java.util.List<ITextComponent> tooltip,
            ITooltipFlag flag) {
        if (BoomboxData.hasDisc(stack)) {
            ITextComponent discName;
            try {
                ItemStack disc =
                        BoomboxData.readDisc(
                                stack, context == null ? null : context.registryAccess());
                discName =
                        com.eyecrasher.lazoboombox.data.DiscNameUtil.get(
                                disc, context == null ? null : context.registryAccess());
            } catch (Throwable t) {
                discName = new net.minecraft.util.text.StringTextComponent("?");
            }
            tooltip.add(
                    new net.minecraft.util.text.TranslationTextComponent(
                                    "lazoboombox.tooltip.contains_disc", discName)
                            .withStyle(TextFormatting.AQUA));
        } else {
            tooltip.add(
                    new net.minecraft.util.text.TranslationTextComponent(
                                    "lazoboombox.tooltip.empty")
                            .withStyle(TextFormatting.DARK_GRAY));
        }
        BoomboxData.readOwner(stack)
                .ifPresent(
                        owner ->
                                tooltip.add(
                                        new net.minecraft.util.text.TranslationTextComponent(
                                                        "lazoboombox.tooltip.owner",
                                                        owner.toString())
                                                .withStyle(TextFormatting.DARK_GRAY)));
    }
}
