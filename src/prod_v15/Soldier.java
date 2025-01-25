package prod_v15;

import battlecode.common.*;

public class Soldier extends Unit {
    static SoldierStrategy strategy;

    @Override
    void play() throws GameActionException {
        if (strategy == null) {
            strategy = new EarlyGameStrategy();
        }
        Logger.log(strategy.toString());
        strategy.act();

        // also wanna upgrade towers nearby if possible
        upgradeTowers();
    }

    static abstract class SoldierStrategy {

        abstract public void act() throws GameActionException;
    }

    static class EarlyGameStrategy extends SoldierStrategy {
        enum StrategyState {
            BUILDING_RUIN,
            BUILDING_SRP,
            KITING,
            REFILLING, // this is different from the state: (NO_PROJECT and returning to paint tower)
            NO_PROJECT,
        }

        // The target for the current project:
        // buildingRuin - ruin location
        // buildingSrp - proposed SRP center
        // kiting - the enemy tower
        // explore - explore location
        static MapLocation exploreTarget;
        static int turnsWithoutExploreProgress;

        static MapLocation target;
        static StrategyState state;
        static int stepsOut; // for BUILDING_SRP
        static int moveOutTurn; // for KITING
        static long[] srpBlocked;
        static long[] ruinBlocked;
        static long[] srpDone;
        static int[] precompIsSrpNotOk = new int[3];

        EarlyGameStrategy() {
            srpBlocked = new long[mapHeight];
            ruinBlocked = new long[mapHeight];
            srpDone = new long[mapHeight];
            state = StrategyState.NO_PROJECT;
        }

