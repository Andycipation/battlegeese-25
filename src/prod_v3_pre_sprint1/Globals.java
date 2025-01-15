package prod_v3_pre_sprint1;

import battlecode.common.*;

import java.util.Random;

public class Globals {

    public static RobotController rc;
    public static int id;
    
    public static Random rng;

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
        rng = new Random(id);

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
    
}