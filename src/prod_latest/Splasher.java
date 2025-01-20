package prod_latest;

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
            target = new MapLocation(rng.nextInt(mapWidth), rng.nextInt(mapHeight));
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

            if (useNetwork) {
                for (int i = nearbyAllyRobots.length; --i >= 0;) {
                    RobotInfo robot = nearbyAllyRobots[i];
                    if (robot.getType().isTowerType()) {
                        switchStrategy(new PathTowardsEnemyStrategy(robot.getLocation()));
                    }
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
    
    static class PathTowardsEnemyStrategy extends SplasherStrategy {

        MapLocation curTarget;
        int failedMessages = 0;

        PathTowardsEnemyStrategy(MapLocation linkedTower) {
            curTarget = linkedTower;
        }

        @Override
        public void act() throws GameActionException {
            // if (failedMessages >= 10) {
            //     useNetwork = false;
            //     yieldStrategy();
            // }

            for (int i = lastRoundMessages.length; --i >= 0;) {
                int message = lastRoundMessages[i].getBytes();
                if (Comms.getProtocol(message) == Comms.Protocal.TOWER_NETWORK_RESPONSE) {
                    int[] decoded = Comms.towerNetworkResponseComms.decode(message);
                    boolean successful = decoded[1] == 1;
                    MapLocation newTarget = Comms.decodeMapLocation(decoded[2]);
                    if (successful) {
                        curTarget = newTarget;
                        failedMessages = 0;
                    }
                    else failedMessages++;
                }
            }

            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapInfo tile = nearbyMapInfos[i];
                if (tile.getPaint().isEnemy()) {
                    switchStrategy(new AggroStrategy());
                    return;
                }
            }

            BugNav.moveToward(curTarget);

            int messageContent = Comms.towerNetworkRequestComms.encode(new int[]{
                Comms.Protocal.TOWER_NETWORK_REQUEST.ordinal(),
                1, // move forward
                1, // enemy network
                id
            });
            if (rc.canSenseLocation(curTarget) && rc.senseRobotAtLocation(curTarget) != null && rc.canSendMessage(curTarget)) {
                rc.sendMessage(curTarget, messageContent);
            }
            rc.setIndicatorLine(rc.getLocation(), curTarget, 0, 255, 0);
        }

        public String toString() {
            return "path towards enemy " + curTarget; 
        }

    }
}
