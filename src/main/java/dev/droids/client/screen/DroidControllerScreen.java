package dev.droids.client.screen;

import dev.droids.DroidsMod;
import dev.droids.menu.DroidControllerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Clean Steampunk Telemetry Dashboard for Droid Controller.
 */
public class DroidControllerScreen extends AbstractContainerScreen<DroidControllerMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DroidsMod.MODID, "textures/gui/droid_station_gui.png");

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

        // Button: Recall Droid to Base
        this.recallButton = this.addRenderableWidget(
                Button.builder(Component.literal("§6Dock Unit"), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                }).bounds(x + 108, y + 20, 60, 16).build()
        );
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.recallButton != null) {
            // Only active if a droid is assigned
            this.recallButton.active = this.menu.getDroidType() != 0;
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Dark terminal screen background
        guiGraphics.fill(x + 7, y + 15, x + 103, y + 78, 0xFF181C20);
        guiGraphics.fill(x + 8, y + 16, x + 102, y + 77, 0xFF0D1114);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Title
        guiGraphics.drawString(this.font, "§e◈ DROID MONITOR", 8, 6, 0x404040, false);

        int droidType = this.menu.getDroidType();
        int status    = this.menu.getDroidStatus();
        int fuelSec   = this.menu.getFuelSeconds();
        int dist      = this.menu.getDistance();
        int upgr      = this.menu.getUpgradeCount();

        String typeStr = switch (droidType) {
            case 1 -> "§fMiner Droid";
            case 2 -> "§fFarmer Droid";
            default -> "§8No Droid";
        };

        String statusStr = switch (status) {
            case 1 -> (droidType == 1 ? "§aBreaking" : "§aHarvesting");
            case 2 -> "§cUnpowered";
            case 3 -> "§cNo Tool";
            default -> "§7Standby / Idle";
        };

        // Telemetry lines
        guiGraphics.drawString(this.font, "Unit:  " + typeStr, 11, 20, 0x44DDFF, false);
        guiGraphics.drawString(this.font, "State: " + statusStr, 11, 29, 0x44DDFF, false);
        guiGraphics.drawString(this.font, "Fuel:  §a" + fuelSec + "s", 11, 38, 0x44DDFF, false);
        guiGraphics.drawString(this.font, "Dist:  §b" + dist + "m", 11, 47, 0x44DDFF, false);

        if (droidType == 1) { // Miner specific
            int dur = this.menu.getPickaxeDurabilityPercent();
            String durColor = dur > 50 ? "§a" : dur > 20 ? "§e" : "§c";
            guiGraphics.drawString(this.font, "Tool:  " + durColor + dur + "%", 11, 56, 0x44DDFF, false);
        } else if (droidType == 2) { // Farmer specific
            guiGraphics.drawString(this.font, "Mode:  §aCultivator", 11, 56, 0x44DDFF, false);
        }

        if (upgr > 0) {
            guiGraphics.drawString(this.font, "Range: §6+" + upgr + " Tier", 11, 65, 0x44DDFF, false);
        } else {
            guiGraphics.drawString(this.font, "Base:  §7(" + this.menu.getStationX() + ", " + this.menu.getStationZ() + ")", 11, 65, 0x8899AA, false);
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
