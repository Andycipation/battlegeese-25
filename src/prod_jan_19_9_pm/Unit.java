package prod_jan_19_9_pm;

import battlecode.common.*;

public abstract class Unit extends Robot {
    static MapLocation paintTowerLoc;

    /**
     * Perform actions at the beginning of the robot's turn.
     * Here we can record new information, update robot state, etc...
     */
    void initTurn() throws GameActionException {
        super.initTurn();
        for (var info : nearbyAllyRobots) {
            if (Globals.isAllyPaintTower(info)) {
                paintTowerLoc = info.location;
            }
        }
    }

    /**
     * Perform main robot turn actions. This includes moving and attacking.
     */
    void play() throws GameActionException {
        super.play();
    }

    /**
     * Perform actions at the end of robot's turn. This can be used for cleanup
     * and/or for using spare bytecode to preform expensive calculations that may
     * span across several turns.
     */
    void endTurn() throws GameActionException {
        super.initTurn();
    }
}
