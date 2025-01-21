package prod_latest;

import battlecode.common.*;
import prod_latest.Soldier.EarlyGameStrategy.StrategyState;

public class Soldier extends Unit {
    /**
     * Our strategy is to pick a random location and wander over for 8 turns, and if
     * ruin is found switch to build tower strategy.
     */

    static SoldierStrategy strategy;

    public static MapLocation prevLoc = null;

    @Override
    void play() throws GameActionException {
        if (strategy == null) {
            strategy = new EarlyGameStrategy();
            return;
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
        static int turnsLeftToExplore;
        static long[] srpBlocked;
        static long[] ruinBlocked;
        static long[] srpDone;

        EarlyGameStrategy() {
            turnsLeftToExplore = 0;
            srpBlocked = new long[mapHeight];
            ruinBlocked = new long[mapHeight];
            srpDone = new long[mapHeight];
        }

        static UnitType getTowerToBuild() {
            // The first two are in case we drop below 2 towers
            final int[] ORDER = {1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0};
            if (numTowers >= ORDER.length) {
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

            if (rc.getNumberTowers() < 25 && rc.getChips() > 500) {
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

            // Check for places to build SRPs

            for (int i = nearbyMapInfos.length; --i >= 0;) {
                var tile = nearbyMapInfos[i];
                var loc = tile.getMapLocation();
                if (loc.x % 4 == 2 && loc.y % 4 == 2 && isPatternOnMap(loc)) {
                    if (((srpDone[loc.y] >> loc.x) & 1) == 0 && isSrpOk(loc)) {
                        state = StrategyState.BUILDING_SRP;
                        target = loc;
                        return;
                    }
                    break;
                }
            }

            if (turnsLeftToExplore == 0 || rc.getLocation() == target) {
                turnsLeftToExplore = 8;
                if (rng.nextInt(100) < 15) {
                    target = informedEmptyPaintLoc;
                }
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

        @Override
        public void act() throws GameActionException {
            if (state == StrategyState.BUILDING_RUIN) {
                tryRefill(target);
            }

            int startBytecodes = Clock.getBytecodeNum();
            getProject();
            int endBytecodes = Clock.getBytecodeNum();
            // System.out.println("Bytecodes used to get project: " + (endBytecodes - startBytecodes));

            if (state == StrategyState.BUILDING_RUIN) {
                // TODO: get closest tower to being built based on the pattern and keep building it
                var ruinLoc = target;
                if (rc.canSenseRobotAtLocation(ruinLoc)) {
                    // Tower has been finished
                    state = null;
                    return;
                }
                if (!patternAppearsClear(ruinLoc, false)) {
                    state = null;
                    return;
                }
                BugNav.moveToward(ruinLoc);
                MapInfo[] actionableMapInfos = rc.senseNearbyMapInfos(rc.getLocation(), actionRadiusSquared);
                final var towerType = getTowerToBuild();
                for (int j = actionableMapInfos.length; --j >= 0;) {
                    MapInfo tile = actionableMapInfos[j];
                    MapLocation loc = tile.getMapLocation();
                    if (target.distanceSquaredTo(loc) > GameConstants.RESOURCE_PATTERN_RADIUS_SQUARED) {
                        continue;
                    }
                    if (tryPaint(loc, getTowerPaintColor(ruinLoc, loc, towerType))) {
                        break;
                    }
                }

                if (rc.canCompleteTowerPattern(towerType, ruinLoc)) {
                    rc.completeTowerPattern(towerType, ruinLoc);
                    int paintWanted = Math.min(rc.senseRobotAtLocation(ruinLoc).paintAmount, paintCapacity - rc.getPaint());
                    if (rc.canTransferPaint(ruinLoc, -paintWanted)) {
                        rc.transferPaint(ruinLoc, -paintWanted);
                    }
                    Logger.log("completed" + paintWanted);
                }
                Logger.log("building ruin");
                return;
            }

            if (state == StrategyState.BUILDING_SRP) {
                var srpCenter = target;
                if (!isSrpOk(srpCenter)) {
                    state = null;
                    return;
                }

                if (rc.getLocation() != srpCenter) {
                    BugNav.moveToward(srpCenter);
                }
                
                // Try painting
                MapInfo[] actionableMapInfos = rc.senseNearbyMapInfos(rc.getLocation(), actionRadiusSquared);
                boolean painted = false;
                for (int j = actionableMapInfos.length; --j >= 0;) {
                    MapInfo tile = actionableMapInfos[j];
                    MapLocation loc = tile.getMapLocation();
                    if (target.distanceSquaredTo(loc) > GameConstants.RESOURCE_PATTERN_RADIUS_SQUARED) {
                        continue;
                    }
                    if (tryPaint(loc, getSrpPaintColor(loc))) {
                        painted = true;
                        break;
                    }
                }

                if (!painted) {
                    // SRP is finished
                    state = null;
                    srpDone[srpCenter.y] |= 1L << srpCenter.x;
                    return;
                }

                // Try completing the SRP
                if (rc.canCompleteResourcePattern(srpCenter)) {
                    // rc.setIndicatorDot(loc, 0, 255, 0);
                    rc.completeResourcePattern(srpCenter);
                    state = null;
                    srpDone[srpCenter.y] |= 1L << srpCenter.x;
                }

                Logger.log("building SRP");
                return;
            }

            if (state == StrategyState.KITING) {
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
        }

        @Override
        public String toString() {
            return "EarlyGameStrategy " + state + " " + target;
        }
    }

}
