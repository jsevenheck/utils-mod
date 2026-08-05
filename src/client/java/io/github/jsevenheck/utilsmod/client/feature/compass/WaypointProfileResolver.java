package io.github.jsevenheck.utilsmod.client.feature.compass;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Optional;

/** Resolves the active world into a privacy-preserving, stable local waypoint profile key. */
final class WaypointProfileResolver {

    Optional<String> resolve(Minecraft minecraft) {
        ServerData server = minecraft.getCurrentServer();
        if (server != null && server.ip != null && !server.ip.isBlank()) {
            return Optional.of("multiplayer:" + digest(server.ip.trim().toLowerCase(Locale.ROOT)));
        }

        if (minecraft.getSingleplayerServer() != null) {
            String savePath = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT)
                .toAbsolutePath().normalize().toString();
            return Optional.of("singleplayer:" + digest(savePath));
        }

        return Optional.empty();
    }

    private static String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte valueByte : bytes) {
                int unsignedByte = Byte.toUnsignedInt(valueByte);
                hex.append(Character.forDigit(unsignedByte >>> 4, 16));
                hex.append(Character.forDigit(unsignedByte & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Java runtime does not provide SHA-256", exception);
        }
    }
}
