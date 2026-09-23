package com.longdrange.ldattribute.points;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 點券資料核心
 * 從 TcPoints 的 Data.java 改寫
 */
public class PointData {

    private static final String POINTS_LIST = "PointsList";

    private static YamlConfiguration data;
    private static File dataFile;
    private static final Map<String, Integer> pointsMap = new HashMap<>();

    public static void init(LDAttribute plugin) {
        File dir = plugin.getDataFolder();
        if (!dir.exists()) dir.mkdirs();
        dataFile = new File(dir, "points.dat");
        loadData(plugin);
    }

    public static void loadData(LDAttribute plugin) {
        pointsMap.clear();

        if (!dataFile.exists()) {
            plugin.getLogger().info("建立 points.dat");
            data = new YamlConfiguration();
            data.set(POINTS_LIST, new ArrayList<String>());
            try { data.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
        } else {
            plugin.getLogger().info("讀取 points.dat");
        }

        data = new YamlConfiguration();
        try {
            data.load(dataFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            plugin.getLogger().warning("讀取 points.dat 失敗");
        }

        List<String> pointsList = getPointsList();
        if (pointsList == null) return;
        for (String str : pointsList) {
            String[] parts = str.split(":");
            if (parts.length >= 2) {
                try {
                    pointsMap.put(parts[0], Integer.parseInt(parts[1]));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<String> getPointsList() {
        Object obj = data.getList(POINTS_LIST);
        if (obj instanceof ArrayList) return (ArrayList<String>) obj;
        return new ArrayList<>();
    }

    public static void savePointsList(ArrayList<String> pointsList) {
        data.set(POINTS_LIST, pointsList);
        try { data.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    // ==================== 查詢 ====================

    public static int getPlayerPoints(String playerName) {
        return pointsMap.getOrDefault(playerName, 0);
    }

    public static boolean isPlayerPoints(String playerName, int points) {
        return getPlayerPoints(playerName) >= points;
    }

    // ==================== 修改 ====================

    public static void addPlayerPoints(String playerName, int addPoints) {
        int points = getPlayerPoints(playerName);
        int newPoints = points + addPoints;
        setPlayerPoints(playerName, newPoints);
        if (Bukkit.getOfflinePlayer(playerName).isOnline()) {
            Bukkit.getPlayer(playerName).sendMessage(
                    Message.get("Points.Player.Add", addPoints, newPoints));
        }
    }

    public static void takePlayerPoints(String playerName, int takePoints) {
        int points = getPlayerPoints(playerName);
        int newPoints = points - takePoints;
        setPlayerPoints(playerName, newPoints);
        if (Bukkit.getOfflinePlayer(playerName).isOnline()) {
            Bukkit.getPlayer(playerName).sendMessage(
                    Message.get("Points.Player.Take", takePoints, newPoints));
        }
    }

    public static void setPlayerPoints(String playerName, int setPoints) {
        ArrayList<String> pointsList = getPointsList();
        for (int i = 0; i < pointsList.size(); i++) {
            String str = pointsList.get(i);
            if (str.split(":")[0].equalsIgnoreCase(playerName)) {
                pointsList.set(i, playerName + ":" + setPoints);
                savePointsList(pointsList);
                pointsMap.put(playerName, setPoints);
                return;
            }
        }
        pointsList.add(playerName + ":" + setPoints);
        pointsMap.put(playerName, setPoints);
        savePointsList(pointsList);
    }

    // ==================== 排行榜 ====================

    public static void sendPointsTop(CommandSender sender, int page) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(pointsMap.entrySet());
        if (list.size() > 1) {
            Collections.sort(list, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        }

        sender.sendMessage(Message.get("Points.Player.TopTitle"));

        int value = 0;
        int valuePage = 0;
        for (Map.Entry<String, Integer> entry : list) {
            value++;
            valuePage++;
            if (valuePage <= (page - 1) * 10 || valuePage > page * 10) continue;
            String name = entry.getKey();
            int points = entry.getValue();
            if (points == 0) continue;
            sender.sendMessage(Message.get("Points.Player.TopItem",
                    value, name, points));
        }

        int maxPage = valuePage / 10;
        if (valuePage % 10 != 0) maxPage++;
        sender.sendMessage(Message.get("Points.Player.TopEnd", page, maxPage));
    }
}
