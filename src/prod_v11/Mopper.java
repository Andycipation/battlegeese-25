package prod_v11;

import battlecode.common.*;

public class Mopper extends Unit {
    public static String message = "";

    public static MopperStrategy strategy;
    public static int[] sweepScore = new int[9];
    public static int adjEnemyTile;
    public static int adjEnemyTileWithRobot;
    public static int adjWithEnemyRobot;
    public static int[] adjacentAllyTiles;
    public static int numAllyTilesAdjacent;

    public static void switchStrategy(MopperStrategy newStrategy, boolean acted) throws GameActionException {
        strategy = newStrategy;
        if (!acted) strategy.act();
    }

    public static void yieldStrategy() throws GameActionException {
        strategy = new OptimalPathingStrategy();
        // strategy = new ExploreStrategy(8, 6);
    }


    // public static boolean canAttackEnemeyRobotFromDir(Direction dir)  throws GameActionException{
    //     MapLocation attackLoc = rc.getLocation().add(dir);
    //     for (int i = nearbyEnemyRobots.length; --i >= 0;) {
    //         RobotInfo robot = nearbyEnemyRobots[i];
    //         MapLocation loc = robot.getLocation();
    //         if (rc.canAttack(loc) && robot.getPaintAmount() > 0 && robot.getType().isRobotType()) {
    //             if (attackLoc == null) {
    //                 attackLoc = loc;
    //             } 
    //             if (rc.senseMapInfo(loc).getPaint().isEnemy()) {
    //                 attackLoc = loc;
    //                 break;
    //             }
    //         }
    //     }
    //     if (attackLoc != null) {
    //         rc.attack(attackLoc);
    //         return true;
    //     }
    //     return false;
    // }


    public static boolean tryAttackEnemyRobot() throws GameActionException {
        MapLocation attackLoc = null;
        for (int i = nearbyEnemyRobots.length; --i >= 0;) {
            RobotInfo robot = nearbyEnemyRobots[i];
            MapLocation loc = robot.getLocation();
            if (rc.canAttack(loc) && robot.getPaintAmount() > 0 && robot.getType().isRobotType()) {
                if (attackLoc == null) {
                    attackLoc = loc;
                } 
                if (rc.senseMapInfo(loc).getPaint().isEnemy()) {
                    attackLoc = loc;
                    break;
                }
            }
        }
        if (attackLoc != null) {
            rc.attack(attackLoc);
            return true;
        }
        return false;
    }

    public static boolean tryPaintBelowSelf() throws GameActionException {
        return tryPaint(rc.getLocation(), PaintType.ALLY_PRIMARY);
    }

    public static boolean tryMopTile() throws GameActionException {
        for (int i = nearbyMapInfos.length; --i >= 0;) {
            MapInfo tile = nearbyMapInfos[i];
            MapLocation loc = tile.getMapLocation();
            if (rc.canAttack(loc) && tile.getPaint().isEnemy()) {
                rc.attack(loc);
                return true;
            }
        }
        return false;
    }

    // public static boolean tryMopRuinStrategy() throws GameActionException {
    //     for (int i = nearbyMapInfos.length; --i >= 0;) {
    //         MapInfo tile = nearbyMapInfos[i];
    //         MapLocation loc = tile.getMapLocation();
    //         RobotInfo robotInfo = rc.senseRobotAtLocation(loc);
    //         if(tile.hasRuin() && robotInfo == null) {
    //             switchStrategy(new MopRuinStrategy(loc));
    //             return true;
    //         }
    //     }
    //     return false;
    // }

    public static boolean tryTransferPaint(int amount) throws GameActionException {
        RobotInfo bestAlly = null;
        int leastPaint = 10000;
        for (int i = nearbyAllyRobots.length; --i >= 0;) {
            RobotInfo ally = nearbyAllyRobots[i];
            if (!ally.getType().isRobotType() || (ally.getPaintAmount() > 0 && ally.getType() == UnitType.MOPPER)) continue;
            if (rc.canTransferPaint(ally.getLocation(), amount) && ally.getPaintAmount() + amount <= ally.getType().paintCapacity) {
                if (ally.getPaintAmount() < leastPaint) {
                    leastPaint = ally.getPaintAmount();
                    bestAlly = ally;
                }
            }
        }
        if (bestAlly != null) {
            rc.transferPaint(bestAlly.getLocation(), amount);
            // rc.setTimelineMarker("transferred paint!", 0, 255, 0);
            return true;
        }
        return false;
    }

