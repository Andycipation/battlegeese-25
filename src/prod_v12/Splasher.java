package prod_v12;

import battlecode.common.*;

public class Splasher extends Unit {

    public static SplasherStrategy strategy;
    public static boolean useNetwork = rng.nextInt(2) == 0;
    

    public static void switchStrategy(SplasherStrategy newStrategy, boolean acted) throws GameActionException{
        strategy = newStrategy;
        if (!acted) strategy.act();
    }

    public static void yieldStrategy() throws GameActionException{
        strategy = new ExploreStrategy();
    }

    @Override
    void play() throws GameActionException {
        if (strategy == null) {
            strategy = new ExploreStrategy();
        }
        strategy.act();
        Logger.log(strategy.toString());
        if (rc.getPaint() < 50 && paintTowerLoc != null) {
            Logger.log("refilling paint");
            Logger.flush();
            strategy = new RefillPaintStrategy(300);
        }
        // also wanna upgrade towers nearby if possible
        upgradeTowers();
    }

    static abstract class SplasherStrategy {
        abstract public void act() throws GameActionException;
    }

    static class ExploreStrategy extends SplasherStrategy {
    
        public static MapLocation target;
        public static int turnsNotMoved;
        public static int switchStrategyCooldown;
    
        public ExploreStrategy() throws GameActionException {
            turnsNotMoved = 0;

            MapLocation possibleTarget = null;
            if (informedEnemyPaintLoc != null && rc.canSenseLocation(informedEnemyPaintLoc) && rc.senseMapInfo(informedEnemyPaintLoc).getPaint().isEnemy()) {
                possibleTarget = informedEnemyPaintLoc;
            }
            if (possibleTarget != null) {
                target = project(locBeforeTurn, possibleTarget);
            }
            else {
                target = new MapLocation(rng.nextInt(mapWidth), rng.nextInt(mapHeight));
            }
        }
    
        @Override
        public void act() throws GameActionException {
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapInfo tile = nearbyMapInfos[i];
                if (tile.getPaint().isEnemy()) {
                    switchStrategy(new AggroStrategy(), false);
                    return;
                }
            }

            if (chebyshevDist(locBeforeTurn, target) <= 2) { // my target is likely outdated, reset!
                switchStrategy(new ExploreStrategy(), false);
                return;
            }

            BugNav.moveToward(target);
            rc.setIndicatorLine(rc.getLocation(), target, 0, 255, 0);
            if (rc.getLocation() == locBeforeTurn) {
                turnsNotMoved++;
                if (turnsNotMoved >= 3) {
                    yieldStrategy();
                    return;
                }
            }

            else turnsNotMoved = 0;
        }
    
