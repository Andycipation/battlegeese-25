package prod_latest;

import battlecode.common.*;

public class Mopper extends Unit {

    public static MopperStrategy strategy;
    public static int[] sweepScore = new int[9];
    public static int adjEnemyTile;
    public static int adjEnemyTileWithRobot;
    public static int adjWithEnemyRobot;
    public static int[] adjacentAllyTiles;
    public static int numAllyTilesAdjacent;

    public static void switchStrategy(MopperStrategy newStrategy) {
        strategy = newStrategy;
    }

    public static void yieldStrategy() {
        strategy = new ExploreStrategy(8, 6);
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

    public static boolean tryMopRuinStrategy() throws GameActionException {
        for (int i = nearbyMapInfos.length; --i >= 0;) {
            MapInfo tile = nearbyMapInfos[i];
            MapLocation loc = tile.getMapLocation();
            RobotInfo robotInfo = rc.senseRobotAtLocation(loc);
            if(tile.hasRuin() && robotInfo == null) {
                switchStrategy(new MopRuinStrategy(loc));
                return true;
            }
        }
        return false;
    }

    public static boolean tryTransferPaintSoldier(int amount) throws GameActionException {
        for (int i = nearbyAllyRobots.length; --i >= 0;) {
            RobotInfo ally = nearbyAllyRobots[i];
            if (ally.getType().isRobotType() && ally.getType() != UnitType.MOPPER) continue;
            if (rc.canTransferPaint(ally.getLocation(), amount) && ally.getPaintAmount() + amount <= ally.getType().paintCapacity) {
                rc.transferPaint(ally.getLocation(), amount);
                return true;
            }
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
            if (robot.getType().isTowerType()) continue;
            MapLocation loc = robot.getLocation();
            MapLocation diff = loc.translate(locBeforeTurn.x, locBeforeTurn.y);
            switch ((diff.x + 4) * 9 + (diff.y)) {
                case 7: sweepScore[8] |= 4096; break; // (-3, -2)
                case 8: sweepScore[1] |= 4096; sweepScore[8] |= 4096; break; // (-3, -1)
                case 9: sweepScore[1] |= 4096; sweepScore[2] |= 4096; sweepScore[8] |= 4096; break; // (-3, 0)
                case 10: sweepScore[1] |= 4096; sweepScore[2] |= 4096; break; // (-3, 1)
                case 11: sweepScore[2] |= 4096; break; // (-3, 2)
                case 15: sweepScore[8] |= 256; break; // (-2, -3)
                case 16: sweepScore[1] |= 256; sweepScore[7] |= 4096; sweepScore[8] |= 4352; break; // (-2, -2)
                case 17: sweepScore[0] |= 4096; sweepScore[1] |= 4352; sweepScore[2] |= 256; sweepScore[7] |= 4096; sweepScore[8] |= 4096; break; // (-2, -1)
                case 18: sweepScore[0] |= 4096; sweepScore[1] |= 4096; sweepScore[2] |= 4352; sweepScore[3] |= 4096; sweepScore[7] |= 4096; sweepScore[8] |= 4097; break; // (-2, 0)
                case 19: sweepScore[0] |= 4096; sweepScore[1] |= 4097; sweepScore[2] |= 4096; sweepScore[3] |= 4096; sweepScore[8] |= 1; break; // (-2, 1)
                case 20: sweepScore[1] |= 1; sweepScore[2] |= 4097; sweepScore[3] |= 4096; break; // (-2, 2)
                case 21: sweepScore[2] |= 1; break; // (-2, 3)
                case 24: sweepScore[7] |= 256; sweepScore[8] |= 256; break; // (-1, -3)
                case 25: sweepScore[0] |= 256; sweepScore[1] |= 256; sweepScore[6] |= 4096; sweepScore[7] |= 4352; sweepScore[8] |= 256; break; // (-1, -2)
                case 26: sweepScore[0] |= 4352; sweepScore[1] |= 256; sweepScore[2] |= 256; sweepScore[3] |= 256; sweepScore[5] |= 4096; sweepScore[6] |= 4096; sweepScore[7] |= 4096; break; // (-1, -1)
                case 27: sweepScore[0] |= 4096; sweepScore[2] |= 256; sweepScore[3] |= 4352; sweepScore[4] |= 4096; sweepScore[5] |= 4096; sweepScore[6] |= 4096; sweepScore[7] |= 4097; sweepScore[8] |= 1; break; // (-1, 0)
                case 28: sweepScore[0] |= 4097; sweepScore[1] |= 1; sweepScore[3] |= 4096; sweepScore[4] |= 4096; sweepScore[5] |= 4096; sweepScore[7] |= 1; sweepScore[8] |= 1; break; // (-1, 1)
                case 29: sweepScore[0] |= 1; sweepScore[1] |= 1; sweepScore[2] |= 1; sweepScore[3] |= 4097; sweepScore[4] |= 4096; break; // (-1, 2)
                case 30: sweepScore[2] |= 1; sweepScore[3] |= 1; break; // (-1, 3)
                case 33: sweepScore[6] |= 256; sweepScore[7] |= 256; sweepScore[8] |= 256; break; // (0, -3)
                case 34: sweepScore[0] |= 256; sweepScore[1] |= 256; sweepScore[5] |= 256; sweepScore[6] |= 4352; sweepScore[7] |= 256; sweepScore[8] |= 272; break; // (0, -2)
                case 35: sweepScore[0] |= 256; sweepScore[1] |= 272; sweepScore[2] |= 256; sweepScore[3] |= 256; sweepScore[4] |= 256; sweepScore[5] |= 4352; sweepScore[6] |= 4096; sweepScore[8] |= 16; break; // (0, -1)
                case 36: sweepScore[1] |= 16; sweepScore[2] |= 272; sweepScore[3] |= 256; sweepScore[4] |= 4352; sweepScore[5] |= 4096; sweepScore[6] |= 4097; sweepScore[7] |= 1; sweepScore[8] |= 17; break; // (0, 0)
                case 37: sweepScore[0] |= 1; sweepScore[1] |= 17; sweepScore[2] |= 16; sweepScore[4] |= 4096; sweepScore[5] |= 4097; sweepScore[6] |= 1; sweepScore[7] |= 1; sweepScore[8] |= 1; break; // (0, 1)
                case 38: sweepScore[0] |= 1; sweepScore[1] |= 1; sweepScore[2] |= 17; sweepScore[3] |= 1; sweepScore[4] |= 4097; sweepScore[5] |= 1; break; // (0, 2)
                case 39: sweepScore[2] |= 1; sweepScore[3] |= 1; sweepScore[4] |= 1; break; // (0, 3)
                case 42: sweepScore[6] |= 256; sweepScore[7] |= 256; break; // (1, -3)
                case 43: sweepScore[0] |= 256; sweepScore[5] |= 256; sweepScore[6] |= 256; sweepScore[7] |= 272; sweepScore[8] |= 16; break; // (1, -2)
                case 44: sweepScore[0] |= 272; sweepScore[1] |= 16; sweepScore[3] |= 256; sweepScore[4] |= 256; sweepScore[5] |= 256; sweepScore[7] |= 16; sweepScore[8] |= 16; break; // (1, -1)
                case 45: sweepScore[0] |= 16; sweepScore[1] |= 16; sweepScore[2] |= 16; sweepScore[3] |= 272; sweepScore[4] |= 256; sweepScore[6] |= 1; sweepScore[7] |= 17; sweepScore[8] |= 16; break; // (1, 0)
                case 46: sweepScore[0] |= 17; sweepScore[1] |= 16; sweepScore[2] |= 16; sweepScore[3] |= 16; sweepScore[5] |= 1; sweepScore[6] |= 1; sweepScore[7] |= 1; break; // (1, 1)
                case 47: sweepScore[0] |= 1; sweepScore[2] |= 16; sweepScore[3] |= 17; sweepScore[4] |= 1; sweepScore[5] |= 1; break; // (1, 2)
                case 48: sweepScore[3] |= 1; sweepScore[4] |= 1; break; // (1, 3)
                case 51: sweepScore[6] |= 256; break; // (2, -3)
                case 52: sweepScore[5] |= 256; sweepScore[6] |= 272; sweepScore[7] |= 16; break; // (2, -2)
                case 53: sweepScore[0] |= 16; sweepScore[4] |= 256; sweepScore[5] |= 272; sweepScore[6] |= 16; sweepScore[7] |= 16; break; // (2, -1)
                case 54: sweepScore[0] |= 16; sweepScore[3] |= 16; sweepScore[4] |= 272; sweepScore[5] |= 16; sweepScore[6] |= 17; sweepScore[7] |= 16; break; // (2, 0)
                case 55: sweepScore[0] |= 16; sweepScore[3] |= 16; sweepScore[4] |= 16; sweepScore[5] |= 17; sweepScore[6] |= 1; break; // (2, 1)
                case 56: sweepScore[3] |= 16; sweepScore[4] |= 17; sweepScore[5] |= 1; break; // (2, 2)
                case 57: sweepScore[4] |= 1; break; // (2, 3)
                case 61: sweepScore[6] |= 16; break; // (3, -2)
                case 62: sweepScore[5] |= 16; sweepScore[6] |= 16; break; // (3, -1)
                case 63: sweepScore[4] |= 16; sweepScore[5] |= 16; sweepScore[6] |= 16; break; // (3, 0)
                case 64: sweepScore[4] |= 16; sweepScore[5] |= 16; break; // (3, 1)
                case 65: sweepScore[4] |= 16; break; // (3, 2)
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
            MapLocation diff = loc.translate(locBeforeTurn.x, locBeforeTurn.y);
            if (tile.getPaint().isAlly()) {
                switch ((diff.x + 4) * 9 + (diff.y + 4)) {
                    case 21: numAllyTilesAdjacent |= 16777216; break; // (-2, -1)
                    case 22: numAllyTilesAdjacent |= 8; break; // (-2, 0)
                    case 23: numAllyTilesAdjacent |= 64; break; // (-2, 1)
                    case 29: numAllyTilesAdjacent |= 16777216; break; // (-1, -2)
                    case 30: numAllyTilesAdjacent |= 18874376; break; // (-1, -1)
                    case 31: numAllyTilesAdjacent |= 16777289; break; // (-1, 0)
                    case 32: numAllyTilesAdjacent |= 584; break; // (-1, 1)
                    case 33: numAllyTilesAdjacent |= 64; break; // (-1, 2)
                    case 38: numAllyTilesAdjacent |= 2097152; break; // (0, -2)
                    case 39: numAllyTilesAdjacent |= 19136513; break; // (0, -1)
                    case 40: numAllyTilesAdjacent |= 2130441; break; // (0, 0)
                    case 41: numAllyTilesAdjacent |= 4673; break; // (0, 1)
                    case 42: numAllyTilesAdjacent |= 512; break; // (0, 2)
                    case 47: numAllyTilesAdjacent |= 262144; break; // (1, -2)
                    case 48: numAllyTilesAdjacent |= 2392064; break; // (1, -1)
                    case 49: numAllyTilesAdjacent |= 299009; break; // (1, 0)
                    case 50: numAllyTilesAdjacent |= 37376; break; // (1, 1)
                    case 51: numAllyTilesAdjacent |= 4096; break; // (1, 2)
                    case 57: numAllyTilesAdjacent |= 262144; break; // (2, -1)
                    case 58: numAllyTilesAdjacent |= 32768; break; // (2, 0)
                    case 59: numAllyTilesAdjacent |= 4096; break; // (2, 1)
                    case 60: numAllyTilesAdjacent |= 16; break; // (2, 2)
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

    public static int enemiesSwept(Direction moveDir, Direction swing) {
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

    public static boolean hasEnemyTileWithRobot(Direction moveDir) {
        return (1 & (adjEnemyTileWithRobot >> (moveDir.getDirectionOrderNum()))) == 1;
    }

    public static boolean hasEnemyTile(Direction moveDir) {
        return (1 & (adjEnemyTile >> (moveDir.getDirectionOrderNum()))) == 1;
    }

    // only adjacent by an edge (no corners)
    public static int countNumAllyTilesAdjacent(Direction attackDir) {
        return (numAllyTilesAdjacent >> (attackDir.getDirectionOrderNum() * 3)) & 0b111;
    }

    public static boolean getAdjWithEnemyRobot(Direction moveDir) {
        return (1 & (adjWithEnemyRobot >> (moveDir.getDirectionOrderNum()))) == 1;
    }
    // return true if just moved
    public static boolean tryMoveAttackEnemyTileWithRobot() throws GameActionException {
        for (int i = Direction.DIRECTION_ORDER.length; --i >= 0;) {
            Direction dir = Direction.DIRECTION_ORDER[i];
            if ((i == 0 || rc.canMove(dir)) && !dirInEnemyTowerRange(dir) && hasEnemyTileWithRobot(dir)) {
                rc.move(dir);
                for (int j = nearbyEnemyRobots.length; --j >= 0;) {
                    RobotInfo enemy = nearbyEnemyRobots[j];
                    if (enemy.getType().isRobotType() && rc.canAttack(enemy.getLocation()) && rc.senseMapInfo(enemy.getLocation()).getPaint().isEnemy()) {
                        rc.attack(enemy.getLocation());
                        return true;
                    }
                }
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
            if ((i == 0 || rc.canMove(moveDir)) && !dirInEnemyTowerRange(moveDir)) {
                for (int j = 4; --j >= 0;) {
                    Direction swingDir = cardinalDirections[j];
                    int cnt = enemiesSwept(moveDir, swingDir);
                    if (cnt > bestSweep) {
                        bestMoveDir = moveDir;
                        bestSweepDir = swingDir;
                        bestSweep = cnt;
                    }
                } 
            }
        }
        if (bestMoveDir != null) {
            rc.move(bestMoveDir);
            if (rc.canMopSwing(bestSweepDir)) {
                rc.mopSwing(bestSweepDir);
            }
            rc.setTimelineMarker("SWEEEEPT", 0, 255, 0);
            return true;
        } 
        return false;
    }

    public static boolean tryMoveAttackEnemyRobotWithoutTile() throws GameActionException {
        for (int i = Direction.DIRECTION_ORDER.length; --i >= 0;) {
            Direction dir = Direction.DIRECTION_ORDER[i];
            if ((i == 0 || rc.canMove(dir)) && !dirInEnemyTowerRange(dir) && getAdjWithEnemyRobot(dir)) {
                rc.move(dir);
                tryAttackEnemyRobot();
                return true;
            }
        }
        return false;
    }

    public static boolean tryMoveAttackEnemyTile() throws GameActionException {
        for (int i = Direction.DIRECTION_ORDER.length; --i >= 0;) {
            Direction dir = Direction.DIRECTION_ORDER[i];
            if ((i == 0 || rc.canMove(dir)) && !dirInEnemyTowerRange(dir) && hasEnemyTile(dir)) {
                rc.move(dir);
                tryMopTile();
                return true;
            }
        }
        return false;
    }

    public static boolean tryAttackInternalEnemyTile() throws GameActionException {
        for (int i = Direction.DIRECTION_ORDER.length; --i >= 0;) {
            Direction dir = Direction.DIRECTION_ORDER[i];
            MapLocation attackLoc = rc.getLocation().add(dir);
            MapInfo tile = rc.senseMapInfo(attackLoc);
            if (rc.canAttack(attackLoc) && tile.getPaint().isEnemy() &&  countNumAllyTilesAdjacent(dir) >= 2) {
                rc.attack(attackLoc);
                return true;
            }
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
        if (rc.getPaint() < 5 && paintTowerLoc != null) {
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
                // rc.setTimelineMarker("SWEEP", 0, 255, 0);
                return true;
            }
        }

        return false;
    }

    abstract static class MopperStrategy {
        abstract public void act() throws GameActionException;
    }
    
    static class AggroStrategy extends MopperStrategy {

        int cleanRuinCoolDown;
        AggroStrategy(int _cleanRuinCoolDown) {
            cleanRuinCoolDown = _cleanRuinCoolDown;
        }
    
        @Override
        public void act() throws GameActionException {
            trySweep();

            if (cleanRuinCoolDown-- <= 0) {
                for (int i = nearbyMapInfos.length; --i >= 0;) {
                    MapInfo tile = nearbyMapInfos[i];
                    MapLocation loc = tile.getMapLocation();
                    RobotInfo robotInfo = rc.senseRobotAtLocation(loc);
                    if(tile.hasRuin() && robotInfo == null) {
                        switchStrategy(new MopRuinStrategy(loc));
                        return;
                    }
                }
            }

            MapLocation nextTarget = null;

            for (int i = nearbyEnemyRobots.length; --i >= 0;) {
                RobotInfo robot = nearbyEnemyRobots[i];
                MapLocation loc = robot.getLocation();
                if (rc.canAttack(loc) && robot.getPaintAmount() > 0) {
                    rc.attack(loc);
                } else {
                    nextTarget = loc;
                }
            }
    
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapInfo tile = nearbyMapInfos[i];
                MapLocation loc = tile.getMapLocation();
                if (rc.canAttack(loc) && tile.getPaint().isEnemy()) {
                    rc.attack(loc);
                    break;
                }
            }
            
            if (nextTarget == null) {
                for (int i = nearbyMapInfos.length; --i >= 0;) {
                    MapInfo tile = nearbyMapInfos[i];
                    MapLocation loc = tile.getMapLocation();
                    if (tile.getPaint().isEnemy()) {
                        nextTarget = loc;
                        break;
                    }
                }    
            }

            if (nextTarget == null) {
                yieldStrategy();
                return;
            }
            BugNav.moveToward(nextTarget);
        }

        @Override
        public String toString() {
            return "Aggro ";
        }
    
    }

    static class MopRuinStrategy extends MopperStrategy {

        public static MapLocation ruinLoc;
        public static int turnsWithNothingToMop;

        MopRuinStrategy(MapLocation _ruinloc) {
            ruinLoc = _ruinloc;
            turnsWithNothingToMop = 0;
        }

        // Assumes robot is spinning around ruin and returns true if there is something adjacent to mop
        public boolean mopNearby() throws GameActionException {
            boolean ret = false;

            MapInfo[] actionableMapInfos = rc.senseNearbyMapInfos(rc.getLocation(), actionRadiusSquared);
            for(int i = actionableMapInfos.length; --i >= 0;) {
                MapInfo tile = actionableMapInfos[i];
                MapLocation loc = tile.getMapLocation();
                if(withinPattern(ruinLoc, loc) && tile.getPaint().isEnemy()) {
                    ret = true;
                    if(rc.canAttack(loc)) {
                        rc.attack(loc);
                        return ret;
                    }
                }
            }

            return ret;
        }

        @Override
        public void act() throws GameActionException {
            if (ruinLoc == null || !rc.canSenseLocation(ruinLoc)) {
                yieldStrategy();
                return;
            }

            // if tower is already built, we ignore
            if (rc.getLocation().isWithinDistanceSquared(ruinLoc, visionRadiusSquared)) {
                RobotInfo robotInfo = rc.senseRobotAtLocation(ruinLoc);
                if (robotInfo != null) {
                    yieldStrategy();
                    return ;
                }
            }
            

            if(!locBeforeTurn.isAdjacentTo(ruinLoc)) {
                BugNav.moveToward(ruinLoc);
                mopNearby();
            }
            else {
                // walk around tower every turn
                Direction dir = locBeforeTurn.directionTo(ruinLoc).rotateLeft();
                if(rc.canMove(dir)) {
                    rc.move(dir);
                }

                if(!mopNearby()) {
                    turnsWithNothingToMop++;
                }
            }

            if(turnsWithNothingToMop >= 4) {
                yieldStrategy();
                return;
            }
        }

        @Override
        public String toString() {
            return "MopRuin " + ruinLoc + " " + turnsWithNothingToMop;
        }
     }
    
    /**
     * Strategy to pick a random location and wander over for X turns, and if ruin is found
     * switch to build tower strategy.
     */
    static class ExploreStrategy extends MopperStrategy {
    
        public static int turnsLeft;
        public static MapLocation target;
        public static int turnsNotMoved;
        public static int switchStrategyCooldown;
    
        ExploreStrategy(int turns, int _switchStrategyCooldown) {
            turnsLeft = turns;
            switchStrategyCooldown = _switchStrategyCooldown;
            turnsNotMoved = 0;
            target = new MapLocation(rng.nextInt(mapWidth), rng.nextInt(mapHeight));
        }
    
        @Override
        public void act() throws GameActionException {
            if (informedEmptyPaintLoc != null) {
                switchStrategy(new CampFrontier());
                return ;
            }

            trySweep();
            
            for (int i = nearbyEnemyRobots.length; --i >= 0;) {
                RobotInfo robot = nearbyEnemyRobots[i];
                if (rc.canAttack(robot.location)) {
                    rc.attack(robot.location);
                    break;
                }
            }

            tryAttackEnemyRobot();

            if (switchStrategyCooldown <= 0) {
                tryMopRuinStrategy();
            }
            else {
                switchStrategyCooldown--;
            }

            BugNav.moveToward(target);

            if (rc.getLocation() == locBeforeTurn) {
                turnsNotMoved++;
                if (turnsNotMoved >= 3) {
                    yieldStrategy();
                    return;
                }
            }

            else turnsNotMoved = 0;
            turnsLeft--;
            if (turnsLeft <= 0) { // if turn counter is up, also yield
                yieldStrategy();
                return;
            }

        }
    
        @Override
        public String toString() {
            return "Explore " + turnsLeft + " " + target;
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
            if (dir == null) {
                // We have no valid moves!
                return;
            }

            if (!rc.canSenseRobotAtLocation(paintTowerLoc)) {
                // We're still very far, just move closer
                rc.move(dir);
                tryPaintBelowSelf(getSrpPaintColor(rc.getLocation()));
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
                tryPaintBelowSelf(getSrpPaintColor(rc.getLocation()));
                return;
            }

            final var spaceToFill = refillTo - rc.getPaint();
            if (paintTowerInfo.getPaintAmount() >= spaceToFill) {
                rc.move(dir);
                tryPaintBelowSelf(getSrpPaintColor(rc.getLocation()));
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

            // tryAttackInternalEnemyTile();

            // tryMoveSweepCrowd();

            tryAttackEnemyRobot();

            tryMopTile();

            if (rc.getPaint() >= 50) {
                tryTransferPaintSoldier(rc.getPaint()-30);
            }
            
            trySweep();
        }

        @Override
        public String toString() {
            return "CampFrontier ";
        }

    }

    static class OptimalPathingStrategy extends MopperStrategy {
        public void act() throws GameActionException {
            precomputeMovementInfo();

            // 1. attacking enemy on enemy tile 
            tryMoveAttackEnemyTileWithRobot();

            // 2. sweep more than 2 ppl
            tryMoveSweepCrowd();

            // 3. attack enemy not on tile
            tryMoveAttackEnemyRobotWithoutTile();

            // 4. Attacking enemy tile
            tryMoveAttackEnemyTile();

            tryMoveToFrontier();

            tryMoveToSafeTile();

            tryMoveLessSafeTile();
        }

        @Override
        public String toString() {
            return "Optimal Pathing";
        }
    }
}
