package prod_v16_bad;

import battlecode.common.*;

public abstract class Robot extends Globals {

    public static MapInfo[] nearbyMapInfos;
    public static RobotInfo[] nearbyAllyRobots;
    public static RobotInfo[] nearbyEnemyRobots;
    public static MapLocation[] nearbyRuins;
    public static MapLocation locBeforeTurn;
    public static int numTowers;
    public static int spawnRound = -1;
    public static int roundNum;
    static Message[] lastRoundMessages;
    // numAllyAdjacent is indexed by the same was as Direction.getDirectionOrderNum
    public static int[] numAllyAdjacent;
    public static int inEnemyTowerRangeMask;

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

    // is my current location plus dir in an enemy tower range?
    public static boolean dirInEnemyTowerRange(Direction dir) {
        return (1 & (inEnemyTowerRangeMask >> dir.getDirectionOrderNum())) == 1;
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
        if (spawnRound == -1) spawnRound = roundNum;
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
        for (int i = nearbyEnemyRobots.length; --i >= 0;) {
            RobotInfo enemy = nearbyEnemyRobots[i];
            if (!enemy.getType().isTowerType()) continue;
            MapLocation diff = enemy.getLocation().translate(-locBeforeTurn.x, -locBeforeTurn.y);
            if (isDefenseTower(enemy.getType())) {
                switch ((diff.x + 4) * 9 + (diff.y + 4)) {
                    case 2: inEnemyTowerRangeMask |= 258; break; // (-4, -2)
                    case 3: inEnemyTowerRangeMask |= 390; break; // (-4, -1)
                    case 4: inEnemyTowerRangeMask |= 263; break; // (-4, 0)
                    case 5: inEnemyTowerRangeMask |= 270; break; // (-4, 1)
                    case 6: inEnemyTowerRangeMask |= 6; break; // (-4, 2)
                    case 10: inEnemyTowerRangeMask |= 386; break; // (-3, -3)
                    case 11: inEnemyTowerRangeMask |= 391; break; // (-3, -2)
                    case 12: inEnemyTowerRangeMask |= 463; break; // (-3, -1)
                    case 13: inEnemyTowerRangeMask |= 431; break; // (-3, 0)
                    case 14: inEnemyTowerRangeMask |= 415; break; // (-3, 1)
                    case 15: inEnemyTowerRangeMask |= 271; break; // (-3, 2)
                    case 16: inEnemyTowerRangeMask |= 14; break; // (-3, 3)
                    case 18: inEnemyTowerRangeMask |= 384; break; // (-2, -4)
                    case 19: inEnemyTowerRangeMask |= 451; break; // (-2, -3)
                    case 20: inEnemyTowerRangeMask |= 495; break; // (-2, -2)
                    case 21: inEnemyTowerRangeMask |= 511; break; // (-2, -1)
                    case 22: inEnemyTowerRangeMask |= 511; break; // (-2, 0)
                    case 23: inEnemyTowerRangeMask |= 511; break; // (-2, 1)
                    case 24: inEnemyTowerRangeMask |= 447; break; // (-2, 2)
                    case 25: inEnemyTowerRangeMask |= 31; break; // (-2, 3)
                    case 26: inEnemyTowerRangeMask |= 12; break; // (-2, 4)
                    case 27: inEnemyTowerRangeMask |= 450; break; // (-1, -4)
                    case 28: inEnemyTowerRangeMask |= 487; break; // (-1, -3)
                    case 29: inEnemyTowerRangeMask |= 511; break; // (-1, -2)
                    case 30: inEnemyTowerRangeMask |= 511; break; // (-1, -1)
                    case 31: inEnemyTowerRangeMask |= 511; break; // (-1, 0)
                    case 32: inEnemyTowerRangeMask |= 511; break; // (-1, 1)
                    case 33: inEnemyTowerRangeMask |= 511; break; // (-1, 2)
                    case 34: inEnemyTowerRangeMask |= 319; break; // (-1, 3)
                    case 35: inEnemyTowerRangeMask |= 30; break; // (-1, 4)
                    case 36: inEnemyTowerRangeMask |= 449; break; // (0, -4)
                    case 37: inEnemyTowerRangeMask |= 491; break; // (0, -3)
                    case 38: inEnemyTowerRangeMask |= 511; break; // (0, -2)
                    case 39: inEnemyTowerRangeMask |= 511; break; // (0, -1)
                    case 40: inEnemyTowerRangeMask |= 511; break; // (0, 0)
                    case 41: inEnemyTowerRangeMask |= 511; break; // (0, 1)
                    case 42: inEnemyTowerRangeMask |= 511; break; // (0, 2)
                    case 43: inEnemyTowerRangeMask |= 191; break; // (0, 3)
                    case 44: inEnemyTowerRangeMask |= 29; break; // (0, 4)
                    case 45: inEnemyTowerRangeMask |= 480; break; // (1, -4)
                    case 46: inEnemyTowerRangeMask |= 499; break; // (1, -3)
                    case 47: inEnemyTowerRangeMask |= 511; break; // (1, -2)
                    case 48: inEnemyTowerRangeMask |= 511; break; // (1, -1)
                    case 49: inEnemyTowerRangeMask |= 511; break; // (1, 0)
                    case 50: inEnemyTowerRangeMask |= 511; break; // (1, 1)
                    case 51: inEnemyTowerRangeMask |= 511; break; // (1, 2)
                    case 52: inEnemyTowerRangeMask |= 127; break; // (1, 3)
                    case 53: inEnemyTowerRangeMask |= 60; break; // (1, 4)
                    case 54: inEnemyTowerRangeMask |= 192; break; // (2, -4)
                    case 55: inEnemyTowerRangeMask |= 481; break; // (2, -3)
                    case 56: inEnemyTowerRangeMask |= 507; break; // (2, -2)
                    case 57: inEnemyTowerRangeMask |= 511; break; // (2, -1)
                    case 58: inEnemyTowerRangeMask |= 511; break; // (2, 0)
                    case 59: inEnemyTowerRangeMask |= 511; break; // (2, 1)
                    case 60: inEnemyTowerRangeMask |= 255; break; // (2, 2)
                    case 61: inEnemyTowerRangeMask |= 61; break; // (2, 3)
                    case 62: inEnemyTowerRangeMask |= 24; break; // (2, 4)
                    case 64: inEnemyTowerRangeMask |= 224; break; // (3, -3)
                    case 65: inEnemyTowerRangeMask |= 241; break; // (3, -2)
                    case 66: inEnemyTowerRangeMask |= 505; break; // (3, -1)
                    case 67: inEnemyTowerRangeMask |= 251; break; // (3, 0)
                    case 68: inEnemyTowerRangeMask |= 253; break; // (3, 1)
                    case 69: inEnemyTowerRangeMask |= 121; break; // (3, 2)
                    case 70: inEnemyTowerRangeMask |= 56; break; // (3, 3)
                    case 74: inEnemyTowerRangeMask |= 96; break; // (4, -2)
                    case 75: inEnemyTowerRangeMask |= 240; break; // (4, -1)
                    case 76: inEnemyTowerRangeMask |= 113; break; // (4, 0)
                    case 77: inEnemyTowerRangeMask |= 120; break; // (4, 1)
                    case 78: inEnemyTowerRangeMask |= 48; break; // (4, 2)
                }
            }
            else {
                switch ((diff.x + 4) * 9 + (diff.y + 4)) {
                    case 3: inEnemyTowerRangeMask |= 256; break; // (-4, -1)
                    case 4: inEnemyTowerRangeMask |= 2; break; // (-4, 0)
                    case 5: inEnemyTowerRangeMask |= 4; break; // (-4, 1)
                    case 10: inEnemyTowerRangeMask |= 256; break; // (-3, -3)
                    case 11: inEnemyTowerRangeMask |= 258; break; // (-3, -2)
                    case 12: inEnemyTowerRangeMask |= 390; break; // (-3, -1)
                    case 13: inEnemyTowerRangeMask |= 263; break; // (-3, 0)
                    case 14: inEnemyTowerRangeMask |= 270; break; // (-3, 1)
                    case 15: inEnemyTowerRangeMask |= 6; break; // (-3, 2)
                    case 16: inEnemyTowerRangeMask |= 4; break; // (-3, 3)
                    case 19: inEnemyTowerRangeMask |= 384; break; // (-2, -3)
                    case 20: inEnemyTowerRangeMask |= 387; break; // (-2, -2)
                    case 21: inEnemyTowerRangeMask |= 463; break; // (-2, -1)
                    case 22: inEnemyTowerRangeMask |= 431; break; // (-2, 0)
                    case 23: inEnemyTowerRangeMask |= 415; break; // (-2, 1)
                    case 24: inEnemyTowerRangeMask |= 15; break; // (-2, 2)
                    case 25: inEnemyTowerRangeMask |= 12; break; // (-2, 3)
                    case 27: inEnemyTowerRangeMask |= 256; break; // (-1, -4)
                    case 28: inEnemyTowerRangeMask |= 450; break; // (-1, -3)
                    case 29: inEnemyTowerRangeMask |= 487; break; // (-1, -2)
                    case 30: inEnemyTowerRangeMask |= 511; break; // (-1, -1)
                    case 31: inEnemyTowerRangeMask |= 511; break; // (-1, 0)
                    case 32: inEnemyTowerRangeMask |= 511; break; // (-1, 1)
                    case 33: inEnemyTowerRangeMask |= 319; break; // (-1, 2)
                    case 34: inEnemyTowerRangeMask |= 30; break; // (-1, 3)
                    case 35: inEnemyTowerRangeMask |= 4; break; // (-1, 4)
                    case 36: inEnemyTowerRangeMask |= 128; break; // (0, -4)
                    case 37: inEnemyTowerRangeMask |= 449; break; // (0, -3)
                    case 38: inEnemyTowerRangeMask |= 491; break; // (0, -2)
                    case 39: inEnemyTowerRangeMask |= 511; break; // (0, -1)
                    case 40: inEnemyTowerRangeMask |= 511; break; // (0, 0)
                    case 41: inEnemyTowerRangeMask |= 511; break; // (0, 1)
                    case 42: inEnemyTowerRangeMask |= 191; break; // (0, 2)
                    case 43: inEnemyTowerRangeMask |= 29; break; // (0, 3)
                    case 44: inEnemyTowerRangeMask |= 8; break; // (0, 4)
                    case 45: inEnemyTowerRangeMask |= 64; break; // (1, -4)
                    case 46: inEnemyTowerRangeMask |= 480; break; // (1, -3)
                    case 47: inEnemyTowerRangeMask |= 499; break; // (1, -2)
                    case 48: inEnemyTowerRangeMask |= 511; break; // (1, -1)
                    case 49: inEnemyTowerRangeMask |= 511; break; // (1, 0)
                    case 50: inEnemyTowerRangeMask |= 511; break; // (1, 1)
                    case 51: inEnemyTowerRangeMask |= 127; break; // (1, 2)
                    case 52: inEnemyTowerRangeMask |= 60; break; // (1, 3)
                    case 53: inEnemyTowerRangeMask |= 16; break; // (1, 4)
                    case 55: inEnemyTowerRangeMask |= 192; break; // (2, -3)
                    case 56: inEnemyTowerRangeMask |= 225; break; // (2, -2)
                    case 57: inEnemyTowerRangeMask |= 505; break; // (2, -1)
                    case 58: inEnemyTowerRangeMask |= 251; break; // (2, 0)
                    case 59: inEnemyTowerRangeMask |= 253; break; // (2, 1)
                    case 60: inEnemyTowerRangeMask |= 57; break; // (2, 2)
                    case 61: inEnemyTowerRangeMask |= 24; break; // (2, 3)
                    case 64: inEnemyTowerRangeMask |= 64; break; // (3, -3)
                    case 65: inEnemyTowerRangeMask |= 96; break; // (3, -2)
                    case 66: inEnemyTowerRangeMask |= 240; break; // (3, -1)
                    case 67: inEnemyTowerRangeMask |= 113; break; // (3, 0)
                    case 68: inEnemyTowerRangeMask |= 120; break; // (3, 1)
                    case 69: inEnemyTowerRangeMask |= 48; break; // (3, 2)
                    case 70: inEnemyTowerRangeMask |= 16; break; // (3, 3)
                    case 75: inEnemyTowerRangeMask |= 64; break; // (4, -1)
                    case 76: inEnemyTowerRangeMask |= 32; break; // (4, 0)
                    case 77: inEnemyTowerRangeMask |= 16; break; // (4, 1)
                }
            }
        }

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