    public static void precomputeMovementInfo() throws GameActionException {
        sweepScore = new int[9];
        adjWithEnemyRobot = 0;
        adjEnemyTile = 0;
        adjEnemyTileWithRobot = 0;
        numAllyTilesAdjacent = 0;

        for (int i = nearbyEnemyRobots.length; --i >= 0;) {
            RobotInfo robot = nearbyEnemyRobots[i];
            if (robot.getType().isTowerType() || robot.getPaintAmount() == 0) continue;
            MapLocation loc = robot.getLocation();
            MapLocation diff = loc.translate(-locBeforeTurn.x, -locBeforeTurn.y);
            switch ((diff.x + 4) * 9 + (diff.y)) {
                case 7: sweepScore[8] += 4096; break; // (-3, -2)
                case 8: sweepScore[1] += 4096; sweepScore[8] += 4096; break; // (-3, -1)
                case 9: sweepScore[1] += 4096; sweepScore[2] += 4096; sweepScore[8] += 4096; break; // (-3, 0)
                case 10: sweepScore[1] += 4096; sweepScore[2] += 4096; break; // (-3, 1)
                case 11: sweepScore[2] += 4096; break; // (-3, 2)
                case 15: sweepScore[8] += 256; break; // (-2, -3)
                case 16: sweepScore[1] += 256; sweepScore[7] += 4096; sweepScore[8] += 4352; break; // (-2, -2)
                case 17: sweepScore[0] += 4096; sweepScore[1] += 4352; sweepScore[2] += 256; sweepScore[7] += 4096; sweepScore[8] += 4096; break; // (-2, -1)
                case 18: sweepScore[0] += 4096; sweepScore[1] += 4096; sweepScore[2] += 4352; sweepScore[3] += 4096; sweepScore[7] += 4096; sweepScore[8] += 4097; break; // (-2, 0)
                case 19: sweepScore[0] += 4096; sweepScore[1] += 4097; sweepScore[2] += 4096; sweepScore[3] += 4096; sweepScore[8] += 1; break; // (-2, 1)
                case 20: sweepScore[1] += 1; sweepScore[2] += 4097; sweepScore[3] += 4096; break; // (-2, 2)
                case 21: sweepScore[2] += 1; break; // (-2, 3)
                case 24: sweepScore[7] += 256; sweepScore[8] += 256; break; // (-1, -3)
                case 25: sweepScore[0] += 256; sweepScore[1] += 256; sweepScore[6] += 4096; sweepScore[7] += 4352; sweepScore[8] += 256; break; // (-1, -2)
                case 26: sweepScore[0] += 4352; sweepScore[1] += 256; sweepScore[2] += 256; sweepScore[3] += 256; sweepScore[5] += 4096; sweepScore[6] += 4096; sweepScore[7] += 4096; break; // (-1, -1)
                case 27: sweepScore[0] += 4096; sweepScore[2] += 256; sweepScore[3] += 4352; sweepScore[4] += 4096; sweepScore[5] += 4096; sweepScore[6] += 4096; sweepScore[7] += 4097; sweepScore[8] += 1; break; // (-1, 0)
                case 28: sweepScore[0] += 4097; sweepScore[1] += 1; sweepScore[3] += 4096; sweepScore[4] += 4096; sweepScore[5] += 4096; sweepScore[7] += 1; sweepScore[8] += 1; break; // (-1, 1)
                case 29: sweepScore[0] += 1; sweepScore[1] += 1; sweepScore[2] += 1; sweepScore[3] += 4097; sweepScore[4] += 4096; break; // (-1, 2)
                case 30: sweepScore[2] += 1; sweepScore[3] += 1; break; // (-1, 3)
                case 33: sweepScore[6] += 256; sweepScore[7] += 256; sweepScore[8] += 256; break; // (0, -3)
                case 34: sweepScore[0] += 256; sweepScore[1] += 256; sweepScore[5] += 256; sweepScore[6] += 4352; sweepScore[7] += 256; sweepScore[8] += 272; break; // (0, -2)
                case 35: sweepScore[0] += 256; sweepScore[1] += 272; sweepScore[2] += 256; sweepScore[3] += 256; sweepScore[4] += 256; sweepScore[5] += 4352; sweepScore[6] += 4096; sweepScore[8] += 16; break; // (0, -1)
                case 36: sweepScore[1] += 16; sweepScore[2] += 272; sweepScore[3] += 256; sweepScore[4] += 4352; sweepScore[5] += 4096; sweepScore[6] += 4097; sweepScore[7] += 1; sweepScore[8] += 17; break; // (0, 0)
                case 37: sweepScore[0] += 1; sweepScore[1] += 17; sweepScore[2] += 16; sweepScore[4] += 4096; sweepScore[5] += 4097; sweepScore[6] += 1; sweepScore[7] += 1; sweepScore[8] += 1; break; // (0, 1)
                case 38: sweepScore[0] += 1; sweepScore[1] += 1; sweepScore[2] += 17; sweepScore[3] += 1; sweepScore[4] += 4097; sweepScore[5] += 1; break; // (0, 2)
                case 39: sweepScore[2] += 1; sweepScore[3] += 1; sweepScore[4] += 1; break; // (0, 3)
                case 42: sweepScore[6] += 256; sweepScore[7] += 256; break; // (1, -3)
                case 43: sweepScore[0] += 256; sweepScore[5] += 256; sweepScore[6] += 256; sweepScore[7] += 272; sweepScore[8] += 16; break; // (1, -2)
                case 44: sweepScore[0] += 272; sweepScore[1] += 16; sweepScore[3] += 256; sweepScore[4] += 256; sweepScore[5] += 256; sweepScore[7] += 16; sweepScore[8] += 16; break; // (1, -1)
                case 45: sweepScore[0] += 16; sweepScore[1] += 16; sweepScore[2] += 16; sweepScore[3] += 272; sweepScore[4] += 256; sweepScore[6] += 1; sweepScore[7] += 17; sweepScore[8] += 16; break; // (1, 0)
                case 46: sweepScore[0] += 17; sweepScore[1] += 16; sweepScore[2] += 16; sweepScore[3] += 16; sweepScore[5] += 1; sweepScore[6] += 1; sweepScore[7] += 1; break; // (1, 1)
                case 47: sweepScore[0] += 1; sweepScore[2] += 16; sweepScore[3] += 17; sweepScore[4] += 1; sweepScore[5] += 1; break; // (1, 2)
                case 48: sweepScore[3] += 1; sweepScore[4] += 1; break; // (1, 3)
                case 51: sweepScore[6] += 256; break; // (2, -3)
                case 52: sweepScore[5] += 256; sweepScore[6] += 272; sweepScore[7] += 16; break; // (2, -2)
                case 53: sweepScore[0] += 16; sweepScore[4] += 256; sweepScore[5] += 272; sweepScore[6] += 16; sweepScore[7] += 16; break; // (2, -1)
                case 54: sweepScore[0] += 16; sweepScore[3] += 16; sweepScore[4] += 272; sweepScore[5] += 16; sweepScore[6] += 17; sweepScore[7] += 16; break; // (2, 0)
                case 55: sweepScore[0] += 16; sweepScore[3] += 16; sweepScore[4] += 16; sweepScore[5] += 17; sweepScore[6] += 1; break; // (2, 1)
                case 56: sweepScore[3] += 16; sweepScore[4] += 17; sweepScore[5] += 1; break; // (2, 2)
                case 57: sweepScore[4] += 1; break; // (2, 3)
                case 61: sweepScore[6] += 16; break; // (3, -2)
                case 62: sweepScore[5] += 16; sweepScore[6] += 16; break; // (3, -1)
                case 63: sweepScore[4] += 16; sweepScore[5] += 16; sweepScore[6] += 16; break; // (3, 0)
                case 64: sweepScore[4] += 16; sweepScore[5] += 16; break; // (3, 1)
                case 65: sweepScore[4] += 16; break; // (3, 2)
            }
            switch ((diff.x + 4) * 9 + (diff.y + 4)) {
                case 20: adjWithEnemyRobot |= 256; break; // (-2, -2)
                case 21: adjWithEnemyRobot |= 258; break; // (-2, -1)
                case 22: adjWithEnemyRobot |= 262; break; // (-2, 0)
                case 23: adjWithEnemyRobot |= 6; break; // (-2, 1)
                case 24: adjWithEnemyRobot |= 4; break; // (-2, 2)
                case 29: adjWithEnemyRobot |= 384; break; // (-1, -2)
                case 30: adjWithEnemyRobot |= 387; break; // (-1, -1)
                case 31: adjWithEnemyRobot |= 399; break; // (-1, 0)
                case 32: adjWithEnemyRobot |= 15; break; // (-1, 1)
                case 33: adjWithEnemyRobot |= 12; break; // (-1, 2)
                case 38: adjWithEnemyRobot |= 448; break; // (0, -2)
                case 39: adjWithEnemyRobot |= 483; break; // (0, -1)
                case 40: adjWithEnemyRobot |= 511; break; // (0, 0)
                case 41: adjWithEnemyRobot |= 63; break; // (0, 1)
                case 42: adjWithEnemyRobot |= 28; break; // (0, 2)
                case 47: adjWithEnemyRobot |= 192; break; // (1, -2)
                case 48: adjWithEnemyRobot |= 225; break; // (1, -1)
                case 49: adjWithEnemyRobot |= 249; break; // (1, 0)
                case 50: adjWithEnemyRobot |= 57; break; // (1, 1)
                case 51: adjWithEnemyRobot |= 24; break; // (1, 2)
                case 56: adjWithEnemyRobot |= 64; break; // (2, -2)
                case 57: adjWithEnemyRobot |= 96; break; // (2, -1)
                case 58: adjWithEnemyRobot |= 112; break; // (2, 0)
                case 59: adjWithEnemyRobot |= 48; break; // (2, 1)
                case 60: adjWithEnemyRobot |= 16; break; // (2, 2)
            }
        }
        
        for (int i = nearbyMapInfos.length; --i >= 0;) {
            MapInfo tile = nearbyMapInfos[i];
            MapLocation loc = tile.getMapLocation();
            MapLocation diff = loc.translate(-locBeforeTurn.x, -locBeforeTurn.y);
            if (tile.getPaint().isAlly()) {
                switch ((diff.x + 4) * 9 + (diff.y + 4)) {
                    case 21: numAllyTilesAdjacent += 16777216; break; // (-2, -1)
                    case 22: numAllyTilesAdjacent += 8; break; // (-2, 0)
                    case 23: numAllyTilesAdjacent += 64; break; // (-2, 1)
                    case 29: numAllyTilesAdjacent += 16777216; break; // (-1, -2)
                    case 30: numAllyTilesAdjacent += 18874376; break; // (-1, -1)
                    case 31: numAllyTilesAdjacent += 16777289; break; // (-1, 0)
                    case 32: numAllyTilesAdjacent += 584; break; // (-1, 1)
                    case 33: numAllyTilesAdjacent += 64; break; // (-1, 2)
                    case 38: numAllyTilesAdjacent += 2097152; break; // (0, -2)
                    case 39: numAllyTilesAdjacent += 19136513; break; // (0, -1)
                    case 40: numAllyTilesAdjacent += 2130441; break; // (0, 0)
                    case 41: numAllyTilesAdjacent += 4673; break; // (0, 1)
                    case 42: numAllyTilesAdjacent += 512; break; // (0, 2)
                    case 47: numAllyTilesAdjacent += 262144; break; // (1, -2)
                    case 48: numAllyTilesAdjacent += 2392064; break; // (1, -1)
                    case 49: numAllyTilesAdjacent += 299009; break; // (1, 0)
                    case 50: numAllyTilesAdjacent += 37376; break; // (1, 1)
                    case 51: numAllyTilesAdjacent += 4096; break; // (1, 2)
                    case 57: numAllyTilesAdjacent += 262144; break; // (2, -1)
                    case 58: numAllyTilesAdjacent += 32768; break; // (2, 0)
                    case 59: numAllyTilesAdjacent += 4096; break; // (2, 1)
                    case 60: numAllyTilesAdjacent += 16; break; // (2, 2)
                }
            }
            else if (tile.getPaint().isEnemy()){
                switch ((diff.x + 4) * 9 + (diff.y + 4)) {
                    case 20: adjEnemyTile |= 256; break; // (-2, -2)
                    case 21: adjEnemyTile |= 258; break; // (-2, -1)
                    case 22: adjEnemyTile |= 262; break; // (-2, 0)
                    case 23: adjEnemyTile |= 6; break; // (-2, 1)
                    case 24: adjEnemyTile |= 4; break; // (-2, 2)
                    case 29: adjEnemyTile |= 384; break; // (-1, -2)
                    case 30: adjEnemyTile |= 387; break; // (-1, -1)
                    case 31: adjEnemyTile |= 399; break; // (-1, 0)
                    case 32: adjEnemyTile |= 15; break; // (-1, 1)
                    case 33: adjEnemyTile |= 12; break; // (-1, 2)
                    case 38: adjEnemyTile |= 448; break; // (0, -2)
                    case 39: adjEnemyTile |= 483; break; // (0, -1)
                    case 40: adjEnemyTile |= 511; break; // (0, 0)
                    case 41: adjEnemyTile |= 63; break; // (0, 1)
                    case 42: adjEnemyTile |= 28; break; // (0, 2)
                    case 47: adjEnemyTile |= 192; break; // (1, -2)
                    case 48: adjEnemyTile |= 225; break; // (1, -1)
                    case 49: adjEnemyTile |= 249; break; // (1, 0)
                    case 50: adjEnemyTile |= 57; break; // (1, 1)
                    case 51: adjEnemyTile |= 24; break; // (1, 2)
                    case 56: adjEnemyTile |= 64; break; // (2, -2)
                    case 57: adjEnemyTile |= 96; break; // (2, -1)
                    case 58: adjEnemyTile |= 112; break; // (2, 0)
                    case 59: adjEnemyTile |= 48; break; // (2, 1)
                    case 60: adjEnemyTile |= 16; break; // (2, 2)
                }
                RobotInfo robotInfo = rc.senseRobotAtLocation(loc);
                if (robotInfo != null && robotInfo.getTeam() == opponentTeam) {
                    switch ((diff.x + 4) * 9 + (diff.y + 4)) {
                        case 20: adjEnemyTileWithRobot |= 256; break; // (-2, -2)
                        case 21: adjEnemyTileWithRobot |= 258; break; // (-2, -1)
                        case 22: adjEnemyTileWithRobot |= 262; break; // (-2, 0)
                        case 23: adjEnemyTileWithRobot |= 6; break; // (-2, 1)
                        case 24: adjEnemyTileWithRobot |= 4; break; // (-2, 2)
                        case 29: adjEnemyTileWithRobot |= 384; break; // (-1, -2)
                        case 30: adjEnemyTileWithRobot |= 387; break; // (-1, -1)
                        case 31: adjEnemyTileWithRobot |= 399; break; // (-1, 0)
                        case 32: adjEnemyTileWithRobot |= 15; break; // (-1, 1)
                        case 33: adjEnemyTileWithRobot |= 12; break; // (-1, 2)
                        case 38: adjEnemyTileWithRobot |= 448; break; // (0, -2)
                        case 39: adjEnemyTileWithRobot |= 483; break; // (0, -1)
                        case 40: adjEnemyTileWithRobot |= 511; break; // (0, 0)
                        case 41: adjEnemyTileWithRobot |= 63; break; // (0, 1)
                        case 42: adjEnemyTileWithRobot |= 28; break; // (0, 2)
                        case 47: adjEnemyTileWithRobot |= 192; break; // (1, -2)
                        case 48: adjEnemyTileWithRobot |= 225; break; // (1, -1)
                        case 49: adjEnemyTileWithRobot |= 249; break; // (1, 0)
                        case 50: adjEnemyTileWithRobot |= 57; break; // (1, 1)
                        case 51: adjEnemyTileWithRobot |= 24; break; // (1, 2)
                        case 56: adjEnemyTileWithRobot |= 64; break; // (2, -2)
                        case 57: adjEnemyTileWithRobot |= 96; break; // (2, -1)
                        case 58: adjEnemyTileWithRobot |= 112; break; // (2, 0)
                        case 59: adjEnemyTileWithRobot |= 48; break; // (2, 1)
                        case 60: adjEnemyTileWithRobot |= 16; break; // (2, 2)
                    }
                }
            }
        }
    }

