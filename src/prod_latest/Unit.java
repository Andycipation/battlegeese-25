package prod_latest;

import battlecode.common.*;

public abstract class Unit extends Robot {
    static MapLocation paintTowerLoc;

    /**
     * Preform actions at the beginning of the robot's turn.
     * Here we can record new information, update robot state, etc...
     */
    void initTurn() throws GameActionException {
        super.initTurn();
        var infos = rc.senseNearbyRobots();
        for (var info : infos) {
            if (Globals.isPaintTower(info.type) && info.team == Globals.myTeam) {
                paintTowerLoc = info.location;
            }
        }
    }

    /**
     * Preform main robot turn actions. This includes moving and attacking.
     */
    void play() throws GameActionException {
        super.play();
    }

    /**
     * Preform actions at the end of robot's turn. This can be used for cleanup
     * and/or for using spare bytecode to preform expensive calculations that may
     * span across several turns.
     */
    void endTurn() throws GameActionException {
        super.initTurn();
    }
}
