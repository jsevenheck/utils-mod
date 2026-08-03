package io.github.jsevenheck.compasshud.client;

import io.github.jsevenheck.compasshud.CompassHUD;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

public class CompassHUDClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HudElementRegistry.addLast(CompassHUD.id("compass"), new CompassHudRenderer());
	}
}