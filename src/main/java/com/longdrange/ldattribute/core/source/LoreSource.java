package com.longdrange.ldattribute.core.source;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.api.AttributeSource;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 通用 Lore 来源
 * 包装一个 (Player) -> List<String> 函数
 * 返回 ["属性名: +值", ...]
 */
public class LoreSource implements AttributeSource {

    private final String name;
    private final Function<Player, List<String>> provider;

    public LoreSource(String name, Function<Player, List<String>> provider) {
        this.name = name;
        this.provider = provider;
    }

    @Override
    public String getName() { return name; }

    @Override
    public List<Entry> getEntries(Player player) {
        List<Entry> out = new ArrayList<>();
        try {
            List<String> lore = provider.apply(player);
            if (lore == null || lore.isEmpty()) return out;
            LDAttribute plugin = LDAttribute.getInstance();
            if (plugin == null || plugin.getApi() == null) return out;
            LDAttributeData d = plugin.getApi().getLoreData(player, null, lore);
            out.add(new Entry(name, d));
        } catch (Throwable ignored) {}
        return out;
    }
}