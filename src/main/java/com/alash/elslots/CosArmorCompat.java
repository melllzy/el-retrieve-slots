package com.alash.elslots;

import lain.mods.cos.api.CosArmorAPI;
import lain.mods.cos.api.inventory.CAStacksBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * Cosmetic Armor Reworked slots (the 4 cosmetic armor slots next to the player model). Optional: every method is a no-op
 * when the mod is absent, and its classes are only touched after the {@link #LOADED} check.
 */
final class CosArmorCompat {
    static final boolean LOADED = ModList.get().isLoaded("cosmeticarmorreworked");

    private CosArmorCompat() {
    }

    /** Copies of the cosmetic armor stacks, index = cosmetic slot (empty slots included). */
    static List<ItemStack> stacks(Player player) {
        List<ItemStack> out = new ArrayList<>();
        if (LOADED) {
            CAStacksBase cos = CosArmorAPI.getCAStacks(player.getUUID());
            for (int i = 0; i < cos.getSlots(); i++) {
                out.add(cos.getStackInSlot(i).copy());
            }
        }
        return out;
    }

    /** Puts {@code stack} into cosmetic slot {@code idx} if it exists and is empty. */
    static boolean putIfEmpty(Player player, int idx, ItemStack stack) {
        if (!LOADED) {
            return false;
        }
        CAStacksBase cos = CosArmorAPI.getCAStacks(player.getUUID());
        if (idx < 0 || idx >= cos.getSlots() || !cos.getStackInSlot(idx).isEmpty()) {
            return false;
        }
        cos.setStackInSlot(idx, stack.copy());
        return true;
    }
}
