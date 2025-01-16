package template;

import java.util.Random;

import battlecode.common.*;

public class Globals {

    public static RobotController rc;
    public static int id;
    
    public static Random rng = new Random(1097);

    public static int mapWidth;
    public static int mapHeight;

    public static UnitType unitType;
    public static int paintCapacity;
    public static int actionRadiusSquared;
    public static int visionRadiusSquared;

    public static Team myTeam;
    public static Team opponentTeam;

    public static Direction[] allDirections = Direction.values();
    public static Direction[] adjacentDirections = {
        Direction.NORTH,
        Direction.EAST,
        Direction.SOUTH,
        Direction.WEST,
        Direction.NORTHEAST,
        Direction.SOUTHEAST,
        Direction.SOUTHWEST,
        Direction.NORTHWEST
    };

    public static void init(RobotController _rc) {
        rc = _rc;
        id = rc.getID();

        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();

        unitType = rc.getType();
        paintCapacity = unitType.paintCapacity;
        actionRadiusSquared = unitType.actionRadiusSquared;
        visionRadiusSquared = GameConstants.VISION_RADIUS_SQUARED;

        myTeam = rc.getTeam();
        opponentTeam =  myTeam.opponent();
    }

    public static double getPaintProportion() {
        return (double)rc.getPaint() / (rc.getType().paintCapacity);
    }

    public static MapLocation randomMapLocation() {
        return new MapLocation(rng.nextInt(mapWidth), rng.nextInt(mapHeight));
    }

    /**
     * Picks a random integer 0-1 with P[i] = wi / sum_i wi
     */
    public static int randChoice(int w1, int w2) {
        return rng.nextInt(w1 + w2) < w1 ? 0 : 1;
    }

    /**
     * Picks a random integer 0-2 with P[i] = wi / sum_i wi
     */
    public static int randChoice(int w1, int w2, int w3) {
        int x = rng.nextInt(w1 + w2 + w3);
        if (x < w1) return 0;
        return x < w1 + w2 ? 1 : 2;
    }

    /**
     * Picks a random integer 0-3 with P[i] = wi / sum_i wi
     */
    public static int randChoice(int w1, int w2, int w3, int w4) {
        int x = rng.nextInt(w1 + w2 + w3 + w4);
        if (x < w1) return 0;
        if (x < w1 + w2) return 1;
        return x < w1 + w2 + w3 ? 2 : 3;
    }

    /**
     * Picks a random integer 0-4 with P[i] = wi / sum_i wi
     */
    public static int randChoice(int w1, int w2, int w3, int w4, int w5) {
        int x = rng.nextInt(w1 + w2 + w3 + w4 + w5);
        if (x < w1) return 0;
        if (x < w1 + w2) return 1;
        if (x < w1 + w2 + w3) return 2;
        return x < w1 + w2 + w3 + w4 ? 3 : 4;
    }
    
}
