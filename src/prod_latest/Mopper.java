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

    public static boolean tryMoveToSafeTile() throws GameActionException {
        Direction[] dirs = adjacentDirections.clone();
        shuffleArray(dirs);
        Direction bestDir = null;
        int minPenalty = 1000;
        for (int i = dirs.length; --i >= 0;) {
            Direction dir = dirs[i];
            MapLocation nxtLoc = rc.getLocation().add(dir);
            if (!withinBounds(nxtLoc)) continue;
            MapInfo tile = rc.senseMapInfo(nxtLoc);
            if (tile.getPaint().isAlly() && rc.canMove(dir) && !inEnemyTowerRange(nxtLoc)) {
                int penalty = numAllyAdjacent[dir.getDirectionOrderNum()];
                if (penalty < minPenalty) {
                    minPenalty = penalty;
                    bestDir = dir;
                }
            }
        }
        if (bestDir != null) {
            rc.move(bestDir);
            return true;
        }
        return false;
    }

    public static boolean tryMoveLessSafeTile() throws GameActionException {
        Direction[] dirs = adjacentDirections.clone();
        shuffleArray(dirs);
        Direction bestDir = null;
        int minPenalty = 100;
        for (int i = dirs.length; --i >= 0;) {
            Direction dir = dirs[i];
            MapLocation nxtLoc = rc.getLocation().add(dir);
            if (!withinBounds(nxtLoc)) continue;
            MapInfo tile = rc.senseMapInfo(nxtLoc);
            if (!tile.getPaint().isEnemy() && rc.canMove(dir) && !inEnemyTowerRange(nxtLoc)) {
                int penalty = numAllyAdjacent[dir.getDirectionOrderNum()];
                if (penalty < minPenalty) {
                    minPenalty = penalty;
                    bestDir = dir;
                }
            }
        }
        if (bestDir != null) {
            rc.move(bestDir);
            return true;
        }
        return false;
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

    public static boolean inEnemyTowerRange(MapLocation loc) throws GameActionException {
        for (int i = nearbyEnemyRobots.length; --i >= 0;) {
            RobotInfo robotInfo = nearbyEnemyRobots[i];
            MapLocation enemyLoc = nearbyEnemyRobots[i].getLocation();
            if (robotInfo.getType().isTowerType() && loc.isWithinDistanceSquared(enemyLoc, robotInfo.getType().actionRadiusSquared)) {
                return true;
            }
        }
        return false;
    }

    public static boolean tryMoveToFrontier() throws GameActionException {
        // Optional optimization, remove if degrades performance or uses excess bytecode
        for (int i = adjacentDirections.length; --i >= 0;) {
            Direction dir = adjacentDirections[i];
            MapLocation nxtLoc = rc.getLocation().add(dir);
            if (!withinBounds(nxtLoc)) continue;
            MapInfo tile = rc.senseMapInfo(nxtLoc);
            if (tile.getPaint().isEnemy()) {
                informedEmptyPaintLoc = nxtLoc;
                break;
            }
        }
        if (informedEnemyPaintLoc != null) {
            Direction dir = BugNav.getDirectionToMove(informedEnemyPaintLoc);
            if (dir != null && rc.canMove(dir)) {
                MapLocation nxtLoc = rc.getLocation().add(dir);
                MapInfo tile = rc.senseMapInfo(nxtLoc);
                if (!tile.getPaint().isEnemy() && !inEnemyTowerRange(nxtLoc)) {
                    rc.move(dir);
                    return true;
                }
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
