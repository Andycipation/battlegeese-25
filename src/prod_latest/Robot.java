package prod_latest;

import battlecode.common.*;

public abstract class Robot extends Globals {

    public static MapInfo[] nearbyMapInfos;
    public static RobotInfo[] nearbyAllyRobots;
    public static RobotInfo[] nearbyEnemyRobots;
    public static MapLocation curLoc;
    public static int numTowers;
    public static int roundNum;
    public static int numAllyAdjacent[][];

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
        numAllyAdjacent = new int[3][3];
        for (int i = nearbyAllyRobots.length; --i >= 0;) {
            MapLocation diff = nearbyAllyRobots[i].getLocation().translate(curLoc.x, curLoc.y);
            switch (diff.x * 10 + diff.y) {
                case -22: numAllyAdjacent[0][0]++; break;
                case -21: numAllyAdjacent[0][0]++; numAllyAdjacent[0][1]++; break;
                case -20: numAllyAdjacent[0][0]++; numAllyAdjacent[0][1]++; numAllyAdjacent[0][2]++; break;
                case -19: numAllyAdjacent[0][1]++; numAllyAdjacent[0][2]++; break;
                case -18: numAllyAdjacent[0][2]++; break;
                case -12: numAllyAdjacent[0][0]++; numAllyAdjacent[1][0]++; break;
                case -11: numAllyAdjacent[0][0]++; numAllyAdjacent[0][1]++; numAllyAdjacent[1][0]++; numAllyAdjacent[1][1]++; break;
                case -10: numAllyAdjacent[0][0]++; numAllyAdjacent[0][1]++; numAllyAdjacent[0][2]++; numAllyAdjacent[1][0]++; numAllyAdjacent[1][1]++; numAllyAdjacent[1][2]++; break;
                case -9: numAllyAdjacent[0][1]++; numAllyAdjacent[0][2]++; numAllyAdjacent[1][1]++; numAllyAdjacent[1][2]++; break;
                case -8: numAllyAdjacent[0][2]++; numAllyAdjacent[1][2]++; break;
                case -2: numAllyAdjacent[0][0]++; numAllyAdjacent[1][0]++; numAllyAdjacent[2][0]++; break;
                case -1: numAllyAdjacent[0][0]++; numAllyAdjacent[0][1]++; numAllyAdjacent[1][0]++; numAllyAdjacent[1][1]++; numAllyAdjacent[2][0]++; numAllyAdjacent[2][1]++; break;
                case 0: numAllyAdjacent[0][0]++; numAllyAdjacent[0][1]++; numAllyAdjacent[0][2]++; numAllyAdjacent[1][0]++; numAllyAdjacent[1][1]++; numAllyAdjacent[1][2]++; numAllyAdjacent[2][0]++; numAllyAdjacent[2][1]++; numAllyAdjacent[2][2]++; break;
                case 1: numAllyAdjacent[0][1]++; numAllyAdjacent[0][2]++; numAllyAdjacent[1][1]++; numAllyAdjacent[1][2]++; numAllyAdjacent[2][1]++; numAllyAdjacent[2][2]++; break;
                case 2: numAllyAdjacent[0][2]++; numAllyAdjacent[1][2]++; numAllyAdjacent[2][2]++; break;
                case 8: numAllyAdjacent[1][0]++; numAllyAdjacent[2][0]++; break;
                case 9: numAllyAdjacent[1][0]++; numAllyAdjacent[1][1]++; numAllyAdjacent[2][0]++; numAllyAdjacent[2][1]++; break;
                case 10: numAllyAdjacent[1][0]++; numAllyAdjacent[1][1]++; numAllyAdjacent[1][2]++; numAllyAdjacent[2][0]++; numAllyAdjacent[2][1]++; numAllyAdjacent[2][2]++; break;
                case 11: numAllyAdjacent[1][1]++; numAllyAdjacent[1][2]++; numAllyAdjacent[2][1]++; numAllyAdjacent[2][2]++; break;
                case 12: numAllyAdjacent[1][2]++; numAllyAdjacent[2][2]++; break;
                case 18: numAllyAdjacent[2][0]++; break;
                case 19: numAllyAdjacent[2][0]++; numAllyAdjacent[2][1]++; break;
                case 20: numAllyAdjacent[2][0]++; numAllyAdjacent[2][1]++; numAllyAdjacent[2][2]++; break;
                case 21: numAllyAdjacent[2][1]++; numAllyAdjacent[2][2]++; break;
                case 22: numAllyAdjacent[2][2]++; break;
            }
        }
        // really cheap startup, takes around 300-600 bytecodes up to here
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
