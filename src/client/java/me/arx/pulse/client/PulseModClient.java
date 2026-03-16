package me.arx.pulse.client;

import me.arx.pulse.client.PulseHudOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class PulseModClient implements ClientModInitializer {

    public static KeyBinding armorInfoKey;

    @Override
    public void onInitializeClient() {
        armorInfoKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pulse.show_enchants",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X, 
                "category.pulse.utility"
        ));

        HudRenderCallback.EVENT.register(new PulseHudOverlay());
    }
}
