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

    // 0.0 - скрыто, 1.0 - раскрыто полностью
    private float animationProgress = 0f;
    // Флаг, чтобы один раз при заходе меню "выросло"
    private boolean firstLaunch = true;

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        // --- 1. ЛОГИКА АНИМАЦИИ (ВХОД И КЛАВИША) ---
        boolean isPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_X);
        float animationSpeed = 0.05f; // Чуть медленнее для красоты

        // Если это первый запуск после захода — форсим рост до 0.5 (или 1.0)
        if (firstLaunch) {
            animationProgress += animationSpeed * tickDelta;
            if (animationProgress >= 0.7f) { // Показали на 70% и хватит
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

        // --- 2. СБОР ПРЕДМЕТОВ (ВАЖНО ДЛЯ ВЫСОТЫ) ---
        List<ItemStack> items = new ArrayList<>();
        for (int i = 3; i >= 0; i--) {
            ItemStack s = client.player.getInventory().getArmorStack(i);
            if (!s.isEmpty()) items.add(s);
        }
        if (!client.player.getMainHandStack().isEmpty()) items.add(client.player.getMainHandStack());
        if (!client.player.getOffHandStack().isEmpty()) items.add(client.player.getOffHandStack());

        if (items.isEmpty()) return;

        // --- 3. РАСЧЕТ ГЕОМЕТРИИ (ФИКС "ПАЛКИ") ---
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // Базовая высота слота 20, при зажатом X увеличивается до 28
        float currentItemHeight = 20f + (8f * animationProgress);
        int panelWidth = (int) (85f + (20f * animationProgress));

        // Высота всей панели зависит от кол-ва предметов
        int panelHeight = (int) (items.size() * currentItemHeight) + 4;

        // Координаты (правый нижний угол)
        int x = width - panelWidth - 10;
        int y = height - panelHeight - 10;

        // Прозрачность тоже плавная
        int alpha = (int) (140 * Math.max(0.2f, animationProgress));
        int backgroundColor = (alpha << 24);
        int borderColor = (alpha << 24) | 0x606060;

        // --- 4. ОТРИСОВКА ---
        // Рисуем основной фон
        context.fill(x, y, x + panelWidth, y + panelHeight, backgroundColor);
        // Рисуем верхнюю границу (линию)
        context.fill(x, y, x + panelWidth, y + 1, borderColor);

        float currentSlotY = y + 2;
        for (ItemStack stack : items) {
            // Рисуем иконку предмета
            int iconY = (int) (currentSlotY + (currentItemHeight - 16) / 2);
            context.drawItem(stack, x + 4, iconY);

            // Текст (прочность/кол-во)
            String infoText = "";
            if (stack.isDamageable()) {
                infoText = (stack.getMaxDamage() - stack.getDamage()) + "/" + stack.getMaxDamage();
            } else if (stack.getCount() > 1) {
                infoText = "x" + stack.getCount();
            }

            int textY = (int) (currentSlotY + (animationProgress > 0.5f ? 4 : 6));
            context.drawText(client.textRenderer, infoText, x + 24, textY, 0xFFFFFFFF, true);

            // Зачарования (только если панель достаточно широкая)
            if (animationProgress > 0.7f) {
                renderEnchants(context, client, stack, x + 24, (int) currentSlotY + 14);
            }

            currentSlotY += currentItemHeight;
        }
    }

    // ВЫНЕСЕННЫЙ МЕТОД (вне onHudRender!)
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
