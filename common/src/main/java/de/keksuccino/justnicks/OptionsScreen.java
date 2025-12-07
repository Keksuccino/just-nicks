package de.keksuccino.justnicks;

import de.keksuccino.justnicks.util.AbstractOptions;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OptionsScreen extends Screen {

    @Nullable
    protected Screen parent;

    public OptionsScreen(@Nullable Screen parent) {
        super(Component.translatable("justnicks.options"));
        this.parent = parent;
    }

    @Override
    protected void init() {

        int centerX = this.width / 2;
        // Define a fixed top position for the first option
        int topY = 50; // Starting position for the first option
        int spacing = 25; // Consistent spacing between elements

        StringWidget titleWidget = this.addRenderableWidget(new StringWidget(this.getTitle(), this.font));
        titleWidget.setX(centerX - (titleWidget.getWidth() / 2));
        titleWidget.setY(20);

        int currentY = topY; // Start from the top position

        //Base Zoom Modifier
        this.addFloatInput(JustNicks.getOptions().baseZoomFactor, currentY, "justnicks.options.base_zoom_modifier");
        currentY += spacing;

        //Zoom In Per Scroll
        this.addFloatInput(JustNicks.getOptions().zoomInPerScroll, currentY, "justnicks.options.zoom_in_change_modifier_per_scroll");
        currentY += spacing;

        //Zoom Out Per Scroll
        this.addFloatInput(JustNicks.getOptions().zoomOutPerScroll, currentY, "justnicks.options.zoom_out_change_modifier_per_scroll");
        currentY += spacing;

        //Smooth Zooming
        this.addRenderableWidget(this.buildToggleButton(JustNicks.getOptions().smoothZoomInOut, currentY, "justnicks.options.smooth_zoom_in_out"));
        currentY += spacing;

        //Smooth Camera Movement
        this.addRenderableWidget(this.buildToggleButton(JustNicks.getOptions().smoothCameraOnZoom, currentY, "justnicks.options.smooth_camera_movement_on_zoom"));
        currentY += spacing;

        //Normalize Mouse Sensitivity
        this.addRenderableWidget(this.buildToggleButton(JustNicks.getOptions().normalizeMouseSensitivityOnZoom, currentY, "justnicks.options.normalize_mouse_sensitivity_on_zoom"));
        currentY += spacing;

        //Allow Zoom in Mirrored View
        this.addRenderableWidget(this.buildToggleButton(JustNicks.getOptions().allowZoomInMirroredView, currentY, "justnicks.options.allow_zoom_in_mirrored_view"));
        currentY += spacing;

        //Hide Arms When Zooming
        this.addRenderableWidget(this.buildToggleButton(JustNicks.getOptions().hideArmsWhenZooming, currentY, "justnicks.options.hide_arms_when_zooming"));
        currentY += spacing;

        //Reset Zoom Factor When Stop Zooming
        this.addRenderableWidget(this.buildToggleButton(JustNicks.getOptions().resetZoomFactorOnStopZooming, currentY, "justnicks.options.reset_zoom_factor_when_stop_zooming"));
        currentY += spacing;

        //Options Button Corner
        this.addRenderableWidget(this.buildCornerButton(JustNicks.getOptions().optionsButtonCorner, currentY, "justnicks.options.options_button_corner"));

        //DONE
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).bounds(centerX - 75, this.height - 40, 150, 20).build());

    }

    protected Button buildToggleButton(@NotNull AbstractOptions.Option<Boolean> option, int y, @NotNull String labelBaseKey) {

        int centerX = this.width / 2;
        int buttonWidth = 200;

        Component enabled = Component.translatable(labelBaseKey, Component.translatable("justnicks.options.toggle.enabled").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)));
        Component disabled = Component.translatable(labelBaseKey, Component.translatable("justnicks.options.toggle.disabled").withStyle(Style.EMPTY.withColor(ChatFormatting.RED)));

        return Button.builder(option.getValue() ? enabled : disabled, button -> {
                    option.setValue(!option.getValue());
                    button.setMessage(option.getValue() ? enabled : disabled);
                }).bounds(centerX - (buttonWidth / 2), y, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable(labelBaseKey + ".desc"))).build();

    }

    protected Button buildCornerButton(@NotNull AbstractOptions.Option<Integer> option, int y, @NotNull String labelBaseKey) {

        int centerX = this.width / 2;
        int buttonWidth = 200;

        String[] cornerKeys = new String[] {
                "justnicks.options.corner.bottom_left",
                "justnicks.options.corner.bottom_right",
                "justnicks.options.corner.top_left",
                "justnicks.options.corner.top_right"
        };

        int currentValue = option.getValue();
        Component buttonText = Component.translatable(labelBaseKey, Component.translatable(cornerKeys[currentValue]).withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));

        return Button.builder(buttonText, button -> {
                    // Cycle through corners (0-3)
                    int newValue = (option.getValue() + 1) % 4;
                    option.setValue(newValue);
                    button.setMessage(Component.translatable(labelBaseKey, Component.translatable(cornerKeys[newValue]).withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD))));
                }).bounds(centerX - (buttonWidth / 2), y, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable(labelBaseKey + ".desc"))).build();

    }

    protected void addFloatInput(@NotNull AbstractOptions.Option<Float> option, int y, @NotNull String labelBaseKey) {

        int centerX = this.width / 2;

        StringWidget zoomOutPerScrollText = this.addRenderableWidget(new StringWidget(Component.translatable(labelBaseKey), this.font));
        zoomOutPerScrollText.setX(centerX - 5 - zoomOutPerScrollText.getWidth());
        zoomOutPerScrollText.setY(y + 10 - (this.font.lineHeight / 2));
        zoomOutPerScrollText.setTooltip(Tooltip.create(Component.translatable(labelBaseKey + ".desc")));
        EditBox zoomOutPerScroll = this.addRenderableWidget(new EditBox(this.font, centerX + 5, y, 150, 20, Component.translatable(labelBaseKey)));
        zoomOutPerScroll.setValue("" + option.getValue());
        zoomOutPerScroll.setResponder(s -> {
            if (MathUtils.isFloat(s)) {
                option.setValue(Float.parseFloat(s));
            }
        });
        zoomOutPerScroll.setTooltip(Tooltip.create(Component.translatable(labelBaseKey + ".desc")));

    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

}