        static UnitType getTowerToBuild() {
            // The first two are in case we drop below 2 towers
            final int[] ORDER = switch (mapSize) {
                case SMALL -> new int[]{0, 1, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 1, 1};
                case MEDIUM -> new int[]{0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0};
                default -> new int[]{0, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 2, 2};
            };

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
                if (chebyshevDist(center, tile.getMapLocation()) > 2) {
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

        static boolean isSrpOkLookup(MapLocation center, MapLocation curLoc) { // THIS IS WITH CHECKPASSABLE=1
            int x = center.x + 4 - curLoc.x;
            int y = center.y + 4 - curLoc.y;
            int bit = 9 * (x % 3) + y;
            return (1 & (precompIsSrpNotOk[x / 3] >> bit)) == 0;
        }

        static void precomputeIsSrpOk() { // THIS IS WITH CHECKPASSABLE=1
            precompIsSrpNotOk = new int[3];
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                var tile = nearbyMapInfos[i];
                if (tile.getPaint().isEnemy() || !tile.isPassable()) {
                    var diff = tile.getMapLocation().translate(-locBeforeTurn.x, -locBeforeTurn.y);
                    switch ((diff.x + 4) * 9 + (diff.y + 4)) {
                        case 2: precompIsSrpNotOk[0] |= 8142367; break; // (-4, -2)
                        case 3: precompIsSrpNotOk[0] |= 16284734; break; // (-4, -1)
                        case 4: precompIsSrpNotOk[0] |= 32569468; break; // (-4, 0)
                        case 5: precompIsSrpNotOk[0] |= 65138936; break; // (-4, 1)
                        case 6: precompIsSrpNotOk[0] |= 130277872; break; // (-4, 2)
                        case 10: precompIsSrpNotOk[0] |= 3939855; precompIsSrpNotOk[1] |= 15; break; // (-3, -3)
                        case 11: precompIsSrpNotOk[0] |= 8142367; precompIsSrpNotOk[1] |= 31; break; // (-3, -2)
                        case 12: precompIsSrpNotOk[0] |= 16284734; precompIsSrpNotOk[1] |= 62; break; // (-3, -1)
                        case 13: precompIsSrpNotOk[0] |= 32569468; precompIsSrpNotOk[1] |= 124; break; // (-3, 0)
                        case 14: precompIsSrpNotOk[0] |= 65138936; precompIsSrpNotOk[1] |= 248; break; // (-3, 1)
                        case 15: precompIsSrpNotOk[0] |= 130277872; precompIsSrpNotOk[1] |= 496; break; // (-3, 2)
                        case 16: precompIsSrpNotOk[0] |= 126075360; precompIsSrpNotOk[1] |= 480; break; // (-3, 3)
                        case 18: precompIsSrpNotOk[0] |= 1838599; precompIsSrpNotOk[1] |= 3591; break; // (-2, -4)
                        case 19: precompIsSrpNotOk[0] |= 3939855; precompIsSrpNotOk[1] |= 7695; break; // (-2, -3)
                        case 20: precompIsSrpNotOk[0] |= 8142367; precompIsSrpNotOk[1] |= 15903; break; // (-2, -2)
                        case 21: precompIsSrpNotOk[0] |= 16284734; precompIsSrpNotOk[1] |= 31806; break; // (-2, -1)
                        case 22: precompIsSrpNotOk[0] |= 32569468; precompIsSrpNotOk[1] |= 63612; break; // (-2, 0)
                        case 23: precompIsSrpNotOk[0] |= 65138936; precompIsSrpNotOk[1] |= 127224; break; // (-2, 1)
                        case 24: precompIsSrpNotOk[0] |= 130277872; precompIsSrpNotOk[1] |= 254448; break; // (-2, 2)
                        case 25: precompIsSrpNotOk[0] |= 126075360; precompIsSrpNotOk[1] |= 246240; break; // (-2, 3)
                        case 26: precompIsSrpNotOk[0] |= 117670336; precompIsSrpNotOk[1] |= 229824; break; // (-2, 4)
                        case 27: precompIsSrpNotOk[0] |= 1838592; precompIsSrpNotOk[1] |= 1838599; break; // (-1, -4)
                        case 28: precompIsSrpNotOk[0] |= 3939840; precompIsSrpNotOk[1] |= 3939855; break; // (-1, -3)
                        case 29: precompIsSrpNotOk[0] |= 8142336; precompIsSrpNotOk[1] |= 8142367; break; // (-1, -2)
                        case 30: precompIsSrpNotOk[0] |= 16284672; precompIsSrpNotOk[1] |= 16284734; break; // (-1, -1)
                        case 31: precompIsSrpNotOk[0] |= 32569344; precompIsSrpNotOk[1] |= 32569468; break; // (-1, 0)
                        case 32: precompIsSrpNotOk[0] |= 65138688; precompIsSrpNotOk[1] |= 65138936; break; // (-1, 1)
                        case 33: precompIsSrpNotOk[0] |= 130277376; precompIsSrpNotOk[1] |= 130277872; break; // (-1, 2)
                        case 34: precompIsSrpNotOk[0] |= 126074880; precompIsSrpNotOk[1] |= 126075360; break; // (-1, 3)
                        case 35: precompIsSrpNotOk[0] |= 117669888; precompIsSrpNotOk[1] |= 117670336; break; // (-1, 4)
                        case 36: precompIsSrpNotOk[0] |= 1835008; precompIsSrpNotOk[1] |= 1838599; precompIsSrpNotOk[2] |= 7; break; // (0, -4)
                        case 37: precompIsSrpNotOk[0] |= 3932160; precompIsSrpNotOk[1] |= 3939855; precompIsSrpNotOk[2] |= 15; break; // (0, -3)
                        case 38: precompIsSrpNotOk[0] |= 8126464; precompIsSrpNotOk[1] |= 8142367; precompIsSrpNotOk[2] |= 31; break; // (0, -2)
                        case 39: precompIsSrpNotOk[0] |= 16252928; precompIsSrpNotOk[1] |= 16284734; precompIsSrpNotOk[2] |= 62; break; // (0, -1)
                        case 40: precompIsSrpNotOk[0] |= 32505856; precompIsSrpNotOk[1] |= 32569468; precompIsSrpNotOk[2] |= 124; break; // (0, 0)
                        case 41: precompIsSrpNotOk[0] |= 65011712; precompIsSrpNotOk[1] |= 65138936; precompIsSrpNotOk[2] |= 248; break; // (0, 1)
                        case 42: precompIsSrpNotOk[0] |= 130023424; precompIsSrpNotOk[1] |= 130277872; precompIsSrpNotOk[2] |= 496; break; // (0, 2)
                        case 43: precompIsSrpNotOk[0] |= 125829120; precompIsSrpNotOk[1] |= 126075360; precompIsSrpNotOk[2] |= 480; break; // (0, 3)
                        case 44: precompIsSrpNotOk[0] |= 117440512; precompIsSrpNotOk[1] |= 117670336; precompIsSrpNotOk[2] |= 448; break; // (0, 4)
                        case 45: precompIsSrpNotOk[1] |= 1838599; precompIsSrpNotOk[2] |= 3591; break; // (1, -4)
                        case 46: precompIsSrpNotOk[1] |= 3939855; precompIsSrpNotOk[2] |= 7695; break; // (1, -3)
                        case 47: precompIsSrpNotOk[1] |= 8142367; precompIsSrpNotOk[2] |= 15903; break; // (1, -2)
                        case 48: precompIsSrpNotOk[1] |= 16284734; precompIsSrpNotOk[2] |= 31806; break; // (1, -1)
                        case 49: precompIsSrpNotOk[1] |= 32569468; precompIsSrpNotOk[2] |= 63612; break; // (1, 0)
                        case 50: precompIsSrpNotOk[1] |= 65138936; precompIsSrpNotOk[2] |= 127224; break; // (1, 1)
                        case 51: precompIsSrpNotOk[1] |= 130277872; precompIsSrpNotOk[2] |= 254448; break; // (1, 2)
                        case 52: precompIsSrpNotOk[1] |= 126075360; precompIsSrpNotOk[2] |= 246240; break; // (1, 3)
                        case 53: precompIsSrpNotOk[1] |= 117670336; precompIsSrpNotOk[2] |= 229824; break; // (1, 4)
                        case 54: precompIsSrpNotOk[1] |= 1838592; precompIsSrpNotOk[2] |= 1838599; break; // (2, -4)
                        case 55: precompIsSrpNotOk[1] |= 3939840; precompIsSrpNotOk[2] |= 3939855; break; // (2, -3)
                        case 56: precompIsSrpNotOk[1] |= 8142336; precompIsSrpNotOk[2] |= 8142367; break; // (2, -2)
                        case 57: precompIsSrpNotOk[1] |= 16284672; precompIsSrpNotOk[2] |= 16284734; break; // (2, -1)
                        case 58: precompIsSrpNotOk[1] |= 32569344; precompIsSrpNotOk[2] |= 32569468; break; // (2, 0)
                        case 59: precompIsSrpNotOk[1] |= 65138688; precompIsSrpNotOk[2] |= 65138936; break; // (2, 1)
                        case 60: precompIsSrpNotOk[1] |= 130277376; precompIsSrpNotOk[2] |= 130277872; break; // (2, 2)
                        case 61: precompIsSrpNotOk[1] |= 126074880; precompIsSrpNotOk[2] |= 126075360; break; // (2, 3)
                        case 62: precompIsSrpNotOk[1] |= 117669888; precompIsSrpNotOk[2] |= 117670336; break; // (2, 4)
                        case 64: precompIsSrpNotOk[1] |= 3932160; precompIsSrpNotOk[2] |= 3939855; break; // (3, -3)
                        case 65: precompIsSrpNotOk[1] |= 8126464; precompIsSrpNotOk[2] |= 8142367; break; // (3, -2)
                        case 66: precompIsSrpNotOk[1] |= 16252928; precompIsSrpNotOk[2] |= 16284734; break; // (3, -1)
                        case 67: precompIsSrpNotOk[1] |= 32505856; precompIsSrpNotOk[2] |= 32569468; break; // (3, 0)
                        case 68: precompIsSrpNotOk[1] |= 65011712; precompIsSrpNotOk[2] |= 65138936; break; // (3, 1)
                        case 69: precompIsSrpNotOk[1] |= 130023424; precompIsSrpNotOk[2] |= 130277872; break; // (3, 2)
                        case 70: precompIsSrpNotOk[1] |= 125829120; precompIsSrpNotOk[2] |= 126075360; break; // (3, 3)
                        case 74: precompIsSrpNotOk[2] |= 8142367; break; // (4, -2)
                        case 75: precompIsSrpNotOk[2] |= 16284734; break; // (4, -1)
                        case 76: precompIsSrpNotOk[2] |= 32569468; break; // (4, 0)
                        case 77: precompIsSrpNotOk[2] |= 65138936; break; // (4, 1)
                        case 78: precompIsSrpNotOk[2] |= 130277872; break; // (4, 2)
                    }
                }
            }
            for (int i = nearbyRuins.length; --i >= 0;) {
                var ruinLoc = nearbyRuins[i];
                if (rc.canSenseRobotAtLocation(ruinLoc)) continue;
                var diff = ruinLoc.translate(-locBeforeTurn.x, -locBeforeTurn.y);
                switch ((diff.x + 4) * 9 + (diff.y + 4)) {
                    case 2: precompIsSrpNotOk[0] |= 33357439; precompIsSrpNotOk[1] |= 65151; break; // (-4, -2)
                    case 3: precompIsSrpNotOk[0] |= 66977535; precompIsSrpNotOk[1] |= 130815; break; // (-4, -1)
                    case 4: precompIsSrpNotOk[0] |= 134217727; precompIsSrpNotOk[1] |= 262143; break; // (-4, 0)
                    case 5: precompIsSrpNotOk[0] |= 133955070; precompIsSrpNotOk[1] |= 261630; break; // (-4, 1)
                    case 6: precompIsSrpNotOk[0] |= 133429756; precompIsSrpNotOk[1] |= 260604; break; // (-4, 2)
                    case 10: precompIsSrpNotOk[0] |= 16547391; precompIsSrpNotOk[1] |= 16547391; break; // (-3, -3)
                    case 11: precompIsSrpNotOk[0] |= 33357439; precompIsSrpNotOk[1] |= 33357439; break; // (-3, -2)
                    case 12: precompIsSrpNotOk[0] |= 66977535; precompIsSrpNotOk[1] |= 66977535; break; // (-3, -1)
                    case 13: precompIsSrpNotOk[0] |= 134217727; precompIsSrpNotOk[1] |= 134217727; break; // (-3, 0)
                    case 14: precompIsSrpNotOk[0] |= 133955070; precompIsSrpNotOk[1] |= 133955070; break; // (-3, 1)
                    case 15: precompIsSrpNotOk[0] |= 133429756; precompIsSrpNotOk[1] |= 133429756; break; // (-3, 2)
                    case 16: precompIsSrpNotOk[0] |= 132379128; precompIsSrpNotOk[1] |= 132379128; break; // (-3, 3)
                    case 18: precompIsSrpNotOk[0] |= 8142367; precompIsSrpNotOk[1] |= 8142367; precompIsSrpNotOk[2] |= 31; break; // (-2, -4)
                    case 19: precompIsSrpNotOk[0] |= 16547391; precompIsSrpNotOk[1] |= 16547391; precompIsSrpNotOk[2] |= 63; break; // (-2, -3)
                    case 20: precompIsSrpNotOk[0] |= 33357439; precompIsSrpNotOk[1] |= 33357439; precompIsSrpNotOk[2] |= 127; break; // (-2, -2)
                    case 21: precompIsSrpNotOk[0] |= 66977535; precompIsSrpNotOk[1] |= 66977535; precompIsSrpNotOk[2] |= 255; break; // (-2, -1)
                    case 22: precompIsSrpNotOk[0] |= 134217727; precompIsSrpNotOk[1] |= 134217727; precompIsSrpNotOk[2] |= 511; break; // (-2, 0)
                    case 23: precompIsSrpNotOk[0] |= 133955070; precompIsSrpNotOk[1] |= 133955070; precompIsSrpNotOk[2] |= 510; break; // (-2, 1)
                    case 24: precompIsSrpNotOk[0] |= 133429756; precompIsSrpNotOk[1] |= 133429756; precompIsSrpNotOk[2] |= 508; break; // (-2, 2)
                    case 25: precompIsSrpNotOk[0] |= 132379128; precompIsSrpNotOk[1] |= 132379128; precompIsSrpNotOk[2] |= 504; break; // (-2, 3)
                    case 26: precompIsSrpNotOk[0] |= 130277872; precompIsSrpNotOk[1] |= 130277872; precompIsSrpNotOk[2] |= 496; break; // (-2, 4)
                    case 27: precompIsSrpNotOk[0] |= 8142367; precompIsSrpNotOk[1] |= 8142367; precompIsSrpNotOk[2] |= 15903; break; // (-1, -4)
                    case 28: precompIsSrpNotOk[0] |= 16547391; precompIsSrpNotOk[1] |= 16547391; precompIsSrpNotOk[2] |= 32319; break; // (-1, -3)
                    case 29: precompIsSrpNotOk[0] |= 33357439; precompIsSrpNotOk[1] |= 33357439; precompIsSrpNotOk[2] |= 65151; break; // (-1, -2)
                    case 30: precompIsSrpNotOk[0] |= 66977535; precompIsSrpNotOk[1] |= 66977535; precompIsSrpNotOk[2] |= 130815; break; // (-1, -1)
                    case 31: precompIsSrpNotOk[0] |= 134217727; precompIsSrpNotOk[1] |= 134217727; precompIsSrpNotOk[2] |= 262143; break; // (-1, 0)
                    case 32: precompIsSrpNotOk[0] |= 133955070; precompIsSrpNotOk[1] |= 133955070; precompIsSrpNotOk[2] |= 261630; break; // (-1, 1)
                    case 33: precompIsSrpNotOk[0] |= 133429756; precompIsSrpNotOk[1] |= 133429756; precompIsSrpNotOk[2] |= 260604; break; // (-1, 2)
                    case 34: precompIsSrpNotOk[0] |= 132379128; precompIsSrpNotOk[1] |= 132379128; precompIsSrpNotOk[2] |= 258552; break; // (-1, 3)
                    case 35: precompIsSrpNotOk[0] |= 130277872; precompIsSrpNotOk[1] |= 130277872; precompIsSrpNotOk[2] |= 254448; break; // (-1, 4)
                    case 36: precompIsSrpNotOk[0] |= 8142367; precompIsSrpNotOk[1] |= 8142367; precompIsSrpNotOk[2] |= 8142367; break; // (0, -4)
                    case 37: precompIsSrpNotOk[0] |= 16547391; precompIsSrpNotOk[1] |= 16547391; precompIsSrpNotOk[2] |= 16547391; break; // (0, -3)
                    case 38: precompIsSrpNotOk[0] |= 33357439; precompIsSrpNotOk[1] |= 33357439; precompIsSrpNotOk[2] |= 33357439; break; // (0, -2)
                    case 39: precompIsSrpNotOk[0] |= 66977535; precompIsSrpNotOk[1] |= 66977535; precompIsSrpNotOk[2] |= 66977535; break; // (0, -1)
                    case 40: precompIsSrpNotOk[0] |= 134217727; precompIsSrpNotOk[1] |= 134217727; precompIsSrpNotOk[2] |= 134217727; break; // (0, 0)
                    case 41: precompIsSrpNotOk[0] |= 133955070; precompIsSrpNotOk[1] |= 133955070; precompIsSrpNotOk[2] |= 133955070; break; // (0, 1)
                    case 42: precompIsSrpNotOk[0] |= 133429756; precompIsSrpNotOk[1] |= 133429756; precompIsSrpNotOk[2] |= 133429756; break; // (0, 2)
                    case 43: precompIsSrpNotOk[0] |= 132379128; precompIsSrpNotOk[1] |= 132379128; precompIsSrpNotOk[2] |= 132379128; break; // (0, 3)
                    case 44: precompIsSrpNotOk[0] |= 130277872; precompIsSrpNotOk[1] |= 130277872; precompIsSrpNotOk[2] |= 130277872; break; // (0, 4)
                    case 45: precompIsSrpNotOk[0] |= 8142336; precompIsSrpNotOk[1] |= 8142367; precompIsSrpNotOk[2] |= 8142367; break; // (1, -4)
                    case 46: precompIsSrpNotOk[0] |= 16547328; precompIsSrpNotOk[1] |= 16547391; precompIsSrpNotOk[2] |= 16547391; break; // (1, -3)
                    case 47: precompIsSrpNotOk[0] |= 33357312; precompIsSrpNotOk[1] |= 33357439; precompIsSrpNotOk[2] |= 33357439; break; // (1, -2)
                    case 48: precompIsSrpNotOk[0] |= 66977280; precompIsSrpNotOk[1] |= 66977535; precompIsSrpNotOk[2] |= 66977535; break; // (1, -1)
                    case 49: precompIsSrpNotOk[0] |= 134217216; precompIsSrpNotOk[1] |= 134217727; precompIsSrpNotOk[2] |= 134217727; break; // (1, 0)
                    case 50: precompIsSrpNotOk[0] |= 133954560; precompIsSrpNotOk[1] |= 133955070; precompIsSrpNotOk[2] |= 133955070; break; // (1, 1)
                    case 51: precompIsSrpNotOk[0] |= 133429248; precompIsSrpNotOk[1] |= 133429756; precompIsSrpNotOk[2] |= 133429756; break; // (1, 2)
                    case 52: precompIsSrpNotOk[0] |= 132378624; precompIsSrpNotOk[1] |= 132379128; precompIsSrpNotOk[2] |= 132379128; break; // (1, 3)
                    case 53: precompIsSrpNotOk[0] |= 130277376; precompIsSrpNotOk[1] |= 130277872; precompIsSrpNotOk[2] |= 130277872; break; // (1, 4)
                    case 54: precompIsSrpNotOk[0] |= 8126464; precompIsSrpNotOk[1] |= 8142367; precompIsSrpNotOk[2] |= 8142367; break; // (2, -4)
                    case 55: precompIsSrpNotOk[0] |= 16515072; precompIsSrpNotOk[1] |= 16547391; precompIsSrpNotOk[2] |= 16547391; break; // (2, -3)
                    case 56: precompIsSrpNotOk[0] |= 33292288; precompIsSrpNotOk[1] |= 33357439; precompIsSrpNotOk[2] |= 33357439; break; // (2, -2)
                    case 57: precompIsSrpNotOk[0] |= 66846720; precompIsSrpNotOk[1] |= 66977535; precompIsSrpNotOk[2] |= 66977535; break; // (2, -1)
                    case 58: precompIsSrpNotOk[0] |= 133955584; precompIsSrpNotOk[1] |= 134217727; precompIsSrpNotOk[2] |= 134217727; break; // (2, 0)
                    case 59: precompIsSrpNotOk[0] |= 133693440; precompIsSrpNotOk[1] |= 133955070; precompIsSrpNotOk[2] |= 133955070; break; // (2, 1)
                    case 60: precompIsSrpNotOk[0] |= 133169152; precompIsSrpNotOk[1] |= 133429756; precompIsSrpNotOk[2] |= 133429756; break; // (2, 2)
                    case 61: precompIsSrpNotOk[0] |= 132120576; precompIsSrpNotOk[1] |= 132379128; precompIsSrpNotOk[2] |= 132379128; break; // (2, 3)
                    case 62: precompIsSrpNotOk[0] |= 130023424; precompIsSrpNotOk[1] |= 130277872; precompIsSrpNotOk[2] |= 130277872; break; // (2, 4)
                    case 64: precompIsSrpNotOk[1] |= 16547391; precompIsSrpNotOk[2] |= 16547391; break; // (3, -3)
                    case 65: precompIsSrpNotOk[1] |= 33357439; precompIsSrpNotOk[2] |= 33357439; break; // (3, -2)
                    case 66: precompIsSrpNotOk[1] |= 66977535; precompIsSrpNotOk[2] |= 66977535; break; // (3, -1)
                    case 67: precompIsSrpNotOk[1] |= 134217727; precompIsSrpNotOk[2] |= 134217727; break; // (3, 0)
                    case 68: precompIsSrpNotOk[1] |= 133955070; precompIsSrpNotOk[2] |= 133955070; break; // (3, 1)
                    case 69: precompIsSrpNotOk[1] |= 133429756; precompIsSrpNotOk[2] |= 133429756; break; // (3, 2)
                    case 70: precompIsSrpNotOk[1] |= 132379128; precompIsSrpNotOk[2] |= 132379128; break; // (3, 3)
                    case 74: precompIsSrpNotOk[1] |= 33357312; precompIsSrpNotOk[2] |= 33357439; break; // (4, -2)
                    case 75: precompIsSrpNotOk[1] |= 66977280; precompIsSrpNotOk[2] |= 66977535; break; // (4, -1)
                    case 76: precompIsSrpNotOk[1] |= 134217216; precompIsSrpNotOk[2] |= 134217727; break; // (4, 0)
                    case 77: precompIsSrpNotOk[1] |= 133954560; precompIsSrpNotOk[2] |= 133955070; break; // (4, 1)
                    case 78: precompIsSrpNotOk[1] |= 133429248; precompIsSrpNotOk[2] |= 133429756; break; // (4, 2)
                }
            }
        }

        static boolean isSrpOk(MapLocation center) throws GameActionException {
            if (((srpBlocked[center.y] >> center.x) & 1) == 1) {
                return false;
            }
            if (!isSrpOkLookup(center, locBeforeTurn)) {
                srpBlocked[center.y] |= 1L << center.x;
                return false;
            }
            return true;
        }

        static boolean isSrpOkManual(MapLocation center) throws GameActionException {
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

        static MapLocation getExploreTarget() {
            return getRandomNearbyLocation(locBeforeTurn, 10, 20);
            // if (roundNum < 100) {
            //     return getRandomNearbyLocation(locBeforeTurn, 10, 20);
            // }
            // int distD = locBeforeTurn.y;
            // int distU = mapHeight - 1 - locBeforeTurn.y;
            // int distL = locBeforeTurn.x;
            // int distR = mapWidth - 1 - locBeforeTurn.x;
            // var dir = switch (randChoice(distD, distU, distL, distR)) {
            //     case 0 -> Direction.SOUTH;
            //     case 1 -> Direction.NORTH;
            //     case 2 -> Direction.WEST;
            //     case 3 -> Direction.EAST;
            //     default -> null;
            // };
            // assert(dir != null);
            // return project(locBeforeTurn, locBeforeTurn.add(dir));
        }

        void getProject() throws GameActionException {
            // Check if we need to attack an enemy tower
            if (rc.getPaint() >= 60) {
                for (int i = nearbyRuins.length; --i >= 0;) {
                    var robotInfo = rc.senseRobotAtLocation(nearbyRuins[i]);
                    if (robotInfo != null && robotInfo.type.isTowerType() && robotInfo.team == opponentTeam) {
                        state = StrategyState.KITING;
                        exploreTarget = null;
                        moveOutTurn = -1;
                        target = robotInfo.location;
                        return;
                    }
                }
            }

            final int REFILL_THRESHOLD = 5;

            if (state == StrategyState.BUILDING_RUIN || state == StrategyState.BUILDING_SRP) {
                if (paintTowerLoc != null && rc.getNumberTowers() < 10 && rc.getPaint() < REFILL_THRESHOLD) {
                    state = StrategyState.NO_PROJECT;
                    exploreTarget = paintTowerLoc;
                }
                return;
            }

            if (exploreTarget == null || turnsWithoutExploreProgress == 5 || locBeforeTurn.equals(exploreTarget)) {
                exploreTarget = getExploreTarget();
                turnsWithoutExploreProgress = 0;
            }

            if (paintTowerLoc != null && rc.getNumberTowers() < 10) {
                boolean shouldRefill = (rc.getPaint() < REFILL_THRESHOLD);
                final int distToTarget = chebyshevDist(locBeforeTurn, exploreTarget);
                final int distToRefill = chebyshevDist(locBeforeTurn, paintTowerLoc);
                shouldRefill |= (distToTarget > distToRefill && rc.getPaint() < 120 && distToRefill <= 8);
                if (shouldRefill) {
                    exploreTarget = paintTowerLoc;
                    turnsWithoutExploreProgress = 0;
                    return;
                }
            }

            // Check for nearby ruins
            if (rc.getNumberTowers() < 25) {
                boolean startBuilding = (rc.getChips() > 500 || rc.getNumberTowers() >= 5);
                if (startBuilding) {
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
            precomputeIsSrpOk();
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                var tile = nearbyMapInfos[i];
                var loc = tile.getMapLocation();
                if (isInSrpCenterLocation(loc) && isPatternOnMap(loc)) {
                    if (((srpDone[loc.y] >> loc.x) & 1) == 0 && isSrpOk(loc)) {
                        state = StrategyState.BUILDING_SRP;
                        target = loc;
                        stepsOut = 0;
                        return;
                    }
                }
            }

            // Check for nearby places to refill
            for (int i = nearbyAllyRobots.length; --i >= 0;) {
                var robotInfo = nearbyAllyRobots[i];
                if (robotInfo.team == myTeam && isWorthRefillingFrom(robotInfo)) {
                    state = StrategyState.REFILLING;
                    target = robotInfo.location;
                    return;
                }
            }
        }

        static boolean isWorthRefillingFrom(RobotInfo allyTowerInfo) {
            return rc.getPaint() < 110 && allyTowerInfo.paintAmount >= 160 - rc.getPaint();
        }

        private static MapLocation getRandomNearbyLocation(MapLocation center, int minChebyshevDist, int maxChebyshevDist) {
            int dx = rng.nextInt(minChebyshevDist, maxChebyshevDist);
            if (center.x < minChebyshevDist) {}
            else if (center.x >= mapWidth - minChebyshevDist || rng.nextInt(2) == 1) dx = -dx;
            int dy = rng.nextInt(minChebyshevDist, maxChebyshevDist);
            if (center.y < minChebyshevDist) {}
            else if (center.y >= mapHeight - minChebyshevDist || rng.nextInt(2) == 1) dy = -dy;

            return new MapLocation(Math.clamp(center.x + dx, 0, mapWidth - 1), Math.clamp(center.y + dy, 0, mapHeight - 1));
        }

        static boolean tryRefillHint(MapLocation towerLoc) throws GameActionException {
            if (rc.canSenseRobotAtLocation(towerLoc)) {
                var towerInfo = rc.senseRobotAtLocation(towerLoc);
                if (towerInfo.team == myTeam) {
                    int paintWanted = Math.min(towerInfo.paintAmount, paintCapacity - rc.getPaint());
                    if (rc.canTransferPaint(towerLoc, -paintWanted)) {
                        rc.transferPaint(towerLoc, -paintWanted);
                        return true;
                    }
                }
            }
            return false;
        }

        static boolean tryRefill() throws GameActionException {
            for (int i = nearbyAllyRobots.length; --i >= 0;) {
                var info = nearbyAllyRobots[i];
                if (info.type.isTowerType()) {
                    int paintWanted = Math.min(info.paintAmount, paintCapacity - rc.getPaint());
                    if (rc.canTransferPaint(info.location, -paintWanted)) {
                        rc.transferPaint(info.location, -paintWanted);
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
            if (roundNum % 10 == 0) {
                srpBlocked = new long[mapHeight];
                ruinBlocked = new long[mapHeight];
            }
            if (state == StrategyState.BUILDING_RUIN) {
                if (!rc.isActionReady()) {
                    return;
                }
                tryRefillHint(target);
            }

            getProject();
            switch (state) {
                case StrategyState.BUILDING_RUIN -> {
                    turnsWithoutExploreProgress = 0;
                    var ruinLoc = target;
                    if (rc.canSenseRobotAtLocation(ruinLoc)) {
                        // Tower has been finished
                        state = StrategyState.NO_PROJECT;
                        return;
                    }
                    if (!patternAppearsClear(ruinLoc, false)) {
                        // Enemy painted in the pattern
                        state = StrategyState.NO_PROJECT;
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

                case StrategyState.BUILDING_SRP -> {
                    turnsWithoutExploreProgress = 0;
                    var srpCenter = target;
                    if (!isSrpOkManual(srpCenter)) {
                        state = StrategyState.NO_PROJECT;
                        return;
                    }

                    if (!rc.getLocation().equals(srpCenter)) {
                        BugNav.moveToward(srpCenter);
                    } else {
                        if (stepsOut < 4) {
                            // Take a step out to see if the SRP location is actually bad
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

                    if (newLoc.distanceSquaredTo(srpCenter) <= 2 && rc.isActionReady() && !painted) {
                        // SRP is finished
                        state = StrategyState.NO_PROJECT;
                        srpDone[srpCenter.y] |= 1L << srpCenter.x;
                        return;
                    }

                    // Try completing the SRP
                    if (rc.canCompleteResourcePattern(srpCenter)) {
                        rc.completeResourcePattern(srpCenter);
                        state = StrategyState.NO_PROJECT;
                        srpDone[srpCenter.y] |= 1L << srpCenter.x;
                    }
                    return;
                }

                case StrategyState.KITING -> {
                    turnsWithoutExploreProgress = 0;
                    if (!rc.canSenseRobotAtLocation(target)) {
                        state = StrategyState.NO_PROJECT;
                        return;
                    }
                    var robotInfo = rc.senseRobotAtLocation(target);
                    if (robotInfo.team.equals(myTeam)) {
                        state = StrategyState.NO_PROJECT;
                        return;
                    }
                    if (tryAttack(target)) {
                        var curLoc = rc.getLocation();
                        var reflected = new MapLocation(2 * curLoc.x - target.x, 2 * curLoc.y - target.y);
                        BugNav.moveToward(reflected);
                        moveOutTurn = roundNum;
                    } else {
                        if (rc.getPaint() < 50 && moveOutTurn == roundNum - 1) {
                            // Wait one turn so we can move in and then out the very next turn
                            return;
                        }
                        BugNav.moveToward(target);
                        tryAttack(target);
                    }
                    return;
                }

                case StrategyState.REFILLING -> {
                    turnsWithoutExploreProgress = 0;
                    var towerLoc = target;
                    if (!rc.canSenseRobotAtLocation(towerLoc)) {
                        // The tower got destroyed
                        state = StrategyState.NO_PROJECT;
                        // act();
                        return;
                    }
                    var towerInfo = rc.senseRobotAtLocation(towerLoc);
                    if (!towerInfo.team.equals(myTeam)) {
                        state = StrategyState.NO_PROJECT;
                        // act();
                        return;
                    }
                    if (!isWorthRefillingFrom(towerInfo)) {
                        state = StrategyState.NO_PROJECT;
                        // act();
                        return;
                    }
                    BugNav.moveToward(towerLoc);
                    int paintWanted = Math.min(towerInfo.paintAmount, paintCapacity - rc.getPaint());
                    if (rc.canTransferPaint(towerLoc, -paintWanted)) {
                        rc.transferPaint(towerLoc, -paintWanted);
                        state = StrategyState.NO_PROJECT;
                        return;
                    }
                    tryPaintBelowSelf(getSrpPaintColor(rc.getLocation()));
                }

                case StrategyState.NO_PROJECT -> {
                    BugNav.moveToward(exploreTarget);
                    var newLoc = rc.getLocation();
                    if (newLoc.distanceSquaredTo(exploreTarget) >= locBeforeTurn.distanceSquaredTo(exploreTarget)) {
                        turnsWithoutExploreProgress += 1;
                    } else {
                        turnsWithoutExploreProgress = 0;
                    }
                    boolean painted = false;
                    if (rc.senseMapInfo(newLoc).getPaint() == PaintType.EMPTY) {
                        painted = tryPaintBelowSelf(getSrpPaintColor(newLoc));
                    }

                    if (!painted && rc.getRoundNum() >= 200) {
                        MapInfo[] attackableTiles = rc.senseNearbyMapInfos(newLoc, actionRadiusSquared);
                        for (int i = attackableTiles.length; --i >= 0;) {
                            MapInfo tile = attackableTiles[i];
                            var loc = tile.getMapLocation();
                            if (!tile.getPaint().isAlly() && tryPaint(loc, getSrpPaintColor(loc))) {
                                break;
                            }
                        }
                    }
                    // Always try to complete any resource pattern in range
                    for (int i = nearbyMapInfos.length; --i >= 0;) {
                        var loc = nearbyMapInfos[i].getMapLocation();
                        if (isInSrpCenterLocation(loc) && rc.canCompleteResourcePattern(loc)) {
                            rc.completeResourcePattern(loc);
                        }
                    }
                    tryRefill();
                }

            }

        }

        @Override
        public String toString() {
            return "EarlyGameStrategy, explore to " + exploreTarget + ", state " + state + " " + target;
        }
    }
}