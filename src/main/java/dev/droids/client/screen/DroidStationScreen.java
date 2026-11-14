package dev.droids.client.screen;

import dev.droids.DroidsMod;
import dev.droids.menu.DroidStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DroidStationScreen extends AbstractContainerScreen<DroidStationMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DroidsMod.MODID, "textures/gui/droid_station_gui.png");

    private Button modeButton;
    private Button dirButton;

    public DroidStationScreen(DroidStationMenu menu, Inventory playerInventory, Component title) {
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

        this.modeButton = this.addRenderableWidget(
                Button.builder(getModeText(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                }).bounds(x + 46, y + 42, 48, 16).build()
        );

        this.dirButton = this.addRenderableWidget(
                Button.builder(getDirText(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    }
                }).bounds(x + 46, y + 62, 48, 16).build()
        );
    }

    private Component getModeText() {
        return this.menu.getMode() == 0
                ? Component.translatable("gui.droids.mode.tunnel")
                : Component.translatable("gui.droids.mode.quarry");
    }

    private Component getDirText() {
        return Component.literal(this.menu.getWorkDirection().getName().substring(0, 1).toUpperCase());
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
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Title
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x333333, false);

        // Slot indicator labels
        guiGraphics.drawString(this.font, Component.translatable("gui.droids.fuel"), 8, 24, 0x555555, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.droids.tool"), 8, 46, 0x555555, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.droids.upgrade"), 8, 68, 0x555555, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.droids.buffer"), 98, 7, 0x555555, false);

        // Assigned Droid status
        boolean hasDroid = this.menu.hasAssignedDroid();
        Component statusText = hasDroid
                ? Component.translatable("gui.droids.docked")
                : Component.translatable("gui.droids.empty_dock");
        int statusColor = hasDroid ? 0x228B22 : 0x888888;
        guiGraphics.drawString(this.font, statusText, 48, 24, statusColor, false);

        // Player Inventory label
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 72, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
