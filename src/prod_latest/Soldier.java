package prod_latest;

import battlecode.common.*;
import prod_latest.Unit.MapLocationType;

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
        if (rc.getPaint() < 50 && paintTowerLoc != null && getProgress() < 0.3) {
            Logger.log("refilling paint");
            Logger.flush();
            strategy = new RefillPaintStrategy(120);
        } else {
            MapLocation target = new MapLocation(rng.nextInt(mapWidth), rng.nextInt(mapHeight));
            strategy = new ExploreStrategy(target);
        }
        if (!acted) {
            Logger.log(strategy.toString());
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
        Logger.log(strategy.toString());
        strategy.act();

        // also wanna upgrade towers nearby if possible
        upgradeTowers();
    }

    static abstract class SoldierStrategy extends Soldier {

        abstract public void act() throws GameActionException;
    }

    // static class EarlyGameStrategy extends SoldierStrategy {

    //     @Override
    //     public void act() throws GameActionException {
            
    //     }

    // }

    /**
     * Strategy to build a tower at specified ruin location
     */
    static class BuildTowerStrategy extends SoldierStrategy {

        public static MapLocation ruinLoc;
        public static UnitType towerType;

        BuildTowerStrategy(MapLocation _ruinLoc) {
            // Precondition: we can sense the ruin location
            ruinLoc = _ruinLoc;
            assert(ruinLoc != null);
        }

        @Override
        public void act() throws GameActionException {
            if (!rc.canSenseLocation(ruinLoc) || rc.getNumberTowers() == GameConstants.MAX_NUMBER_OF_TOWERS) {
                yieldStrategy(false);
                return;
            }
            if (rc.senseRobotAtLocation(ruinLoc) != null) {
                int paintWanted = Math.min(rc.senseRobotAtLocation(ruinLoc).paintAmount, paintCapacity - rc.getPaint());
                System.out.println("trying to take " + paintWanted + " from " + ruinLoc);
                if (rc.canTransferPaint(ruinLoc, -paintWanted))
                    rc.transferPaint(ruinLoc, -paintWanted);
                yieldStrategy(true);
                return;
            }

            // If ruin is not adjacent, walk closer
            if (!locBeforeTurn.isAdjacentTo(ruinLoc)) {
                BugNav.moveToward(ruinLoc);
                tryPaintBelowSelf(getTowerPaintColor(ruinLoc, locBeforeTurn, towerType));
                return;
            }

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
                int paintWanted = Math.min(rc.senseRobotAtLocation(ruinLoc).paintAmount, paintCapacity - rc.getPaint());
                if (rc.canTransferPaint(ruinLoc, -paintWanted))
                    rc.transferPaint(ruinLoc, -paintWanted);
                return;
            }

            // Try to paint any tiles outside radius while we're waiting
            // if (rc.getPaint() > 30) {
            //     for (int i = actionableMapInfos.length; --i >= 0;) {
            //         MapInfo tile = actionableMapInfos[i];
            //         MapLocation loc = tile.getMapLocation();
            //         if (ruinLoc.distanceSquaredTo(loc) > GameConstants.RESOURCE_PATTERN_RADIUS_SQUARED) {
            //             if (tryPaint(loc, getSrpPaintColor(loc))) {
            //                 break;
            //             }
            //         }
            //     }
            // }
        }

        @Override
        public String toString() {
            return "BuildTower " + ruinLoc + " " + towerType;
        }
    }

    // Moves towards `target` for `turns` turns.
    static class ExploreStrategy extends SoldierStrategy {

        // The target for the current project:
        // buildingRuin - ruin location
        // buildingSrp - proposed SRP center
        // neither - explore location
        public static MapLocation target;
        boolean buildingRuin;
        boolean buildingSrp;
        static int precompAppearsClear[];

        ExploreStrategy(MapLocation _target) {
            target = _target;
            buildingRuin = false;
            buildingSrp = false;
        }

        UnitType getTowerToBuild() {
            // The first two are in case we drop below 2 towers
            final int[] ORDER = {1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1};
            if (numTowers >= ORDER.length) {
                return UnitType.LEVEL_ONE_PAINT_TOWER;
            }
            return switch (ORDER[numTowers]) {
                case 0 -> UnitType.LEVEL_ONE_MONEY_TOWER;
                case 1 -> UnitType.LEVEL_ONE_PAINT_TOWER;
                default -> throw new IllegalArgumentException();
            };
        }

        void precomputePatternAppearsClear() {
            precompAppearsClear = new int[3];
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapInfo tile = nearbyMapInfos[i];
                if (!tile.getPaint().isEnemy() && !tile.isWall()) {
                    continue;
                }
                MapLocation loc = tile.getMapLocation();
                MapLocation diff = loc.translate(-locBeforeTurn.x, -locBeforeTurn.y);
                switch (diff.x * 1000 + diff.y) {
                    case -4002: precompAppearsClear[0] |= 8142367; precompAppearsClear[1] |= 0; precompAppearsClear[2] |= 0; break; // (-4, -2)
                    case -4001: precompAppearsClear[0] |= 16284734; precompAppearsClear[1] |= 0; precompAppearsClear[2] |= 0; break; // (-4, -1)
                    case -4000: precompAppearsClear[0] |= 32569468; precompAppearsClear[1] |= 0; precompAppearsClear[2] |= 0; break; // (-4, 0)
                    case -3999: precompAppearsClear[0] |= 65138936; precompAppearsClear[1] |= 0; precompAppearsClear[2] |= 0; break; // (-4, 1)
                    case -3998: precompAppearsClear[0] |= 130277872; precompAppearsClear[1] |= 0; precompAppearsClear[2] |= 0; break; // (-4, 2)
                    case -3003: precompAppearsClear[0] |= 3939855; precompAppearsClear[1] |= 15; precompAppearsClear[2] |= 0; break; // (-3, -3)
                    case -3002: precompAppearsClear[0] |= 8142367; precompAppearsClear[1] |= 31; precompAppearsClear[2] |= 0; break; // (-3, -2)
                    case -3001: precompAppearsClear[0] |= 16284734; precompAppearsClear[1] |= 62; precompAppearsClear[2] |= 0; break; // (-3, -1)
                    case -3000: precompAppearsClear[0] |= 32569468; precompAppearsClear[1] |= 124; precompAppearsClear[2] |= 0; break; // (-3, 0)
                    case -2999: precompAppearsClear[0] |= 65138936; precompAppearsClear[1] |= 248; precompAppearsClear[2] |= 0; break; // (-3, 1)
                    case -2998: precompAppearsClear[0] |= 130277872; precompAppearsClear[1] |= 496; precompAppearsClear[2] |= 0; break; // (-3, 2)
                    case -2997: precompAppearsClear[0] |= 126075360; precompAppearsClear[1] |= 480; precompAppearsClear[2] |= 0; break; // (-3, 3)
                    case -2004: precompAppearsClear[0] |= 1838599; precompAppearsClear[1] |= 3591; precompAppearsClear[2] |= 0; break; // (-2, -4)
                    case -2003: precompAppearsClear[0] |= 3939855; precompAppearsClear[1] |= 7695; precompAppearsClear[2] |= 0; break; // (-2, -3)
                    case -2002: precompAppearsClear[0] |= 8142367; precompAppearsClear[1] |= 15903; precompAppearsClear[2] |= 0; break; // (-2, -2)
                    case -2001: precompAppearsClear[0] |= 16284734; precompAppearsClear[1] |= 31806; precompAppearsClear[2] |= 0; break; // (-2, -1)
                    case -2000: precompAppearsClear[0] |= 32569468; precompAppearsClear[1] |= 63612; precompAppearsClear[2] |= 0; break; // (-2, 0)
                    case -1999: precompAppearsClear[0] |= 65138936; precompAppearsClear[1] |= 127224; precompAppearsClear[2] |= 0; break; // (-2, 1)
                    case -1998: precompAppearsClear[0] |= 130277872; precompAppearsClear[1] |= 254448; precompAppearsClear[2] |= 0; break; // (-2, 2)
                    case -1997: precompAppearsClear[0] |= 126075360; precompAppearsClear[1] |= 246240; precompAppearsClear[2] |= 0; break; // (-2, 3)
                    case -1996: precompAppearsClear[0] |= 117670336; precompAppearsClear[1] |= 229824; precompAppearsClear[2] |= 0; break; // (-2, 4)
                    case -1004: precompAppearsClear[0] |= 1838592; precompAppearsClear[1] |= 1838599; precompAppearsClear[2] |= 0; break; // (-1, -4)
                    case -1003: precompAppearsClear[0] |= 3939840; precompAppearsClear[1] |= 3939855; precompAppearsClear[2] |= 0; break; // (-1, -3)
                    case -1002: precompAppearsClear[0] |= 8142336; precompAppearsClear[1] |= 8142367; precompAppearsClear[2] |= 0; break; // (-1, -2)
                    case -1001: precompAppearsClear[0] |= 16284672; precompAppearsClear[1] |= 16284734; precompAppearsClear[2] |= 0; break; // (-1, -1)
                    case -1000: precompAppearsClear[0] |= 32569344; precompAppearsClear[1] |= 32569468; precompAppearsClear[2] |= 0; break; // (-1, 0)
                    case -999: precompAppearsClear[0] |= 65138688; precompAppearsClear[1] |= 65138936; precompAppearsClear[2] |= 0; break; // (-1, 1)
                    case -998: precompAppearsClear[0] |= 130277376; precompAppearsClear[1] |= 130277872; precompAppearsClear[2] |= 0; break; // (-1, 2)
                    case -997: precompAppearsClear[0] |= 126074880; precompAppearsClear[1] |= 126075360; precompAppearsClear[2] |= 0; break; // (-1, 3)
                    case -996: precompAppearsClear[0] |= 117669888; precompAppearsClear[1] |= 117670336; precompAppearsClear[2] |= 0; break; // (-1, 4)
                    case -4: precompAppearsClear[0] |= 1835008; precompAppearsClear[1] |= 1838599; precompAppearsClear[2] |= 7; break; // (0, -4)
                    case -3: precompAppearsClear[0] |= 3932160; precompAppearsClear[1] |= 3939855; precompAppearsClear[2] |= 15; break; // (0, -3)
                    case -2: precompAppearsClear[0] |= 8126464; precompAppearsClear[1] |= 8142367; precompAppearsClear[2] |= 31; break; // (0, -2)
                    case -1: precompAppearsClear[0] |= 16252928; precompAppearsClear[1] |= 16284734; precompAppearsClear[2] |= 62; break; // (0, -1)
                    case 0: precompAppearsClear[0] |= 32505856; precompAppearsClear[1] |= 32569468; precompAppearsClear[2] |= 124; break; // (0, 0)
                    case 1: precompAppearsClear[0] |= 65011712; precompAppearsClear[1] |= 65138936; precompAppearsClear[2] |= 248; break; // (0, 1)
                    case 2: precompAppearsClear[0] |= 130023424; precompAppearsClear[1] |= 130277872; precompAppearsClear[2] |= 496; break; // (0, 2)
                    case 3: precompAppearsClear[0] |= 125829120; precompAppearsClear[1] |= 126075360; precompAppearsClear[2] |= 480; break; // (0, 3)
                    case 4: precompAppearsClear[0] |= 117440512; precompAppearsClear[1] |= 117670336; precompAppearsClear[2] |= 448; break; // (0, 4)
                    case 996: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 1838599; precompAppearsClear[2] |= 3591; break; // (1, -4)
                    case 997: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 3939855; precompAppearsClear[2] |= 7695; break; // (1, -3)
                    case 998: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 8142367; precompAppearsClear[2] |= 15903; break; // (1, -2)
                    case 999: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 16284734; precompAppearsClear[2] |= 31806; break; // (1, -1)
                    case 1000: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 32569468; precompAppearsClear[2] |= 63612; break; // (1, 0)
                    case 1001: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 65138936; precompAppearsClear[2] |= 127224; break; // (1, 1)
                    case 1002: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 130277872; precompAppearsClear[2] |= 254448; break; // (1, 2)
                    case 1003: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 126075360; precompAppearsClear[2] |= 246240; break; // (1, 3)
                    case 1004: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 117670336; precompAppearsClear[2] |= 229824; break; // (1, 4)
                    case 1996: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 1838592; precompAppearsClear[2] |= 1838599; break; // (2, -4)
                    case 1997: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 3939840; precompAppearsClear[2] |= 3939855; break; // (2, -3)
                    case 1998: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 8142336; precompAppearsClear[2] |= 8142367; break; // (2, -2)
                    case 1999: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 16284672; precompAppearsClear[2] |= 16284734; break; // (2, -1)
                    case 2000: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 32569344; precompAppearsClear[2] |= 32569468; break; // (2, 0)
                    case 2001: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 65138688; precompAppearsClear[2] |= 65138936; break; // (2, 1)
                    case 2002: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 130277376; precompAppearsClear[2] |= 130277872; break; // (2, 2)
                    case 2003: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 126074880; precompAppearsClear[2] |= 126075360; break; // (2, 3)
                    case 2004: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 117669888; precompAppearsClear[2] |= 117670336; break; // (2, 4)
                    case 2997: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 3932160; precompAppearsClear[2] |= 3939855; break; // (3, -3)
                    case 2998: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 8126464; precompAppearsClear[2] |= 8142367; break; // (3, -2)
                    case 2999: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 16252928; precompAppearsClear[2] |= 16284734; break; // (3, -1)
                    case 3000: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 32505856; precompAppearsClear[2] |= 32569468; break; // (3, 0)
                    case 3001: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 65011712; precompAppearsClear[2] |= 65138936; break; // (3, 1)
                    case 3002: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 130023424; precompAppearsClear[2] |= 130277872; break; // (3, 2)
                    case 3003: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 125829120; precompAppearsClear[2] |= 126075360; break; // (3, 3)
                    case 3998: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 0; precompAppearsClear[2] |= 8142367; break; // (4, -2)
                    case 3999: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 0; precompAppearsClear[2] |= 16284734; break; // (4, -1)
                    case 4000: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 0; precompAppearsClear[2] |= 32569468; break; // (4, 0)
                    case 4001: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 0; precompAppearsClear[2] |= 65138936; break; // (4, 1)
                    case 4002: precompAppearsClear[0] |= 0; precompAppearsClear[1] |= 0; precompAppearsClear[2] |= 130277872; break; // (4, 2)
                }
            }
        }

        boolean patternAppearsClear(MapLocation center) throws GameActionException {
            MapLocation diff = center.translate(-locBeforeTurn.x, -locBeforeTurn.y);
            int x = diff.x + 4;
            int y = diff.y + 4;
            // System.out.println((1 & (precompAppearsClear[x / 3] >> (9 * (x % 3) + y))) == 0);
            return (1 & (precompAppearsClear[x / 3] >> (9 * (x % 3) + y))) == 0;
        }

        boolean knowsIsBadSrpCenter(MapLocation center) throws GameActionException {
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapInfo tile = nearbyMapInfos[i];
                if (!tile.isPassable()) {
                    return true;
                }
            }

            // for (int dx = -4; dx <= 4; dx++) {
            //     for (int dy = -4; dy <= 4; dy++) {
            //         var t = new MapLocation(center.x + dx, center.y + dy);
            //         if (rc.onTheMap(t)) {
            //             MapLocationType memo = memory[t.x][t.y];
            //             if (memo == MapLocationType.RUIN) {
            //                 return true;
            //             }
            //         }
            //     }
            // }
            return false;
        }

        void getProject() throws GameActionException {
            if (buildingRuin || buildingSrp) {
                return;
            }
            precomputePatternAppearsClear();
            if (rc.getNumberTowers() < 25 && rc.getChips() > 500) {
                // Check for nearby ruins
                MapLocation ruinLoc = null;
                for (int i = nearbyRuins.length; --i >= 0;) {
                    MapLocation loc = nearbyRuins[i];
                    if (rc.canSenseLocation(loc) && rc.senseRobotAtLocation(loc) == null) {
                        ruinLoc = loc;
                    }
                }
                if (ruinLoc != null && patternAppearsClear(ruinLoc)) {
                    buildingRuin = true;
                    target = ruinLoc;
                    return;
                }
            }

            // Check for places to build SRPs
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                var tile = nearbyMapInfos[i];
                var loc = tile.getMapLocation();
                if (loc.x % 4 == 2
                        && loc.y % 4 == 2
                        && patternAppearsClear(loc)
                        // && !knowsIsBadSrpCenter(loc)
                ) {
                    // buildingSrp = true;
                    target = loc;
                    break;
                }
            }

            target = new MapLocation(rng.nextInt(mapWidth), rng.nextInt(mapHeight));
        }

        private MapLocation getRandomNearbyLocation(MapLocation center, int chebyshevDist) {
            int x1 = Math.max(center.x - chebyshevDist, 0);
            int x2 = Math.min(center.x + chebyshevDist, mapWidth - 1);
            int y1 = Math.max(center.y - chebyshevDist, 0);
            int y2 = Math.min(center.y + chebyshevDist, mapHeight - 1);
            return new MapLocation(rng.nextInt(x1, x2 + 1), rng.nextInt(y1, y2 + 1));
        }

        @Override
        public void act() throws GameActionException {
            // System.out.println("Bytecodes used (1): " + Clock.getBytecodeNum());
            getProject();
            // System.out.println("Bytecodes used (2): " + Clock.getBytecodeNum());

            if (buildingRuin) {
                var ruinLoc = target;
                // Check if it's already been finished
                if (!patternAppearsClear(ruinLoc)) {
                    buildingRuin = false;
                    return;
                }
                if (rc.canSenseRobotAtLocation(ruinLoc)) {
                    var robotInfo = rc.senseRobotAtLocation(ruinLoc);
                    if (robotInfo.team.equals(myTeam)) {
                        int paintWanted = Math.min(rc.senseRobotAtLocation(ruinLoc).paintAmount, paintCapacity - rc.getPaint());
                        if (rc.canTransferPaint(ruinLoc, -paintWanted)) {
                            rc.transferPaint(ruinLoc, -paintWanted);
                        }
                        buildingRuin = false;
                        return;
                    }
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
                    buildingRuin = false;
                }
                Logger.log("building ruin");
                return;
            }
            if (buildingSrp) {
                var srpCenter = target;
                /*
                Only need to check the outer 2x2 strip for ruins, i.e. the C's here:

                CCCCCCCCC
                CCCCCCCCC
                CC.....CC
                CC.....CC
                CC.....CC
                CC.....CC
                CC.....CC
                CCCCCCCCC
                CCCCCCCCC
                */
            //     MapLocation unseenLoc = null;
            // outer:
            //     for (int dx = -4; dx <= 4; dx++) {
            //         for (int dy = -4; dy <= 4; dy++) {
            //             var t = new MapLocation(srpCenter.x + dx, srpCenter.y + dy);
            //             if (rc.onTheMap(t)) {
            //                 MapLocationType memo = memory[t.x][t.y];
            //                 if (memo == null) {
            //                     unseenLoc = t;
            //                     break outer;
            //                 }
            //             }
            //         }
            //     }
            //     if (unseenLoc != null) {
            //         BugNav.moveToward(unseenLoc);
            //         tryPaintBelowSelf(getSrpPaintColor(rc.getLocation()));
            //     } else {
            //         // No unseen locations, just go about the SRP happily :)
            //     }

                BugNav.moveToward(srpCenter);
                
                // Try painting
                MapInfo[] actionableMapInfos = rc.senseNearbyMapInfos(rc.getLocation(), actionRadiusSquared);
                for (int j = actionableMapInfos.length; --j >= 0;) {
                    MapInfo tile = actionableMapInfos[j];
                    MapLocation loc = tile.getMapLocation();
                    if (target.distanceSquaredTo(loc) > GameConstants.RESOURCE_PATTERN_RADIUS_SQUARED) {
                        continue;
                    }
                    if (tryPaint(loc, getSrpPaintColor(loc))) {
                        break;
                    }
                }

                // Try completing the SRP
                if (rc.canCompleteResourcePattern(srpCenter)) {
                    // rc.setIndicatorDot(loc, 0, 255, 0);
                    rc.completeResourcePattern(srpCenter);
                    buildingSrp = false;
                }
                Logger.log("building SRP");
                return;
            }

            // Just explore towards target
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
        }

        @Override
        public String toString() {
            return "Explore " + target;
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
                // tryPaintBelowSelf(getSrpPaintColor(rc.getLocation()));
                final var totalSpace = 200 - rc.getPaint();
                final var toTransfer = Math.min(paintTowerInfo.getPaintAmount(), totalSpace);
                if (rc.canTransferPaint(paintTowerLoc, -toTransfer)) {
                    rc.transferPaint(paintTowerLoc, -toTransfer);
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
            for (int i = trolledLocs.length; --i >= 0;) { 
                MapLocation trolledLoc = trolledLocs[i];
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
