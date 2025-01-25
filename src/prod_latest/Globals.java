package prod_latest;

import java.util.Random;

import battlecode.common.*;

public class Globals {

    public static RobotController rc;
    public static int id;

    public static Random rng;

    public static int mapWidth;
    public static int mapHeight;

    public static UnitType unitType;
    public static int paintCapacity;
    public static int actionRadiusSquared;
    public static int visionRadiusSquared;

    public static Team myTeam;
    public static Team opponentTeam;

    public static Direction[] allDirections = Direction.values();
    public static Direction[] adjacentDirections = {
        Direction.NORTH,
        Direction.EAST,
        Direction.SOUTH,
        Direction.WEST,
        Direction.NORTHEAST,
        Direction.SOUTHEAST,
        Direction.SOUTHWEST,
        Direction.NORTHWEST
    };
    public static Direction[] cardinalDirections = {
        Direction.NORTH,
        Direction.EAST,
        Direction.SOUTH,
        Direction.WEST
    };
    public static Direction[] diagonalDirections = {
        Direction.NORTHEAST,
        Direction.SOUTHEAST,
        Direction.SOUTHWEST,
        Direction.NORTHWEST,
    };

    // not in game constants for some reason?
    public static final int SOLDIER_ATTACK_COOLDOWN = 10;
    public static final int SPLASHER_ATTACK_COOLDOWN = 10;
    public static final int SOLDIER_ATTACK_COST = 5;
    public static final int SPLASHER_ATTACK_COST = 50;

    public static boolean[][] paintTowerPattern;
    public static boolean[][] defenseTowerPattern;
    public static boolean[][] moneyTowerPattern;
    public static boolean[][] resourcePattern;

    public static MapLocation spawnLocation;