    public static int getMoveEnemiesSwept(Direction moveDir, Direction swing) {
        int idx;
        switch (swing) {
            case NORTH:
                idx = 0;
                break;
            case EAST:
                idx = 4;
                break;
            case SOUTH:
                idx = 8;
                break;
            case WEST:
                idx = 16;
                break;
            default:
                idx = -1;
                break;
        }
        return (sweepScore[moveDir.getDirectionOrderNum()] >> idx) & 15;
    }

    public static boolean getMoveAdjEnemyTileWithEnemyRobot(Direction moveDir) {
        return (1 & (adjEnemyTileWithRobot >> (moveDir.getDirectionOrderNum()))) == 1;
    }

    public static boolean getMoveAdjEnemyTile(Direction moveDir) {
        return (1 & (adjEnemyTile >> (moveDir.getDirectionOrderNum()))) == 1;
    }

    // only adjacent by an edge (no corners)
    public static int getNumAllyTilesAdjacent(Direction attackDir) {
        return (numAllyTilesAdjacent >> (attackDir.getDirectionOrderNum() * 3)) & 0b111;
    }

    public static boolean getMoveAdjEnemyRobot(Direction moveDir) {
        return (1 & (adjWithEnemyRobot >> (moveDir.getDirectionOrderNum()))) == 1;
    }

