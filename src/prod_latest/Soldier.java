package prod_latest;

import battlecode.common.*;

public class Soldier extends Unit {
    /**
     * Our strategy is to pick a random location and wander over for 8 turns, and if
     * ruin is found switch to build tower strategy.
     */

    static SoldierStrategy strategy;

    public static MapLocation prevLoc = null;

    static void switchStrategy(SoldierStrategy newStrategy, boolean acted) throws GameActionException {
        strategy = newStrategy;
        if (!acted) {
            strategy.act();
        }
    }

    public static void yieldStrategy(boolean acted) throws GameActionException {
        if (rc.getPaint() < 50 && paintTowerLoc != null) {
            Logger.log("refilling paint");
            Logger.flush();
            strategy = new RefillPaintStrategy(120);
        } else {
            var target = new MapLocation(rng.nextInt(mapWidth), rng.nextInt(mapHeight));
            strategy = new ExploreStrategy(target, 8);
        }
        if (!acted) {
            strategy.act();
        }
    }

    @Override
    void play() throws GameActionException {
        if (strategy == null) {
            yieldStrategy(false);
            return;
        }
        var paint = rc.getPaint();
        if (paint < 60) {
            if (paintTowerLoc != null) {
                Logger.log("refilling paint");
                Logger.flush();
                strategy = new RefillPaintStrategy(120);
            } else if (paint < 30) {
                yieldStrategy(false);
            }
        }
        strategy.act();
        Logger.log(strategy.toString());

        // also wanna upgrade towers nearby if possible
        upgradeTowers();
    }

    static abstract class SoldierStrategy extends Soldier {

        abstract public void act() throws GameActionException;
    }

    enum MarkType {
        // BUILD_SRP,
        BUILD_PAINT_TOWER,
        BUILD_MONEY_TOWER,
    }

    private static int markToInt(PaintType mark) {
        return switch (mark) {
            case ALLY_PRIMARY -> 1;
            case ALLY_SECONDARY -> 2;
            default -> 0;
        };
    }

    private static boolean canMarkRuin(MapLocation center) {
        var loc1 = center.add(Direction.NORTH);
        var loc2 = center.add(Direction.NORTHEAST);
        return rc.canMark(loc1) && rc.canMark(loc2);
    }

    private static boolean canSenseMarks(MapLocation center) throws GameActionException {
        var loc1 = center.add(Direction.NORTH);
        var loc2 = center.add(Direction.NORTHEAST);
        return rc.canSenseLocation(loc1) && rc.canSenseLocation(loc2);
    }

    private static MarkType getMarkType(MapLocation center) throws GameActionException {
        var loc1 = center.add(Direction.NORTH);
        var loc2 = center.add(Direction.NORTHEAST);
        int mark1 = markToInt(rc.senseMapInfo(loc1).getMark());
        int mark2 = markToInt(rc.senseMapInfo(loc2).getMark());
        if (mark1 == 1) return MarkType.BUILD_MONEY_TOWER;
        if (mark1 == 2 && mark2 == 1) return MarkType.BUILD_PAINT_TOWER;
        // if (mark1 == 2 && mark2 == 2) return MarkType.BUILD_SRP;
        return null;
    }

    private static boolean markMap(MapLocation center, MarkType markType) throws GameActionException {
        var loc1 = center.add(Direction.NORTH);
        var loc2 = center.add(Direction.NORTHEAST);
        if (rc.getPaint() >= 2 && rc.canMark(loc1) && rc.canMark(loc2)) {
            switch (markType) {
                case BUILD_MONEY_TOWER -> {
                    rc.mark(loc1, false);
                }
                case BUILD_PAINT_TOWER -> {
                    rc.mark(loc1, true);
                    rc.mark(loc2, false);
                }
                // case BUILD_SRP -> {
                //     rc.mark(loc1, true);
                //     rc.mark(loc2, true);
                // }
            }
            return true;
        }
        return false;
    }

