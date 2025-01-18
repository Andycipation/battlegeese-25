package prod_v4_demo;

import battlecode.common.*;

public abstract class Robot extends Globals {

    public static MapInfo[] nearbyMapInfos;
    public static RobotInfo[] nearbyAllyRobots;
    public static RobotInfo[] nearbyEnemyRobots;
    public static MapLocation curLoc;
    public static int numTowers;
    public static int roundNum;

    /**
     * Preform actions at the beginning of the robot's turn.
     * Here we can record new information, update robot state, etc...
     */
    void initTurn() throws GameActionException {
        nearbyMapInfos = rc.senseNearbyMapInfos();
        nearbyAllyRobots = rc.senseNearbyRobots(-1, myTeam);
        nearbyEnemyRobots = rc.senseNearbyRobots(-1, opponentTeam);
        curLoc = rc.getLocation();
        numTowers = rc.getNumberTowers();
        roundNum = rc.getRoundNum();
    }

    /**
     * Preform main robot turn actions. This includes moving and attacking.
     */
    void play() throws GameActionException {
        
    }

    /**
     * Preform actions at the end of robot's turn. This can be used for cleanup
     * and/or for using spare bytecode to preform expensive calculations that may
     * span across several turns.
     */
    void endTurn() throws GameActionException {

    }
    
}