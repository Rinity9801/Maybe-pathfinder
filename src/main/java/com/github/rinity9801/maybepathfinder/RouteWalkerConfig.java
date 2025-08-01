package com.github.rinity9801.maybepathfinder;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;

public class RouteWalkerConfig extends Config {

    @Switch(name = "Enable Sprinting")
    public static boolean sprintEnabled = true;

    @Switch(name = "Hold Left Click While Walking")
    public static boolean holdLeftClick = false;

    @Switch(name = "Repeat Route When Back at First Waypoint")
    public static boolean repeatRoute = true;

    @Switch(name = "Enable Pitch Lock")
    public static boolean pitchLockEnabled = false;

    @Switch(name = "Enable Debug Logs")
    public static boolean debugLogs = true;

    @Slider(name = "Locked Pitch Angle", min = -90, max = 90)
    public static float pitchValue = 45f;

    public RouteWalkerConfig() {
        super(new Mod("Route Walker", ModType.UTIL_QOL), "routewalker.json", false, false);
        initialize();
    }
}
