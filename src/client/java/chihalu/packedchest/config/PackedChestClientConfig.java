package chihalu.packedchest.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import chihalu.packedchest.PackedChest;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Client-side configuration that controls optional UX tweaks such as preview mutability.
 */
public final class PackedChestClientConfig {
        private static final String CONFIG_FILE = "packed-chest-client.json";
        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

        private static PackedChestClientConfig instance;

        private boolean allowPreviewItemMovement = false;

        private PackedChestClientConfig() {
        }

        public static PackedChestClientConfig get() {
                if (instance == null) {
                        instance = load();
                }
                return instance;
        }

        public boolean allowPreviewItemMovement() {
                return this.allowPreviewItemMovement;
        }

        public void setAllowPreviewItemMovement(boolean allow) {
                this.allowPreviewItemMovement = allow;
                save();
        }

        public void save() {
                Path path = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
                try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                        GSON.toJson(this, writer);
                } catch (IOException ex) {
                        PackedChest.LOGGER.error("Failed to save Packed Chest client config", ex);
                }
        }

        private static PackedChestClientConfig load() {
                Path path = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
                if (Files.exists(path)) {
                        try (BufferedReader reader = Files.newBufferedReader(path)) {
                                PackedChestClientConfig loaded = GSON.fromJson(reader, PackedChestClientConfig.class);
                                if (loaded != null) {
                                        return loaded;
                                }
                        } catch (IOException | JsonParseException ex) {
                                PackedChest.LOGGER.warn("Failed to read Packed Chest client config, using defaults", ex);
                        }
                }

                PackedChestClientConfig defaults = new PackedChestClientConfig();
                defaults.save();
                return defaults;
        }
}
