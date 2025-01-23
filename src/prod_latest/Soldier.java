package prod_latest;

import battlecode.common.*;

public class Soldier extends Unit {
    /**
     * Our strategy is to pick a random location and wander over for 8 turns, and if
     * ruin is found switch to build tower strategy.
     */

    static SoldierStrategy strategy;

    public static MapLocation prevLoc = null;

    public static void switchStrategy(SoldierStrategy newStrategy) {
        strategy = newStrategy;
    }

    @Override
    void play() throws GameActionException {
        if (strategy == null) {
            // if (roundNum >= 30 && rng.nextInt(2) == 1) {
            //     strategy = new CrusadeStrategy();
            // }
            // else strategy = new EarlyGameStrategy();
            strategy = new EarlyGameStrategy();
        }
        Logger.log(strategy.toString());
        strategy.act();

        // also wanna upgrade towers nearby if possible
        upgradeTowers();
    }

    static abstract class SoldierStrategy extends Soldier {

        abstract public void act() throws GameActionException;
    }

    // Moves towards `target` for `turns` turns.
    static class EarlyGameStrategy extends SoldierStrategy {
        enum StrategyState {
            BUILDING_RUIN,
            BUILDING_SRP,
            KITING,
            EXPLORING,
        }

        // The target for the current project:
        // buildingRuin - ruin location
        // buildingSrp - proposed SRP center
        // kiting - the enemy tower
        // explore - explore location
        public static MapLocation target;
        static StrategyState state;
        static int stepsOut;
        static int turnsLeftToExplore;
        static long[] srpBlocked;
        static long[] ruinBlocked;
        static long[] srpDone;
        static int turnsSinceInterestingActivity;

        EarlyGameStrategy() {
            srpBlocked = new long[mapHeight];
            ruinBlocked = new long[mapHeight];
            srpDone = new long[mapHeight];
        }

        static UnitType getTowerToBuild() {
            // The first two are in case we drop below 2 towers
            final int[] ORDER = {1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 2, 2};
            if (numTowers >= ORDER.length || rc.getChips() >= 40000) {
                return UnitType.LEVEL_ONE_PAINT_TOWER;
            }
            return switch (ORDER[numTowers]) {
                case 0 -> UnitType.LEVEL_ONE_MONEY_TOWER;
                case 1 -> UnitType.LEVEL_ONE_PAINT_TOWER;
                case 2 -> UnitType.LEVEL_ONE_DEFENSE_TOWER;
                default -> throw new IllegalArgumentException();
            };
        }

        static boolean patternAppearsClear(MapLocation center, boolean checkPassable) throws GameActionException {
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                var tile = nearbyMapInfos[i];
                if (center.distanceSquaredTo(tile.getMapLocation()) > GameConstants.RESOURCE_PATTERN_RADIUS_SQUARED) {
                    continue;
                }
                if (tile.getPaint().isEnemy()) {
                    return false;
                }
                if (checkPassable && !tile.isPassable()) {
                    return false;
                }
            }
            return true;
        }

        static boolean isSrpOk(MapLocation center) throws GameActionException {
            if (((srpBlocked[center.y] >> center.x) & 1) == 1) {
                return false;
            }
            if (!patternAppearsClear(center, true)) {
                srpBlocked[center.y] |= 1L << center.x;
                return false;
            }
            for (int i = nearbyRuins.length; --i >= 0;) {
                var ruinLoc = nearbyRuins[i];
                if (rc.canSenseRobotAtLocation(ruinLoc)) {
                    continue;
                }
                if (chebyshevDist(center, ruinLoc) <= 4) {
                    srpBlocked[center.y] |= 1L << center.x;
                    return false;
                }
            }
            return true;
        }

        static boolean isRuinOk(MapLocation ruinLoc) throws GameActionException {
            if (((ruinBlocked[ruinLoc.y] >> ruinLoc.x) & 1) == 1) {
                return false;
            }
            if (!patternAppearsClear(ruinLoc, false)) {
                ruinBlocked[ruinLoc.y] |= 1L << ruinLoc.x;
                return false;
            }
            return true;
        }

