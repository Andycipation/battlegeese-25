package prod_latest;

import battlecode.common.*;

public class Splasher extends Unit {

    public static SplasherStrategy strategy;
    public static boolean useNetwork = rng.nextInt(2) == 0;
    

    public static void switchStrategy(SplasherStrategy newStrategy, boolean acted) throws GameActionException{
        strategy = newStrategy;
        if (!acted) strategy.act();
    }

    public static void yieldStrategy() throws GameActionException{
        strategy = new ExploreStrategy();
    }

    @Override
    void play() throws GameActionException {
        if (strategy == null) {
            strategy = new ExploreStrategy();
        }
        strategy.act();
        Logger.log(strategy.toString());
        if (rc.getPaint() < 50 && paintTowerLoc != null) {
            Logger.log("refilling paint");
            Logger.flush();
            strategy = new RefillPaintStrategy(300);
        }
        // also wanna upgrade towers nearby if possible
        upgradeTowers();
    }

    static abstract class SplasherStrategy {
        abstract public void act() throws GameActionException;
    }

    static class ExploreStrategy extends SplasherStrategy {
    
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
                    switchStrategy(new AggroStrategy(), false);
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



    static class AggroStrategy extends SplasherStrategy {

        AggroStrategy() {}

        @Override
        public void act() throws GameActionException {


            int[][] points = new int[5][5];
            int emptyWeight = 1, enemyWeight = 3;
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapInfo tile = nearbyMapInfos[i];
                MapLocation loc = tile.getMapLocation();
                MapLocation diff = loc.translate(-locBeforeTurn.x, -locBeforeTurn.y);
                if (tile.getPaint().isEnemy()) {
                    switch (diff.x * 100 + diff.y) {
                        case -301: points[0][2] += enemyWeight; break; // (-3, -1)
                        case -300: points[0][2] += enemyWeight; break; // (-3, 0)
                        case -299: points[0][2] += enemyWeight; break; // (-3, 1)
                        case -202: points[1][1] += enemyWeight; break; // (-2, -2)
                        case -201: points[0][2] += enemyWeight; points[1][1] += enemyWeight; points[1][2] += enemyWeight; break; // (-2, -1)
                        case -200: points[0][2] += enemyWeight; points[1][1] += enemyWeight; points[1][2] += enemyWeight; points[1][3] += enemyWeight; break; // (-2, 0)
                        case -199: points[0][2] += enemyWeight; points[1][2] += enemyWeight; points[1][3] += enemyWeight; break; // (-2, 1)
                        case -198: points[1][3] += enemyWeight; break; // (-2, 2)
                        case -103: points[2][0] += enemyWeight; break; // (-1, -3)
                        case -102: points[1][1] += enemyWeight; points[2][0] += enemyWeight; points[2][1] += enemyWeight; break; // (-1, -2)
                        case -101: points[0][2] += enemyWeight; points[1][1] += enemyWeight; points[1][2] += enemyWeight; points[2][0] += enemyWeight; points[2][1] += enemyWeight; points[2][2] += enemyWeight; break; // (-1, -1)
                        case -100: points[0][2] += enemyWeight; points[1][1] += enemyWeight; points[1][2] += enemyWeight; points[1][3] += enemyWeight; points[2][1] += enemyWeight; points[2][2] += enemyWeight; points[2][3] += enemyWeight; break; // (-1, 0)
                        case -99: points[0][2] += enemyWeight; points[1][2] += enemyWeight; points[1][3] += enemyWeight; points[2][2] += enemyWeight; points[2][3] += enemyWeight; break; // (-1, 1)
                        case -98: points[1][3] += enemyWeight; points[2][3] += enemyWeight; break; // (-1, 2)
                        case -3: points[2][0] += enemyWeight; break; // (0, -3)
                        case -2: points[1][1] += enemyWeight; points[2][0] += enemyWeight; points[2][1] += enemyWeight; points[3][1] += enemyWeight; break; // (0, -2)
                        case -1: points[1][1] += enemyWeight; points[1][2] += enemyWeight; points[2][0] += enemyWeight; points[2][1] += enemyWeight; points[2][2] += enemyWeight; points[3][1] += enemyWeight; points[3][2] += enemyWeight; break; // (0, -1)
                        case 0: points[1][1] += enemyWeight; points[1][2] += enemyWeight; points[1][3] += enemyWeight; points[2][1] += enemyWeight; points[2][2] += enemyWeight; points[2][3] += enemyWeight; points[3][1] += enemyWeight; points[3][2] += enemyWeight; points[3][3] += enemyWeight; break; // (0, 0)
                        case 1: points[1][2] += enemyWeight; points[1][3] += enemyWeight; points[2][2] += enemyWeight; points[2][3] += enemyWeight; points[3][2] += enemyWeight; points[3][3] += enemyWeight; break; // (0, 1)
                        case 2: points[1][3] += enemyWeight; points[2][3] += enemyWeight; points[3][3] += enemyWeight; break; // (0, 2)
                        case 97: points[2][0] += enemyWeight; break; // (1, -3)
                        case 98: points[2][0] += enemyWeight; points[2][1] += enemyWeight; points[3][1] += enemyWeight; break; // (1, -2)
                        case 99: points[2][0] += enemyWeight; points[2][1] += enemyWeight; points[2][2] += enemyWeight; points[3][1] += enemyWeight; points[3][2] += enemyWeight; break; // (1, -1)
                        case 100: points[2][1] += enemyWeight; points[2][2] += enemyWeight; points[2][3] += enemyWeight; points[3][1] += enemyWeight; points[3][2] += enemyWeight; points[3][3] += enemyWeight; break; // (1, 0)
                        case 101: points[2][2] += enemyWeight; points[2][3] += enemyWeight; points[3][2] += enemyWeight; points[3][3] += enemyWeight; break; // (1, 1)
                        case 102: points[2][3] += enemyWeight; points[3][3] += enemyWeight; break; // (1, 2)
                        case 198: points[3][1] += enemyWeight; break; // (2, -2)
                        case 199: points[3][1] += enemyWeight; points[3][2] += enemyWeight; break; // (2, -1)
                        case 200: points[3][1] += enemyWeight; points[3][2] += enemyWeight; points[3][3] += enemyWeight; break; // (2, 0)
                        case 201: points[3][2] += enemyWeight; points[3][3] += enemyWeight; break; // (2, 1)
                        case 202: points[3][3] += enemyWeight; break; // (2, 2)
                    }
                }
                else if (tile.getPaint() == PaintType.EMPTY) {
                    switch (diff.x * 100 + diff.y) {
                        case -301: points[1][1] += emptyWeight; break; // (-3, -1)
                        case -300: points[1][2] += emptyWeight; break; // (-3, 0)
                        case -299: points[1][3] += emptyWeight; break; // (-3, 1)
                        case -202: points[1][1] += emptyWeight; break; // (-2, -2)
                        case -201: points[1][1] += emptyWeight; points[1][2] += emptyWeight; points[2][1] += emptyWeight; break; // (-2, -1)
                        case -200: points[1][1] += emptyWeight; points[1][2] += emptyWeight; points[1][3] += emptyWeight; points[2][2] += emptyWeight; break; // (-2, 0)
                        case -199: points[1][2] += emptyWeight; points[1][3] += emptyWeight; points[2][3] += emptyWeight; break; // (-2, 1)
                        case -198: points[1][3] += emptyWeight; break; // (-2, 2)
                        case -103: points[1][1] += emptyWeight; break; // (-1, -3)
                        case -102: points[1][1] += emptyWeight; points[1][2] += emptyWeight; points[2][1] += emptyWeight; break; // (-1, -2)
                        case -101: points[1][1] += emptyWeight; points[1][2] += emptyWeight; points[1][3] += emptyWeight; points[2][1] += emptyWeight; points[2][2] += emptyWeight; points[3][1] += emptyWeight; break; // (-1, -1)
                        case -100: points[1][1] += emptyWeight; points[1][2] += emptyWeight; points[1][3] += emptyWeight; points[2][1] += emptyWeight; points[2][2] += emptyWeight; points[2][3] += emptyWeight; points[3][2] += emptyWeight; break; // (-1, 0)
                        case -99: points[1][1] += emptyWeight; points[1][2] += emptyWeight; points[1][3] += emptyWeight; points[2][2] += emptyWeight; points[2][3] += emptyWeight; points[3][3] += emptyWeight; break; // (-1, 1)
                        case -98: points[1][2] += emptyWeight; points[1][3] += emptyWeight; points[2][3] += emptyWeight; break; // (-1, 2)
                        case -97: points[1][3] += emptyWeight; break; // (-1, 3)
                        case -3: points[2][1] += emptyWeight; break; // (0, -3)
                        case -2: points[1][1] += emptyWeight; points[2][1] += emptyWeight; points[2][2] += emptyWeight; points[3][1] += emptyWeight; break; // (0, -2)
                        case -1: points[1][1] += emptyWeight; points[1][2] += emptyWeight; points[2][1] += emptyWeight; points[2][2] += emptyWeight; points[2][3] += emptyWeight; points[3][1] += emptyWeight; points[3][2] += emptyWeight; break; // (0, -1)
                        case 0: points[1][1] += emptyWeight; points[1][2] += emptyWeight; points[1][3] += emptyWeight; points[2][1] += emptyWeight; points[2][2] += emptyWeight; points[2][3] += emptyWeight; points[3][1] += emptyWeight; points[3][2] += emptyWeight; points[3][3] += emptyWeight; break; // (0, 0)
                        case 1: points[1][2] += emptyWeight; points[1][3] += emptyWeight; points[2][1] += emptyWeight; points[2][2] += emptyWeight; points[2][3] += emptyWeight; points[3][2] += emptyWeight; points[3][3] += emptyWeight; break; // (0, 1)
                        case 2: points[1][3] += emptyWeight; points[2][2] += emptyWeight; points[2][3] += emptyWeight; points[3][3] += emptyWeight; break; // (0, 2)
                        case 3: points[2][3] += emptyWeight; break; // (0, 3)
                        case 97: points[3][1] += emptyWeight; break; // (1, -3)
                        case 98: points[2][1] += emptyWeight; points[3][1] += emptyWeight; points[3][2] += emptyWeight; break; // (1, -2)
                        case 99: points[1][1] += emptyWeight; points[2][1] += emptyWeight; points[2][2] += emptyWeight; points[3][1] += emptyWeight; points[3][2] += emptyWeight; points[3][3] += emptyWeight; break; // (1, -1)
                        case 100: points[1][2] += emptyWeight; points[2][1] += emptyWeight; points[2][2] += emptyWeight; points[2][3] += emptyWeight; points[3][1] += emptyWeight; points[3][2] += emptyWeight; points[3][3] += emptyWeight; break; // (1, 0)
                        case 101: points[1][3] += emptyWeight; points[2][2] += emptyWeight; points[2][3] += emptyWeight; points[3][1] += emptyWeight; points[3][2] += emptyWeight; points[3][3] += emptyWeight; break; // (1, 1)
                        case 102: points[2][3] += emptyWeight; points[3][2] += emptyWeight; points[3][3] += emptyWeight; break; // (1, 2)
                        case 103: points[3][3] += emptyWeight; break; // (1, 3)
                        case 198: points[3][1] += emptyWeight; break; // (2, -2)
                        case 199: points[2][1] += emptyWeight; points[3][1] += emptyWeight; points[3][2] += emptyWeight; break; // (2, -1)
                        case 200: points[2][2] += emptyWeight; points[3][1] += emptyWeight; points[3][2] += emptyWeight; points[3][3] += emptyWeight; break; // (2, 0)
                        case 201: points[2][3] += emptyWeight; points[3][2] += emptyWeight; points[3][3] += emptyWeight; break; // (2, 1)
                        case 202: points[3][3] += emptyWeight; break; // (2, 2)
                        case 299: points[3][1] += emptyWeight; break; // (3, -1)
                        case 300: points[3][2] += emptyWeight; break; // (3, 0)
                        case 301: points[3][3] += emptyWeight; break; // (3, 1)
                    }
                }
            }

            MapLocation bestAttackLoc = null;
            int bestAttackPoints = 0;
    
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapInfo tile = nearbyMapInfos[i];
                MapLocation loc = tile.getMapLocation();
                if (loc.distanceSquaredTo(locBeforeTurn) > actionRadiusSquared)
                    continue;
                MapLocation diff = loc.translate(-locBeforeTurn.x, -locBeforeTurn.y);
                int x = diff.x + 2;
                int y = diff.y + 2;
                if (tile.isPassable() && points[x][y] > bestAttackPoints) {
                    bestAttackPoints = points[x][y];
                    bestAttackLoc = loc;
                }
            }

            Logger.log("attack points: " + bestAttackPoints + " " + bestAttackLoc);
            if (bestAttackPoints >= 9 && rc.canAttack(bestAttackLoc)) {
                rc.attack(bestAttackLoc);
            }
            
            tryMoveToFrontier();

            tryMoveToSafeTile();

            tryMoveLessSafeTile();
        }

        public String toString() {
            return "Aggro";
        }
    }
    
    static class RefillPaintStrategy extends SplasherStrategy {
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
}
