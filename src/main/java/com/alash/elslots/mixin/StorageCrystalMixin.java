package com.alash.elslots.mixin;

import com.aizistral.enigmaticlegacy.items.StorageCrystal;
import com.alash.elslots.SlotMemory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/** Hooks into Enigmatic Legacy's Storage Crystal (the Extradimensional Vessel's contents). See {@link SlotMemory}. */
@Mixin(value = StorageCrystal.class, remap = false)
public abstract class StorageCrystalMixin {

    @Inject(method = "storeDropsOnCrystal", at = @At("RETURN"))
    private void elslots$rememberSlots(Collection<ItemEntity> drops, Player player, ItemStack soulCrystal,
                                       CallbackInfoReturnable<ItemStack> cir) {
        SlotMemory.writeSlots(player, cir.getReturnValue());
    }

    @Inject(method = "retrieveDropsFromCrystal", at = @At("HEAD"))
    private void elslots$begin(ItemStack crystal, Player player, ItemStack soulCrystal,
                               CallbackInfoReturnable<ItemStack> cir) {
        SlotMemory.begin(crystal);
    }

    @Redirect(method = "retrieveDropsFromCrystal",
            at = @At(value = "INVOKE", remap = true,
                    target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean elslots$placeInOldSlot(Inventory inventory, ItemStack stack) {
        return SlotMemory.place(inventory, stack);
    }

    @Inject(method = "retrieveDropsFromCrystal", at = @At("RETURN"))
    private void elslots$end(ItemStack crystal, Player player, ItemStack soulCrystal,
                             CallbackInfoReturnable<ItemStack> cir) {
        SlotMemory.end(player);
    }
}
