package net.tecnihardcore;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** A synchronized, label-free 3D color wheel rendered above the world spawn. */
public final class RouletteVisuals {
    private static final double DURATION_TICKS = 140.0;
    private static final double RADIUS = 4.25;
    private static final float[][] COLORS = {
        {.88F,.16F,.20F}, {1F,.42F,.12F}, {1F,.76F,.16F}, {.36F,.82F,.28F},
        {.10F,.78F,.68F}, {.15F,.57F,.96F}, {.34F,.35F,.94F}, {.62F,.28F,.87F},
        {.91F,.25F,.62F}, {.96F,.42F,.38F}
    };
    private static Vec3d center;
    private static long startedAt;
    private static long lastTone = -1;
    private static boolean preview;
    private static float finishOffset;
    private static String dimension;
    private static Object world;

    private RouletteVisuals() {}

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(Hardcore.ROULETTE, (client, handler, buf, response) -> {
            // The first two strings preserve compatibility with the older HUD-only roulette packet.
            buf.readString(64);
            buf.readString(128);
            String dimensionId = buf.readString(32);
            double x = buf.readDouble(), y = buf.readDouble(), z = buf.readDouble();
            long start = buf.readLong();
            boolean isPreview = buf.readBoolean();
            float offset = buf.readFloat();
            client.execute(() -> {
                if (client.world == null) return;
                dimension = dimensionId;
                center = new Vec3d(x, y, z);
                startedAt = start;
                preview = isPreview;
                finishOffset = offset;
                lastTone = -1;
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (world != client.world) {
                world = client.world;
                center = null;
            }
            if (client.world == null || client.player == null || center == null || !client.world.getRegistryKey().getValue().toString().equals(dimension)) return;
            double age = age(client);
            double distance = client.player.getPos().squaredDistanceTo(center);
            if (age > DURATION_TICKS + 32 || distance > 160.0 * 160.0) {
                center = null;
                return;
            }
            if (distance > 128.0 * 128.0) return;
            long tick = (long) age;
            if (tick < 0 || tick > DURATION_TICKS) return;
            // Sparse positional clicks make the deceleration legible without a sound/particle flood.
            long interval = tick < 70 ? 5 : tick < 112 ? 7 : tick < 132 ? 10 : 14;
            if (tick % interval == 0 && tick != lastTone) {
                lastTone = tick;
                float pitch = .72F + .7F * MathHelper.clamp((float) tick / (float) DURATION_TICKS, 0, 1);
                client.world.playSound(center.x, center.y, center.z, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(),
                    SoundCategory.BLOCKS, .7F, pitch, false);
            }
            if (tick % 3 == 0) {
                double a = tick * .27;
                client.world.addParticle(ParticleTypes.END_ROD,
                    center.x + Math.cos(a) * (RADIUS + .3), center.y + Math.sin(a) * (RADIUS + .3), center.z,
                    0, .012, 0);
            }
            if (tick == 140 && lastTone != 140) {
                lastTone = 140;
                client.world.playSound(center.x, center.y, center.z, SoundEvents.BLOCK_BEACON_POWER_SELECT,
                    SoundCategory.BLOCKS, 1.1F, preview ? 1.15F : .9F, false);
                for (int i = 0; i < 48; i++) {
                    double a = Math.PI * 2 * i / 48.0;
                    client.world.addParticle(ParticleTypes.FIREWORK,
                        center.x + Math.cos(a) * RADIUS, center.y + Math.sin(a) * RADIUS, center.z,
                        Math.cos(a) * .035, Math.sin(a) * .035, 0);
                }
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null || center == null || !client.world.getRegistryKey().getValue().toString().equals(dimension) || context.matrixStack() == null || context.consumers() == null) return;
            double age = age(client) + context.tickDelta();
            if (age > DURATION_TICKS + 32 || client.player.getPos().squaredDistanceTo(center) > 160.0 * 160.0) return;

            double progress = MathHelper.clamp(age / DURATION_TICKS, 0, 1);
            double eased = 1.0 - Math.pow(1.0 - progress, 3.65);
            double angle = Math.PI * 2.0 * (5.2 * eased) + finishOffset * eased;
            double entrance = Math.min(1.0, age / 12.0);
            float alpha = (float) MathHelper.clamp(entrance, 0, 1);
            float pulse = (float) (1.0 + .025 * Math.sin(age * .22));

            MatrixStack matrices = context.matrixStack();
            Vec3d camera = context.camera().getPos();
            matrices.push();
            matrices.translate(center.x - camera.x, center.y - camera.y, center.z - camera.z);
            // Face the same world-anchored wheel toward each viewer so it remains readable around the plaza.
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(client.player.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotation((float) angle));
            matrices.scale(pulse, pulse, 1);
            VertexConsumer vertices = context.consumers().getBuffer(SanctuaryLayers.DOME);
            drawWheel(vertices, matrices.peek().getPositionMatrix(), alpha, age);
            matrices.pop();
        });
    }

    private static double age(MinecraftClient client) {
        return client.world == null ? 0 : client.world.getTime() - startedAt;
    }

    private static void drawWheel(VertexConsumer out, Matrix4f matrix, float alpha, double age) {
        int segments = 10;
        int subdivisions = 12;
        double back = -.24, front = .24;
        // Ten broad, unlabelled color fields form the face of the wheel.
        for (int i = 0; i < segments; i++) {
            float[] color = COLORS[i];
            for (int j = 0; j < subdivisions; j++) {
                double a = (Math.PI * 2 * (i + j / (double) subdivisions) / segments) - Math.PI / 2;
                double b = (Math.PI * 2 * (i + (j + 1) / (double) subdivisions) / segments) - Math.PI / 2;
                triangle(out, matrix, 0, 0, front, Math.cos(a) * RADIUS, Math.sin(a) * RADIUS, front,
                    Math.cos(b) * RADIUS, Math.sin(b) * RADIUS, front, color, alpha);
                triangle(out, matrix, 0, 0, back, Math.cos(b) * RADIUS, Math.sin(b) * RADIUS, back,
                    Math.cos(a) * RADIUS, Math.sin(a) * RADIUS, back, dark(color), alpha);
                quad(out, matrix, Math.cos(a) * RADIUS, Math.sin(a) * RADIUS, back,
                    Math.cos(a) * RADIUS, Math.sin(a) * RADIUS, front,
                    Math.cos(b) * RADIUS, Math.sin(b) * RADIUS, front,
                    Math.cos(b) * RADIUS, Math.sin(b) * RADIUS, back, color, alpha * .82F);
            }
        }

        // A bright segmented outer rail and a small rotating multicolor hub add depth, without lettering.
        double rail = RADIUS + .12 + .035 * Math.sin(age * .18);
        for (int i = 0; i < segments; i++) {
            float[] c = COLORS[i];
            double a = Math.PI * 2 * i / segments;
            double b = Math.PI * 2 * (i + 1) / segments;
            double inner = RADIUS - .11, outer = rail;
            quad(out, matrix, Math.cos(a) * inner, Math.sin(a) * inner, front + .015,
                Math.cos(a) * outer, Math.sin(a) * outer, front + .015,
                Math.cos(b) * outer, Math.sin(b) * outer, front + .015,
                Math.cos(b) * inner, Math.sin(b) * inner, front + .015, c, alpha);
            double h0 = Math.PI * 2 * i / segments, h1 = Math.PI * 2 * (i + 1) / segments;
            triangle(out, matrix, 0, 0, front + .025, Math.cos(h0) * .72, Math.sin(h0) * .72, front + .025,
                Math.cos(h1) * .72, Math.sin(h1) * .72, front + .025, c, alpha);
        }
        // Small jewel at the axle, colored gold rather than text or a symbol.
        circle(out, matrix, .24, front + .04, new float[]{1F,.86F,.36F}, alpha);
    }

    private static float[] dark(float[] c) { return new float[]{c[0] * .58F, c[1] * .58F, c[2] * .58F}; }
    private static void circle(VertexConsumer out, Matrix4f m, double r, double z, float[] c, float alpha) {
        for (int i = 0; i < 32; i++) {
            double a = Math.PI * 2 * i / 32, b = Math.PI * 2 * (i + 1) / 32;
            triangle(out, m, 0, 0, z, Math.cos(a) * r, Math.sin(a) * r, z,
                Math.cos(b) * r, Math.sin(b) * r, z, c, alpha);
        }
    }
    private static void triangle(VertexConsumer v, Matrix4f m,
        double ax,double ay,double az,double bx,double by,double bz,double cx,double cy,double cz,float[] c,float alpha) {
        // The shared translucent layer is QUADS; repeat the last corner to make a triangle.
        vertex(v,m,ax,ay,az,c,alpha); vertex(v,m,bx,by,bz,c,alpha); vertex(v,m,cx,cy,cz,c,alpha); vertex(v,m,cx,cy,cz,c,alpha);
    }
    private static void quad(VertexConsumer v, Matrix4f m,
        double ax,double ay,double az,double bx,double by,double bz,double cx,double cy,double cz,double dx,double dy,double dz,float[] c,float alpha) {
        vertex(v,m,ax,ay,az,c,alpha); vertex(v,m,bx,by,bz,c,alpha); vertex(v,m,cx,cy,cz,c,alpha); vertex(v,m,dx,dy,dz,c,alpha);
    }
    private static void vertex(VertexConsumer v, Matrix4f m,double x,double y,double z,float[] c,float alpha) {
        v.vertex(m,(float)x,(float)y,(float)z).color(c[0],c[1],c[2],alpha).next();
    }
}
