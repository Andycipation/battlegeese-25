package prod;

import battlecode.common.*;

import java.util.Random;

public abstract class Robot {

    final RobotController rc;
    final Random rng = new Random(1097);
    final int id;
    final int mapWidth;
    final int mapHeight;
    final UnitType unitType;
    final int paintCapacity;
    final int actionRadiusSquared;
    final int visionRadiusSquared;

    public Robot(RobotController _rc) {
        rc = _rc;
        id = rc.getID();
        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();
        unitType = rc.getType();
        paintCapacity = unitType.paintCapacity;
        actionRadiusSquared = unitType.actionRadiusSquared;
        visionRadiusSquared = GameConstants.VISION_RADIUS_SQUARED;
    }

    /**
     * Preform actions at the beginning of the robot's turn.
     * Here we can record new information, update robot state, etc...
     */
    void initTurn() throws GameActionException {

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
