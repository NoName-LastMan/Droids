package dev.droids.client.screen;

import dev.droids.DroidsMod;
import dev.droids.entity.MinerDroidEntity;
import dev.droids.menu.MinerDroidMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MinerDroidScreen extends AbstractContainerScreen<MinerDroidMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DroidsMod.MODID, "textures/gui/miner_droid_gui.png");

    private Button modeButton;

    public MinerDroidScreen(MinerDroidMenu menu, Inventory playerInventory, Component title) {
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
    }

    private Component getModeText() {
        return this.menu.getMode() == MinerDroidEntity.MODE_TUNNEL
                ? Component.translatable("gui.droids.mode.tunnel")
                : Component.translatable("gui.droids.mode.quarry");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.modeButton != null) {
            this.modeButton.setMessage(getModeText());
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
        guiGraphics.drawString(this.font, Component.translatable("gui.droids.inventory"), 98, 7, 0x555555, false);

        // Real-time Fuel countdown
        int fuelSec = this.menu.getFuelSeconds();
        String fuelText = fuelSec > 0 ? fuelSec + "s" : "OFF";
        int fuelColor = fuelSec > 0 ? 0x228B22 : 0xAA0000;
        guiGraphics.drawString(this.font, fuelText, 56, 24, fuelColor, false);

        // Player Inventory label
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 72, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
