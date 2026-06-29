package dev.lazurite.quadz.common.hooks;

import dev.lazurite.quadz.common.util.Bindable;
import dev.lazurite.quadz.common.util.Search;
import dev.lazurite.quadz.common.util.event.CameraEvents;
import dev.lazurite.quadz.common.entity.Quadcopter;
import dev.lazurite.toolbox.api.network.PacketRegistry;

import java.util.ArrayList;
import java.util.Optional;

public class ServerNetworkEventHooks {

    public static void onJoystickInput(PacketRegistry.ServerboundContext context) {
        var player = context.player();
        var buf = context.byteBuf();
        var axisCount = buf.readInt();

        for (int i = 0; i < axisCount; i++) {
            var axis = buf.readIdentifier();
            var value = buf.readFloat();
            player.quadz$setJoystickValue(axis, value);
        }
    }

    public static void onQuadcopterViewRequested(PacketRegistry.ServerboundContext context) {
        var player = context.player();
        var buf = context.byteBuf();
        var spectateDirection = buf.readInt();

        Optional.ofNullable(player.level().getServer()).ifPresent(server -> {
            server.execute(() -> {
                if (player.getCamera() instanceof Quadcopter quadcopter) {
                    var allQuadcopters = new ArrayList<>(Search.forAllViewed(server));
                    var index = Math.max(allQuadcopters.lastIndexOf(quadcopter) + spectateDirection, 0);
                    var entity = allQuadcopters.get(index % allQuadcopters.size());
                    player.setCamera(entity);
                    CameraEvents.SWITCH_CAMERA_EVENT.invoke(player.getCamera(), entity);
                } else {
                    Bindable.get(player.getMainHandItem()).ifPresent(bindable -> {
                        Search.forQuadWithBindId(
                                        player.level(),
                                        player.getCamera().position(),
                                        bindable.getBindId(),
                                        server.getPlayerList().getViewDistance() * 16)
                                .ifPresentOrElse(entity -> {
                                    player.setCamera(entity);
                                    CameraEvents.SWITCH_CAMERA_EVENT.invoke(player.getCamera(), entity);
                                }, () -> Search.forAllViewed(server).stream().findFirst().ifPresent(entity -> {
                                    player.setCamera(entity);
                                    CameraEvents.SWITCH_CAMERA_EVENT.invoke(player.getCamera(), entity);
                                }));
                    });

                }
            });
        });
    }

    public static void onPlayerViewRequestReceived(PacketRegistry.ServerboundContext context) {
        var player = context.player();
        Optional.ofNullable(player.level().getServer()).ifPresent(server -> server.execute(() -> player.setCamera(player)));
    }

    /**
     * Nudges the camera uptilt of the quadcopter the player is currently viewing by the given
     * delta (degrees), clamped to 0–90. Stored in synced entity data so it persists with the
     * drone and any spectators see the same view.
     */
    public static void onAdjustCameraAngle(PacketRegistry.ServerboundContext context) {
        var player = context.player();
        var delta = context.byteBuf().readInt();

        Optional.ofNullable(player.level().getServer()).ifPresent(server -> server.execute(() -> {
            if (player.getCamera() instanceof Quadcopter quadcopter) {
                var current = quadcopter.getEntityData().get(Quadcopter.CAMERA_ANGLE);
                var updated = Math.max(0, Math.min(90, current + delta));
                quadcopter.getEntityData().set(Quadcopter.CAMERA_ANGLE, updated);
            }
        }));
    }

    /**
     * Toggles the armed state of the quadcopter the player is currently viewing — the explicit
     * override on top of auto-arm. Disarming cuts motors (see {@link Quadcopter#tick}).
     */
    public static void onArmDisarm(PacketRegistry.ServerboundContext context) {
        var player = context.player();

        Optional.ofNullable(player.level().getServer()).ifPresent(server -> server.execute(() -> {
            if (player.getCamera() instanceof Quadcopter quadcopter) {
                quadcopter.setArmed(!quadcopter.isArmed());
            }
        }));
    }

    /**
     * Sets the viewed drone's armed state to a specific value — used by the controller arm switch,
     * which is a direct position (switch up/down) rather than a toggle.
     */
    public static void onSetArmed(PacketRegistry.ServerboundContext context) {
        var player = context.player();
        var armed = context.byteBuf().readBoolean();

        Optional.ofNullable(player.level().getServer()).ifPresent(server -> server.execute(() -> {
            if (player.getCamera() instanceof Quadcopter quadcopter) {
                quadcopter.setArmed(armed);
            }
        }));
    }

}
