package chihalu.packedchest.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Exposes the client config screen to Mod Menu so players can toggle preview behaviours.
 */
public final class PackedChestModMenuIntegration implements ModMenuApi {
        @Override
        public ConfigScreenFactory<?> getModConfigScreenFactory() {
                return PackedChestConfigScreen::new;
        }
}
