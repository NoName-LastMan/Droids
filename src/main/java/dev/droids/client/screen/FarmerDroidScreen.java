package dev.droids.client.screen;

import dev.droids.DroidsMod;
import dev.droids.menu.FarmerDroidMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class FarmerDroidScreen extends AbstractContainerScreen<FarmerDroidMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DroidsMod.MODID, "textures/gui/farmer_droid_gui.png");

    public FarmerDroidScreen(FarmerDroidMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
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
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        // Labels for slots
        guiGraphics.drawString(this.font, Component.translatable("gui.droids.fuel"), 25, 16, 0x555555, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.droids.upgrade"), 25, 46, 0x555555, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.droids.inventory"), 98, 7, 0x555555, false);

        // Fuel Time indicator
        int fuelSec = this.menu.getFuelSeconds();
        String fuelText = fuelSec > 0 ? fuelSec + "s" : "OFF";
        int fuelColor = fuelSec > 0 ? 0x228B22 : 0xAA0000;
        guiGraphics.drawString(this.font, fuelText, 56, 30, fuelColor, false);

        // Upgrade status indicator
        int upgrades = this.menu.getUpgradeCount();
        int maxDim = 5 + (upgrades * 4);
        guiGraphics.drawString(this.font, maxDim + "x" + maxDim, 56, 60, 0x4A4A4A, false);

        // Player Inventory label
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 72, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
