package com.eyecrasher.lazoboombox.block;

import com.eyecrasher.lazoboombox.config.BoomboxConfig;
import com.eyecrasher.lazoboombox.server.BoomboxPlaybackManager;
import com.eyecrasher.lazodiscs.data.CustomDiscData;
import com.eyecrasher.lazodiscs.data.DiscDataUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class BoomboxBlockEntity extends BlockEntity {
    private ItemStack disc = ItemStack.EMPTY;
    private UUID owner;
    private int color = com.eyecrasher.lazoboombox.data.BoomboxData.UNPAINTED;

    public BoomboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BOOMBOX_ENTITY.get(), pos, state);
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!disc.isEmpty()) {
            tag.put("disc", disc.save(registries));
        }
        if (owner != null) tag.putString("owner", owner.toString());
        if (color != com.eyecrasher.lazoboombox.data.BoomboxData.UNPAINTED)
            tag.putInt("color", color);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.disc = ItemStack.parseOptional(registries, tag.getCompound("disc"));
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
        setColor(tag.contains("color", 3) ? tag.getInt("color") : -1);
        // A block-entity packet must invalidate the cached chunk tint as well.
        if (level != null && level.isClientSide()) {
            BlockState currentState = getBlockState();
            level.sendBlockUpdated(
                    getBlockPos(),
                    currentState,
                    currentState,
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider r) {
        return saveWithFullMetadata(r);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Resume saved playback when NeoForge attaches the block entity to its level. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (!hasDisc()) return;
        if (!(level instanceof ServerLevel sl)) return;
        // A moved block entity may briefly remain attached after its block disappears.
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

    public void serverTick() {
        if (!hasDisc()) return;
        if ((++tickCounter) % 20 != 0) return;
        if (!(level instanceof ServerLevel sl)) return;

        // A moved block entity may briefly remain attached after its block disappears.
        if (sl.getBlockState(getBlockPos()).isAir()) return;

        // Restart path: source died (Plasmo Voice addon unloaded, chunk reload, etc.) — restart it.
        if (!BoomboxPlaybackManager.INSTANCE.isPlacedActive(sl, getBlockPos())) {
            CustomDiscData data = DiscDataUtil.read(disc).orElse(null);
            if (data == null || !BoomboxConfig.FEATURE_PLACED_BOOMBOX.get()) return;
            BoomboxPlaybackManager.INSTANCE.startPlaced(sl, getBlockPos(), data);
            return;
        }

        // Sable position update path: if the placed boombox is on a moving platform, project its
        // position to real-world coordinates and update the underlying Plasmo static source.
        // This is the key fix for "boombox on Sable platform is inaudible": without this update,
        // Plasmo's source stays at the original projected position, which becomes stale as soon as
        // the platform moves.
        //
        // We do this every 20 ticks (every server-iterated second) as a fallback — for fast-moving
        // platforms, the SablePostPhysicsTickEvent listener (see BoomboxSableEvents) provides a
        // more responsive update path that fires after every Sable physics step.
        BoomboxPlaybackManager.INSTANCE.updatePlacedPositionIfDynamic(sl, getBlockPos());
    }
}
