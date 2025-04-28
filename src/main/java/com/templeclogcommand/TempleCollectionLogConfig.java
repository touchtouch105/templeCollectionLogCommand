package net.runelite.client.plugins.templecollectionlog;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("templecollectionlog")
public interface TempleCollectionLogConfig extends Config {
    @ConfigItem(
            position = 0,
            keyName = "log",
            name = "LOG Command",
            description = "Configures whether the TempleOSRS !log command is enabled: !log [boss]"
    )
    default boolean log() {
        return true;
    }
    @ConfigItem(
            position = 1,
            keyName = "bosses",
            name = "BOSSES Command",
            description = "Enable !log bosses command"
    )
    default boolean bosses() {
        return false;
    }
    @ConfigItem(
            position = 2,
            keyName = "minigames",
            name = "MINIGAMES Command",
            description = "Enable !log minigames command"
    )
    default boolean minigames() {
        return false;
    }
    @ConfigItem(
            position = 3,
            keyName = "other",
            name = "OTHER Command",
            description = "Enable !log other command"
    )
    default boolean other() {
        return false;
    }
    @ConfigItem(
            position = 4,
            keyName = "clues all",
            name = "CLUES (all) Command",
            description = "Enable !log clues command"
    )
    default boolean clues() {
        return false;
    }
    @ConfigItem(
            position = 5,
            keyName = "entire clog",
            name = "entire clog Command",
            description = "Enable entire clog !log all command"
    )
    default boolean all() {
        return false;
    }
}
