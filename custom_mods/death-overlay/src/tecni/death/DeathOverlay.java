package tecni.death;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import net.minecraft.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.*;

/** Client-only overlay, compiled against Fabric's 1.20.1 intermediary names.
 * It deliberately outlives the player entity and the death screen on respawn.
 */
public final class DeathOverlay implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("TecniDeath");
    private static volatile byte[][] frames;
    private static long requestedAt, startedAt;
    private static int currentFrame = -1;
    private static class_1043 texture; // NativeImageBackedTexture
    private static class_2960 textureId;
    private static class_1109 sound; // PositionedSoundInstance
    private static final int FPS = 24;

    @Override public void onInitializeClient() {
        // Local preview exercises playback without changing lives.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(literal("tecni-video").executes(context -> {
                class_310 client = class_310.method_1551();
                if (client.field_1724 != null && client.method_1562() != null) {
                    // Trigger only the overlay, never vanilla's respawn logic.
                    onDeath(client.field_1724.method_5628());
                }
                return 1;
            })));
        CompletableFuture.runAsync(() -> {
            try (InputStream input = DeathOverlay.class.getResourceAsStream("/assets/tecni_death/death.fma");
                 ZipInputStream zip = new ZipInputStream(Objects.requireNonNull(input))) {
                Map<Integer, byte[]> decoded = new TreeMap<>();
                for (ZipEntry e; (e = zip.getNextEntry()) != null;) {
                    if (e.getName().matches("frames/[0-9]+\\.png")) {
                        int index = Integer.parseInt(e.getName().substring(7, e.getName().length() - 4));
                        decoded.put(index, zip.readAllBytes());
                    }
                }
                if (decoded.isEmpty()) throw new IOException("No animation frames");
                frames = decoded.values().toArray(byte[][]::new);
                LOG.info("Loaded {} transparent death frames at {} fps", frames.length, FPS);
            } catch (Exception e) { LOG.error("Cannot load death animation", e); }
        });
    }

    public static void onDeath(int entityId) {
        class_310 client = class_310.method_1551();
        if (client.field_1724 == null || client.field_1724.method_5628() != entityId) return;
        clear(client);
        requestedAt = System.nanoTime();
        LOG.info("Death animation requested for local player (entity {})", entityId);
    }

    private static void clear(class_310 client) {
        requestedAt = startedAt = 0;
        currentFrame = -1;
        if (sound != null) client.method_1483().method_4870(sound);
        sound = null;
    }

    public static void render() {
        if (requestedAt == 0) return;
        class_310 client = class_310.method_1551();
        if (client.field_1687 == null) { clear(client); return; }
        long now = System.nanoTime();
        // Let the immediate respawn's sound reset finish before starting both tracks.
        if (frames == null || now - requestedAt < 250_000_000L) return;
        try {
            if (startedAt == 0) {
                startedAt = now;
                sound = class_1109.method_4758(class_3414.method_47908(
                    new class_2960("tecni_death", "break")), 1.0F);
                client.method_1483().method_4873(sound);
                LOG.info("Playing death animation with audio");
            }
            int frame = (int)((now - startedAt) * FPS / 1_000_000_000L);
            if (frame >= frames.length) { clear(client); return; }
            if (frame != currentFrame) {
                class_1011 image = class_1011.method_4309(new ByteArrayInputStream(frames[frame]));
                if (texture == null) {
                    texture = new class_1043(image);
                    textureId = client.method_1531().method_4617("tecni_death", texture);
                } else {
                    texture.method_4526(image); // closes the previous NativeImage
                    texture.method_4524();
                }
                currentFrame = frame;
            }
            int screenWidth = client.method_22683().method_4486();
            int screenHeight = client.method_22683().method_4502();
            int width = Math.min(480, (int)(screenWidth * 0.8));
            width = Math.min(width, (int)(screenHeight * 0.7 * 640 / 360));
            int height = width * 360 / 640;
            class_332 context = new class_332(client, client.method_22940().method_23000());
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            // Center the entire 16:9 image on the crosshair at every GUI scale.
            context.method_25293(textureId, (screenWidth-width)/2, (screenHeight-height)/2,
                width, height, 0F, 0F, 640, 360, 640, 360);
            context.method_51452();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        } catch (Exception e) {
            LOG.error("Cannot play death overlay", e);
            clear(client);
        }
    }
}
