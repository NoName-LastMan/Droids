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
                }).bounds(x + 110, y + 20, 58, 16).build()
        );

        // Button 1: Cycle Direction (N, E, S, W)
        this.dirButton = this.addRenderableWidget(
                Button.builder(getDirText(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    }
                }).bounds(x + 110, y + 38, 58, 16).build()
        );

        // Button 2: Recall Droid to Base via Steam Thrusters
        this.recallButton = this.addRenderableWidget(
                Button.builder(Component.literal("§cRECALL"), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
                    }
                }).bounds(x + 110, y + 56, 58, 16).build()
        );
    }

    private Component getModeText() {
        return this.menu.getMode() == 0
                ? Component.translatable("gui.droids.mode.tunnel")
                : Component.translatable("gui.droids.mode.quarry");
    }

    private Component getDirText() {
        return Component.literal(this.menu.getWorkDirection().getName().toUpperCase());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.modeButton != null) {
            this.modeButton.setMessage(getModeText());
        }
        if (this.dirButton != null) {
            this.dirButton.setMessage(getDirText());
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Render sleek terminal screen background in upper area
        guiGraphics.fill(x + 7, y + 18, x + 106, y + 74, 0xFF181C20);
        guiGraphics.fill(x + 8, y + 19, x + 105, y + 73, 0xFF0E1216);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Title
        guiGraphics.drawString(this.font, Component.literal("§e◈ DROID TELEMETRY"), 8, 6, 0x404040, false);

        BlockPos sp = this.menu.getStationPos();
        int droidType = this.menu.getDroidType();
        int status = this.menu.getDroidStatus();
        int fuelSec = this.menu.getFuelSeconds();
        int dist = this.menu.getDistance();

        String typeStr = switch (droidType) {
            case 1 -> "Miner";
            case 2 -> "Farmer";
            default -> "No Droid";
        };

        String statusStr = switch (status) {
            case 1 -> "§aMining";
            case 2 -> "§6Thrusters";
            case 3 -> "§cNo Fuel";
            default -> "§7Standby";
        };

        // Live Telemetry Readout
        guiGraphics.drawString(this.font, "§7Unit: §f" + typeStr, 11, 22, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "§7State: " + statusStr, 11, 32, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "§7Fuel: §a" + fuelSec + "s §7| §b" + dist + "m", 11, 42, 0xFFFFFF, false);

        if (droidType == 1) { // Miner specific
            int durability = this.menu.getPickaxeDurabilityPercent();
            int depth = this.menu.getQuarryDepth();
            int maxDepth = this.menu.getMaxDepth();

            guiGraphics.drawString(this.font, "§7Pick: §e" + durability + "%", 11, 52, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, "§7Depth: §e" + depth + "§7/§e" + maxDepth + "m", 11, 62, 0xFFFFFF, false);
        } else {
            guiGraphics.drawString(this.font, "§7Base: §8(" + sp.getX() + "," + sp.getZ() + ")", 11, 52, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, "§7Link: §aONLINE", 11, 62, 0xFFFFFF, false);
        }

        // Inventory title
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 72, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