    public static boolean[][] patternToBooleanArray(int pattern) {
        boolean[][] boolArray = new boolean[5][5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                boolArray[i][j] = (1 & (pattern >> (5 * i + j))) == 1;
            }
        }
        return boolArray;
    }

    public static enum MapSize {
        SMALL, MEDIUM, LARGE,
    }
    public static MapSize mapSize;

    public static void init(RobotController _rc) {
        rc = _rc;
        id = rc.getID();
        rng = new Random(id);

        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();

        unitType = rc.getType();
        paintCapacity = unitType.paintCapacity;
        actionRadiusSquared = unitType.actionRadiusSquared;
        visionRadiusSquared = GameConstants.VISION_RADIUS_SQUARED;

        myTeam = rc.getTeam();
        opponentTeam = myTeam.opponent();

        paintTowerPattern = patternToBooleanArray(GameConstants.PAINT_TOWER_PATTERN);
        defenseTowerPattern = patternToBooleanArray(GameConstants.DEFENSE_TOWER_PATTERN);
        moneyTowerPattern = patternToBooleanArray(GameConstants.MONEY_TOWER_PATTERN);
        resourcePattern = patternToBooleanArray(GameConstants.RESOURCE_PATTERN);

        spawnLocation = rc.getLocation();

        final int mapArea = mapWidth * mapHeight;
        if (mapArea <= 900) {
            mapSize = MapSize.SMALL;
        } else if (mapArea < 1800) {
            mapSize = MapSize.MEDIUM;
        } else {
            mapSize = MapSize.LARGE;
        }
    }

    /**
     * Shuffles a direction array   
     */
    public static void shuffleArray(Direction[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);

            Direction temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

    /*
     * Check if location is in bounds
     */

    public static boolean withinBounds(MapLocation loc) {
        return 0 <= loc.x && loc.x < mapWidth && 0 <= loc.y && loc.y < mapHeight;
    }
    /**
     * Picks a random integer 0-1 with P[i] = wi / sum_i wi
     */
    public static int randChoice(int w1, int w2) {
        return rng.nextInt(w1 + w2) < w1 ? 0 : 1;
    }

    /**
     * Picks a random integer 0-2 with P[i] = wi / sum_i wi
     */
    public static int randChoice(int w1, int w2, int w3) {
        int x = rng.nextInt(w1 + w2 + w3);
        if (x < w1) return 0;
        return x < w1 + w2 ? 1 : 2;
    }

    /**
     * Picks a random integer 0-3 with P[i] = wi / sum_i wi
     */
    public static int randChoice(int w1, int w2, int w3, int w4) {
        int x = rng.nextInt(w1 + w2 + w3 + w4);
        if (x < w1) return 0;
        if (x < w1 + w2) return 1;
        return x < w1 + w2 + w3 ? 2 : 3;
    }

    /**
     * Picks a random integer 0-4 with P[i] = wi / sum_i wi
     */
    public static int randChoice(int w1, int w2, int w3, int w4, int w5) {
        int x = rng.nextInt(w1 + w2 + w3 + w4 + w5);
        if (x < w1) return 0;
        if (x < w1 + w2) return 1;
        if (x < w1 + w2 + w3) return 2;
        return x < w1 + w2 + w3 + w4 ? 3 : 4;
    }

    public static boolean withinPattern(MapLocation center, MapLocation loc) {
        return center.isWithinDistanceSquared(loc, 8);
    }
    
    /**
     * Returns whether the given type is the type of a paint tower.
     */
    public static boolean isPaintTower(UnitType type) {
        return type == UnitType.LEVEL_ONE_PAINT_TOWER || type == UnitType.LEVEL_TWO_PAINT_TOWER || type == UnitType.LEVEL_THREE_PAINT_TOWER;
    }

    public static boolean isDefenseTower(UnitType type) {
        return type == UnitType.LEVEL_ONE_DEFENSE_TOWER || type == UnitType.LEVEL_TWO_DEFENSE_TOWER || type == UnitType.LEVEL_THREE_DEFENSE_TOWER;
    }

    public static boolean isAllyPaintTower(RobotInfo info) {
        return isPaintTower(info.type) && info.team.equals(myTeam);
    }

    public static boolean isEnemyTower(RobotInfo robotInfo) {
        return robotInfo.getType().isTowerType() && robotInfo.getTeam() == rc.getTeam().opponent();
    }

    public static Direction invertDir(Direction dir) {
        int idx = dir.ordinal();
        // cardinal dir
        if (idx < 4) {
            return cardinalDirections[(idx + 2)%4];
        // diagional dir
        } else {
            idx -= 4; 
            return diagonalDirections[(idx + 2)%4];
        }
    }

    public static MapLocation invertLoc(MapLocation loc) {
        return loc.translate(mapWidth-loc.x*2 + 1, mapHeight-loc.y*2+1);
    }

    public static MapLocation reflectX(MapLocation loc) {
        return new MapLocation(mapWidth - loc.x - 1, loc.y);
    }

    public static MapLocation reflectY(MapLocation loc) {
        return new MapLocation(loc.x, mapHeight - loc.y - 1);
    }

    public static MapLocation reflectXY(MapLocation loc) {
        return new MapLocation(mapWidth - loc.x - 1, mapHeight - loc.y - 1);
    }

    public static MapLocation[] sorted(MapLocation curLoc, MapLocation[] locs) {
        int n = locs.length;
    
        MapLocation[] d0 = new MapLocation[1];
        MapLocation[] d1 = new MapLocation[8];
        MapLocation[] d2 = new MapLocation[16];
        MapLocation[] d3 = new MapLocation[24];
        MapLocation[] d4 = new MapLocation[32];
        MapLocation[] sortedLocs = new MapLocation[n];

        int idx0 = 0, idx1 = 0, idx2 = 0, idx3 = 0, idx4 = 0;
    
        for (int i = 0; i < n; i++) {
            int dist = locs[i].distanceSquaredTo(curLoc);
            if (dist <= 1) {
                d0[idx0++] = locs[i];
            } else if (dist <= 8) {
                d1[idx1++] = locs[i];
            } else if (dist <= 16) {
                d2[idx2++] = locs[i];
            } else if (dist <= 24) {
                d3[idx3++] = locs[i];
            } else if (dist <= 32) {
                d4[idx4++] = locs[i];
            }
        }
    
        int pos = 0;
        System.arraycopy(d0, 0, sortedLocs, pos, idx0);
        pos += idx0;
        System.arraycopy(d1, 0, sortedLocs, pos, idx1);
        pos += idx1;
        System.arraycopy(d2, 0, sortedLocs, pos, idx2);
        pos += idx2;
        System.arraycopy(d3, 0, sortedLocs, pos, idx3);
        pos += idx3;
        System.arraycopy(d4, 0, sortedLocs, pos, idx4);
    
        return sortedLocs;
    }

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

    public static PaintType getTowerPaintColor(MapLocation center, MapLocation loc, UnitType towerType) {
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

    public static boolean getSrpUseSecondary(MapLocation loc) {
        int dx = loc.x;
        int dy = loc.y;
        if (rc.getTeam() == Team.B) {
            dx = mapWidth - 1 - dx;
            dy = mapHeight - 1 - dy;
        }
        return resourcePattern[dx % 4][dy % 4];
    }

    public static PaintType getSrpPaintColor(MapLocation loc) {
        return (getSrpUseSecondary(loc) ? PaintType.ALLY_SECONDARY : PaintType.ALLY_PRIMARY);
    }

    public static PaintType getPatternPaintColor(MapLocation center, MapLocation loc, UnitType type) {
        if (type == null) return getSrpPaintColor(loc);
        return getTowerPaintColor(center, loc, type);
    }

    public static boolean isInSrpCenterLocation(MapLocation loc) {
        int dx = loc.x;
        int dy = loc.y;
        if (myTeam == Team.B) {
            dx = mapWidth - 1 - dx;
            dy = mapHeight - 1 - dy;
        }
        return dx % 4 == 2 && dy % 4 == 2;
    }

    public static int chebyshevDist(MapLocation a, MapLocation b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }

    // project the ray a -> b onto the border of the map, then returns this location
    // assumes a != b, written by copilot so idk how good this is lol
    public static MapLocation project(MapLocation a, MapLocation b) {
        int dx = b.x - a.x;
        int dy = b.y - a.y;
        int x = a.x;
        int y = a.y;
        if (dx == 0) {
            return new MapLocation(x, dy > 0 ? mapHeight - 1 : 0);
        }
        if (dy == 0) {
            return new MapLocation(dx > 0 ? mapWidth - 1 : 0, y);
        }
        // project, stopping on all four borders
        int x1 = (dx > 0 ? mapWidth - 1 : 0);
        int y1 = (dy > 0 ? mapHeight - 1 : 0);
        int x2 = (int)(dy > 0 ? (double)(mapHeight - 1 - y) * dx / dy + x : -y * dx / dy + x);
        int y2 = (int)(dx > 0 ? (double)(mapWidth - 1 - x) * dy / dx + y : -x * dy / dx + y);
        if (0 <= x2 && x2 < mapWidth) {
            return clamp(new MapLocation(x2, y1));
        }
        return clamp(new MapLocation(x1, y2));
    }

    public static MapLocation clamp(MapLocation loc) {
        return new MapLocation(Math.min(Math.max(0, loc.x), mapWidth - 1), Math.min(Math.max(0, loc.y), mapHeight - 1));
    }

    public static void mdir(Direction dir) throws GameActionException {
        if (dir.getDirectionOrderNum() == 0) return;
        rc.move(dir);
    }

    public static boolean isLateGame() {
        int roundThreshold = switch (mapSize) {
            case SMALL -> 60;
            case MEDIUM -> 120;
            case LARGE -> 200;
        };
        return rc.getRoundNum() >= roundThreshold;
    }
}