    public static boolean isEnemyTerritory(Direction dir) throws GameActionException {
        MapLocation loc = rc.getLocation().add(dir);
        if (!withinBounds(loc)) return false;
        return rc.senseMapInfo(loc).getPaint().isEnemy();
    }
    
    // return true if just moved
    public static boolean tryMoveAttackEnemyTileWithEnemyRobot() throws GameActionException {
        for (int i = Direction.DIRECTION_ORDER.length; --i >= 0;) {
            Direction dir = Direction.DIRECTION_ORDER[i];
            if (isEnemyTerritory(dir)) continue;
            if ((i == 0 || rc.canMove(dir)) && !dirInEnemyTowerRange(dir) && getMoveAdjEnemyTileWithEnemyRobot(dir)) {
                mdir(dir);
                for (int j = nearbyEnemyRobots.length; --j >= 0;) {
                    RobotInfo enemy = nearbyEnemyRobots[j];
                    if (enemy.getType().isRobotType() && rc.canAttack(enemy.getLocation()) && rc.senseMapInfo(enemy.getLocation()).getPaint().isEnemy()) {
                        rc.attack(enemy.getLocation());
                        message += "MoveAttackEnemyTileWithEnemyRobot" + enemy.getLocation();
                        return true;
                    }
                }
                message += "NO ATTACK DONE WAAAAAH";
                return true;
            }
        }
        return false;
    }