        @Override
        public String toString() {
            return "Explore " + " " + target;
        }
    }



    static class AggroStrategy extends SplasherStrategy {

        public static long[] precompPointsMask = new long[4];
        public static int[] precompPoints = new int[41];
        public static FastSet splashSpotIndices = new FastSet();

        public static void precompAllPoints() {
            precompPointsMask = new long[4];
            precompPoints = new int[41];
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapInfo tile = nearbyMapInfos[i];
                MapLocation loc = tile.getMapLocation();
                MapLocation diff = loc.translate(-locBeforeTurn.x, -locBeforeTurn.y);
                if (tile.getPaint().isEnemy()) { switch ((diff.x + 4) * 9 + (diff.y + 4)) {case 2: precompPointsMask[0] += 96L; break; case 3: precompPointsMask[0] += 3171L; break; case 4: precompPointsMask[0] += 101475L; break; case 5: precompPointsMask[0] += 101379L; break; case 6: precompPointsMask[0] += 98304L; break; case 10: precompPointsMask[0] += 3145728L; break; case 11: precompPointsMask[0] += 103809120L; break; case 12: precompPointsMask[0] += 3325037667L; break; case 13: precompPointsMask[0] += 106401205347L; break; case 14: precompPointsMask[0] += 3404835425283L; break; case 15: precompPointsMask[0] += 3401614196736L; break; case 16: precompPointsMask[0] += 3298534883328L; break; case 18: precompPointsMask[0] += 105553116266496L; break; case 19: precompPointsMask[0] += 3483252839940096L; break; case 20: precompPointsMask[0] += 111569643997495392L; break; case 21: precompPointsMask[0] += 111464094102457440L; precompPointsMask[1] += 3L; break; case 22: precompPointsMask[0] += 108086497458097248L; precompPointsMask[1] += 99L; break; case 23: precompPointsMask[0] += 3404835425280L; precompPointsMask[1] += 3171L; break; case 24: precompPointsMask[0] += 3401614196736L; precompPointsMask[1] += 101472L; break; case 25: precompPointsMask[0] += 3298534883328L; precompPointsMask[1] += 101376L; break; case 26: precompPointsMask[1] += 98304L; break; case 27: precompPointsMask[0] += 105553116266496L; precompPointsMask[1] += 103809024L; break; case 28: precompPointsMask[0] += 3483252839940096L; precompPointsMask[1] += 3325034496L; break; case 29: precompPointsMask[0] += 111569643997495296L; precompPointsMask[1] += 106401103872L; break; case 30: precompPointsMask[0] += 111464094102454272L; precompPointsMask[1] += 3404835323907L; break; case 31: precompPointsMask[0] += 108086497457995776L; precompPointsMask[1] += 108954730365027L; break; case 32: precompPointsMask[0] += 3404835323904L; precompPointsMask[1] += 3486551371680867L; break; case 33: precompPointsMask[0] += 3401614098432L; precompPointsMask[1] += 111569643893787744L; break; case 34: precompPointsMask[0] += 3298534883328L; precompPointsMask[1] += 111464090777521152L; precompPointsMask[2] += 3L; break; case 35: precompPointsMask[1] += 108086391056990208L; precompPointsMask[2] += 3L; break; case 36: precompPointsMask[0] += 105553116266496L; precompPointsMask[1] += 103809024L; precompPointsMask[2] += 96L; break; case 37: precompPointsMask[0] += 3483252836794368L; precompPointsMask[1] += 3325034496L; precompPointsMask[2] += 3168L; break; case 38: precompPointsMask[0] += 111569643893686272L; precompPointsMask[1] += 106401103872L; precompPointsMask[2] += 101472L; break; case 39: precompPointsMask[0] += 111464090777419776L; precompPointsMask[1] += 3404835323907L; precompPointsMask[2] += 3247104L; break; case 40: precompPointsMask[0] += 108086391056891904L; precompPointsMask[1] += 108954730365027L; precompPointsMask[2] += 103907328L; break; case 41: precompPointsMask[1] += 3486551371680867L; precompPointsMask[2] += 3325034496L; break; case 42: precompPointsMask[1] += 111569643893787744L; precompPointsMask[2] += 106401103872L; break; case 43: precompPointsMask[1] += 111464090777521152L; precompPointsMask[2] += 106300440579L; break; case 44: precompPointsMask[1] += 108086391056990208L; precompPointsMask[2] += 103079215107L; break; case 45: precompPointsMask[1] += 103809024L; precompPointsMask[2] += 96L; break; case 46: precompPointsMask[1] += 3325034496L; precompPointsMask[2] += 3298534886496L; break; case 47: precompPointsMask[1] += 106401103872L; precompPointsMask[2] += 108851651251296L; break; case 48: precompPointsMask[1] += 3404835323904L; precompPointsMask[2] += 3486551374924800L; break; case 49: precompPointsMask[1] += 108954730364928L; precompPointsMask[2] += 111569643997593600L; break; case 50: precompPointsMask[1] += 3486551371677696L; precompPointsMask[2] += 111464094102454272L; precompPointsMask[3] += 3L; break; case 51: precompPointsMask[1] += 111569643893686272L; precompPointsMask[2] += 108086497457995776L; precompPointsMask[3] += 3L; break; case 52: precompPointsMask[1] += 111464090777419776L; precompPointsMask[2] += 106300440579L; precompPointsMask[3] += 3L; break; case 53: precompPointsMask[1] += 108086391056891904L; precompPointsMask[2] += 103079215107L; break; case 54: precompPointsMask[2] += 96L; break; case 55: precompPointsMask[2] += 3298534886496L; break; case 56: precompPointsMask[2] += 108851651251296L; precompPointsMask[3] += 96L; break; case 57: precompPointsMask[2] += 3486551374924800L; precompPointsMask[3] += 3168L; break; case 58: precompPointsMask[2] += 111569643997593600L; precompPointsMask[3] += 101472L; break; case 59: precompPointsMask[2] += 111464094102454272L; precompPointsMask[3] += 101379L; break; case 60: precompPointsMask[2] += 108086497457995776L; precompPointsMask[3] += 98307L; break; case 61: precompPointsMask[2] += 106300440576L; precompPointsMask[3] += 3L; break; case 62: precompPointsMask[2] += 103079215104L; break; case 64: precompPointsMask[2] += 3298534883328L; break; case 65: precompPointsMask[2] += 108851651149824L; precompPointsMask[3] += 96L; break; case 66: precompPointsMask[2] += 3486551371677696L; precompPointsMask[3] += 3148896L; break; case 67: precompPointsMask[2] += 111569643893686272L; precompPointsMask[3] += 3247200L; break; case 68: precompPointsMask[2] += 111464090777419776L; precompPointsMask[3] += 3247107L; break; case 69: precompPointsMask[2] += 108086391056891904L; precompPointsMask[3] += 98307L; break; case 70: precompPointsMask[3] += 3L; break; case 74: precompPointsMask[3] += 96L; break; case 75: precompPointsMask[3] += 3148896L; break; case 76: precompPointsMask[3] += 3247200L; break; case 77: precompPointsMask[3] += 3247104L; break; case 78: precompPointsMask[3] += 98304L; break; } }
                if (tile.isPassable() && tile.getPaint() == PaintType.EMPTY) { switch ((diff.x + 4) * 9 + (diff.y + 4)) {case 2: precompPointsMask[0] += 1048609L; break; case 3: precompPointsMask[0] += 33555489L; break; case 4: precompPointsMask[0] += 1073775649L; break; case 5: precompPointsMask[0] += 34359772161L; break; case 6: precompPointsMask[0] += 1099511660545L; break; case 10: precompPointsMask[0] += 35184373137440L; break; case 11: precompPointsMask[0] += 1125899941446688L; break; case 12: precompPointsMask[0] += 36028798127342625L; break; case 13: precompPointsMask[0] += 35467068449L; precompPointsMask[1] += 1L; break; case 14: precompPointsMask[0] += 1134945141793L; precompPointsMask[1] += 32L; break; case 15: precompPointsMask[0] += 1133871399936L; precompPointsMask[1] += 1024L; break; case 16: precompPointsMask[0] += 1099511660544L; precompPointsMask[1] += 32768L; break; case 18: precompPointsMask[0] += 35184373137408L; precompPointsMask[1] += 1048576L; break; case 19: precompPointsMask[0] += 1161084313534464L; precompPointsMask[1] += 33554432L; break; case 20: precompPointsMask[0] += 37189882406240288L; precompPointsMask[1] += 1073741824L; break; case 21: precompPointsMask[0] += 37154732393890848L; precompPointsMask[1] += 34359738369L; break; case 22: precompPointsMask[0] += 36029931998708769L; precompPointsMask[1] += 1099511627809L; break; case 23: precompPointsMask[0] += 1134978696192L; precompPointsMask[1] += 35184372089889L; break; case 24: precompPointsMask[0] += 1134945140736L; precompPointsMask[1] += 1125899906876448L; break; case 25: precompPointsMask[0] += 1133871366144L; precompPointsMask[1] += 36028797018997760L; break; case 26: precompPointsMask[0] += 1099511627776L; precompPointsMask[1] += 32768L; precompPointsMask[2] += 1L; break; case 27: precompPointsMask[0] += 1161084278931456L; precompPointsMask[1] += 34603008L; break; case 28: precompPointsMask[0] += 37189881298944000L; precompPointsMask[1] += 1108344832L; precompPointsMask[2] += 32L; break; case 29: precompPointsMask[0] += 37189881332498432L; precompPointsMask[1] += 35467034625L; precompPointsMask[2] += 1024L; break; case 30: precompPointsMask[0] += 37189882406240288L; precompPointsMask[1] += 1134945108001L; precompPointsMask[2] += 32768L; break; case 31: precompPointsMask[0] += 37154732392842240L; precompPointsMask[1] += 36318243456033L; precompPointsMask[2] += 1048576L; break; case 32: precompPointsMask[0] += 36029931964104704L; precompPointsMask[1] += 1162183790593057L; precompPointsMask[2] += 33554432L; break; case 33: precompPointsMask[0] += 1133871366144L; precompPointsMask[1] += 37189881297929249L; precompPointsMask[2] += 1073741824L; break; case 34: precompPointsMask[0] += 1099511627776L; precompPointsMask[1] += 37154696925840416L; precompPointsMask[2] += 34359738369L; break; case 35: precompPointsMask[1] += 36028797018997760L; precompPointsMask[2] += 1L; break; case 36: precompPointsMask[0] += 35184372088832L; precompPointsMask[1] += 1108344832L; precompPointsMask[2] += 32L; break; case 37: precompPointsMask[0] += 1161084278931456L; precompPointsMask[1] += 35468083200L; precompPointsMask[2] += 1056L; break; case 38: precompPointsMask[0] += 37189881298944000L; precompPointsMask[1] += 1134979710976L; precompPointsMask[2] += 1099511661600L; break; case 39: precompPointsMask[0] += 37154696959361024L; precompPointsMask[1] += 36319350751233L; precompPointsMask[2] += 35184373171200L; break; case 40: precompPointsMask[0] += 36028798092705792L; precompPointsMask[1] += 1162219224039457L; precompPointsMask[2] += 1125899941478400L; break; case 41: precompPointsMask[0] += 34359738368L; precompPointsMask[1] += 37191015169262625L; precompPointsMask[2] += 36028798127308800L; break; case 42: precompPointsMask[0] += 1099511627776L; precompPointsMask[1] += 37190980809557024L; precompPointsMask[2] += 35467034625L; precompPointsMask[3] += 1L; break; case 43: precompPointsMask[1] += 37189881297929216L; precompPointsMask[2] += 35433480193L; break; case 44: precompPointsMask[1] += 37154696925839360L; precompPointsMask[2] += 34359738369L; break; case 45: precompPointsMask[1] += 34603008L; precompPointsMask[2] += 1056L; break; case 46: precompPointsMask[0] += 35184372088832L; precompPointsMask[1] += 1108344832L; precompPointsMask[2] += 1099511661600L; break; case 47: precompPointsMask[0] += 1125899906842624L; precompPointsMask[1] += 35467034624L; precompPointsMask[2] += 36283884799008L; break; case 48: precompPointsMask[0] += 36028797018963968L; precompPointsMask[1] += 1134945107968L; precompPointsMask[2] += 1162183825196064L; precompPointsMask[3] += 32L; break; case 49: precompPointsMask[1] += 36318243454977L; precompPointsMask[2] += 37189882406274048L; precompPointsMask[3] += 1024L; break; case 50: precompPointsMask[1] += 1162183790559264L; precompPointsMask[2] += 37154732393922560L; precompPointsMask[3] += 32769L; break; case 51: precompPointsMask[1] += 37189881297896448L; precompPointsMask[2] += 36028832487047168L; precompPointsMask[3] += 1L; break; case 52: precompPointsMask[1] += 37154696925839360L; precompPointsMask[2] += 35467034625L; precompPointsMask[3] += 1L; break; case 53: precompPointsMask[1] += 36028797018963968L; precompPointsMask[2] += 35433480193L; break; case 54: precompPointsMask[1] += 1048576L; precompPointsMask[2] += 1099511627808L; break; case 55: precompPointsMask[1] += 33554432L; precompPointsMask[2] += 36283883717664L; break; case 56: precompPointsMask[1] += 1073741824L; precompPointsMask[2] += 1162183790593056L; precompPointsMask[3] += 32L; break; case 57: precompPointsMask[1] += 34359738368L; precompPointsMask[2] += 37190980810605568L; precompPointsMask[3] += 1056L; break; case 58: precompPointsMask[1] += 1099511627776L; precompPointsMask[2] += 37190980844158976L; precompPointsMask[3] += 1082401L; break; case 59: precompPointsMask[1] += 35184372088832L; precompPointsMask[2] += 37189882406240256L; precompPointsMask[3] += 33793L; break; case 60: precompPointsMask[1] += 1125899906842624L; precompPointsMask[2] += 37154732392841216L; precompPointsMask[3] += 32769L; break; case 61: precompPointsMask[1] += 36028797018963968L; precompPointsMask[2] += 36028832452444160L; precompPointsMask[3] += 1L; break; case 62: precompPointsMask[2] += 34359738369L; precompPointsMask[3] += 1L; break; case 64: precompPointsMask[2] += 1099511627808L; precompPointsMask[3] += 32L; break; case 65: precompPointsMask[2] += 36283883717632L; precompPointsMask[3] += 1056L; break; case 66: precompPointsMask[2] += 1162183790592000L; precompPointsMask[3] += 1082400L; break; case 67: precompPointsMask[2] += 37189881298944000L; precompPointsMask[3] += 1082400L; break; case 68: precompPointsMask[2] += 37154696959361024L; precompPointsMask[3] += 1082401L; break; case 69: precompPointsMask[2] += 36028798092705792L; precompPointsMask[3] += 33793L; break; case 70: precompPointsMask[2] += 34359738368L; precompPointsMask[3] += 32769L; break; case 74: precompPointsMask[2] += 1099511627776L; precompPointsMask[3] += 1048608L; break; case 75: precompPointsMask[2] += 35184372088832L; precompPointsMask[3] += 1049632L; break; case 76: precompPointsMask[2] += 1125899906842624L; precompPointsMask[3] += 1082400L; break; case 77: precompPointsMask[2] += 36028797018963968L; precompPointsMask[3] += 1082368L; break; case 78: precompPointsMask[3] += 1081345L; break; } }
            }

            for (int i = 0; i < 41; ++i) {
                int l = i / 12;
                int b = i % 12;
                precompPoints[i] = (int)((precompPointsMask[l] >> (5 * b)) & 0b11111);
            }
        }
        

        AggroStrategy() {
            splashSpotIndices.add(new MapLocation(0, 4)); splashSpotIndices.add(new MapLocation(1, 3)); splashSpotIndices.add(new MapLocation(1, 4)); splashSpotIndices.add(new MapLocation(1, 5)); splashSpotIndices.add(new MapLocation(2, 2)); splashSpotIndices.add(new MapLocation(2, 3)); splashSpotIndices.add(new MapLocation(2, 4)); splashSpotIndices.add(new MapLocation(2, 5)); splashSpotIndices.add(new MapLocation(2, 6)); splashSpotIndices.add(new MapLocation(3, 1)); splashSpotIndices.add(new MapLocation(3, 2)); splashSpotIndices.add(new MapLocation(3, 3)); splashSpotIndices.add(new MapLocation(3, 4)); splashSpotIndices.add(new MapLocation(3, 5)); splashSpotIndices.add(new MapLocation(3, 6)); splashSpotIndices.add(new MapLocation(3, 7)); splashSpotIndices.add(new MapLocation(4, 0)); splashSpotIndices.add(new MapLocation(4, 1)); splashSpotIndices.add(new MapLocation(4, 2)); splashSpotIndices.add(new MapLocation(4, 3)); splashSpotIndices.add(new MapLocation(4, 4)); splashSpotIndices.add(new MapLocation(4, 5)); splashSpotIndices.add(new MapLocation(4, 6)); splashSpotIndices.add(new MapLocation(4, 7)); splashSpotIndices.add(new MapLocation(4, 8)); splashSpotIndices.add(new MapLocation(5, 1)); splashSpotIndices.add(new MapLocation(5, 2)); splashSpotIndices.add(new MapLocation(5, 3)); splashSpotIndices.add(new MapLocation(5, 4)); splashSpotIndices.add(new MapLocation(5, 5)); splashSpotIndices.add(new MapLocation(5, 6)); splashSpotIndices.add(new MapLocation(5, 7)); splashSpotIndices.add(new MapLocation(6, 2)); splashSpotIndices.add(new MapLocation(6, 3)); splashSpotIndices.add(new MapLocation(6, 4)); splashSpotIndices.add(new MapLocation(6, 5)); splashSpotIndices.add(new MapLocation(6, 6)); splashSpotIndices.add(new MapLocation(7, 3)); splashSpotIndices.add(new MapLocation(7, 4)); splashSpotIndices.add(new MapLocation(7, 5)); splashSpotIndices.add(new MapLocation(8, 4));
        }

        @Override
        public void act() throws GameActionException {

            precompAllPoints();
            int curX = locBeforeTurn.x; int curY = locBeforeTurn.y;
            MapLocation[] splashSpots = {new MapLocation(curX + -4, curY + 0),new MapLocation(curX + -3, curY + -1),new MapLocation(curX + -3, curY + 0),new MapLocation(curX + -3, curY + 1),new MapLocation(curX + -2, curY + -2),new MapLocation(curX + -2, curY + -1),new MapLocation(curX + -2, curY + 0),new MapLocation(curX + -2, curY + 1),new MapLocation(curX + -2, curY + 2),new MapLocation(curX + -1, curY + -3),new MapLocation(curX + -1, curY + -2),new MapLocation(curX + -1, curY + -1),new MapLocation(curX + -1, curY + 0),new MapLocation(curX + -1, curY + 1),new MapLocation(curX + -1, curY + 2),new MapLocation(curX + -1, curY + 3),new MapLocation(curX + 0, curY + -4),new MapLocation(curX + 0, curY + -3),new MapLocation(curX + 0, curY + -2),new MapLocation(curX + 0, curY + -1),new MapLocation(curX + 0, curY + 0),new MapLocation(curX + 0, curY + 1),new MapLocation(curX + 0, curY + 2),new MapLocation(curX + 0, curY + 3),new MapLocation(curX + 0, curY + 4),new MapLocation(curX + 1, curY + -3),new MapLocation(curX + 1, curY + -2),new MapLocation(curX + 1, curY + -1),new MapLocation(curX + 1, curY + 0),new MapLocation(curX + 1, curY + 1),new MapLocation(curX + 1, curY + 2),new MapLocation(curX + 1, curY + 3),new MapLocation(curX + 2, curY + -2),new MapLocation(curX + 2, curY + -1),new MapLocation(curX + 2, curY + 0),new MapLocation(curX + 2, curY + 1),new MapLocation(curX + 2, curY + 2),new MapLocation(curX + 3, curY + -1),new MapLocation(curX + 3, curY + 0),new MapLocation(curX + 3, curY + 1),new MapLocation(curX + 4, curY + 0),};
            boolean[] attackable = new boolean[41];
            attackable[0] = withinBounds(splashSpots[0]) && rc.senseMapInfo(splashSpots[0]).isPassable();
            attackable[1] = withinBounds(splashSpots[1]) && rc.senseMapInfo(splashSpots[1]).isPassable();
            attackable[2] = withinBounds(splashSpots[2]) && rc.senseMapInfo(splashSpots[2]).isPassable();
            attackable[3] = withinBounds(splashSpots[3]) && rc.senseMapInfo(splashSpots[3]).isPassable();
            attackable[4] = withinBounds(splashSpots[4]) && rc.senseMapInfo(splashSpots[4]).isPassable();
            attackable[5] = withinBounds(splashSpots[5]) && rc.senseMapInfo(splashSpots[5]).isPassable();
            attackable[6] = withinBounds(splashSpots[6]) && rc.senseMapInfo(splashSpots[6]).isPassable();
            attackable[7] = withinBounds(splashSpots[7]) && rc.senseMapInfo(splashSpots[7]).isPassable();
            attackable[8] = withinBounds(splashSpots[8]) && rc.senseMapInfo(splashSpots[8]).isPassable();
            attackable[9] = withinBounds(splashSpots[9]) && rc.senseMapInfo(splashSpots[9]).isPassable();
            attackable[10] = withinBounds(splashSpots[10]) && rc.senseMapInfo(splashSpots[10]).isPassable();
            attackable[11] = withinBounds(splashSpots[11]) && rc.senseMapInfo(splashSpots[11]).isPassable();
            attackable[12] = withinBounds(splashSpots[12]) && rc.senseMapInfo(splashSpots[12]).isPassable();
            attackable[13] = withinBounds(splashSpots[13]) && rc.senseMapInfo(splashSpots[13]).isPassable();
            attackable[14] = withinBounds(splashSpots[14]) && rc.senseMapInfo(splashSpots[14]).isPassable();
            attackable[15] = withinBounds(splashSpots[15]) && rc.senseMapInfo(splashSpots[15]).isPassable();
            attackable[16] = withinBounds(splashSpots[16]) && rc.senseMapInfo(splashSpots[16]).isPassable();
            attackable[17] = withinBounds(splashSpots[17]) && rc.senseMapInfo(splashSpots[17]).isPassable();
            attackable[18] = withinBounds(splashSpots[18]) && rc.senseMapInfo(splashSpots[18]).isPassable();
            attackable[19] = withinBounds(splashSpots[19]) && rc.senseMapInfo(splashSpots[19]).isPassable();
            attackable[20] = withinBounds(splashSpots[20]) && rc.senseMapInfo(splashSpots[20]).isPassable();
            attackable[21] = withinBounds(splashSpots[21]) && rc.senseMapInfo(splashSpots[21]).isPassable();
            attackable[22] = withinBounds(splashSpots[22]) && rc.senseMapInfo(splashSpots[22]).isPassable();
            attackable[23] = withinBounds(splashSpots[23]) && rc.senseMapInfo(splashSpots[23]).isPassable();
            attackable[24] = withinBounds(splashSpots[24]) && rc.senseMapInfo(splashSpots[24]).isPassable();
            attackable[25] = withinBounds(splashSpots[25]) && rc.senseMapInfo(splashSpots[25]).isPassable();
            attackable[26] = withinBounds(splashSpots[26]) && rc.senseMapInfo(splashSpots[26]).isPassable();
            attackable[27] = withinBounds(splashSpots[27]) && rc.senseMapInfo(splashSpots[27]).isPassable();
            attackable[28] = withinBounds(splashSpots[28]) && rc.senseMapInfo(splashSpots[28]).isPassable();
            attackable[29] = withinBounds(splashSpots[29]) && rc.senseMapInfo(splashSpots[29]).isPassable();
            attackable[30] = withinBounds(splashSpots[30]) && rc.senseMapInfo(splashSpots[30]).isPassable();
            attackable[31] = withinBounds(splashSpots[31]) && rc.senseMapInfo(splashSpots[31]).isPassable();
            attackable[32] = withinBounds(splashSpots[32]) && rc.senseMapInfo(splashSpots[32]).isPassable();
            attackable[33] = withinBounds(splashSpots[33]) && rc.senseMapInfo(splashSpots[33]).isPassable();
            attackable[34] = withinBounds(splashSpots[34]) && rc.senseMapInfo(splashSpots[34]).isPassable();
            attackable[35] = withinBounds(splashSpots[35]) && rc.senseMapInfo(splashSpots[35]).isPassable();
            attackable[36] = withinBounds(splashSpots[36]) && rc.senseMapInfo(splashSpots[36]).isPassable();
            attackable[37] = withinBounds(splashSpots[37]) && rc.senseMapInfo(splashSpots[37]).isPassable();
            attackable[38] = withinBounds(splashSpots[38]) && rc.senseMapInfo(splashSpots[38]).isPassable();
            attackable[39] = withinBounds(splashSpots[39]) && rc.senseMapInfo(splashSpots[39]).isPassable();
            attackable[40] = withinBounds(splashSpots[40]) && rc.senseMapInfo(splashSpots[40]).isPassable();

            int bestPoints = 0;
            Direction bestMove = null;
            int bestAttack = 0;
            if (rc.canMove(Direction.DIRECTION_ORDER[0]) && !inEnemyTowerRange(locBeforeTurn.add(Direction.DIRECTION_ORDER[0]))) {if (precompPoints[6] > bestPoints && attackable[6]) { bestPoints = precompPoints[6]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 6;} if (precompPoints[11] > bestPoints && attackable[11]) { bestPoints = precompPoints[11]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 11;} if (precompPoints[12] > bestPoints && attackable[12]) { bestPoints = precompPoints[12]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 12;} if (precompPoints[13] > bestPoints && attackable[13]) { bestPoints = precompPoints[13]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 13;} if (precompPoints[18] > bestPoints && attackable[18]) { bestPoints = precompPoints[18]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 18;} if (precompPoints[19] > bestPoints && attackable[19]) { bestPoints = precompPoints[19]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 19;} if (precompPoints[20] > bestPoints && attackable[20]) { bestPoints = precompPoints[20]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 20;} if (precompPoints[21] > bestPoints && attackable[21]) { bestPoints = precompPoints[21]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 21;} if (precompPoints[22] > bestPoints && attackable[22]) { bestPoints = precompPoints[22]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 22;} if (precompPoints[27] > bestPoints && attackable[27]) { bestPoints = precompPoints[27]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 27;} if (precompPoints[28] > bestPoints && attackable[28]) { bestPoints = precompPoints[28]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 28;} if (precompPoints[29] > bestPoints && attackable[29]) { bestPoints = precompPoints[29]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 29;} if (precompPoints[34] > bestPoints && attackable[34]) { bestPoints = precompPoints[34]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 34;} }
            if (rc.canMove(Direction.DIRECTION_ORDER[1]) && !inEnemyTowerRange(locBeforeTurn.add(Direction.DIRECTION_ORDER[1]))) {if (precompPoints[2] > bestPoints && attackable[2]) { bestPoints = precompPoints[2]; bestMove = Direction.DIRECTION_ORDER[1]; bestAttack = 2;} if (precompPoints[5] > bestPoints && attackable[5]) { bestPoints = precompPoints[5]; bestMove = Direction.DIRECTION_ORDER[1]; bestAttack = 5;} if (precompPoints[6] > bestPoints && attackable[6]) { bestPoints = precompPoints[6]; bestMove = Direction.DIRECTION_ORDER[1]; bestAttack = 6;} if (precompPoints[7] > bestPoints && attackable[7]) { bestPoints = precompPoints[7]; bestMove = Direction.DIRECTION_ORDER[1]; bestAttack = 7;} if (precompPoints[10] > bestPoints && attackable[10]) { bestPoints = precompPoints[10]; bestMove = Direction.DIRECTION_ORDER[1]; bestAttack = 10;} if (precompPoints[11] > bestPoints && attackable[11]) { bestPoints = precompPoints[11]; bestMove = Direction.DIRECTION_ORDER[1]; bestAttack = 11;} if (precompPoints[12] > bestPoints && attackable[12]) { bestPoints = precompPoints[12]; bestMove = Direction.DIRECTION_ORDER[1]; bestAttack = 12;} if (precompPoints[13] > bestPoints && attackable[13]) { bestPoints = precompPoints[13]; bestMove = Direction.DIRECTION_ORDER[1]; bestAttack = 13;} if (precompPoints[14] > bestPoints && attackable[14]) { bestPoints = precompPoints[14]; bestMove = Direction.DIRECTION_ORDER[1]; bestAttack = 14;} if (precompPoints[19] > bestPoints && attackable[19]) { bestPoints = precompPoints[19]; bestMove = Direction.DIRECTION_ORDER[1]; bestAttack = 19;} if (precompPoints[20] > bestPoints && attackable[20]) { bestPoints = precompPoints[20]; bestMove = Direction.DIRECTION_ORDER[1]; bestAttack = 20;} if (precompPoints[21] > bestPoints && attackable[21]) { bestPoints = precompPoints[21]; bestMove = Direction.DIRECTION_ORDER[1]; bestAttack = 21;} if (precompPoints[28] > bestPoints && attackable[28]) { bestPoints = precompPoints[28]; bestMove = Direction.DIRECTION_ORDER[1]; bestAttack = 28;} }
            if (rc.canMove(Direction.DIRECTION_ORDER[2]) && !inEnemyTowerRange(locBeforeTurn.add(Direction.DIRECTION_ORDER[2]))) {if (precompPoints[3] > bestPoints && attackable[3]) { bestPoints = precompPoints[3]; bestMove = Direction.DIRECTION_ORDER[2]; bestAttack = 3;} if (precompPoints[6] > bestPoints && attackable[6]) { bestPoints = precompPoints[6]; bestMove = Direction.DIRECTION_ORDER[2]; bestAttack = 6;} if (precompPoints[7] > bestPoints && attackable[7]) { bestPoints = precompPoints[7]; bestMove = Direction.DIRECTION_ORDER[2]; bestAttack = 7;} if (precompPoints[8] > bestPoints && attackable[8]) { bestPoints = precompPoints[8]; bestMove = Direction.DIRECTION_ORDER[2]; bestAttack = 8;} if (precompPoints[11] > bestPoints && attackable[11]) { bestPoints = precompPoints[11]; bestMove = Direction.DIRECTION_ORDER[2]; bestAttack = 11;} if (precompPoints[12] > bestPoints && attackable[12]) { bestPoints = precompPoints[12]; bestMove = Direction.DIRECTION_ORDER[2]; bestAttack = 12;} if (precompPoints[13] > bestPoints && attackable[13]) { bestPoints = precompPoints[13]; bestMove = Direction.DIRECTION_ORDER[2]; bestAttack = 13;} if (precompPoints[14] > bestPoints && attackable[14]) { bestPoints = precompPoints[14]; bestMove = Direction.DIRECTION_ORDER[2]; bestAttack = 14;} if (precompPoints[15] > bestPoints && attackable[15]) { bestPoints = precompPoints[15]; bestMove = Direction.DIRECTION_ORDER[2]; bestAttack = 15;} if (precompPoints[20] > bestPoints && attackable[20]) { bestPoints = precompPoints[20]; bestMove = Direction.DIRECTION_ORDER[2]; bestAttack = 20;} if (precompPoints[21] > bestPoints && attackable[21]) { bestPoints = precompPoints[21]; bestMove = Direction.DIRECTION_ORDER[2]; bestAttack = 21;} if (precompPoints[22] > bestPoints && attackable[22]) { bestPoints = precompPoints[22]; bestMove = Direction.DIRECTION_ORDER[2]; bestAttack = 22;} if (precompPoints[29] > bestPoints && attackable[29]) { bestPoints = precompPoints[29]; bestMove = Direction.DIRECTION_ORDER[2]; bestAttack = 29;} }
            if (rc.canMove(Direction.DIRECTION_ORDER[3]) && !inEnemyTowerRange(locBeforeTurn.add(Direction.DIRECTION_ORDER[3]))) {if (precompPoints[7] > bestPoints && attackable[7]) { bestPoints = precompPoints[7]; bestMove = Direction.DIRECTION_ORDER[3]; bestAttack = 7;} if (precompPoints[12] > bestPoints && attackable[12]) { bestPoints = precompPoints[12]; bestMove = Direction.DIRECTION_ORDER[3]; bestAttack = 12;} if (precompPoints[13] > bestPoints && attackable[13]) { bestPoints = precompPoints[13]; bestMove = Direction.DIRECTION_ORDER[3]; bestAttack = 13;} if (precompPoints[14] > bestPoints && attackable[14]) { bestPoints = precompPoints[14]; bestMove = Direction.DIRECTION_ORDER[3]; bestAttack = 14;} if (precompPoints[19] > bestPoints && attackable[19]) { bestPoints = precompPoints[19]; bestMove = Direction.DIRECTION_ORDER[3]; bestAttack = 19;} if (precompPoints[20] > bestPoints && attackable[20]) { bestPoints = precompPoints[20]; bestMove = Direction.DIRECTION_ORDER[3]; bestAttack = 20;} if (precompPoints[21] > bestPoints && attackable[21]) { bestPoints = precompPoints[21]; bestMove = Direction.DIRECTION_ORDER[3]; bestAttack = 21;} if (precompPoints[22] > bestPoints && attackable[22]) { bestPoints = precompPoints[22]; bestMove = Direction.DIRECTION_ORDER[3]; bestAttack = 22;} if (precompPoints[23] > bestPoints && attackable[23]) { bestPoints = precompPoints[23]; bestMove = Direction.DIRECTION_ORDER[3]; bestAttack = 23;} if (precompPoints[28] > bestPoints && attackable[28]) { bestPoints = precompPoints[28]; bestMove = Direction.DIRECTION_ORDER[3]; bestAttack = 28;} if (precompPoints[29] > bestPoints && attackable[29]) { bestPoints = precompPoints[29]; bestMove = Direction.DIRECTION_ORDER[3]; bestAttack = 29;} if (precompPoints[30] > bestPoints && attackable[30]) { bestPoints = precompPoints[30]; bestMove = Direction.DIRECTION_ORDER[3]; bestAttack = 30;} if (precompPoints[35] > bestPoints && attackable[35]) { bestPoints = precompPoints[35]; bestMove = Direction.DIRECTION_ORDER[3]; bestAttack = 35;} }
            if (rc.canMove(Direction.DIRECTION_ORDER[4]) && !inEnemyTowerRange(locBeforeTurn.add(Direction.DIRECTION_ORDER[4]))) {if (precompPoints[13] > bestPoints && attackable[13]) { bestPoints = precompPoints[13]; bestMove = Direction.DIRECTION_ORDER[4]; bestAttack = 13;} if (precompPoints[20] > bestPoints && attackable[20]) { bestPoints = precompPoints[20]; bestMove = Direction.DIRECTION_ORDER[4]; bestAttack = 20;} if (precompPoints[21] > bestPoints && attackable[21]) { bestPoints = precompPoints[21]; bestMove = Direction.DIRECTION_ORDER[4]; bestAttack = 21;} if (precompPoints[22] > bestPoints && attackable[22]) { bestPoints = precompPoints[22]; bestMove = Direction.DIRECTION_ORDER[4]; bestAttack = 22;} if (precompPoints[27] > bestPoints && attackable[27]) { bestPoints = precompPoints[27]; bestMove = Direction.DIRECTION_ORDER[4]; bestAttack = 27;} if (precompPoints[28] > bestPoints && attackable[28]) { bestPoints = precompPoints[28]; bestMove = Direction.DIRECTION_ORDER[4]; bestAttack = 28;} if (precompPoints[29] > bestPoints && attackable[29]) { bestPoints = precompPoints[29]; bestMove = Direction.DIRECTION_ORDER[4]; bestAttack = 29;} if (precompPoints[30] > bestPoints && attackable[30]) { bestPoints = precompPoints[30]; bestMove = Direction.DIRECTION_ORDER[4]; bestAttack = 30;} if (precompPoints[31] > bestPoints && attackable[31]) { bestPoints = precompPoints[31]; bestMove = Direction.DIRECTION_ORDER[4]; bestAttack = 31;} if (precompPoints[34] > bestPoints && attackable[34]) { bestPoints = precompPoints[34]; bestMove = Direction.DIRECTION_ORDER[4]; bestAttack = 34;} if (precompPoints[35] > bestPoints && attackable[35]) { bestPoints = precompPoints[35]; bestMove = Direction.DIRECTION_ORDER[4]; bestAttack = 35;} if (precompPoints[36] > bestPoints && attackable[36]) { bestPoints = precompPoints[36]; bestMove = Direction.DIRECTION_ORDER[4]; bestAttack = 36;} if (precompPoints[39] > bestPoints && attackable[39]) { bestPoints = precompPoints[39]; bestMove = Direction.DIRECTION_ORDER[4]; bestAttack = 39;} }
            if (rc.canMove(Direction.DIRECTION_ORDER[5]) && !inEnemyTowerRange(locBeforeTurn.add(Direction.DIRECTION_ORDER[5]))) {if (precompPoints[12] > bestPoints && attackable[12]) { bestPoints = precompPoints[12]; bestMove = Direction.DIRECTION_ORDER[5]; bestAttack = 12;} if (precompPoints[19] > bestPoints && attackable[19]) { bestPoints = precompPoints[19]; bestMove = Direction.DIRECTION_ORDER[5]; bestAttack = 19;} if (precompPoints[20] > bestPoints && attackable[20]) { bestPoints = precompPoints[20]; bestMove = Direction.DIRECTION_ORDER[5]; bestAttack = 20;} if (precompPoints[21] > bestPoints && attackable[21]) { bestPoints = precompPoints[21]; bestMove = Direction.DIRECTION_ORDER[5]; bestAttack = 21;} if (precompPoints[26] > bestPoints && attackable[26]) { bestPoints = precompPoints[26]; bestMove = Direction.DIRECTION_ORDER[5]; bestAttack = 26;} if (precompPoints[27] > bestPoints && attackable[27]) { bestPoints = precompPoints[27]; bestMove = Direction.DIRECTION_ORDER[5]; bestAttack = 27;} if (precompPoints[28] > bestPoints && attackable[28]) { bestPoints = precompPoints[28]; bestMove = Direction.DIRECTION_ORDER[5]; bestAttack = 28;} if (precompPoints[29] > bestPoints && attackable[29]) { bestPoints = precompPoints[29]; bestMove = Direction.DIRECTION_ORDER[5]; bestAttack = 29;} if (precompPoints[30] > bestPoints && attackable[30]) { bestPoints = precompPoints[30]; bestMove = Direction.DIRECTION_ORDER[5]; bestAttack = 30;} if (precompPoints[33] > bestPoints && attackable[33]) { bestPoints = precompPoints[33]; bestMove = Direction.DIRECTION_ORDER[5]; bestAttack = 33;} if (precompPoints[34] > bestPoints && attackable[34]) { bestPoints = precompPoints[34]; bestMove = Direction.DIRECTION_ORDER[5]; bestAttack = 34;} if (precompPoints[35] > bestPoints && attackable[35]) { bestPoints = precompPoints[35]; bestMove = Direction.DIRECTION_ORDER[5]; bestAttack = 35;} if (precompPoints[38] > bestPoints && attackable[38]) { bestPoints = precompPoints[38]; bestMove = Direction.DIRECTION_ORDER[5]; bestAttack = 38;} }
            if (rc.canMove(Direction.DIRECTION_ORDER[6]) && !inEnemyTowerRange(locBeforeTurn.add(Direction.DIRECTION_ORDER[6]))) {if (precompPoints[11] > bestPoints && attackable[11]) { bestPoints = precompPoints[11]; bestMove = Direction.DIRECTION_ORDER[6]; bestAttack = 11;} if (precompPoints[18] > bestPoints && attackable[18]) { bestPoints = precompPoints[18]; bestMove = Direction.DIRECTION_ORDER[6]; bestAttack = 18;} if (precompPoints[19] > bestPoints && attackable[19]) { bestPoints = precompPoints[19]; bestMove = Direction.DIRECTION_ORDER[6]; bestAttack = 19;} if (precompPoints[20] > bestPoints && attackable[20]) { bestPoints = precompPoints[20]; bestMove = Direction.DIRECTION_ORDER[6]; bestAttack = 20;} if (precompPoints[25] > bestPoints && attackable[25]) { bestPoints = precompPoints[25]; bestMove = Direction.DIRECTION_ORDER[6]; bestAttack = 25;} if (precompPoints[26] > bestPoints && attackable[26]) { bestPoints = precompPoints[26]; bestMove = Direction.DIRECTION_ORDER[6]; bestAttack = 26;} if (precompPoints[27] > bestPoints && attackable[27]) { bestPoints = precompPoints[27]; bestMove = Direction.DIRECTION_ORDER[6]; bestAttack = 27;} if (precompPoints[28] > bestPoints && attackable[28]) { bestPoints = precompPoints[28]; bestMove = Direction.DIRECTION_ORDER[6]; bestAttack = 28;} if (precompPoints[29] > bestPoints && attackable[29]) { bestPoints = precompPoints[29]; bestMove = Direction.DIRECTION_ORDER[6]; bestAttack = 29;} if (precompPoints[32] > bestPoints && attackable[32]) { bestPoints = precompPoints[32]; bestMove = Direction.DIRECTION_ORDER[6]; bestAttack = 32;} if (precompPoints[33] > bestPoints && attackable[33]) { bestPoints = precompPoints[33]; bestMove = Direction.DIRECTION_ORDER[6]; bestAttack = 33;} if (precompPoints[34] > bestPoints && attackable[34]) { bestPoints = precompPoints[34]; bestMove = Direction.DIRECTION_ORDER[6]; bestAttack = 34;} if (precompPoints[37] > bestPoints && attackable[37]) { bestPoints = precompPoints[37]; bestMove = Direction.DIRECTION_ORDER[6]; bestAttack = 37;} }
            if (rc.canMove(Direction.DIRECTION_ORDER[7]) && !inEnemyTowerRange(locBeforeTurn.add(Direction.DIRECTION_ORDER[7]))) {if (precompPoints[5] > bestPoints && attackable[5]) { bestPoints = precompPoints[5]; bestMove = Direction.DIRECTION_ORDER[7]; bestAttack = 5;} if (precompPoints[10] > bestPoints && attackable[10]) { bestPoints = precompPoints[10]; bestMove = Direction.DIRECTION_ORDER[7]; bestAttack = 10;} if (precompPoints[11] > bestPoints && attackable[11]) { bestPoints = precompPoints[11]; bestMove = Direction.DIRECTION_ORDER[7]; bestAttack = 11;} if (precompPoints[12] > bestPoints && attackable[12]) { bestPoints = precompPoints[12]; bestMove = Direction.DIRECTION_ORDER[7]; bestAttack = 12;} if (precompPoints[17] > bestPoints && attackable[17]) { bestPoints = precompPoints[17]; bestMove = Direction.DIRECTION_ORDER[7]; bestAttack = 17;} if (precompPoints[18] > bestPoints && attackable[18]) { bestPoints = precompPoints[18]; bestMove = Direction.DIRECTION_ORDER[7]; bestAttack = 18;} if (precompPoints[19] > bestPoints && attackable[19]) { bestPoints = precompPoints[19]; bestMove = Direction.DIRECTION_ORDER[7]; bestAttack = 19;} if (precompPoints[20] > bestPoints && attackable[20]) { bestPoints = precompPoints[20]; bestMove = Direction.DIRECTION_ORDER[7]; bestAttack = 20;} if (precompPoints[21] > bestPoints && attackable[21]) { bestPoints = precompPoints[21]; bestMove = Direction.DIRECTION_ORDER[7]; bestAttack = 21;} if (precompPoints[26] > bestPoints && attackable[26]) { bestPoints = precompPoints[26]; bestMove = Direction.DIRECTION_ORDER[7]; bestAttack = 26;} if (precompPoints[27] > bestPoints && attackable[27]) { bestPoints = precompPoints[27]; bestMove = Direction.DIRECTION_ORDER[7]; bestAttack = 27;} if (precompPoints[28] > bestPoints && attackable[28]) { bestPoints = precompPoints[28]; bestMove = Direction.DIRECTION_ORDER[7]; bestAttack = 28;} if (precompPoints[33] > bestPoints && attackable[33]) { bestPoints = precompPoints[33]; bestMove = Direction.DIRECTION_ORDER[7]; bestAttack = 33;} }
            if (rc.canMove(Direction.DIRECTION_ORDER[8]) && !inEnemyTowerRange(locBeforeTurn.add(Direction.DIRECTION_ORDER[8]))) {if (precompPoints[1] > bestPoints && attackable[1]) { bestPoints = precompPoints[1]; bestMove = Direction.DIRECTION_ORDER[8]; bestAttack = 1;} if (precompPoints[4] > bestPoints && attackable[4]) { bestPoints = precompPoints[4]; bestMove = Direction.DIRECTION_ORDER[8]; bestAttack = 4;} if (precompPoints[5] > bestPoints && attackable[5]) { bestPoints = precompPoints[5]; bestMove = Direction.DIRECTION_ORDER[8]; bestAttack = 5;} if (precompPoints[6] > bestPoints && attackable[6]) { bestPoints = precompPoints[6]; bestMove = Direction.DIRECTION_ORDER[8]; bestAttack = 6;} if (precompPoints[9] > bestPoints && attackable[9]) { bestPoints = precompPoints[9]; bestMove = Direction.DIRECTION_ORDER[8]; bestAttack = 9;} if (precompPoints[10] > bestPoints && attackable[10]) { bestPoints = precompPoints[10]; bestMove = Direction.DIRECTION_ORDER[8]; bestAttack = 10;} if (precompPoints[11] > bestPoints && attackable[11]) { bestPoints = precompPoints[11]; bestMove = Direction.DIRECTION_ORDER[8]; bestAttack = 11;} if (precompPoints[12] > bestPoints && attackable[12]) { bestPoints = precompPoints[12]; bestMove = Direction.DIRECTION_ORDER[8]; bestAttack = 12;} if (precompPoints[13] > bestPoints && attackable[13]) { bestPoints = precompPoints[13]; bestMove = Direction.DIRECTION_ORDER[8]; bestAttack = 13;} if (precompPoints[18] > bestPoints && attackable[18]) { bestPoints = precompPoints[18]; bestMove = Direction.DIRECTION_ORDER[8]; bestAttack = 18;} if (precompPoints[19] > bestPoints && attackable[19]) { bestPoints = precompPoints[19]; bestMove = Direction.DIRECTION_ORDER[8]; bestAttack = 19;} if (precompPoints[20] > bestPoints && attackable[20]) { bestPoints = precompPoints[20]; bestMove = Direction.DIRECTION_ORDER[8]; bestAttack = 20;} if (precompPoints[27] > bestPoints && attackable[27]) { bestPoints = precompPoints[27]; bestMove = Direction.DIRECTION_ORDER[8]; bestAttack = 27;} }

            Logger.log("" + bestPoints);
            Logger.log("" + bestMove);
            Logger.log("" + splashSpots[bestAttack]);
            if (rc.isActionReady() && rc.getPaint() >= SPLASHER_ATTACK_COST && bestMove != null && (bestPoints >= 9 || (bestPoints >= 2 && numAllyAdjacent[Direction.CENTER.getDirectionOrderNum()] >= 3))) {
                rc.move(bestMove);
                if (!rc.canAttack(splashSpots[bestAttack])) {
                    rc.setTimelineMarker("splasher bad!", 255, 255, 0);
                }
                rc.attack(splashSpots[bestAttack]);
            }
            else {
                tryMoveToFrontier();
                tryMoveToSafeTile();
                tryMoveLessSafeTile();
            }
        }

        public String toString() {
            return "Aggro";
        }
    }
    
    static class RefillPaintStrategy extends SplasherStrategy {
        private static int refillTo;

        public RefillPaintStrategy(int _refillTo) {
            // assert(rc.getPaint() < refillTo);
            refillTo = _refillTo;
        }

        @Override
        public void act() throws GameActionException {
            if (rc.getPaint() >= refillTo) {
                yieldStrategy();
                return;
            }
            if (paintTowerLoc == null) {
                yieldStrategy();
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
                yieldStrategy();
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
                    yieldStrategy();
                }
            }
        }

        @Override
        public String toString() {
            return "RefillPaintStrategy " + paintTowerLoc;
        }

    }
}
