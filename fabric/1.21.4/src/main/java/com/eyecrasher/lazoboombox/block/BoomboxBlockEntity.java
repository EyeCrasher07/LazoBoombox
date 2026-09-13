package com.eyecrasher.lazoboombox.block;
import com.eyecrasher.lazoboombox.config.BoomboxConfig; import com.eyecrasher.lazoboombox.server.BoomboxPlaybackManager;
import com.eyecrasher.lazodiscs.data.CustomDiscData; import com.eyecrasher.lazodiscs.data.DiscDataUtil;
import net.minecraft.core.BlockPos; import net.minecraft.core.HolderLookup; import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet; import net.minecraft.network.protocol.game.ClientGamePacketListener; import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel; import net.minecraft.world.item.ItemStack; import net.minecraft.world.level.block.entity.BlockEntity; import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;
public class BoomboxBlockEntity extends BlockEntity {
    private ItemStack disc = ItemStack.EMPTY; private UUID owner;
    public BoomboxBlockEntity(BlockPos pos, BlockState state) { super(ModBlocks.BOOMBOX_ENTITY, pos, state); }
    public boolean hasDisc() { return !disc.isEmpty(); }
    public ItemStack getDisc() { return disc.copy(); }
    public void setDisc(ItemStack d) { disc = d == null ? ItemStack.EMPTY : d; }
    public UUID getOwner() { return owner; }
    public void setOwner(UUID o) { owner = o; }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!disc.isEmpty()) tag.put("disc", disc.save(registries));
        if (owner != null) tag.putString("owner", owner.toString());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.disc = ItemStack.parse(registries, tag.getCompound("disc")).orElse(ItemStack.EMPTY);
        String os = tag.getString("owner");
        if (os != null && !os.isEmpty()) { try { this.owner = UUID.fromString(os); } catch (Exception e) { this.owner = null; } }
        else { this.owner = null; }
        // Resync playback immediately after loading from NBT.
        // This is called when the chunk loads or Sable re-places the block entity.
        resyncPlayback();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithFullMetadata(r); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    private void resyncPlayback() {
        if (!hasDisc()) return;
        if (!(level instanceof ServerLevel sl)) return;
        if (sl.getBlockState(getBlockPos()).isAir()) return;
        CustomDiscData data = DiscDataUtil.read(disc).orElse(null);
        if (data == null || !BoomboxConfig.FEATURE_PLACED_BOOMBOX.get()) return;
        BoomboxPlaybackManager.INSTANCE.startPlaced(sl, getBlockPos(), data);
    }
    private int tickCounter = 0;
    public void serverTick() {
        if (!hasDisc()) return; if ((++tickCounter) % 20 != 0) return; if (!(level instanceof ServerLevel sl)) return;
        if (sl.getBlockState(getBlockPos()).isAir()) return;
        if (!BoomboxPlaybackManager.INSTANCE.isPlacedActive(sl, getBlockPos())) {
            CustomDiscData data = DiscDataUtil.read(disc).orElse(null); if (data == null || !BoomboxConfig.FEATURE_PLACED_BOOMBOX.get()) return;
            BoomboxPlaybackManager.INSTANCE.startPlaced(sl, getBlockPos(), data);
        }
        BoomboxPlaybackManager.INSTANCE.updatePlacedPositionIfDynamic(sl, getBlockPos());
    }
}
