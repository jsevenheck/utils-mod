package io.github.jsevenheck.utilsmod.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accesses the live GUI origin used by vanilla slot hit-testing. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("leftPos")
    int utilsMod$getLeftPos();

    @Accessor("topPos")
    int utilsMod$getTopPos();
}
