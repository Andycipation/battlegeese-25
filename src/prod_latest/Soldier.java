package prod_latest;

import battlecode.common.*;

public class Soldier extends Unit {

    static SoldierStrategy strategy;

    public static MapLocation prevLoc = null;

    public static boolean tryPaint(MapLocation loc, PaintType paintType) throws GameActionException {
        if (rc.isActionReady() && rc.canAttack(loc) && rc.canPaint(loc) && rc.senseMapInfo(loc).getPaint() != paintType) {
            rc.attack(loc, paintType == PaintType.ALLY_SECONDARY);
            return true;
        }
        return false;
    }

    public static boolean tryPaintBelowSelf(PaintType paintType) throws GameActionException {
        return tryPaint(rc.getLocation(), paintType);
    }

    public static boolean tryAttack(MapLocation loc) throws GameActionException {
        if (rc.isActionReady() && rc.canAttack(loc)) {
            rc.attack(loc);
            return true;
        }
        return false;
    }

    public static boolean isEnemyTower(RobotInfo robotInfo) {
        return robotInfo.getType().isTowerType() && robotInfo.getTeam() == rc.getTeam().opponent();
    }

    public static boolean withinPattern(MapLocation center, MapLocation loc) {
        return Math.abs(center.x - loc.x) <= 2 && Math.abs(center.y - loc.y) <= 2;
    }

    public static PaintType getTowerPaintColor(MapLocation center, MapLocation loc, UnitType towerType) throws GameActionException {
        if (!withinPattern(center, loc)) {
            return PaintType.ALLY_PRIMARY;
        }
        int row = center.x - loc.x + 2;
        int col = center.y - loc.y + 2;
        boolean useSecondary = switch (towerType) {
            case LEVEL_ONE_PAINT_TOWER -> paintTowerPattern[row][col];
            case LEVEL_ONE_MONEY_TOWER -> moneyTowerPattern[row][col];
            case LEVEL_ONE_DEFENSE_TOWER -> defenseTowerPattern[row][col];
            default -> false;
        };
        return useSecondary ? PaintType.ALLY_SECONDARY : PaintType.ALLY_PRIMARY;
    }

    public static boolean getSrpUseSecondary(MapLocation loc) throws GameActionException {
        return resourcePattern[loc.x % 4][loc.y % 4];
    }

    public static PaintType getSrpPaintColor(MapLocation loc) throws GameActionException {
        return (getSrpUseSecondary(loc) ? PaintType.ALLY_SECONDARY : PaintType.ALLY_PRIMARY);
    }

    public static boolean isInSrpCenterLocation(MapLocation loc) {
        return loc.x % 4 == 2 && loc.y % 4 == 2;
    }

    static void switchStrategy(SoldierStrategy newStrategy) {
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
        if (rc.getPaint() < 60 && paintTowerLoc != null) {
            Logger.log("refilling paint");
            Logger.flush();
            strategy = new RefillPaintStrategy();
        }
        strategy.act();
        Logger.log(strategy.toString());
    }

    static abstract class SoldierStrategy extends Soldier {

        abstract public void act() throws GameActionException;
    }

    /**
     * Strategy to build a tower at specified ruin location
     */
    static class BuildTowerStrategy extends SoldierStrategy {

        public static MapLocation ruinLoc;
        public static UnitType towerType;

        BuildTowerStrategy(MapLocation _ruinLoc) {
            ruinLoc = _ruinLoc;

            int moneyTowerWeight = 8;
            int paintTowerWeight = numTowers;
            towerType = switch (randChoice(moneyTowerWeight, paintTowerWeight)) {
                case 0 ->
                    UnitType.LEVEL_ONE_MONEY_TOWER;
                default ->
                    UnitType.LEVEL_ONE_PAINT_TOWER;
            };
        }

