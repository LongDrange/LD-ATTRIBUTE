package com.longdrange.ldattribute.core.guide;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GuideStatsProvider {

    public static List<String> getGuideLore(Player player) {
        List<String> out = new ArrayList<>();
        if (player == null) return out;
        try {
            LDAttribute plugin = LDAttribute.getInstance();
            if (plugin == null || plugin.getCoreManager() == null) return out;
            GuideManager mgr = plugin.getCoreManager().getGuideManager();
            if (mgr == null) return out;
            GuideData data = mgr.get(player);
            for (GuideConfig.MonsterDef def : GuideConfig.allMonsters()) {
                if (!data.isUnlocked(def.id)) continue;
                for (String line : def.attribute) out.add(line);
            }

            // 集齐分组 + 称号属性
            GuideScoreManager sm = plugin.getCoreManager().getGuideScoreManager();
            if (sm != null) {
                out.addAll(sm.getSetAttributes(player));
                out.addAll(sm.getTitleAttributes(player));
            }
        } catch (Throwable ignored) {}
        return out;
    }
}
