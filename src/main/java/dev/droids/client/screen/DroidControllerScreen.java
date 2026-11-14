package dev.droids.client.screen;

import dev.droids.DroidsMod;
import dev.droids.menu.DroidControllerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DroidControllerScreen extends AbstractContainerScreen<DroidControllerMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DroidsMod.MODID, "textures/gui/droid_station_gui.png");

    private Button modeButton;
    private Button dirButton;
    private Button recallButton;
    private Button applyAreaButton;

    public DroidControllerScreen(DroidControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Button 0: Toggle Mode (Tunnel / Quarry)
        this.modeButton = this.addRenderableWidget(
                Button.builder(getModeText(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                }).bounds(x + 110, y + 20, 58, 14).build()
        );

        // Button 1: Cycle Direction (N/E/S/W) - only relevant for Tunnel mode
        this.dirButton = this.addRenderableWidget(
                Button.builder(getDirText(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    }
                }).bounds(x + 110, y + 36, 58, 14).build()
        );

        // Button 2: Recall Droid to Base via Steam Thrusters
        this.recallButton = this.addRenderableWidget(
                Button.builder(Component.literal("§c⬆ Recall"), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
                    }
                }).bounds(x + 110, y + 52, 58, 14).build()
        );

        // Button 3: Apply marked area corners to the Droid
        this.applyAreaButton = this.addRenderableWidget(
                Button.builder(getApplyAreaText(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 3);
                    }
                }).bounds(x + 110, y + 68, 58, 14).build()
        );
    }

    private Component getModeText() {
        return this.menu.getMode() == 0
                ? Component.literal("➔ Tunnel")
                : Component.literal("⬇ Quarry");
    }

    private Component getDirText() {
        return Component.literal("Dir: " + this.menu.getWorkDirection().getName().toUpperCase().substring(0, 1));
    }

    private Component getApplyAreaText() {
        if (this.menu.hasCorner1() && this.menu.hasCorner2()) {
            int w = this.menu.getAreaWidth();
            int l = this.menu.getAreaLength();
            return Component.literal("§aApply " + w + "×" + l);
        }
        return Component.literal("§7Set Area");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.modeButton != null) this.modeButton.setMessage(getModeText());
        if (this.dirButton != null) this.dirButton.setMessage(getDirText());
        if (this.applyAreaButton != null) this.applyAreaButton.setMessage(getApplyAreaText());

        // Only show Direction button when in Tunnel mode
        if (this.dirButton != null) {
            this.dirButton.visible = this.menu.getMode() == 0;
        }
        // Only show Apply Area when corners are marked
        if (this.applyAreaButton != null) {
            this.applyAreaButton.active = this.menu.hasCorner1() && this.menu.hasCorner2();
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Dark terminal screen background
        guiGraphics.fill(x + 7, y + 15, x + 103, y + 86, 0xFF181C20);
        guiGraphics.fill(x + 8, y + 16, x + 102, y + 85, 0xFF0D1114);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Title
        guiGraphics.drawString(this.font, "§e◈ DROID TELEMETRY", 8, 6, 0x404040, false);

        int droidType = this.menu.getDroidType();
        int status    = this.menu.getDroidStatus();
        int fuelSec   = this.menu.getFuelSeconds();
        int dist      = this.menu.getDistance();
        int droidY    = this.menu.getDroidY();

        String typeStr = switch (droidType) {
            case 1 -> "Miner";
            case 2 -> "Farmer";
            default -> "§8No Droid";
        };

        String statusStr = switch (status) {
            case 1 -> "§aMining";
            case 2 -> "§6Thrusters";
            case 3 -> "§cNo Fuel";
            default -> "§7Standby";
        };

        // Telemetry lines
        guiGraphics.drawString(this.font, "Unit:   §f" + typeStr, 11, 20, 0x44DDFF, false);
        guiGraphics.drawString(this.font, "State:  " + statusStr, 11, 29, 0x44DDFF, false);
        guiGraphics.drawString(this.font, "Fuel:   §a" + fuelSec + "s", 11, 38, 0x44DDFF, false);
        guiGraphics.drawString(this.font, "Dist:   §b" + dist + "m §7| Y: §b" + droidY, 11, 47, 0x44DDFF, false);

        if (droidType == 1) { // Miner specifics
            int dur   = this.menu.getPickaxeDurabilityPercent();
            int depth = this.menu.getQuarryDepth();
            int maxD  = this.menu.getMaxDepth();
            int upgr  = this.menu.getUpgradeCount();
            String durColor = dur > 50 ? "§a" : dur > 20 ? "§e" : "§c";
            guiGraphics.drawString(this.font, "Pick:   " + durColor + dur + "%", 11, 56, 0x44DDFF, false);
            if (this.menu.getMode() == 1) {
                guiGraphics.drawString(this.font, "Depth:  §e" + depth + "§7/§e" + maxD + "m", 11, 65, 0x44DDFF, false);
            }
            if (upgr > 0) {
                guiGraphics.drawString(this.font, "Upg:    §6+" + upgr + " Range", 11, 74, 0x44DDFF, false);
            }
        }

        // Area marking section (bottom of terminal)
        if (this.menu.hasCorner1() || this.menu.hasCorner2()) {
            guiGraphics.drawString(this.font, "§7──── Work Area ────", 8, 86, 0xFFFFFF, false);
            if (this.menu.hasCorner1()) {
                guiGraphics.drawString(this.font,
                        "§7C1: §e(" + this.menu.getC1X() + ", " + this.menu.getC1Z() + ")",
                        8, 95, 0xFFFFFF, false);
            }
            if (this.menu.hasCorner2()) {
                guiGraphics.drawString(this.font,
                        "§7C2: §e(" + this.menu.getC2X() + ", " + this.menu.getC2Z() + ")",
                        8, 104, 0xFFFFFF, false);
            }
            if (this.menu.hasCorner1() && this.menu.hasCorner2()) {
                guiGraphics.drawString(this.font,
                        "§7→  §a" + this.menu.getAreaWidth() + "×" + this.menu.getAreaLength() + " blocks",
                        8, 113, 0xFFFFFF, false);
            }
        } else {
            guiGraphics.drawString(this.font,
                    "§7Right-click blocks to mark area",
                    8, 86, 0x556655, false);
        }

        // Player inventory label
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 72, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
