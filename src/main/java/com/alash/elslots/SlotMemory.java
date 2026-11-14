package com.alash.elslots;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Death → vessel → pickup bookkeeping.
 * <ol>
 *   <li>On death: snapshot which slot held which stack (inventory, armor, offhand, Curios incl. cosmetic,
 *       Cosmetic Armor Reworked).</li>
 *   <li>When Enigmatic Legacy stores the drops in a Storage Crystal: match each stored stack to the snapshot and
 *       write {@code elslots: [{i: <stack index>, slot: "<key>"}]} into the crystal's NBT (survives restarts).</li>
 *   <li>When the crystal is retrieved: every {@code Inventory.add} call EL makes is redirected to {@link #place},
 *       which puts the stack back into its old slot if that slot is free, otherwise falls back to {@code add}.</li>
 * </ol>
 * Slot keys: {@code inv|N}, {@code armor|N}, {@code offhand|0}, {@code curios|<id>|N}, {@code curios_c|<id>|N},
 * {@code cosarmor|N}.
 */
public final class SlotMemory {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String NBT_KEY = "elslots";
    /** Enigmatic Legacy stores the vessel's contents as {@code storedStack0..storedStack<storedStacks-1>}. */
    private static final String EL_STACK_KEY = "storedStack";
    private static final Map<UUID, List<Entry>> SNAPSHOTS = new ConcurrentHashMap<>();
    private static final ThreadLocal<List<Entry>> RETRIEVING = new ThreadLocal<>();

    private SlotMemory() {
    }

    private static final class Entry {
        final String slot;
        final ItemStack stack;
        boolean used;
        boolean placed;

        Entry(String slot, ItemStack stack) {
            this.slot = slot;
            this.stack = stack;
        }
    }

    // ---- 1. death snapshot -------------------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SNAPSHOTS.put(player.getUUID(), snapshot(player));
        }
    }

    private static List<Entry> snapshot(Player player) {
        List<Entry> out = new ArrayList<>();
        Inventory inv = player.getInventory();
        addAll(out, "inv", inv.items);
        addAll(out, "armor", inv.armor);
        addAll(out, "offhand", inv.offhand);
        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
            for (Map.Entry<String, ICurioStacksHandler> e : handler.getCurios().entrySet()) {
                addAll(out, "curios|" + e.getKey(), e.getValue().getStacks());
                addAll(out, "curios_c|" + e.getKey(), e.getValue().getCosmeticStacks());
            }
        });
        addAll(out, "cosarmor", CosArmorCompat.stacks(player));
        return out;
    }

    private static void addAll(List<Entry> out, String prefix, List<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); i++) {
            if (!stacks.get(i).isEmpty()) {
                out.add(new Entry(prefix + "|" + i, stacks.get(i).copy()));
            }
        }
    }

    private static void addAll(List<Entry> out, String prefix, IDynamicStackHandler stacks) {
        for (int i = 0; i < stacks.getSlots(); i++) {
            if (!stacks.getStackInSlot(i).isEmpty()) {
                out.add(new Entry(prefix + "|" + i, stacks.getStackInSlot(i).copy()));
            }
        }
    }

    // ---- 2. remember slots on the crystal ----------------------------------------------------------------------

    /** Called right after EL filled {@code crystal} with the player's drops. */
    public static void writeSlots(Player player, ItemStack crystal) {
        List<Entry> snapshot = SNAPSHOTS.remove(player.getUUID());
        CompoundTag tag = crystal.getTag();
        if (snapshot == null || tag == null) {
            LOG.info("[elslots] {}: vessel created without a death snapshot (snapshot={}, tag={})",
                    player.getName().getString(), snapshot != null, tag != null);
            return;
        }
        ListTag slots = new ListTag();
        int stored = tag.getInt("storedStacks");
        for (int i = 0; i < stored; i++) {
            ItemStack stack = ItemStack.of(tag.getCompound(EL_STACK_KEY + i));
            Entry match = findUnused(snapshot, stack);
            if (match != null) {
                match.used = true;
                CompoundTag t = new CompoundTag();
                t.putInt("i", i);
                t.putString("slot", match.slot);
                slots.add(t);
            }
        }
        tag.put(NBT_KEY, slots);
        LOG.info("[elslots] {}: vessel holds {} stacks, {} matched to slots (snapshot had {})",
                player.getName().getString(), stored, slots.size(), snapshot.size());
    }

    // ---- 3. put items back on pickup ---------------------------------------------------------------------------

    /** Called at the start of EL's retrieve, before it consumes the stored stacks. */
    public static void begin(ItemStack crystal) {
        CompoundTag tag = crystal.getTag();
        if (tag == null || !tag.contains(NBT_KEY, Tag.TAG_LIST)) {
            RETRIEVING.remove();
            return;
        }
        List<Entry> plan = new ArrayList<>();
        for (Tag t : tag.getList(NBT_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            plan.add(new Entry(c.getString("slot"), ItemStack.of(tag.getCompound(EL_STACK_KEY + c.getInt("i")))));
        }
        RETRIEVING.set(plan);
    }

    public static void end(Player player) {
        List<Entry> plan = RETRIEVING.get();
        RETRIEVING.remove();
        if (plan != null) {
            long placed = plan.stream().filter(e -> e.placed).count();
            LOG.info("[elslots] {}: {} of {} remembered stacks returned to their slots", player.getName().getString(), placed, plan.size());
            plan.stream().filter(e -> !e.placed).forEach(e -> LOG.info("[elslots]   not returned: {} x{} -> {}",
                    e.stack.getItem(), e.stack.getCount(), e.slot));
        }
    }

    /** Replacement for {@code inventory.add(stack)} inside EL's retrieve. */
    public static boolean place(Inventory inventory, ItemStack stack) {
        List<Entry> plan = RETRIEVING.get();
        Entry entry = plan == null ? null : findUnused(plan, stack);
        if (entry != null) {
            entry.used = true;
            if (putInSlot(inventory, entry.slot, stack)) {
                entry.placed = true;
                stack.setCount(0);
                inventory.setChanged();
                return true;
            }
        }
        return inventory.add(stack);
    }

    private static boolean putInSlot(Inventory inv, String key, ItemStack stack) {
        String[] p = key.split("\\|");
        try {
            switch (p[0]) {
                case "inv":
                    return putIfEmpty(inv.items, Integer.parseInt(p[1]), stack);
                case "offhand":
                    return putIfEmpty(inv.offhand, Integer.parseInt(p[1]), stack);
                case "cosarmor":
                    return CosArmorCompat.putIfEmpty(inv.player, Integer.parseInt(p[1]), stack);
                case "armor": {
                    int idx = Integer.parseInt(p[1]);
                    EquipmentSlot slot = EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, idx);
                    return stack.canEquip(slot, inv.player) && putIfEmpty(inv.armor, idx, stack);
                }
                case "curios":
                case "curios_c": {
                    boolean cosmetic = p[0].equals("curios_c");
                    String id = p[1];
                    int idx = Integer.parseInt(p[2]);
                    Optional<IDynamicStackHandler> stacks = CuriosApi.getCuriosHelper().getCuriosHandler(inv.player).resolve()
                            .flatMap(h -> h.getStacksHandler(id))
                            .map(h -> cosmetic ? h.getCosmeticStacks() : h.getStacks());
                    if (stacks.isPresent() && idx < stacks.get().getSlots() && stacks.get().getStackInSlot(idx).isEmpty()) {
                        stacks.get().setStackInSlot(idx, stack.copy());
                        return true;
                    }
                    return false;
                }
                default:
                    return false;
            }
        } catch (RuntimeException e) { // malformed key or a slot that no longer exists: fall back to a normal add
            return false;
        }
    }

    private static boolean putIfEmpty(List<ItemStack> list, int idx, ItemStack stack) {
        if (idx < 0 || idx >= list.size() || !list.get(idx).isEmpty()) {
            return false;
        }
        list.set(idx, stack.copy());
        return true;
    }

    /**
     * First unused entry holding the same stack. Deliberately ignores capability data ({@code ItemStack.matches} does not):
     * the stored stacks went through serializeNBT/ItemStack.of, and some mods' item capabilities don't round-trip exactly,
     * which made every comparison fail. Exact item + count + tag first, then same item + tag with any count.
     */
    private static Entry findUnused(List<Entry> entries, ItemStack stack) {
        for (Entry e : entries) {
            if (!e.used && e.stack.getCount() == stack.getCount() && ItemStack.isSameItemSameTags(e.stack, stack)) {
                return e;
            }
        }
        for (Entry e : entries) {
            if (!e.used && ItemStack.isSameItemSameTags(e.stack, stack)) {
                return e;
            }
        }
        return null;
    }
}
