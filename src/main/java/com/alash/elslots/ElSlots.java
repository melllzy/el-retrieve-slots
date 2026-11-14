package com.alash.elslots;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

/**
 * Makes Enigmatic Legacy's Extradimensional Vessel put items back into the slots they were in when the player died.
 * Server-side only; the hooks into Enigmatic Legacy live in {@link com.alash.elslots.mixin.StorageCrystalMixin}.
 */
@Mod(ElSlots.MOD_ID)
public class ElSlots {
    public static final String MOD_ID = "elslots";

    public ElSlots() {
        MinecraftForge.EVENT_BUS.register(SlotMemory.class);
    }
}