        static boolean isPatternOnMap(MapLocation center) {
            return 2 <= center.x && center.x < mapWidth - 2 && 2 <= center.y && center.y < mapHeight - 2;
        }

        void getProject() throws GameActionException {
            // Check if we need to attack an enemy tower
            for (int i = nearbyEnemyRobots.length; --i >= 0;) {
                var robotInfo = nearbyEnemyRobots[i];
                if (robotInfo.type.isTowerType()) {
                    state = StrategyState.KITING;
                    target = robotInfo.location;
                    return;
                }
            }

            if (state == StrategyState.BUILDING_RUIN || state == StrategyState.BUILDING_SRP) {
                return;
            }

            if (rc.getNumberTowers() < 25) {
                boolean startBuilding = (rc.getChips() > 500 || rc.getNumberTowers() >= 5);
                if (startBuilding) {
                    // Check for nearby ruins
                    MapLocation ruinLoc = null;
                    for (int i = nearbyRuins.length; --i >= 0;) {
                        MapLocation loc = nearbyRuins[i];
                        if (rc.canSenseLocation(loc) && rc.senseRobotAtLocation(loc) == null) {
                            ruinLoc = loc;
                        }
                    }
                    if (ruinLoc != null && isRuinOk(ruinLoc)) {
                        state = StrategyState.BUILDING_RUIN;
                        target = ruinLoc;
                        return;
                    }
                }
            }

            // Check for places to build SRPs

            for (int i = nearbyMapInfos.length; --i >= 0;) {
                var tile = nearbyMapInfos[i];
                var loc = tile.getMapLocation();
                if (loc.x % 4 == 2 && loc.y % 4 == 2 && isPatternOnMap(loc)) {
                    if (((srpDone[loc.y] >> loc.x) & 1) == 0 && isSrpOk(loc)) {
                        state = StrategyState.BUILDING_SRP;
                        target = loc;
                        stepsOut = 0;
                        return;
                    }
                    break;
                }
            }

            if (state == null || turnsLeftToExplore == 0 || rc.getLocation() == target) {
                state = StrategyState.EXPLORING;
                turnsLeftToExplore = 8;
                target = getRandomNearbyLocation(rc.getLocation(), 10, 20);
            }
        }

        private static MapLocation getRandomNearbyLocation(MapLocation center, int minChebyshevDist, int maxChebyshevDist) {
            int dx = rng.nextInt(minChebyshevDist, maxChebyshevDist);
            if (rng.nextInt(2) == 1) dx = -dx;
            int dy = rng.nextInt(minChebyshevDist, maxChebyshevDist);
            if (rng.nextInt(2) == 1) dy = -dy;
            return new MapLocation(Math.clamp(center.x + dx, 0, mapWidth - 1), Math.clamp(center.y + dy, 0, mapHeight - 1));
        }

        static boolean tryRefill(MapLocation towerLoc) throws GameActionException {
            if (rc.canSenseRobotAtLocation(towerLoc)) {
                var towerInfo = rc.senseRobotAtLocation(towerLoc);
                if (towerInfo.team.equals(myTeam)) {
                    int paintWanted = Math.min(towerInfo.paintAmount, paintCapacity - rc.getPaint());
                    if (rc.canTransferPaint(towerLoc, -paintWanted)) {
                        rc.transferPaint(towerLoc, -paintWanted);
                        return true;
                    }
                }
            }
            return false;
        }