    // return true if just moved
    public static boolean tryMoveSweepCrowd() throws GameActionException {
        Direction bestMoveDir = null;
        Direction bestSweepDir = null;
        int bestSweep = 0;
        for (int i = Direction.DIRECTION_ORDER.length; --i >= 0;) {
            Direction moveDir = Direction.DIRECTION_ORDER[i];
            if (isEnemyTerritory(moveDir)) continue;
            if ((i == 0 || rc.canMove(moveDir)) && !dirInEnemyTowerRange(moveDir)) {
                for (int j = 4; --j >= 0;) {
                    Direction swingDir = cardinalDirections[j];
                    int cnt = getMoveEnemiesSwept(moveDir, swingDir);
                    if (cnt > bestSweep) {
                        bestMoveDir = moveDir;
                        bestSweepDir = swingDir;
                        bestSweep = cnt;
                    }
                } 
            }
        }
        message += "sweep gets " + bestSweep;
        if (bestMoveDir != null && bestSweep >= 2) {
            mdir(bestMoveDir);
            if (rc.canMopSwing(bestSweepDir)) {
                rc.mopSwing(bestSweepDir);
            }
            message += "tryMoveSweepCrowd";
            return true;
        } 
        return false;
    }

    public static boolean tryMoveAttackEnemyRobotWithoutTile() throws GameActionException {
        for (int i = Direction.DIRECTION_ORDER.length; --i >= 0;) {
            Direction dir = Direction.DIRECTION_ORDER[i];
            if (isEnemyTerritory(dir)) continue;
            if ((i == 0 || rc.canMove(dir)) && !dirInEnemyTowerRange(dir) && getMoveAdjEnemyRobot(dir)) {
                mdir(dir);
                tryAttackEnemyRobot();
                message += "tryMoveAttackEnemyRobotWithoutTile";
                return true;
            }
        }
        return false;
    }