    /**
     * Strategy to build a tower at specified ruin location
     */
    static class BuildTowerStrategy extends SoldierStrategy {

        public static MapLocation ruinLoc;
        public static UnitType towerType;

        BuildTowerStrategy(MapLocation _ruinLoc, UnitType _towerType) {
            // Precondition: we can sense the ruin location
            ruinLoc = _ruinLoc;
            assert(ruinLoc != null);
            towerType = _towerType;
        }

        private boolean isRuinMostlyDone() throws GameActionException {
            int visible = 0;
            int done = 0;
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                var info = nearbyMapInfos[i];
                if (ruinLoc.distanceSquaredTo(info.getMapLocation()) <= GameConstants.RESOURCE_PATTERN_RADIUS_SQUARED) {
                    visible += 1;
                    var expectedColor = getTowerPaintColor(ruinLoc, info.getMapLocation(), towerType);
                    if (expectedColor == info.getPaint()) {
                        done += 1;
                    }
                }
            }
            // TODO: optimize this return statement to use ints
            return 1.0 * visible / done > 0.8;
        }

        @Override
        public void act() throws GameActionException {
            if (!rc.canSenseLocation(ruinLoc)
                    || rc.senseRobotAtLocation(ruinLoc) != null
                    || rc.getNumberTowers() == 25) {
                // The tower is completed, or we have the maximum number of towers
                yieldStrategy(false);
                return;
            }

            // If we can't sense the marks, move closer without painting underneath us.
            if (!canSenseMarks(ruinLoc)) {
                BugNav.moveToward(ruinLoc);
                return;
            }

            // Get the tower type we are currently trying to build, or make the mark ourselves
            MarkType markType = getMarkType(ruinLoc);
            if (markType == null) {
                // We are the first to arrive; try to mark the ruin accordingly
                var toMarkType = switch (towerType) {
                    case LEVEL_ONE_MONEY_TOWER -> MarkType.BUILD_MONEY_TOWER;
                    case LEVEL_ONE_PAINT_TOWER -> MarkType.BUILD_PAINT_TOWER;
                    default -> {
                        throw new IllegalArgumentException();
                    }
                };
                if (!canMarkRuin(ruinLoc)) {
                    BugNav.moveToward(ruinLoc);
                    return;
                }
                if (!markMap(ruinLoc, toMarkType)) {
                    return;
                }
            } else {
                towerType = switch (markType) {
                    case BUILD_MONEY_TOWER -> UnitType.LEVEL_ONE_MONEY_TOWER;
                    case BUILD_PAINT_TOWER -> UnitType.LEVEL_ONE_PAINT_TOWER;
                };
            }

            Logger.log("has mark " + markType);

            // If we don't have nearly enough chips to finish this, leave
            if (rc.getChips() < 300) {
                yieldStrategy(false);
                return;
            }

            // If there is already a robot next to the ruin and it's mostly finished, we go explore somewhere else instead
            boolean mostlyDone = isRuinMostlyDone();
            if (mostlyDone) {
                for (var robot : nearbyAllyRobots) {
                    if (robot.type == UnitType.SOLDIER && robot.location.isAdjacentTo(ruinLoc)) {
                        yieldStrategy(false);
                        return;
                    }
                }
            }

            // If ruin is not adjacent, walk closer
            if (!locBeforeTurn.isAdjacentTo(ruinLoc)) {
                BugNav.moveToward(ruinLoc);
                tryPaintBelowSelf(getTowerPaintColor(ruinLoc, locBeforeTurn, towerType));
                return;
            }

            // // Check adjacent squares for robots; if there is one, leave if tiebreaker
            // for (int i = adjacentDirections.length; --i >= 0;) {
            //     MapLocation loc = ruinLoc.add(adjacentDirections[i]);
            //     if (loc == rc.getLocation()) {
            //         continue;
            //     }
            //     RobotInfo robot = rc.senseRobotAtLocation(loc);
            //     if (robot != null
            //             && robot.getTeam() == myTeam
            //             && (robot.getPaintAmount() > rc.getPaint() || robot.getPaintAmount() == rc.getPaint() && robot.getID() > rc.getID())) {
            //         yieldStrategy(false);
            //         return;
            //     }
            // }

            // Walk around tower every turn to not miss any squares
            Direction dir = locBeforeTurn.directionTo(ruinLoc).rotateLeft();
            if (rc.canMove(dir)) {
                rc.move(dir);
                tryPaintBelowSelf(getTowerPaintColor(ruinLoc, rc.getLocation(), towerType));
            }

            // Try paint tiles
            MapInfo[] actionableMapInfos = rc.senseNearbyMapInfos(rc.getLocation(), actionRadiusSquared);
            for (int i = actionableMapInfos.length; --i >= 0;) {
                MapInfo tile = actionableMapInfos[i];
                MapLocation loc = tile.getMapLocation();
                if (ruinLoc.distanceSquaredTo(loc) > GameConstants.RESOURCE_PATTERN_RADIUS_SQUARED) {
                    continue;
                }
                if (tryPaint(loc, getTowerPaintColor(ruinLoc, loc, towerType))) {
                    break;
                }
            }

            // Try complete tower
            if (rc.canCompleteTowerPattern(towerType, ruinLoc)) {
                rc.completeTowerPattern(towerType, ruinLoc);
                yieldStrategy(true);
                return;
            }

            // Try to paint any tiles outside radius while we're waiting
            if (rc.getPaint() > 30) {
                for (int i = actionableMapInfos.length; --i >= 0;) {
                    MapInfo tile = actionableMapInfos[i];
                    MapLocation loc = tile.getMapLocation();
                    if (ruinLoc.distanceSquaredTo(loc) > GameConstants.RESOURCE_PATTERN_RADIUS_SQUARED) {
                        if (tryPaint(loc, getSrpPaintColor(loc))) {
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public String toString() {
            return "BuildTower " + ruinLoc + " " + towerType;
        }
    }

    // Moves towards `target` for `turns` turns.
    static class ExploreStrategy extends SoldierStrategy {

        public static MapLocation target;
        public static int turnsLeft;

        public static int targetRuinCooldown;
        public static int turnsNotMoved;

        ExploreStrategy(MapLocation _target, int turns) {
            target = _target;
            turnsLeft = turns;
            targetRuinCooldown = 6;
            turnsNotMoved = 0;
        }

        UnitType getTowerToBuild() {
            if (rc.getNumberTowers() <= 3) {
                return UnitType.LEVEL_ONE_MONEY_TOWER;
            }
            int moneyTowerWeight = 4;
            int paintTowerWeight = numTowers;
            return switch (randChoice(moneyTowerWeight, paintTowerWeight)) {
                case 0 -> UnitType.LEVEL_ONE_MONEY_TOWER;
                default -> UnitType.LEVEL_ONE_PAINT_TOWER;
            };
        }

        @Override
        public void act() throws GameActionException {
            if (turnsLeft == 0) {
                yieldStrategy(false);
                return;
            }
            turnsLeft -= 1;
            targetRuinCooldown -= 1;

            // Check for nearby ruins
            if (targetRuinCooldown <= 0 && rc.getNumberTowers() < 25) {
                for (int i = nearbyMapInfos.length; --i >= 0;) {
                    MapInfo tile = nearbyMapInfos[i];
                    MapLocation loc = tile.getMapLocation();
                    if (rc.canSenseLocation(loc)) {
                        RobotInfo robotInfo = rc.senseRobotAtLocation(loc);
                        if (tile.hasRuin() && robotInfo == null) {
                            var towerType = getTowerToBuild();
                            switchStrategy(new BuildTowerStrategy(loc, towerType), false);
                            return;
                        }
                    }
                }
            }

            // Kiting!
            // TODO: combine this into a single for loop, need to loop in max.
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapInfo tile = nearbyMapInfos[i];
                MapLocation loc = tile.getMapLocation();
                if (rc.canSenseRobotAtLocation(loc)) {
                    RobotInfo robotInfo = rc.senseRobotAtLocation(loc);
                    if (prevLoc != null && !prevLoc.isWithinDistanceSquared(loc, actionRadiusSquared)
                            && robotInfo != null && rc.canAttack(loc) && isEnemyTower(robotInfo) && locBeforeTurn.distanceSquaredTo(prevLoc) < 4) {
                        //    rc.setTimelineMarker("Kiting time!", 0, 255, 0);
                        switchStrategy(new KitingStrategy(prevLoc, locBeforeTurn, loc), false);
                        return;
                    }
                }
            }
            prevLoc = locBeforeTurn;

            BugNav.moveToward(target);
            boolean painted = tryPaintBelowSelf(getSrpPaintColor(rc.getLocation()));
            if (!painted) {
                MapInfo[] attackableTiles = rc.senseNearbyMapInfos(locBeforeTurn, actionRadiusSquared);
                for (int i = attackableTiles.length; --i >= 0;) {
                    MapInfo tile = attackableTiles[i];
                    MapLocation loc = tile.getMapLocation();
                    if (!tile.getPaint().isAlly() && tryPaint(loc, getSrpPaintColor(loc))) {
                        break;
                    }
                }
            }

            // try to complete any resource pattern in range
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapLocation loc = nearbyMapInfos[i].getMapLocation();
                if (isInSrpCenterLocation(loc) && rc.canCompleteResourcePattern(loc)) {
                    rc.setIndicatorDot(loc, 0, 255, 0);
                    rc.completeResourcePattern(loc);
                }
            }

            if (rc.getLocation() == locBeforeTurn) {
                if (++turnsNotMoved == 3) {
                    yieldStrategy(true);
                }
            } else {
                turnsNotMoved = 0;
            }
        }

        @Override
        public String toString() {
            return "Explore " + turnsLeft + " " + target;
        }
    }

    static class KitingStrategy extends SoldierStrategy {

        public static MapLocation outRangeLoc;
        public static MapLocation inRangeLoc;
        public static MapLocation target;
        public static int turnsMoved = 0;

        KitingStrategy(MapLocation _outRangeLoc, MapLocation _inRangeLoc, MapLocation _target) {
            outRangeLoc = _outRangeLoc;
            inRangeLoc = _inRangeLoc;
            target = _target;
        }

        @Override
        public void act() throws GameActionException {
            // sometimes bugnav moves me out of bounds
            // we want to check if we're within sensible distance of target
            if (rc.getLocation().isWithinDistanceSquared(target, 20)) {
                // check if target is still alive
                RobotInfo robotInfo = rc.senseRobotAtLocation(target);
                if (robotInfo == null) {
                    yieldStrategy(false);
                    return;
                }

                int myHealth = rc.getHealth();
                // consider running away if we're low on health and we're on the outRangeLoc
                if (myHealth < 100 && (turnsMoved & 1) == 1) {
                    int damageReceive = robotInfo.getType().attackStrength;
                    int damageDealt = rc.getType().attackStrength;
                    int enemyHealth = robotInfo.getHealth();
                    int turnsToDeath = (myHealth/damageReceive + ((myHealth%damageReceive) & 1)) * 2;
                    int turnsToKill = enemyHealth/damageDealt + ((myHealth%damageReceive) & 1);
                    
                    boolean runAway = false;
                    // if we can kill them, we stay 
                    if (turnsToKill > turnsToDeath) {
                        runAway = true;
                    } else if (turnsToKill == turnsToDeath && rc.getID() > robotInfo.getID()) {
                        runAway = true;
                    }
                    // otherwise skaddadle
                    if (runAway) {
                        switchStrategy(new RunAwayStrategy(target, 4), false);
                        return;
                    }
                }
            }

            if ((turnsMoved & 1) == 0) {
                tryAttack(target);
                BugNav.moveToward(outRangeLoc);
            } else {
                BugNav.moveToward(inRangeLoc);
                tryAttack(target);
            }
            turnsMoved++;
        }

        @Override
        public String toString() {
            return "Kiting " + outRangeLoc + " " + inRangeLoc + " " + target;
        }
    }

    static class RefillPaintStrategy extends SoldierStrategy {
        private static int refillTo;

        public RefillPaintStrategy(int _refillTo) {
            // assert(rc.getPaint() < refillTo);
            refillTo = _refillTo;
        }

        @Override
        public void act() throws GameActionException {
            // TODO: try to spread out among the robots waiting to be refilled.
            if (rc.getPaint() >= refillTo) {
                yieldStrategy(false);
                return;
            }
            if (paintTowerLoc == null) {
                yieldStrategy(false);
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
                yieldStrategy(false);
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
                    yieldStrategy(true);
                }
            }
        }

        @Override
        public String toString() {
            return "RefillPaintStrategy " + paintTowerLoc;
        }

    }

    // Run away from tower we were attacking if health is low
    // Badarded rn, literally just skaddadle in the opposite direction
    static class RunAwayStrategy extends SoldierStrategy {
        MapLocation target;
        int turns;
        RunAwayStrategy(MapLocation _target, int _turns) {
            // rc.setTimelineMarker("running away", 0, 255, 0);
            target = _target;
            turns = _turns;
        }

        @Override
        public void act() throws GameActionException {
            // just move in the opposite direction lol
            if (turns == 0) {
                yieldStrategy(false);
                return;
            }
            Direction dir = target.directionTo(rc.getLocation());
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
            turns--;
        }

        @Override
        public String toString() {
            return "Run away " + target + " " + turns + " " + target.directionTo(rc.getLocation());
        }
    }

    static class TrollingStrategy extends SoldierStrategy {
        MapLocation target;
        MapLocation[] trolledLocs = new MapLocation[5];
        int trolledLocIdx = 0;

        boolean trolledBefore(MapLocation loc) {
            for (MapLocation trolledLoc : trolledLocs) {
                if (trolledLoc != null && trolledLoc.equals(loc)) return true;
            }
            return false;
        }

        TrollingStrategy(MapLocation _target) {
            rc.setTimelineMarker("Trolling begins!", 0, 0, 255);
            target = _target;
        }

        @Override
        public void act() throws GameActionException {
            MapLocation curLoc = rc.getLocation();
            if (curLoc.equals(target)) {
                yieldStrategy(false);
                return;
            }

            MapLocation ruinLoc = null;
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapInfo tile = nearbyMapInfos[i];
                MapLocation loc = tile.getMapLocation();
                RobotInfo robotInfo = rc.senseRobotAtLocation(loc);
                if (tile.hasRuin() && robotInfo == null && !trolledBefore(loc)) {
                    ruinLoc = loc;
                    break; 
                }
            }


            if (ruinLoc != null) {
                for (int i = nearbyMapInfos.length; --i >= 0;) {
                    MapInfo tile = nearbyMapInfos[i];
                    MapLocation loc = tile.getMapLocation();
                    int adx = Math.abs(loc.x - ruinLoc.x);
                    int ady = Math.abs(loc.y - ruinLoc.y);
                    if (adx <= 2 && ady <= 2 && loc.isWithinDistanceSquared(curLoc, actionRadiusSquared) && tile.getPaint() == PaintType.EMPTY) {
                        if (tryPaint(loc, getSrpPaintColor(loc))) {
                            trolledLocs[trolledLocIdx] = ruinLoc;
                            trolledLocIdx = (trolledLocIdx + 1) % trolledLocs.length;
                            break;
                        }
                    }
                }
            }

            BugNav.moveToward(target);
        }

        @Override
        public String toString() {
            return "Trolling " + target + " " + trolledLocs[trolledLocIdx];
        }
    }

}
