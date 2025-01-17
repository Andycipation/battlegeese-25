package prod_latest;

import battlecode.common.*;

public class Mopper extends Unit {

    public static MopperStrategy strategy;

    public static void switchStrategy(MopperStrategy newStrategy) {
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
    }
    
    abstract static class MopperStrategy extends Mopper {
        abstract public void act() throws GameActionException;
    }
    
    static class AggroStrategy extends MopperStrategy {
    
        AggroStrategy() {}
    
        @Override
        public void act() throws GameActionException {
            for (int i = nearbyEnemyRobots.length; --i >= 0;) {
                RobotInfo robot = nearbyEnemyRobots[i];
                MapLocation loc = robot.getLocation();
                if (rc.canAttack(loc)) {
                    rc.attack(loc);
                    break;
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
            BugNav.moveToward(nextTarget);
        }

        public String toStrign() {
            return "Aggro";
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
    
        ExploreStrategy(int turns) {
            turnsLeft = turns;
            turnsNotMoved = 0;
            target = new MapLocation(rng.nextInt(mapWidth), rng.nextInt(mapHeight));
        }
    
        @Override
        public void act() throws GameActionException {
            for (int i = nearbyEnemyRobots.length; --i >= 0;) {
                RobotInfo robot = nearbyEnemyRobots[i];
                if (rc.canAttack(robot.location)) {
                    rc.attack(robot.location);
                    break;
                }
            }
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapInfo tile = nearbyMapInfos[i];
                if (tile.getPaint().isEnemy()) {
                    switchStrategy(new AggroStrategy());
                    return;
                }
            }
    
            BugNav.moveToward(target);
            if (rc.getLocation() == curLoc) {
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
