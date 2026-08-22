package io.github.jsevenheck.utilsmod.client.mixin;

import io.github.jsevenheck.utilsmod.client.config.ModConfig;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's {@code Hud} normally swaps its locator bar (above the hotbar) out for the XP bar for
 * 100 ticks (5 seconds) every time the player gains experience or closes an anvil/enchanting table
 * screen ({@code Hud#willPrioritizeExperienceInfo}), hiding other players' locator dots almost
 * continuously during normal play (e.g. grinding mobs). There is no Fabric API event for this
 * vanilla-internal priority decision, so this mixin forces it off when
 * {@link ModConfig#keepVanillaLocatorBarVisible} is enabled, leaving the locator bar visible
 * whenever it would otherwise have data to show.
 */
@Mixin(Hud.class)
abstract class HudLocatorBarMixin {

    @Inject(method = "willPrioritizeExperienceInfo", at = @At("HEAD"), cancellable = true)
    private void utilsMod$keepLocatorBarVisible(CallbackInfoReturnable<Boolean> cir) {
        if (ModConfig.get().keepVanillaLocatorBarVisible) {
            cir.setReturnValue(false);
        }
    }
}
