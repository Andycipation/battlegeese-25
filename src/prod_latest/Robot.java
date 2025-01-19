package prod_latest;

import battlecode.common.*;

public abstract class Robot extends Globals {

    public static MapInfo[] nearbyMapInfos;
    public static RobotInfo[] nearbyAllyRobots;
    public static RobotInfo[] nearbyEnemyRobots;
    public static MapLocation curLoc;
    public static int numTowers;
    public static int roundNum;
    // numAllyAdjacent is indexed by the same was as Direction.getDirectionOrderNum
    public static int numAllyAdjacent[];

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
        numAllyAdjacent = new int[9];
        for (int i = nearbyAllyRobots.length; --i >= 0;) {
            MapLocation diff = nearbyAllyRobots[i].getLocation().translate(-curLoc.x, -curLoc.y);
            switch (diff.x * 10 + diff.y) {
                case -22: numAllyAdjacent[8]++; break;
                case -21: numAllyAdjacent[8]++; numAllyAdjacent[1]++; break;
                case -20: numAllyAdjacent[8]++; numAllyAdjacent[1]++; numAllyAdjacent[2]++; break;
                case -19: numAllyAdjacent[1]++; numAllyAdjacent[2]++; break;
                case -18: numAllyAdjacent[2]++; break;
                case -12: numAllyAdjacent[8]++; numAllyAdjacent[7]++; break;
                case -11: numAllyAdjacent[8]++; numAllyAdjacent[1]++; numAllyAdjacent[7]++; numAllyAdjacent[0]++; break;
                case -10: numAllyAdjacent[8]++; numAllyAdjacent[1]++; numAllyAdjacent[2]++; numAllyAdjacent[7]++; numAllyAdjacent[0]++; numAllyAdjacent[3]++; break;
                case -9: numAllyAdjacent[1]++; numAllyAdjacent[2]++; numAllyAdjacent[0]++; numAllyAdjacent[3]++; break;
                case -8: numAllyAdjacent[2]++; numAllyAdjacent[3]++; break;
                case -2: numAllyAdjacent[8]++; numAllyAdjacent[7]++; numAllyAdjacent[6]++; break;
                case -1: numAllyAdjacent[8]++; numAllyAdjacent[1]++; numAllyAdjacent[7]++; numAllyAdjacent[0]++; numAllyAdjacent[6]++; numAllyAdjacent[5]++; break;
                case 0: numAllyAdjacent[8]++; numAllyAdjacent[1]++; numAllyAdjacent[2]++; numAllyAdjacent[7]++; numAllyAdjacent[0]++; numAllyAdjacent[3]++; numAllyAdjacent[6]++; numAllyAdjacent[5]++; numAllyAdjacent[4]++; break;
                case 1: numAllyAdjacent[1]++; numAllyAdjacent[2]++; numAllyAdjacent[0]++; numAllyAdjacent[3]++; numAllyAdjacent[5]++; numAllyAdjacent[4]++; break;
                case 2: numAllyAdjacent[2]++; numAllyAdjacent[3]++; numAllyAdjacent[4]++; break;
                case 8: numAllyAdjacent[7]++; numAllyAdjacent[6]++; break;
                case 9: numAllyAdjacent[7]++; numAllyAdjacent[0]++; numAllyAdjacent[6]++; numAllyAdjacent[5]++; break;
                case 10: numAllyAdjacent[7]++; numAllyAdjacent[0]++; numAllyAdjacent[3]++; numAllyAdjacent[6]++; numAllyAdjacent[5]++; numAllyAdjacent[4]++; break;
                case 11: numAllyAdjacent[0]++; numAllyAdjacent[3]++; numAllyAdjacent[5]++; numAllyAdjacent[4]++; break;
                case 12: numAllyAdjacent[3]++; numAllyAdjacent[4]++; break;
                case 18: numAllyAdjacent[6]++; break;
                case 19: numAllyAdjacent[6]++; numAllyAdjacent[5]++; break;
                case 20: numAllyAdjacent[6]++; numAllyAdjacent[5]++; numAllyAdjacent[4]++; break;
                case 21: numAllyAdjacent[5]++; numAllyAdjacent[4]++; break;
                case 22: numAllyAdjacent[4]++; break;
            }
        }
        // relatively cheap startup, takes around <1000 bytecodes up to here even when cluttered
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
