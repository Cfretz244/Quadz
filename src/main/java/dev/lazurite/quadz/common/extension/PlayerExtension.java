package dev.lazurite.quadz.common.extension;

import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public interface PlayerExtension {

    default float quadz$getJoystickValue(Identifier axis) {
        return 0.0f;
    }

    default void quadz$setJoystickValue(Identifier axis, float value) {
    }

    default Map<Identifier, Float> quadz$getAllAxes() {
        return new HashMap<>();
    }

    default void quadz$syncJoystick() {
    }

}
