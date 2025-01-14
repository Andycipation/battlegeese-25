package prod;

import battlecode.common.*;

public abstract class Robot extends Globals {

    public static PaintType[][] paintMemory;
    public static boolean[][] unpaintable;

    Robot() {
        paintMemory = new PaintType[mapWidth][mapHeight];
        for (int i = 0; i < mapWidth; ++i) {
            for (int j = 0; j < mapHeight; ++j) {
                paintMemory[i][j] = PaintType.EMPTY;
            }
        }
        unpaintable = new boolean[mapWidth][mapHeight];
    }

    /**
     * Preform actions at the beginning of the robot's turn.
     * Here we can record new information, update robot state, etc...
     */
    void initTurn() throws GameActionException {
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        for (MapInfo tile : nearbyTiles) {
            MapLocation loc = tile.getMapLocation();
            paintMemory[loc.x][loc.y] = tile.getPaint();
            if (tile.hasRuin() || tile.isWall())
                unpaintable[loc.x][loc.y] = true;
        }
    }

    /**
     * Preform main robot turn actions. This includes moving and attacking.
     */
    abstract void play() throws GameActionException;

    /**
     * Preform actions at the end of robot's turn. This can be used for cleanup
     * and/or for using spare bytecode to preform expensive calculations that may
     * span across several turns.
     */
    void endTurn() throws GameActionException {

    }
    
}
