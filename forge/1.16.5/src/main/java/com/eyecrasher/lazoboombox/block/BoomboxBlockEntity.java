package com.eyecrasher.lazoboombox.block;

import com.eyecrasher.lazoboombox.config.BoomboxConfig;
import com.eyecrasher.lazoboombox.server.BoomboxPlaybackManager;
import com.eyecrasher.lazodiscs.data.CustomDiscData;
import com.eyecrasher.lazodiscs.data.DiscDataUtil;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.server.ServerWorld;

import java.util.UUID;

public class BoomboxBlockEntity extends TileEntity
        implements net.minecraft.tileentity.ITickableTileEntity {
    private ItemStack disc = ItemStack.EMPTY;
    private UUID owner;
    private int color = com.eyecrasher.lazoboombox.data.BoomboxData.UNPAINTED;

    public BoomboxBlockEntity() {
        super(ModBlocks.BOOMBOX_ENTITY.get());
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
    public CompoundNBT save(CompoundNBT tag) {
        super.save(tag);
        if (!disc.isEmpty()) tag.put("disc", disc.save(new CompoundNBT()));
        if (owner != null) tag.putString("owner", owner.toString());
        if (color != com.eyecrasher.lazoboombox.data.BoomboxData.UNPAINTED)
            tag.putInt("color", color);
        return tag;
    }

    @Override
    public void load(BlockState state, CompoundNBT tag) {
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
    public CompoundNBT getUpdateTag() {
        return save(new CompoundNBT());
    }

    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(getBlockPos(), 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(
            net.minecraft.network.NetworkManager connection, SUpdateTileEntityPacket packet) {
        load(getBlockState(), packet.getTag());
    }

    private void resyncPlayback() {
        if (!hasDisc()) return;
        if (!(level instanceof ServerWorld sl)) return;
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
        if (level instanceof ServerWorld sl)
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
        if (!(level instanceof ServerWorld sl)) return;
        if (sl.getBlockState(getBlockPos()).isAir()) return;
        if (!BoomboxPlaybackManager.INSTANCE.isPlacedActive(sl, getBlockPos())) {
            CustomDiscData data = DiscDataUtil.read(disc).orElse(null);
            if (data == null || !BoomboxConfig.FEATURE_PLACED_BOOMBOX.get()) return;
            BoomboxPlaybackManager.INSTANCE.startPlaced(sl, getBlockPos(), data);
        }
        BoomboxPlaybackManager.INSTANCE.updatePlacedPositionIfDynamic(sl, getBlockPos());
    }
}
