package prod_latest;

import battlecode.common.*;

public abstract class Unit extends Robot {
    static MapLocation paintTowerLoc;

    /**
     * Perform actions at the beginning of the robot's turn.
     * Here we can record new information, update robot state, etc...
     */
    void initTurn() throws GameActionException {
        super.initTurn();
        for (int i = nearbyAllyRobots.length; --i >= 0;) {
            var info = nearbyAllyRobots[i];
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
        super.endTurn();
    }

    void upgradeTowers() throws GameActionException {
        for (int i = nearbyAllyRobots.length; --i >= 0;)  {
            RobotInfo robotInfo = nearbyAllyRobots[i];
            if (robotInfo.type.canUpgradeType()) {
                if (rc.canUpgradeTower(robotInfo.location)) {
                    rc.upgradeTower(robotInfo.location);
                }
            }
        }
    }
}
