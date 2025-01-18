package prod_latest;

import battlecode.common.*;

public class Soldier extends Unit {

    public static SoldierStrategy strategy;

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

    public static PaintType getPaintColor(MapLocation center, MapLocation loc, UnitType towerType) throws GameActionException {
        if (!withinPattern(center, loc)) return PaintType.ALLY_PRIMARY;
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

    public static void playRandomPaint() throws GameActionException {
        for (int i = nearbyMapInfos.length; --i >= 0;) {

        }
    }

    public static void switchStrategy(SoldierStrategy newStrategy) {
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
            if (ruinLoc == null || !rc.canSenseLocation(ruinLoc) || rc.senseRobotAtLocation(ruinLoc) != null) { // no ruin found or ruin is completed
                yieldStrategy();
                return;
            }
    
            if (!curLoc.isAdjacentTo(ruinLoc)) { // if ruin is not sensed, walk closer
                BugNav.moveToward(ruinLoc);
                tryPaintBelowSelf(getPaintColor(ruinLoc, curLoc, towerType));
            }
            else { // can sense ruin, so try to paint it in
                
                // walk around tower every turn to not miss any squares
                if (roundNum % 2 == 0) {
                    Direction dir = curLoc.directionTo(ruinLoc).rotateLeft();
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                        tryPaintBelowSelf(getPaintColor(ruinLoc, rc.getLocation(), towerType));
                    }
                }
    
                // try paint tiles
                for (int i = nearbyMapInfos.length; --i >= 0;) {
                    MapInfo tile = nearbyMapInfos[i];
                    MapLocation loc = tile.getMapLocation();
                    if (!withinPattern(ruinLoc, loc)) continue;
                    tryPaint(loc, getPaintColor(ruinLoc, loc, towerType));
                }
    
                // try complete tower
                if (rc.canCompleteTowerPattern(towerType, ruinLoc)) {
                    rc.completeTowerPattern(towerType, ruinLoc);
                    yieldStrategy();
                    return;
                }
            }
        }
    
        public String toString() {
            return "BuildTower " + ruinLoc + " " + towerType;
        }
    }
    
    /**
     * Strategy to pick a random location and wander over for X turns, and if ruin is found
     * switch to build tower strategy.
     */
    static class ExploreStrategy extends SoldierStrategy {
    
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
                MapLocation loc = tile.getMapLocation();
                RobotInfo robotInfo = rc.senseRobotAtLocation(loc);
                if (tile.hasRuin() && robotInfo == null) {
                    switchStrategy(new BuildTowerStrategy(loc));
                    return;
                } 

                if (prevLoc != null && !prevLoc.isWithinDistanceSquared(loc, actionRadiusSquared)
                 && robotInfo != null && rc.canAttack(loc) && isEnemyTower(robotInfo)) {
                    // rc.setTimelineMarker("Kiting time!", 0, 255, 0);
                    switchStrategy(new KitingStrategy(prevLoc, curLoc, loc));
                    return;
                }
            }
            prevLoc = curLoc;
            BugNav.moveToward(target);
            boolean painted = tryPaintBelowSelf(PaintType.ALLY_PRIMARY);
            if (!painted) {
                MapInfo[] attackableTiles = rc.senseNearbyMapInfos(curLoc, actionRadiusSquared);
                for (int i = attackableTiles.length; --i >= 0; ) {
                    MapInfo tile = attackableTiles[i];
                    if (!tile.getPaint().isAlly() && tryPaint(tile.getMapLocation(), PaintType.ALLY_PRIMARY)) {
                        break;
                    }
                }

            }

            if (rc.getLocation() == curLoc) {
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

        public void act() throws GameActionException {
            // check if target is still alive
            RobotInfo robotInfo = rc.senseRobotAtLocation(target);
            if (robotInfo == null) {
                yieldStrategy();
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
}
