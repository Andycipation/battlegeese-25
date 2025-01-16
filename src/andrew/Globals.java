package andrew;

import java.util.Random;

import battlecode.common.*;

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

    static MapLocation[] srpCenters = new MapLocation[1000];
    static int numSrpCenters = 0;

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

        for (int x = 0; x < mapWidth; ++x) {
            for (int y = 0; y < mapHeight; ++y) {
                if ((x + y * 3) % 10 == 0) {
                    srpCenters[numSrpCenters++] = new MapLocation(x, y);
                }
            }
        }
    }
    
    public static PaintType getDefaultColor(MapLocation loc) {
        if ((loc.x + loc.y) % 2 == 0 && (loc.x + loc.y * 3) % 10 != 0) {
            return PaintType.ALLY_SECONDARY;
        }
        return PaintType.ALLY_PRIMARY;
    }

    public static MapLocation randomMapLocation() {
        return new MapLocation(rng.nextInt(mapWidth), rng.nextInt(mapHeight));
    }
}