        @Override
        public void act() throws GameActionException {
            if (ruinLoc == null || !rc.canSenseLocation(ruinLoc) || rc.senseRobotAtLocation(ruinLoc) != null || rc.getNumberTowers() == 25) { // no ruin found or ruin is completed
                yieldStrategy();
                return;
            }

            // if tower is already being built, match to be consistent
            {
                MapLocation loc = ruinLoc.add(Direction.NORTH);
                if (rc.canSenseLocation(loc)) {
                    switch (rc.senseMapInfo(loc).getMark()) {
                        case ALLY_PRIMARY:
                            towerType = UnitType.LEVEL_ONE_PAINT_TOWER;
                            break;
                        case ALLY_SECONDARY:
                            towerType = UnitType.LEVEL_ONE_MONEY_TOWER;
                            break;
                        default:
                            break;
                    }
                }
            }

            // mark the tower being built
            {
                MapLocation loc = ruinLoc.add(Direction.NORTH);
                if (rc.canSenseLocation(loc)) {
                    PaintType toMark = switch (towerType) {
                        case UnitType.LEVEL_ONE_PAINT_TOWER ->
                            PaintType.ALLY_PRIMARY;
                        case UnitType.LEVEL_ONE_MONEY_TOWER ->
                            PaintType.ALLY_SECONDARY;
                        default ->
                            PaintType.ALLY_PRIMARY;
                    };
                    if (rc.senseMapInfo(loc).getMark() != toMark && rc.canMark(loc)) {
                        rc.mark(loc, toMark == PaintType.ALLY_SECONDARY);
                    }
                }
            }

            if (!curLoc.isAdjacentTo(ruinLoc)) { // if ruin is not adjacent, walk closer
                BugNav.moveToward(ruinLoc);
                tryPaintBelowSelf(getTowerPaintColor(ruinLoc, curLoc, towerType));
            } else { // can sense ruin, so try to paint it in

                for (int i = adjacentDirections.length; --i >= 0;) {
                    MapLocation loc = ruinLoc.add(adjacentDirections[i]);
                    if (loc == rc.getLocation()) {
                        continue;
                    }
                    RobotInfo robot = rc.senseRobotAtLocation(loc);
                    if (robot != null && robot.getTeam() == myTeam && (robot.getPaintAmount() > rc.getPaint())) {
                        switchStrategy(new ExploreStrategy(8, 8));
                        return;
                    }
                }

                // walk around tower every turn to not miss any squares
                Direction dir = curLoc.directionTo(ruinLoc).rotateLeft();
                if (rc.canMove(dir)) {
                    rc.move(dir);
                    tryPaintBelowSelf(getTowerPaintColor(ruinLoc, rc.getLocation(), towerType));
                }

                // try paint tiles
                MapInfo[] actionableMapInfos = rc.senseNearbyMapInfos(rc.getLocation(), actionRadiusSquared);
                for (int i = actionableMapInfos.length; --i >= 0;) {
                    MapInfo tile = actionableMapInfos[i];
                    MapLocation loc = tile.getMapLocation();
                    if (!withinPattern(ruinLoc, loc)) {
                        continue;
                    }
                    if (tryPaint(loc, getTowerPaintColor(ruinLoc, loc, towerType))) {
                        break;
                    }
                }

                // try complete tower
                if (rc.canCompleteTowerPattern(towerType, ruinLoc)) {
                    rc.completeTowerPattern(towerType, ruinLoc);
                    yieldStrategy();
                    return;
                }

                // try to paint any tiles outside radius
                if (rc.getPaint() > 30) {
                    for (int i = actionableMapInfos.length; --i >= 0;) {
                        MapInfo tile = actionableMapInfos[i];
                        MapLocation loc = tile.getMapLocation();
                        if (withinPattern(ruinLoc, loc)) {
                            continue;
                        }
                        if (tryPaint(loc, getSrpPaintColor(loc))) {
                            break;
                        }
                    }
                }
            }
        }

        public String toString() {
            return "BuildTower " + ruinLoc + " " + towerType;
        }
    }

    /**
     * Strategy to pick a random location and wander over for X turns, and if
     * ruin is found switch to build tower strategy.
     */
    static class ExploreStrategy extends SoldierStrategy {

        public static int turnsLeft;
        public static MapLocation target;
        public static int turnsNotMoved;
        public static int targetRuinCooldown;

        ExploreStrategy(int turns) {
            this(turns, 0);
        }

        ExploreStrategy(int turns, int _targetRuinCooldown) {
            turnsLeft = turns;
            targetRuinCooldown = _targetRuinCooldown;
            turnsNotMoved = 0;
            target = new MapLocation(rng.nextInt(mapWidth), rng.nextInt(mapHeight));
        }

