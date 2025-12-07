package de.keksuccino.justnicks;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;

public class KeyMappings {

    public static final ResourceLocation JUST_ZOOM_KEYMAPPING_CATEGORY_ID = ResourceLocation.fromNamespaceAndPath("justnicks", "keybind.category.main");
    public static final KeyMapping.Category JUST_ZOOM_KEYMAPPING_CATEGORY = KeyMapping.Category.register(JUST_ZOOM_KEYMAPPING_CATEGORY_ID);

    public static final KeyMapping KEY_TOGGLE_ZOOM = new KeyMapping("justnicks.keybinds.keybind.zoom", InputConstants.KEY_Z, JUST_ZOOM_KEYMAPPING_CATEGORY);

}
