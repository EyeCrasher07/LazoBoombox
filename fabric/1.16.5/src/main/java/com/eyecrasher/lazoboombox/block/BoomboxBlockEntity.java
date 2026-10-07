package com.eyecrasher.lazoboombox.block;

import com.eyecrasher.lazoboombox.config.BoomboxConfig;
import com.eyecrasher.lazoboombox.server.BoomboxPlaybackManager;
import com.eyecrasher.lazodiscs.data.CustomDiscData;
import com.eyecrasher.lazodiscs.data.DiscDataUtil;

import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class BoomboxBlockEntity extends BlockEntity
        implements net.minecraft.world.level.block.entity.TickableBlockEntity,
                BlockEntityClientSerializable {
    private ItemStack disc = ItemStack.EMPTY;
    private UUID owner;
    private int color = com.eyecrasher.lazoboombox.data.BoomboxData.UNPAINTED;

    public BoomboxBlockEntity() {
        super(ModBlocks.BOOMBOX_ENTITY);
    }

    public boolean hasDisc() {
        return !disc.isEmpty();
    }

    public ItemStack getDisc() {
        return disc.copy();
    }

    public void setDisc(ItemStack d) {
        disc = d == null ? ItemStack.EMPTY : d.copy();
    }

    public int getColor() {
        return color;
    }

    public void setColor(int value) {
        color =
                com.eyecrasher.lazoboombox.data.BoomboxData.isValidColor(value)
                        ? value
                        : com.eyecrasher.lazoboombox.data.BoomboxData.UNPAINTED;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID o) {
        owner = o;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        super.save(tag);
        if (!disc.isEmpty()) tag.put("disc", disc.save(new CompoundTag()));
        if (owner != null) tag.putString("owner", owner.toString());
        if (color != com.eyecrasher.lazoboombox.data.BoomboxData.UNPAINTED)
            tag.putInt("color", color);
        return tag;
    }

    @Override
    public void load(BlockState state, CompoundTag tag) {
        super.load(state, tag);
        this.disc = ItemStack.of(tag.getCompound("disc"));
        String os = tag.getString("owner");
        if (os != null && !os.isEmpty()) {
            try {
                this.owner = UUID.fromString(os);
            } catch (Exception e) {
                this.owner = null;
            }
        } else {
            this.owner = null;
        }
        // Resync playback immediately after loading from NBT.
        // This is called when the chunk loads or Sable re-places the block entity.
        setColor(tag.contains("color", 3) ? tag.getInt("color") : -1);
        // A block-entity packet must invalidate the cached chunk tint as well.
        if (level != null && level.isClientSide()) {
            BlockState currentState = getBlockState();
            level.sendBlockUpdated(getBlockPos(), currentState, currentState, 2);
        }
        resyncPlayback();
    }

    @Override
    public CompoundTag getUpdateTag() {
        return save(new CompoundTag());
    }

    @Override
    public void fromClientTag(CompoundTag tag) {
        load(getBlockState(), tag);
    }

    @Override
    public CompoundTag toClientTag(CompoundTag tag) {
        return save(tag);
    }

    private void resyncPlayback() {
        if (!hasDisc()) return;
        if (!(level instanceof ServerLevel sl)) return;
        if (sl.getBlockState(getBlockPos()).isAir()) return;
        CustomDiscData data = DiscDataUtil.read(disc).orElse(null);
        if (data == null || !BoomboxConfig.FEATURE_PLACED_BOOMBOX.get()) return;
        BoomboxPlaybackManager.INSTANCE.startPlaced(sl, getBlockPos(), data);
    }

    @Override
    public void setRemoved() {
        // Level events can arrive just after the client removes this block entity.
        if (level != null && level.isClientSide())
            com.eyecrasher.lazoboombox.data.BoomboxParticlePaint.remember(
                    level, getBlockPos().asLong(), color);
        if (level instanceof ServerLevel sl)
            BoomboxPlaybackManager.INSTANCE.stopPlaced(sl, getBlockPos(), "block-entity-removed");
        super.setRemoved();
    }

    private int tickCounter = 0;

    @Override
    public void tick() {
        serverTick();
    }

    public void serverTick() {
        if (!hasDisc()) return;
        if ((++tickCounter) % 20 != 0) return;
        if (!(level instanceof ServerLevel sl)) return;
        if (sl.getBlockState(getBlockPos()).isAir()) return;
        if (!BoomboxPlaybackManager.INSTANCE.isPlacedActive(sl, getBlockPos())) {
            CustomDiscData data = DiscDataUtil.read(disc).orElse(null);
            if (data == null || !BoomboxConfig.FEATURE_PLACED_BOOMBOX.get()) return;
            BoomboxPlaybackManager.INSTANCE.startPlaced(sl, getBlockPos(), data);
        }
        BoomboxPlaybackManager.INSTANCE.updatePlacedPositionIfDynamic(sl, getBlockPos());
    }
}
