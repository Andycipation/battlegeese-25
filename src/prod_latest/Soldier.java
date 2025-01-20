package prod_latest;

import java.lang.annotation.Target;

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
                case 0 -> UnitType.LEVEL_ONE_MONEY_TOWER;
                default -> UnitType.LEVEL_ONE_PAINT_TOWER;
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
                        case UnitType.LEVEL_ONE_PAINT_TOWER -> PaintType.ALLY_PRIMARY;
                        case UnitType.LEVEL_ONE_MONEY_TOWER -> PaintType.ALLY_SECONDARY;
                        default -> PaintType.ALLY_PRIMARY;
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
            // TODO: combine this into a single for loop, need to loop in max.
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapInfo tile = nearbyMapInfos[i];
                MapLocation loc = tile.getMapLocation();
                RobotInfo robotInfo = rc.senseRobotAtLocation(loc);
                if (prevLoc != null && !prevLoc.isWithinDistanceSquared(loc, actionRadiusSquared)
                        && robotInfo != null && rc.canAttack(loc) && isEnemyTower(robotInfo) && curLoc.distanceSquaredTo(prevLoc) < 4) {
                       rc.setTimelineMarker("Kiting time!", 0, 255, 0);
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
            // sometimes bugnav moves me out of bounds
            // we want to check if we're within sensible distance of target
            if (rc.getLocation().isWithinDistanceSquared(target, 20)) {
                // check if target is still alive
                RobotInfo robotInfo = rc.senseRobotAtLocation(target);
                if (robotInfo == null) {
                    yieldStrategy();
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
                        switchStrategy(new RunAwayStrategy(target, 4));
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
            var dir = BugNav.getDirectionToMove(paintTowerLoc);
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

            var paintTowerInfo = rc.senseRobotAtLocation(paintTowerLoc);
            if (!Globals.isAllyPaintTower(paintTowerInfo)) {
                System.out.println("Our paint tower got destroyed and changed to something else!");
                paintTowerLoc = null;
                yieldStrategy();
                return;
            }

            // If we wouldn't start incurring penalty from the tower, move closer
            var nextLoc = rc.getLocation().add(dir);
            if (nextLoc.distanceSquaredTo(paintTowerLoc) > GameConstants.PAINT_TRANSFER_RADIUS_SQUARED) {
                rc.move(dir);
                tryPaintBelowSelf(getSrpPaintColor(rc.getLocation()));
                return;
            }

            var spaceToFill = Globals.paintCapacity - rc.getPaint();
            if (paintTowerInfo.getPaintAmount() >= spaceToFill) {
                rc.move(dir);
                tryPaintBelowSelf(getSrpPaintColor(rc.getLocation()));
                if (rc.canTransferPaint(paintTowerLoc, -spaceToFill)) {
                    rc.transferPaint(paintTowerLoc, -spaceToFill);
                    yieldStrategy();
                }
            }
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

        // just move in the opposite direction lol
        public void act() throws GameActionException {
            if (turns == 0) {
                yieldStrategy();
                return ;
            }
            Direction dir = target.directionTo(rc.getLocation());
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
            turns--;
        }

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

        public void act() throws GameActionException {
            MapLocation curLoc = rc.getLocation();

            if (curLoc.equals(target)) {
                yieldStrategy();
                return ;
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

        public String toString() {
            return "Trolling " + target + " " + trolledLocs[trolledLocIdx];
        }
    }

}