    public static boolean tryMoveAttackEnemyTile() throws GameActionException {
        for (int i = Direction.DIRECTION_ORDER.length; --i >= 0;) {
            Direction dir = Direction.DIRECTION_ORDER[i];
            if (isEnemyTerritory(dir)) continue;
            if ((i == 0 || rc.canMove(dir)) && !dirInEnemyTowerRange(dir) && getMoveAdjEnemyTile(dir)) {
                rc.move(dir);
                tryMopTile();
                return true;
            }
        }
        return false;
    }


    public static boolean tryAttackMostNestedEnemyTile() throws GameActionException {
        MapLocation curLoc = rc.getLocation();
        MapLocation bestAttackLoc = null;
        int mostAdjacentCnt = 0;
        for (int i = Direction.DIRECTION_ORDER.length; --i >= 0;) {
            Direction dir = Direction.DIRECTION_ORDER[i];
            MapLocation attackLoc = curLoc.add(dir);
            if (!withinBounds(attackLoc)) continue;
            MapInfo tile = rc.senseMapInfo(attackLoc);
            if (tile.getPaint().isEnemy()) {
                int cnt = getNumAllyTilesAdjacent(dir);
                message += "reached";
                if (cnt > mostAdjacentCnt || bestAttackLoc == null) {
                    bestAttackLoc = attackLoc;
                    mostAdjacentCnt = cnt;
                }
            }
        }
        if (bestAttackLoc != null && rc.canAttack(bestAttackLoc)) {
            rc.attack(bestAttackLoc);
            message += "tryAttackMostNestedEnemyTile";
            message += "bruh " + mostAdjacentCnt + " " + bestAttackLoc;
            return true;
        }
        return false;
    }

