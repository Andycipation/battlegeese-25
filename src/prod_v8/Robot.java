package prod_v8;

import battlecode.common.*;

public abstract class Robot extends Globals {

    public static MapInfo[] nearbyMapInfos;
    public static RobotInfo[] nearbyAllyRobots;
    public static RobotInfo[] nearbyEnemyRobots;
    public static MapLocation[] nearbyRuins;
    public static MapLocation locBeforeTurn;
    public static int numTowers;
    public static int roundNum;
    static Message[] lastRoundMessages;
    // numAllyAdjacent is indexed by the same was as Direction.getDirectionOrderNum
    public static int[] numAllyAdjacent;

    public static final int CHIP_AGGREGATION_WINDOW = 50;
    public static int[] chipCountQueue;

    public static double getProgress() {
        double estimatedRuins = 0.02 * mapHeight * mapWidth;
        return Math.min(numTowers / (0.6 * estimatedRuins), 1.0);
        // long sumX = 0, sumX2 = 0;
        // int cnt = 0;
        // for (long x : chipCountQueue) {
        //     if (x != -1) {
        //         sumX += x;
        //         sumX2 += x * x;
        //         cnt += 1;
        //     }
        // }
        // if (cnt < CHIP_AGGREGATION_WINDOW) {
        //     return -1;
        // }
        // double EX2 = 1.0 * sumX2 / cnt;
        // double EX = 1.0 * sumX / cnt;
        // double std = Math.sqrt(EX2 - EX * EX);
        // System.out.println("" + EX + " " + std);
        // return EX / std;
    }

    /**
     * Preform actions at the beginning of the robot's turn.
     * Here we can record new information, update robot state, etc...
     */
    void initTurn() throws GameActionException {
        nearbyMapInfos = rc.senseNearbyMapInfos();
        nearbyAllyRobots = rc.senseNearbyRobots(-1, myTeam);
        nearbyEnemyRobots = rc.senseNearbyRobots(-1, opponentTeam);
        nearbyRuins = rc.senseNearbyRuins(visionRadiusSquared);
        locBeforeTurn = rc.getLocation();
        numTowers = rc.getNumberTowers();
        roundNum = rc.getRoundNum();
        numAllyAdjacent = new int[9];
        lastRoundMessages = rc.readMessages(roundNum - 1);
        for (int i = nearbyAllyRobots.length; --i >= 0;) {
            MapLocation diff = nearbyAllyRobots[i].getLocation().translate(-locBeforeTurn.x, -locBeforeTurn.y);
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

        // TODO: this can be optimized a lot, e.g. with a ring-like queue
        if (chipCountQueue == null) {
            chipCountQueue = new int[CHIP_AGGREGATION_WINDOW];
            for (int i = CHIP_AGGREGATION_WINDOW; --i >= 0;) {
                chipCountQueue[i] = -1;
            }
        }
        for (int i = CHIP_AGGREGATION_WINDOW; --i > 0;) {
            chipCountQueue[i] = chipCountQueue[i - 1];
        }
        chipCountQueue[0] = rc.getChips();
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