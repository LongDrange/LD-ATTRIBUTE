package com.longdrange.ldattribute.points;

/**
 * 點券對外 API
 * 其他插件可透過 Bukkit Services 或直接呼叫
 */
public class PointAPI {

    public static int getPlayerPoints(String playerName) {
        return PointData.getPlayerPoints(playerName);
    }

    public static void setPlayerPoints(String playerName, int setPoints) {
        PointData.setPlayerPoints(playerName, setPoints);
    }

    public static void addPlayerPoints(String playerName, int addPoints) {
        PointData.addPlayerPoints(playerName, addPoints);
    }

    public static void takePlayerPoints(String playerName, int takePoints) {
        PointData.takePlayerPoints(playerName, takePoints);
    }

    public static Boolean isPlayerPoints(String playerName, int points) {
        return PointData.isPlayerPoints(playerName, points);
    }
}
