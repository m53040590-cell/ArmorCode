package me.arx.pulse.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PulseHudOverlay implements HudRenderCallback {

    private float animationProgress = 0f;

    private boolean firstLaunch = true;

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        boolean isPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_X);
        float animationSpeed = 0.05f; 

        if (firstLaunch) {
            animationProgress += animationSpeed * tickDelta;
            if (animationProgress >= 0.7f) { 
                firstLaunch = false;
            }
        } else {
            // Обычная логика кнопки X
            if (isPressed) {
                animationProgress = Math.min(1.0f, animationProgress + animationSpeed * tickDelta);
            } else {
                animationProgress = Math.max(0.0f, animationProgress - animationSpeed * tickDelta);
            }
        }

        List<ItemStack> items = new ArrayList<>();
        for (int i = 3; i >= 0; i--) {
            ItemStack s = client.player.getInventory().getArmorStack(i);
            if (!s.isEmpty()) items.add(s);
        }
        if (!client.player.getMainHandStack().isEmpty()) items.add(client.player.getMainHandStack());
        if (!client.player.getOffHandStack().isEmpty()) items.add(client.player.getOffHandStack());

        if (items.isEmpty()) return;

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        float currentItemHeight = 20f + (8f * animationProgress);
        int panelWidth = (int) (85f + (20f * animationProgress));

        int panelHeight = (int) (items.size() * currentItemHeight) + 4;

        int x = width - panelWidth - 10;
        int y = height - panelHeight - 10;

        int alpha = (int) (140 * Math.max(0.2f, animationProgress));
        int backgroundColor = (alpha << 24);
        int borderColor = (alpha << 24) | 0x606060;

        context.fill(x, y, x + panelWidth, y + panelHeight, backgroundColor);
        context.fill(x, y, x + panelWidth, y + 1, borderColor);

        float currentSlotY = y + 2;
        for (ItemStack stack : items) {
            int iconY = (int) (currentSlotY + (currentItemHeight - 16) / 2);
            context.drawItem(stack, x + 4, iconY);

            String infoText = "";
            if (stack.isDamageable()) {
                infoText = (stack.getMaxDamage() - stack.getDamage()) + "/" + stack.getMaxDamage();
            } else if (stack.getCount() > 1) {
                infoText = "x" + stack.getCount();
            }

            int textY = (int) (currentSlotY + (animationProgress > 0.5f ? 4 : 6));
            context.drawText(client.textRenderer, infoText, x + 24, textY, 0xFFFFFFFF, true);

            if (animationProgress > 0.7f) {
                renderEnchants(context, client, stack, x + 24, (int) currentSlotY + 14);
            }

            currentSlotY += currentItemHeight;
        }
    }

    private void renderEnchants(DrawContext context, MinecraftClient client, ItemStack stack, int x, int y) {
        Map<Enchantment, Integer> enchants = EnchantmentHelper.get(stack);
        if (enchants.isEmpty()) return;

        int eX = x;
        int count = 0;

        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            String id = Registries.ENCHANTMENT.getId(entry.getKey()).getPath();

            String name = switch (id) {
                case "protection" -> "P";
                case "sharpness" -> "S";
                case "unbreaking" -> "U";
                case "mending" -> "M";
                case "binding_curse" -> "§cBC";
                default -> id.substring(0, 1).toUpperCase();
            };

            String level = (id.contains("mending") || id.contains("curse")) ? "" : String.valueOf(entry.getValue());
            context.drawText(client.textRenderer, name + level, eX, y, 0xFFCCCCCC, true);

            eX += 15;
            if (++count >= 3) break;
        }
    }
}
