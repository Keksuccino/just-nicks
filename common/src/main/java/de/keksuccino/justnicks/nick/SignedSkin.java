package de.keksuccino.justnicks.nick;

import com.mojang.authlib.properties.Property;

/**
 * Simple container for a signed skin property pulled from Mojang's session server.
 */
public record SignedSkin(String uuid, String name, String value, String signature) {

    public Property asProperty() {
        return new Property("textures", this.value, this.signature);
    }
}