        @Override
        public void act() throws GameActionException {
            if (rc.getNumberTowers() < 25 && targetRuinCooldown <= 0) {
                for (int i = nearbyMapInfos.length; --i >= 0;) {
                    MapInfo tile = nearbyMapInfos[i];
                    MapLocation loc = tile.getMapLocation();
                    RobotInfo robotInfo = rc.senseRobotAtLocation(loc);
                    if (tile.hasRuin() && robotInfo == null) {
                        switchStrategy(new BuildTowerStrategy(loc));
                        return;
                    }
                }
            } else {
                targetRuinCooldown--;
            }

            // Kiting!
            // TODO: combine this into a single for loop, need too loop in max.
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapInfo tile = nearbyMapInfos[i];
                MapLocation loc = tile.getMapLocation();
                RobotInfo robotInfo = rc.senseRobotAtLocation(loc);
                if (prevLoc != null && !prevLoc.isWithinDistanceSquared(loc, actionRadiusSquared)
                        && robotInfo != null && rc.canAttack(loc) && isEnemyTower(robotInfo)) {
                    //    rc.setTimelineMarker("Kiting time!", 0, 255, 0);
                    switchStrategy(new KitingStrategy(prevLoc, curLoc, loc));
                    return;
                }
            }
            prevLoc = curLoc;

            BugNav.moveToward(target);
            boolean painted = tryPaintBelowSelf(getSrpPaintColor(rc.getLocation()));
            if (!painted) {
                MapInfo[] attackableTiles = rc.senseNearbyMapInfos(curLoc, actionRadiusSquared);
                for (int i = attackableTiles.length; --i >= 0;) {
                    MapInfo tile = attackableTiles[i];
                    MapLocation loc = tile.getMapLocation();
                    if (!tile.getPaint().isAlly() && tryPaint(loc, getSrpPaintColor(loc))) {
                        break;
                    }
                }
            }

            // try to complete any resource pattern in range
            MapInfo[] sensedTiles = rc.senseNearbyMapInfos();
            for (int i = sensedTiles.length; --i >= 0;) {
                MapInfo tile = sensedTiles[i];
                MapLocation loc = sensedTiles[i].getMapLocation();
                if (isInSrpCenterLocation(loc) && rc.canCompleteResourcePattern(loc)) {
                    rc.setIndicatorDot(loc, 0, 255, 0);
                    rc.completeResourcePattern(loc);
                }
            }

            if (rc.getLocation() == curLoc) {
                turnsNotMoved++;
                if (turnsNotMoved >= 3) {
                    yieldStrategy();
                    return;
                }
            } else {
                turnsNotMoved = 0;
            }
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
            // check if target is still alive
            RobotInfo robotInfo = rc.senseRobotAtLocation(target);
            if (robotInfo == null) {
                yieldStrategy();
                return;
            }

            if ((turnsMoved & 1) == 0) {
                tryAttack(target);
                BugNav.moveToward(outRangeLoc);
            } else {
                BugNav.moveToward(inRangeLoc);
                tryAttack(target);
            }
            turnsMoved++;
            System.out.println(toString());
        }

        public String toString() {
            return "Kiting " + outRangeLoc + " " + inRangeLoc + " " + target;
        }
    }

    static class RefillPaintStrategy extends SoldierStrategy {

        public RefillPaintStrategy() {
        }

        @Override
        public void act() throws GameActionException {
            // TODO: wait barely out-of-range until the tower has enough paint to refill.
            // Also try to spread out among the robots waiting to be refilled.
            if (paintTowerLoc == null) {
                yieldStrategy();
                return;
            }
            BugNav.moveToward(paintTowerLoc);
            if (rc.canSenseRobotAtLocation(paintTowerLoc)) {
                var paintTowerInfo = rc.senseRobotAtLocation(paintTowerLoc);
                if (!Globals.isPaintTower(paintTowerInfo.type)) {
                    paintTowerLoc = null;
                    yieldStrategy();
                    return;
                }
                var spaceToFill = Globals.paintCapacity - rc.getPaint();
                if (rc.canTransferPaint(paintTowerLoc, -spaceToFill)) {
                    rc.transferPaint(paintTowerLoc, -spaceToFill);
                    yieldStrategy();
                }
            }
        }

    }
}
