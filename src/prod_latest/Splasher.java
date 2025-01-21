package prod_latest;

import battlecode.common.*;

public class Splasher extends Unit {

    public static SplasherStrategy strategy;
    public static boolean useNetwork = rng.nextInt(2) == 0;
    

    public static void switchStrategy(SplasherStrategy newStrategy) {
        strategy = newStrategy;
    }

    public static void yieldStrategy(boolean acted) throws GameActionException{
        strategy = new ExploreStrategy(15);
        if (!acted) strategy.act();
    }

    @Override
    void play() throws GameActionException {
        if (strategy == null) {
            yieldStrategy(false);
        }
        strategy.act();
        Logger.log(strategy.toString());

        // also wanna upgrade towers nearby if possible
        upgradeTowers();
    }

    static abstract class SplasherStrategy {
        abstract public void act() throws GameActionException;
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
            else if (bestAttackPoints == 0) {
                // no suitable target; yield
                yieldStrategy(false);
                return;
            }
            else {
                tryMoveToFrontier();

                tryMoveToSafeTile();
    
                tryMoveLessSafeTile();
            }
        }

        public String toString() {
            return "Aggro";
        }
    }

    static class ExploreStrategy extends SplasherStrategy {
    
        public static int turnsLeft;
        public static MapLocation target;
        public static int turnsNotMoved;
    
        ExploreStrategy(int turns) {
            turnsLeft = turns;
            turnsNotMoved = 0;
            if (informedEnemyPaintLoc != null && informedEnemyPaintLocTimestamp > roundNum - 30) {
                target = informedEnemyPaintLoc;
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
                    switchStrategy(new AggroStrategy());
                    return;
                }
            }

            // BugNav.moveToward(target);

            tryMoveToFrontier();

            tryMoveToSafeTile();

            tryMoveLessSafeTile();

            if (rc.getLocation() == locBeforeTurn) {
                turnsNotMoved++;
                if (turnsNotMoved >= 3) {
                    yieldStrategy(true);
                    return;
                }
            }
            else turnsNotMoved = 0;
            turnsLeft--;
            if (turnsLeft <= 0) { // if turn counter is up, also yield
                yieldStrategy(true);
                return;
            }
        }
    
        public String toString() {
            return "Explore " + turnsLeft + " " + target;
        }
    }
    
}
