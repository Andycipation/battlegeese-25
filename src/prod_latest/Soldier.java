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
        static int[] precompIsSrpNotOk = new int[3];
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

        void getProject() throws GameActionException {
            // Check if we need to attack an enemy tower
            for (int i = nearbyRuins.length; --i >= 0;) {
                var robotInfo = rc.senseRobotAtLocation(nearbyRuins[i]);
                if (robotInfo != null && robotInfo.type.isTowerType() && robotInfo.team == opponentTeam) {
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
            if (roundNum % 10 == 0) {
                srpBlocked = new long[mapHeight];
                ruinBlocked = new long[mapHeight];
            }
            if (state == StrategyState.BUILDING_RUIN) {
                if (!rc.isActionReady()) {
                    return;
                }
                tryRefill(target);
            }

            getProject();

            if (state == StrategyState.BUILDING_RUIN) {
                turnsSinceInterestingActivity = 0;
                // TODO: get closest tower to being built based on the pattern and keep building it
                System.out.println("used " + Clock.getBytecodeNum());
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
                System.out.println("used " + Clock.getBytecodeNum());
                if (!isSrpOkManual(srpCenter)) {
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

                if (newLoc.distanceSquaredTo(srpCenter) <= 2 && rc.isActionReady() && !painted) {
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
                    act();
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