        static MapLocation locInPatternToAttack(MapLocation center, UnitType type) throws GameActionException {
            // For checking SRPs, pass type = null.
            int maxDist2 = -1;
            MapLocation best = null;
            var myLoc = rc.getLocation();
            var myTile = rc.senseMapInfo(myLoc);
            if (withinPattern(myLoc, center) && rc.canPaint(myLoc) && myTile.getPaint() != getPatternPaintColor(center, myLoc, type)) {
                return myLoc;
            }
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                var tile = nearbyMapInfos[i];
                var loc = tile.getMapLocation();
                if (!withinPattern(center, loc)) {
                    continue;
                }
                var paintType = getPatternPaintColor(center, loc, type);
                int dist2 = loc.distanceSquaredTo(center);
                if (rc.canPaint(loc) && tile.getPaint() != paintType && dist2 > maxDist2) {
                    maxDist2 = dist2;
                    best = loc;
                }
            }
            return best;
        }

        @Override
        public void act() throws GameActionException {
            if (state == StrategyState.BUILDING_RUIN) {
                if (!rc.isActionReady()) {
                    return;
                }
                tryRefill(target);
            }

            // int startBytecodes = Clock.getBytecodeNum();
            getProject();
            // int endBytecodes = Clock.getBytecodeNum();
            // System.out.println("Bytecodes used to get project: " + (endBytecodes - startBytecodes));

            if (state == StrategyState.BUILDING_RUIN) {
                turnsSinceInterestingActivity = 0;
                // TODO: get closest tower to being built based on the pattern and keep building it
                var ruinLoc = target;
                if (rc.canSenseRobotAtLocation(ruinLoc)) {
                    // Tower has been finished
                    state = null;
                    act();
                    return;
                }
                if (!patternAppearsClear(ruinLoc, false)) {
                    // Enemy painted in the pattern
                    state = null;
                    act();
                    return;
                }

                final var towerType = getTowerToBuild();
                BugNav.moveToward(ruinLoc);
                final var paintLoc = locInPatternToAttack(ruinLoc, towerType);
                if (paintLoc != null) {
                    tryPaint(paintLoc, getTowerPaintColor(ruinLoc, paintLoc, towerType));
                }

                if (rc.canCompleteTowerPattern(towerType, ruinLoc)) {
                    rc.completeTowerPattern(towerType, ruinLoc);
                    int paintWanted = Math.min(rc.senseRobotAtLocation(ruinLoc).paintAmount, paintCapacity - rc.getPaint());
                    if (rc.canTransferPaint(ruinLoc, -paintWanted)) {
                        rc.transferPaint(ruinLoc, -paintWanted);
                    }
                }
                return;
            }

            if (state == StrategyState.BUILDING_SRP) {
                turnsSinceInterestingActivity = 0;
                var srpCenter = target;
                if (!isSrpOk(srpCenter)) {
                    state = null;
                    act();
                    return;
                }

                if (!rc.getLocation().equals(srpCenter)) {
                    BugNav.moveToward(srpCenter);
                } else {
                    if (stepsOut < 4) {
                        // Take a step out to see if the SRP pattern is actually bad
                        var dir = diagonalDirections[stepsOut++];
                        if (rc.canMove(dir)) {
                            rc.move(dir);
                        }
                    }
                }
                final var newLoc = rc.getLocation();

                // Try painting
                boolean painted = false;
                final var paintLoc = locInPatternToAttack(srpCenter, null);
                if (paintLoc != null) {
                    painted = tryPaint(paintLoc, getSrpPaintColor(paintLoc));
                }

                if (newLoc.distanceSquaredTo(srpCenter) <= 2 && !painted) {
                    // SRP is finished
                    state = null;
                    srpDone[srpCenter.y] |= 1L << srpCenter.x;
                    return;
                }

                // Try completing the SRP
                if (rc.canCompleteResourcePattern(srpCenter)) {
                    rc.completeResourcePattern(srpCenter);
                    state = null;
                    srpDone[srpCenter.y] |= 1L << srpCenter.x;
                }

                return;
            }

            if (state == StrategyState.KITING) {
                turnsSinceInterestingActivity = 0;
                if (!rc.canSenseRobotAtLocation(target)) {
                    state = null;
                    return;
                }
                RobotInfo robotInfo = rc.senseRobotAtLocation(target);
                if (robotInfo.team.equals(myTeam)) {
                    state = null;
                    return;
                }
                if (tryAttack(target)) {
                    var curLoc = rc.getLocation();
                    var reflected = new MapLocation(2 * curLoc.x - target.x, 2 * curLoc.y - target.y);
                    BugNav.moveToward(reflected);
                } else {
                    BugNav.moveToward(target);
                    tryAttack(target);
                }
                return;
            }

            // Just explore towards target
            turnsLeftToExplore -= 1;
            BugNav.moveToward(target);
            var newLoc = rc.getLocation();
            boolean painted = false;
            if (rc.senseMapInfo(newLoc).getPaint() == PaintType.EMPTY) {
                painted = tryPaintBelowSelf(getSrpPaintColor(newLoc));
            }

            if (!painted && rc.getRoundNum() >= 200) {
                MapInfo[] attackableTiles = rc.senseNearbyMapInfos(newLoc, actionRadiusSquared);
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
                    rc.completeResourcePattern(loc);
                }
            }

            // turnsSinceInterestingActivity++;
            // if (turnsSinceInterestingActivity > 30) {
            //     switchStrategy(new CrusadeStrategy());
            //     return;
            // }
        }

        @Override
        public String toString() {
            return "EarlyGameStrategy " + state + " " + target;
        }
    }

    static class CrusadeStrategy extends SoldierStrategy {
        enum StrategyState {
            KITING,
            TRAVELLING,
        }
        
        public static StrategyState state = null;
        public static MapLocation target = null;
        public static MapLocation[] checkpoints;
        public static int checkpointPtr = 0;

        CrusadeStrategy() {
            if (rng.nextInt(2) == 0) {
                checkpoints = new MapLocation[3];
                checkpoints[0] = reflectXY(spawnLocation);
                checkpoints[1] = reflectX(spawnLocation);
                checkpoints[2] = reflectY(spawnLocation);
            }
            else {
                checkpoints = new MapLocation[4];
                checkpoints[0] = new MapLocation(0, 0);
                checkpoints[1] = new MapLocation(mapWidth-1, 0);
                checkpoints[2] = new MapLocation(mapWidth-1, mapHeight-1);
                checkpoints[3] = new MapLocation(0, mapHeight-1);
                checkpointPtr = rng.nextInt(4);
            }
            state = StrategyState.TRAVELLING;
        }


        @Override
        public void act() throws GameActionException{
            if (state == null) {
                state = StrategyState.TRAVELLING;
            }

            if (state == StrategyState.TRAVELLING) {
                for (int i = nearbyEnemyRobots.length; --i >= 0;) {
                    var robotInfo = nearbyEnemyRobots[i];
                    if (robotInfo.type.isTowerType()) {
                        state = StrategyState.KITING;
                        target = robotInfo.location;
                        act();
                        return;
                    }
                }

                if (rc.getLocation().isAdjacentTo(checkpoints[checkpointPtr])) {
                    checkpointPtr = (checkpointPtr + 1) % checkpoints.length;
                }
                MapLocation checkpoint = checkpoints[checkpointPtr];
                BugNav.moveToward(checkpoint);
                rc.setIndicatorLine(rc.getLocation(), checkpoint, 0, 255, 0);
                if (roundNum % 5 == 0) { // attack infrequently to conserve paint :)
                    MapInfo[] attackableTiles = rc.senseNearbyMapInfos(rc.getLocation(), actionRadiusSquared);
                    for (int i = attackableTiles.length; --i >= 0;) {
                        MapInfo tile = attackableTiles[i];
                        MapLocation loc = tile.getMapLocation();
                        if (tile.getPaint() == PaintType.EMPTY && tryPaint(loc, getSrpPaintColor(loc))) {
                            break;
                        }
                    }
                }
                return;
            }

            if (state == StrategyState.KITING) {
                if (!rc.canSenseRobotAtLocation(target)) {
                    state = null;
                    act();
                    return;
                }
                RobotInfo robotInfo = rc.senseRobotAtLocation(target);
                if (robotInfo.team.equals(myTeam)) {
                    state = null;
                    act();
                    return;
                }
                if (tryAttack(target)) {
                    var curLoc = rc.getLocation();
                    var reflected = new MapLocation(2 * curLoc.x - target.x, 2 * curLoc.y - target.y);
                    BugNav.moveToward(reflected);
                } else {
                    BugNav.moveToward(target);
                    tryAttack(target);
                }
                return;
            }
        }

        public String toString() {
            return "CrusadeStrategy " + state + " " + target;
        }
    }
}
