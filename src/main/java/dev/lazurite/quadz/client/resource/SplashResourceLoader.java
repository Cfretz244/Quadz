package dev.lazurite.quadz.client.resource;

import dev.lazurite.quadz.Quadz;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class SplashResourceLoader implements SimpleSynchronousResourceReloadListener {

    public static final Identifier location = Identifier.fromNamespaceAndPath(Quadz.MODID, "misc/texts/splashes.txt");

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(Quadz.MODID, "splash");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        manager.getResource(location).ifPresent(resource -> {
            try {
                final var bufferedReader = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8));
                // 26.1: SplashManager.splashes holds Components in an immutable list — replace it.
                final var strings = bufferedReader.lines().map(String::trim).filter((string) -> string.hashCode() != 125780783).map(net.minecraft.network.chat.Component::literal).toList();
                final var splashManager = Minecraft.getInstance().getSplashManager();
                final var combined = new java.util.ArrayList<>(splashManager.splashes);
                combined.addAll(strings);
                splashManager.splashes = combined;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
