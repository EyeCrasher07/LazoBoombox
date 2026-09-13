package com.eyecrasher.lazoboombox.block;
import com.eyecrasher.lazoboombox.config.BoomboxConfig; import com.eyecrasher.lazoboombox.server.BoomboxPlaybackManager;
import com.eyecrasher.lazodiscs.data.CustomDiscData; import com.eyecrasher.lazodiscs.data.DiscDataUtil;
import net.minecraft.core.BlockPos; import net.minecraft.core.HolderLookup; import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet; import net.minecraft.network.protocol.game.ClientGamePacketListener; import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel; import net.minecraft.world.item.ItemStack; import net.minecraft.world.level.block.entity.BlockEntity; import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;
public class BoomboxBlockEntity extends BlockEntity {
    private ItemStack disc = ItemStack.EMPTY; private UUID owner;
    public BoomboxBlockEntity(BlockPos pos, BlockState state) { super(ModBlocks.BOOMBOX_ENTITY.get(), pos, state); }
    public boolean hasDisc() { return !disc.isEmpty(); }
    public ItemStack getDisc() { return disc.copy(); }
    public void setDisc(ItemStack d) { disc = d == null ? ItemStack.EMPTY : d; }
    public UUID getOwner() { return owner; }
    public void setOwner(UUID o) { owner = o; }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Use the single-arg save(registries) — same pattern vanilla JukeboxBlockEntity uses
        // ("tag.put("RecordItem", (Tag) item.save(registries))"). The two-arg overload
        // save(registries, discTag) may not populate the CompoundTag argument and instead
        // return the serialised data as its return value; ignoring that return value means
        // the stored tag is empty and the disc is lost on every serialize/deserialize cycle
        // (which Sable triggers on assembly and disassembly).
        if (!disc.isEmpty()) { tag.put("disc", disc.save(registries)); }
        if (owner != null) tag.putString("owner", owner.toString());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.disc = ItemStack.parse(registries, tag.getCompound("disc")).orElse(ItemStack.EMPTY);
        String os = tag.getString("owner");
        if (os != null && !os.isEmpty()) { try { this.owner = UUID.fromString(os); } catch (Exception e) { this.owner = null; } }
        else { this.owner = null; }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithFullMetadata(r); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    /**
     * Called when this block entity is first added to the world — both on normal chunk load
     * and when Sable restores it at a new position after platform assembly or disassembly.
     *
     * <p>Mirrors the approach used by {@code JukeboxBlockEntityMixin}, which injects into
     * {@code loadAdditional}/{@code onLoad} to resync jukebox playback immediately. Without
     * this hook the boombox would wait up to 20 ticks (1 second) for the next
     * {@link #serverTick()} cycle before attempting to restart, which is noticeable on fast
     * platforms and looks broken to players.
     *
     * <p>When Sable assembles a platform the sequence is:
     * <ol>
     *   <li>{@code onRemove(moved=true)} — we stop playback but keep the block entity alive
     *       so Sable can read its NBT (including the disc).</li>
     *   <li>Sable places the block at the sub-level position and sets the block entity with
     *       the saved NBT → {@code loadAdditional} loads the disc → {@code onLoad} fires.</li>
     *   <li>We call {@code startPlaced} here: if {@code allowPlaybackOnSablePlatforms=true}
     *       the source starts immediately at the projected real-world position; otherwise it
     *       is silently refused and the boombox stays quiet on the assembled platform.</li>
     * </ol>
     * Disassembly follows the exact same path in reverse.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (!hasDisc()) return;
        if (!(level instanceof ServerLevel sl)) return;
        // Orphan guard: if for some reason onLoad fires while our position is already AIR
        // (should not happen in practice, but be safe), skip to avoid ghost playback.
        if (sl.getBlockState(getBlockPos()).isAir()) return;
        CustomDiscData data = DiscDataUtil.read(disc).orElse(null);
        if (data == null || !BoomboxConfig.FEATURE_PLACED_BOOMBOX.get()) return;
        BoomboxPlaybackManager.INSTANCE.startPlaced(sl, getBlockPos(), data);
    }

    private int tickCounter = 0;
    public void serverTick() {
        if (!hasDisc()) return; if ((++tickCounter) % 20 != 0) return; if (!(level instanceof ServerLevel sl)) return;

        // Orphan guard: onRemove(moved=true) keeps this block entity alive in the level's
        // block-entity map so that Sable can read our disc NBT after the block at our
        // position has been set to AIR. Until Sable explicitly calls removeBlockEntity()
        // (or the chunk is reloaded), we are "orphaned" — a block entity with no
        // corresponding block. Attempting to start playback from this position would create
        // a source at a location that is now empty space, so we bail out here.
        if (sl.getBlockState(getBlockPos()).isAir()) return;

        // Restart path: source died (Plasmo Voice addon unloaded, chunk reload, etc.) — restart it.
        if (!BoomboxPlaybackManager.INSTANCE.isPlacedActive(sl, getBlockPos())) {
            CustomDiscData data = DiscDataUtil.read(disc).orElse(null); if (data == null || !BoomboxConfig.FEATURE_PLACED_BOOMBOX.get()) return;
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
