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
            if (ally.getType() != UnitType.SOLDIER) continue;
            if (rc.canTransferPaint(ally.getLocation(), amount) && ally.getPaintAmount() + amount <= ally.getType().paintCapacity) {
                rc.transferPaint(ally.getLocation(), amount);
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
        // if (rc.getPaint() < 30 && paintTowerLoc != null) {
        //     Logger.log("refilling paint");
        //     Logger.flush();
        //     strategy = new RefillPaintStrategy(60);
        // }
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
            if(dir_counts[i] + dir_counts[i + 1] + dir_counts[(i + 2) % 8] >= 1) {
                dir = ord_dirs[i / 2];
                break;
            }
        }

        if(dir != null) {
            if(rc.canMopSwing(dir)) {
                rc.mopSwing(dir);
                rc.setTimelineMarker("SWEEP", 0, 255, 0);
                return true;
            }
        }

        return false;
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
            tryMoveToFrontier();

            tryMoveToSafeTile();

            tryMoveLessSafeTile();

            // trySweep();

            tryAttackEnemyRobot();

            tryMopTile();

            if (rc.getPaint() >= 50) {
                tryTransferPaintSoldier(rc.getPaint()-30);
            }
            
            // trySweep();
        }

        @Override
        public String toString() {
            return "CampFrontier ";
        }

    }
}
