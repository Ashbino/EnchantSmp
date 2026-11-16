package com.enchantsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

/** Reads MiniMessage-formatted messages from config.yml (so /enchant reload updates them). */
public final class Messages {

    private final EnchantSmp plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public Messages(EnchantSmp plugin) {
        this.plugin = plugin;
    }

    public Component get(String key, TagResolver... resolvers) {
        FileConfiguration cfg = plugin.getConfig();
        String prefix = cfg.getString("messages.prefix", "");
        String text = cfg.getString("messages." + key, "<red>Missing message: " + key);
        return mm.deserialize(prefix + text, resolvers);
    }

    public void send(CommandSender to, String key, TagResolver... resolvers) {
        to.sendMessage(get(key, resolvers));
    }

    public static TagResolver text(String name, String value) {
        return Placeholder.unparsed(name, value);
    }

    public static TagResolver component(String name, Component value) {
        return Placeholder.component(name, value);
    }
}
