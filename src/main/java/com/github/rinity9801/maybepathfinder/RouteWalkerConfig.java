package com.github.rinity9801.maybepathfinder;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.annotations.Switch;

public class RouteWalkerConfig extends Config {

    @Switch(name = "Enable Sprinting")
    public static boolean sprintEnabled = true;

    @Switch(name = "Hold Left Click While Walking")
    public static boolean holdLeftClick = false;

    @Switch(name = "Repeat Route When Back at First Waypoint")
    public static boolean repeatRoute = true;

    public RouteWalkerConfig() {
        super(new Mod("Route Walker", ModType.UTIL_QOL), "routewalker.json", false, false);
        initialize();
    }
}