    @Override
    void play() throws GameActionException {
        if (strategy == null) {
            yieldStrategy();
        }
        strategy.act();
        Logger.log(strategy.toString());
        if (rc.getPaint() < 30  && paintTowerLoc != null && rc.getChips() < 3000) {
            Logger.log("refilling paint");
            Logger.flush();
            strategy = new RefillPaintStrategy(100);
        }
        // also wanna upgrade towers nearby if possible
        upgradeTowers();
    }

    public static boolean trySweep() throws GameActionException {
        int[] dir_counts = new int[8];

        MapLocation my_loc = rc.getLocation();

        for(int i = nearbyMapInfos.length; --i >= 0;) {
            MapInfo tile = nearbyMapInfos[i];
            MapLocation loc = tile.getMapLocation();

            if(my_loc.isAdjacentTo(loc)) {
                RobotInfo robotInfo = rc.senseRobotAtLocation(loc);
                if(robotInfo != null && robotInfo.getTeam() != rc.getTeam()) {
                    assert(my_loc != loc);
                    dir_counts[my_loc.directionTo(loc).getDirectionOrderNum() % 8]++;
                }
            }
        }

        // Find an ordinal direction where there are 3 enemy robots
        Direction dir = null;
        Direction[] ord_dirs = {Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH};

        for(int i = 0; i < 8; i += 2) {
            if(dir_counts[i] + dir_counts[i + 1] + dir_counts[(i + 2) % 8] >= 2) {
                dir = ord_dirs[i / 2];
                break;
            }
        }

        if(dir != null) {
            if(rc.canMopSwing(dir)) {
                rc.mopSwing(dir);
                return true;
            }
        }

        return false;
    }

    abstract static class MopperStrategy {
        abstract public void act() throws GameActionException;
    }

    static class ExploreStrategy extends MopperStrategy {
    
        public static MapLocation target;
        public static int turnsNotMoved;
        public static int switchStrategyCooldown;
    
        public ExploreStrategy() throws GameActionException {
            turnsNotMoved = 0;

            MapLocation possibleTarget = null;
            if (informedEnemyPaintLoc != null && rc.canSenseLocation(informedEnemyPaintLoc) && rc.senseMapInfo(informedEnemyPaintLoc).getPaint().isEnemy()) {
                possibleTarget = informedEnemyPaintLoc;
            }
            if (possibleTarget != null) {
                target = project(locBeforeTurn, possibleTarget);
            }
            else {
                target = new MapLocation(rng.nextInt(mapWidth), rng.nextInt(mapHeight));
            }
        }
    
        @Override
        public void act() throws GameActionException {
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapInfo tile = nearbyMapInfos[i];
                if (tile.getPaint().isEnemy()) {
                    switchStrategy(new OptimalPathingStrategy(), false);
                    return;
                }
            }
            for (int i = nearbyEnemyRobots.length; --i >= 0;) {
                RobotInfo robot = nearbyEnemyRobots[i];
                if (robot.getType().isRobotType()) {
                    switchStrategy(new OptimalPathingStrategy(), false);
                    return;
                }
            }
            if (chebyshevDist(locBeforeTurn, target) <= 2) { // my target is likely outdated, reset!
                switchStrategy(new ExploreStrategy(), false);
                return;
            }

            BugNav.moveToward(target);
            rc.setIndicatorLine(rc.getLocation(), target, 0, 255, 0);
            if (rc.getLocation() == locBeforeTurn) {
                turnsNotMoved++;
                if (turnsNotMoved >= 3) {
                    yieldStrategy();
                    return;
                }
            }

            else turnsNotMoved = 0;
        }
    
