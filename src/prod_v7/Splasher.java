package prod_v7;

import battlecode.common.*;

public class Splasher extends Unit {

    public static SplasherStrategy strategy;
    public static boolean useNetwork = rng.nextInt(2) == 0;
    

    public static void switchStrategy(SplasherStrategy newStrategy) {
        strategy = newStrategy;
    }

    public static void yieldStrategy() {
        strategy = new ExploreStrategy(8);
    }

    @Override
    void play() throws GameActionException {
        if (strategy == null) {
            yieldStrategy();
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
    
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapInfo tile = nearbyMapInfos[i];
                MapLocation loc = tile.getMapLocation();
                if (rc.canAttack(loc) && tile.getPaint().isEnemy()) {
                    rc.attack(loc);
                    break;
                }
            }
            
            MapLocation nextTarget = null;
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapInfo tile = nearbyMapInfos[i];
                MapLocation loc = tile.getMapLocation();
                if (tile.getPaint().isEnemy()) {
                    nextTarget = loc;
                    break;
                }
            }
    
            if (nextTarget == null) {
                yieldStrategy();
                return;
            }
            if (!nextTarget.isAdjacentTo(locBeforeTurn)) {
                BugNav.moveToward(nextTarget);
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
            if (informedEnemyPaintLoc != null && informedEnemyPaintLocTimestamp > roundNum - 100) {
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
    
        public String toString() {
            return "Explore " + turnsLeft + " " + target;
        }
    }
    
}