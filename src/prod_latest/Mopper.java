package prod_latest;

import battlecode.common.*;

public class Mopper extends Unit {

    public static MopperStrategy strategy;

    public static void switchStrategy(MopperStrategy newStrategy) {
        strategy = newStrategy;
    }

    public static void yieldStrategy() {
        strategy = new ExploreStrategy(8, 6);
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

        int cleanRuinCoolDown;
        AggroStrategy(int _cleanRuinCoolDown) {
            cleanRuinCoolDown = _cleanRuinCoolDown;
        }
    
        @Override
        public void act() throws GameActionException {
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
        public boolean mop_nearby() throws GameActionException {
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
                mop_nearby();
            }
            else {
                // walk around tower every turn
                Direction dir = locBeforeTurn.directionTo(ruinLoc).rotateLeft();
                if(rc.canMove(dir)) {
                    rc.move(dir);
                }

                if(!mop_nearby()) {
                    turnsWithNothingToMop++;
                }
            }

            if(turnsWithNothingToMop >= 4) {
                yieldStrategy();
                return;
            }
        }

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
            for (int i = nearbyEnemyRobots.length; --i >= 0;) {
                RobotInfo robot = nearbyEnemyRobots[i];
                if (rc.canAttack(robot.location)) {
                    rc.attack(robot.location);
                    break;
                }
            }


            if (switchStrategyCooldown <= 0) {
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
            else {
                switchStrategyCooldown--;
            }
            
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapInfo tile = nearbyMapInfos[i];
                MapLocation loc = tile.getMapLocation();
                if (tile.getPaint().isEnemy()) {
                    switchStrategy(new AggroStrategy(20));
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

    static class FollowStrategy extends MopperStrategy {
        FollowStrategy() {
        }

        public void act() throws GameActionException {

        }

        public String toString () {
            return "Following enemy";
        }
    }
}