        @Override
        public String toString() {
            return "Explore " + " " + target;
        }
    }

    // badarded -- refill paint strategy, copied from solder
    // TODO: unify this
    static class RefillPaintStrategy extends MopperStrategy {
        private static int refillTo;

        public RefillPaintStrategy(int _refillTo) {
            // assert(rc.getPaint() < refillTo);
            refillTo = _refillTo;
        }

        @Override
        public void act() throws GameActionException {
            if (rc.getPaint() >= refillTo) {
                yieldStrategy();
                return;
            }
            if (paintTowerLoc == null) {
                yieldStrategy();
                return;
            }
            final var dir = BugNav.getDirectionToMove(paintTowerLoc);

            tryAttackEnemyRobot();
            tryMopTile();

            if (dir == null) {
                // We have no valid moves!
                return;
            }

            if (!rc.canSenseRobotAtLocation(paintTowerLoc)) {
                // We're still very far, just move closer
                rc.move(dir);
                return;
            }

            final var paintTowerInfo = rc.senseRobotAtLocation(paintTowerLoc);
            if (!Globals.isAllyPaintTower(paintTowerInfo)) {
                System.out.println("Our paint tower got destroyed and changed to something else!");
                paintTowerLoc = null;
                yieldStrategy();
                return;
            }

            // If we wouldn't start incurring penalty from the tower, move closer
            final var nextLoc = rc.getLocation().add(dir);
            if (nextLoc.distanceSquaredTo(paintTowerLoc) > GameConstants.PAINT_TRANSFER_RADIUS_SQUARED) {
                rc.move(dir);
                return;
            }

            final var spaceToFill = refillTo - rc.getPaint();
            if (paintTowerInfo.getPaintAmount() >= spaceToFill) {
                rc.move(dir);
                if (rc.canTransferPaint(paintTowerLoc, -spaceToFill)) {
                    rc.transferPaint(paintTowerLoc, -spaceToFill);
                    yieldStrategy();
                }
            }
        }

        @Override
        public String toString() {
            return "RefillPaintStrategy " + paintTowerLoc;
        }

    }

    static class CampFrontier extends MopperStrategy {
        CampFrontier() {

        }

        public void act() throws GameActionException {
            // if (!tryMoveToFrontier()) {
            //     switchStrategy(new OptimalPathingStrategy());
            //     return;
            // }

            tryMoveToFrontier();

            tryMoveToSafeTile();

            tryMoveLessSafeTile();

            trySweep();

            tryAttackEnemyRobot();

            tryMopTile();

            if (rc.getPaint() >= 50) {
                tryTransferPaint(rc.getPaint()-30);
            }
            
        }

        @Override
        public String toString() {
            return "CampFrontier ";
        }

    }

    static class OptimalPathingStrategy extends MopperStrategy {

        public void act() throws GameActionException {
            boolean acted = false;
            message = "";
            if (rc.isMovementReady()) {
                int bytecode = Clock.getBytecodeNum();
                precomputeMovementInfo();
                // System.out.println("Precomp: " + (Clock.getBytecodeNum() - bytecode));
                
                // for (int i = Direction.DIRECTION_ORDER.length; --i >= 0;) {
                    //     Direction dir = Direction.DIRECTION_ORDER[i];
                    //     Logger.log("" + getMoveAdjEnemyTile(dir));
                // }
                
                bytecode = Clock.getBytecodeNum();
                // 1. sweep >= 2 ppl
                if (!acted) acted |= tryMoveSweepCrowd();
                
                // 2. attacking enemy on enemy tile 
                if (!acted) acted |= tryMoveAttackEnemyTileWithEnemyRobot();
    
                // 3. attack enemy not on tile
                if (!acted) acted |= tryMoveAttackEnemyRobotWithoutTile();
                
                // 4. Attacking enemy tile, pick the one with most adjacent friendly paint (nested)
                if (!acted) tryAttackMostNestedEnemyTile();
                // System.out.println("Acting: " + (Clock.getBytecodeNum() - bytecode));
            }

            if (!acted) {
                message += "not acted";
                tryMoveToFrontier();
                int bytecode = Clock.getBytecodeNum();
    
                if (!acted) acted |= tryMoveToSafeTile();
    
                if (!acted) acted |= tryMoveLessSafeTile();
                // System.out.println("Moving: " + (Clock.getBytecodeNum() - bytecode));
            }

            if (rc.getPaint() >= 50) {
                tryTransferPaint(rc.getPaint()-30);
            }
        }

        @Override
        public String toString() {
            return "Optimal Pathing: " + message;
        }
    }
}