package prod_v15;

import battlecode.common.*;

public class Splasher extends Unit {

    public static SplasherStrategy strategy;
    public static boolean useNetwork = rng.nextInt(2) == 0;
    

    public static void switchStrategy(SplasherStrategy newStrategy) throws GameActionException{
        strategy = newStrategy;
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
        if (rc.getPaint() < 50 && paintTowerLoc != null && rc.getNumberTowers() < 10) {
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
            if (informedEnemyPaintLoc != null && 
                informedEnemyPaintLocTimestamp >= roundNum - 30 &&
                !(rc.canSenseLocation(informedEnemyPaintLoc) && !rc.senseMapInfo(informedEnemyPaintLoc).getPaint().isEnemy())) {
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
                    switchStrategy(new AggroStrategy());
                    return;
                }
            }

            if (chebyshevDist(locBeforeTurn, target) <= 2) { // my target is likely outdated, reset!
                switchStrategy(new ExploreStrategy());
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

        public static long[] precompPointsMask = new long[5];
        public static int[] precompPoints = new int[41];
        public static FastSet splashSpotIndices = new FastSet();
        public static MapLocation target;

        public static void precompAllPoints() throws GameActionException {
            precompPointsMask = new long[5];
            precompPoints = new int[41];
            for (int i = nearbyRuins.length; --i >= 0;) {
                RobotInfo robot = rc.senseRobotAtLocation(nearbyRuins[i]);
                if (robot == null) continue;
                MapLocation loc = robot.getLocation();
                MapLocation diff = loc.translate(-locBeforeTurn.x, -locBeforeTurn.y);
                if (robot != null && robot.getTeam() == opponentTeam) { switch ((diff.x + 4) * 9 + (diff.y + 4)) {case 2: precompPointsMask[0] += 320L; break; case 3: precompPointsMask[0] += 20805L; break; case 4: precompPointsMask[0] += 1331525L; break; case 5: precompPointsMask[0] += 1331205L; break; case 6: precompPointsMask[0] += 1310720L; break; case 10: precompPointsMask[0] += 83886080L; break; case 11: precompPointsMask[0] += 5452595520L; break; case 12: precompPointsMask[0] += 349049999685L; break; case 13: precompPointsMask[0] += 22339199979845L; break; case 14: precompPointsMask[0] += 1429708714823685L; break; case 15: precompPointsMask[0] += 1429365117419520L; break; case 16: precompPointsMask[0] += 1407374883553280L; break; case 18: precompPointsMask[0] += 90071992547409920L; break; case 19: precompPointsMask[0] += 90071992631296000L; precompPointsMask[1] += 5L; break; case 20: precompPointsMask[0] += 90071998000005440L; precompPointsMask[1] += 325L; break; case 21: precompPointsMask[0] += 349049999680L; precompPointsMask[1] += 20805L; break; case 22: precompPointsMask[0] += 22339199979840L; precompPointsMask[1] += 1331520L; break; case 23: precompPointsMask[0] += 1429708714823680L; precompPointsMask[1] += 85217280L; break; case 24: precompPointsMask[0] += 1429365117419520L; precompPointsMask[1] += 5453905920L; break; case 25: precompPointsMask[0] += 1407374883553280L; precompPointsMask[1] += 5452595200L; break; case 26: precompPointsMask[1] += 5368709120L; break; case 27: precompPointsMask[0] += 90071992547409920L; precompPointsMask[1] += 22333829939200L; break; case 28: precompPointsMask[0] += 90071992631296000L; precompPointsMask[1] += 1429708713492485L; break; case 29: precompPointsMask[0] += 90071998000005120L; precompPointsMask[1] += 91501357663519045L; break; case 30: precompPointsMask[0] += 349049978880L; precompPointsMask[1] += 91479367430984005L; precompPointsMask[2] += 5L; break; case 31: precompPointsMask[0] += 22339198648320L; precompPointsMask[1] += 90071992548741440L; precompPointsMask[2] += 325L; break; case 32: precompPointsMask[0] += 1429708713492480L; precompPointsMask[1] += 85217280L; precompPointsMask[2] += 20805L; break; case 33: precompPointsMask[0] += 1429365116108800L; precompPointsMask[1] += 5453905920L; precompPointsMask[2] += 1331520L; break; case 34: precompPointsMask[0] += 1407374883553280L; precompPointsMask[1] += 5452595200L; precompPointsMask[2] += 85217280L; break; case 35: precompPointsMask[1] += 5368709120L; precompPointsMask[2] += 85196800L; break; case 36: precompPointsMask[0] += 90071992547409920L; precompPointsMask[1] += 22333829939200L; precompPointsMask[2] += 5368709120L; break; case 37: precompPointsMask[0] += 90071992547409920L; precompPointsMask[1] += 1429708713492485L; precompPointsMask[2] += 348966092800L; break; case 38: precompPointsMask[0] += 90071992547409920L; precompPointsMask[1] += 91501357663519045L; precompPointsMask[2] += 22339198648320L; break; case 39: precompPointsMask[1] += 91479367430984005L; precompPointsMask[2] += 1429708713492485L; break; case 40: precompPointsMask[1] += 90071992548741440L; precompPointsMask[2] += 91501357663519045L; break; case 41: precompPointsMask[1] += 85217280L; precompPointsMask[2] += 91479367430984005L; precompPointsMask[3] += 5L; break; case 42: precompPointsMask[1] += 5453905920L; precompPointsMask[2] += 90071992548741440L; precompPointsMask[3] += 325L; break; case 43: precompPointsMask[1] += 5452595200L; precompPointsMask[2] += 85217280L; precompPointsMask[3] += 325L; break; case 44: precompPointsMask[1] += 5368709120L; precompPointsMask[2] += 85196800L; precompPointsMask[3] += 320L; break; case 45: precompPointsMask[1] += 22333829939200L; precompPointsMask[2] += 5368709120L; break; case 46: precompPointsMask[1] += 1429708713492480L; precompPointsMask[2] += 348966092800L; precompPointsMask[3] += 20480L; break; case 47: precompPointsMask[1] += 91501357663518720L; precompPointsMask[2] += 22339198648320L; precompPointsMask[3] += 1331200L; break; case 48: precompPointsMask[1] += 91479367430963200L; precompPointsMask[2] += 1429708713492485L; precompPointsMask[3] += 85217280L; break; case 49: precompPointsMask[1] += 90071992547409920L; precompPointsMask[2] += 91501357663519045L; precompPointsMask[3] += 5453905920L; break; case 50: precompPointsMask[2] += 91479367430984005L; precompPointsMask[3] += 349049978885L; break; case 51: precompPointsMask[2] += 90071992548741440L; precompPointsMask[3] += 348966093125L; break; case 52: precompPointsMask[2] += 85217280L; precompPointsMask[3] += 343597384005L; break; case 53: precompPointsMask[2] += 85196800L; precompPointsMask[3] += 320L; break; case 54: precompPointsMask[2] += 5368709120L; break; case 55: precompPointsMask[2] += 348966092800L; precompPointsMask[3] += 20480L; break; case 56: precompPointsMask[2] += 22339198648320L; precompPointsMask[3] += 21990233886720L; break; case 57: precompPointsMask[2] += 1429708713492480L; precompPointsMask[3] += 1429365201326080L; break; case 58: precompPointsMask[2] += 91501357663518720L; precompPointsMask[3] += 91501363117424640L; break; case 59: precompPointsMask[2] += 91479367430963200L; precompPointsMask[3] += 91479716480942085L; break; case 60: precompPointsMask[2] += 90071992547409920L; precompPointsMask[3] += 90072341513503045L; break; case 61: precompPointsMask[3] += 343597384005L; break; case 62: precompPointsMask[3] += 320L; break; case 64: precompPointsMask[3] += 20480L; break; case 65: precompPointsMask[3] += 21990233886720L; break; case 66: precompPointsMask[3] += 1429365201326080L; precompPointsMask[4] += 5L; break; case 67: precompPointsMask[3] += 91501363117424640L; precompPointsMask[4] += 5L; break; case 68: precompPointsMask[3] += 91479716480942080L; precompPointsMask[4] += 5L; break; case 69: precompPointsMask[3] += 90072341513502720L; break; case 70: precompPointsMask[3] += 343597383680L; break; case 74: precompPointsMask[3] += 21990232555520L; break; case 75: precompPointsMask[3] += 1429365116108800L; precompPointsMask[4] += 5L; break; case 76: precompPointsMask[3] += 91501357663518720L; precompPointsMask[4] += 5L; break; case 77: precompPointsMask[3] += 91479367430963200L; precompPointsMask[4] += 5L; break; case 78: precompPointsMask[3] += 90071992547409920L; break; } }
            }
            for (int i = nearbyMapInfos.length; --i >= 0;) {
                MapInfo tile = nearbyMapInfos[i];
                MapLocation loc = tile.getMapLocation();
                MapLocation diff = loc.translate(-locBeforeTurn.x, -locBeforeTurn.y);
                if (tile.getPaint().isEnemy()) { switch ((diff.x + 4) * 9 + (diff.y + 4)) {case 2: precompPointsMask[0] += 192L; break; case 3: precompPointsMask[0] += 12483L; break; case 4: precompPointsMask[0] += 798915L; break; case 5: precompPointsMask[0] += 798723L; break; case 6: precompPointsMask[0] += 786432L; break; case 10: precompPointsMask[0] += 50331648L; break; case 11: precompPointsMask[0] += 3271557312L; break; case 12: precompPointsMask[0] += 209429999811L; break; case 13: precompPointsMask[0] += 13403519987907L; break; case 14: precompPointsMask[0] += 857825228894211L; break; case 15: precompPointsMask[0] += 857619070451712L; break; case 16: precompPointsMask[0] += 844424930131968L; break; case 18: precompPointsMask[0] += 54043195528445952L; break; case 19: precompPointsMask[0] += 54043195578777600L; precompPointsMask[1] += 3L; break; case 20: precompPointsMask[0] += 54043198800003264L; precompPointsMask[1] += 195L; break; case 21: precompPointsMask[0] += 209429999808L; precompPointsMask[1] += 12483L; break; case 22: precompPointsMask[0] += 13403519987904L; precompPointsMask[1] += 798912L; break; case 23: precompPointsMask[0] += 857825228894208L; precompPointsMask[1] += 51130368L; break; case 24: precompPointsMask[0] += 857619070451712L; precompPointsMask[1] += 3272343552L; break; case 25: precompPointsMask[0] += 844424930131968L; precompPointsMask[1] += 3271557120L; break; case 26: precompPointsMask[1] += 3221225472L; break; case 27: precompPointsMask[0] += 54043195528445952L; precompPointsMask[1] += 13400297963520L; break; case 28: precompPointsMask[0] += 54043195578777600L; precompPointsMask[1] += 857825228095491L; break; case 29: precompPointsMask[0] += 54043198800003072L; precompPointsMask[1] += 54900814598111427L; break; case 30: precompPointsMask[0] += 209429987328L; precompPointsMask[1] += 54887620458590403L; precompPointsMask[2] += 3L; break; case 31: precompPointsMask[0] += 13403519188992L; precompPointsMask[1] += 54043195529244864L; precompPointsMask[2] += 195L; break; case 32: precompPointsMask[0] += 857825228095488L; precompPointsMask[1] += 51130368L; precompPointsMask[2] += 12483L; break; case 33: precompPointsMask[0] += 857619069665280L; precompPointsMask[1] += 3272343552L; precompPointsMask[2] += 798912L; break; case 34: precompPointsMask[0] += 844424930131968L; precompPointsMask[1] += 3271557120L; precompPointsMask[2] += 51130368L; break; case 35: precompPointsMask[1] += 3221225472L; precompPointsMask[2] += 51118080L; break; case 36: precompPointsMask[0] += 54043195528445952L; precompPointsMask[1] += 13400297963520L; precompPointsMask[2] += 3221225472L; break; case 37: precompPointsMask[0] += 54043195528445952L; precompPointsMask[1] += 857825228095491L; precompPointsMask[2] += 209379655680L; break; case 38: precompPointsMask[0] += 54043195528445952L; precompPointsMask[1] += 54900814598111427L; precompPointsMask[2] += 13403519188992L; break; case 39: precompPointsMask[1] += 54887620458590403L; precompPointsMask[2] += 857825228095491L; break; case 40: precompPointsMask[1] += 54043195529244864L; precompPointsMask[2] += 54900814598111427L; break; case 41: precompPointsMask[1] += 51130368L; precompPointsMask[2] += 54887620458590403L; precompPointsMask[3] += 3L; break; case 42: precompPointsMask[1] += 3272343552L; precompPointsMask[2] += 54043195529244864L; precompPointsMask[3] += 195L; break; case 43: precompPointsMask[1] += 3271557120L; precompPointsMask[2] += 51130368L; precompPointsMask[3] += 195L; break; case 44: precompPointsMask[1] += 3221225472L; precompPointsMask[2] += 51118080L; precompPointsMask[3] += 192L; break; case 45: precompPointsMask[1] += 13400297963520L; precompPointsMask[2] += 3221225472L; break; case 46: precompPointsMask[1] += 857825228095488L; precompPointsMask[2] += 209379655680L; precompPointsMask[3] += 12288L; break; case 47: precompPointsMask[1] += 54900814598111232L; precompPointsMask[2] += 13403519188992L; precompPointsMask[3] += 798720L; break; case 48: precompPointsMask[1] += 54887620458577920L; precompPointsMask[2] += 857825228095491L; precompPointsMask[3] += 51130368L; break; case 49: precompPointsMask[1] += 54043195528445952L; precompPointsMask[2] += 54900814598111427L; precompPointsMask[3] += 3272343552L; break; case 50: precompPointsMask[2] += 54887620458590403L; precompPointsMask[3] += 209429987331L; break; case 51: precompPointsMask[2] += 54043195529244864L; precompPointsMask[3] += 209379655875L; break; case 52: precompPointsMask[2] += 51130368L; precompPointsMask[3] += 206158430403L; break; case 53: precompPointsMask[2] += 51118080L; precompPointsMask[3] += 192L; break; case 54: precompPointsMask[2] += 3221225472L; break; case 55: precompPointsMask[2] += 209379655680L; precompPointsMask[3] += 12288L; break; case 56: precompPointsMask[2] += 13403519188992L; precompPointsMask[3] += 13194140332032L; break; case 57: precompPointsMask[2] += 857825228095488L; precompPointsMask[3] += 857619120795648L; break; case 58: precompPointsMask[2] += 54900814598111232L; precompPointsMask[3] += 54900817870454784L; break; case 59: precompPointsMask[2] += 54887620458577920L; precompPointsMask[3] += 54887829888565251L; break; case 60: precompPointsMask[2] += 54043195528445952L; precompPointsMask[3] += 54043404908101827L; break; case 61: precompPointsMask[3] += 206158430403L; break; case 62: precompPointsMask[3] += 192L; break; case 64: precompPointsMask[3] += 12288L; break; case 65: precompPointsMask[3] += 13194140332032L; break; case 66: precompPointsMask[3] += 857619120795648L; precompPointsMask[4] += 3L; break; case 67: precompPointsMask[3] += 54900817870454784L; precompPointsMask[4] += 3L; break; case 68: precompPointsMask[3] += 54887829888565248L; precompPointsMask[4] += 3L; break; case 69: precompPointsMask[3] += 54043404908101632L; break; case 70: precompPointsMask[3] += 206158430208L; break; case 74: precompPointsMask[3] += 13194139533312L; break; case 75: precompPointsMask[3] += 857619069665280L; precompPointsMask[4] += 3L; break; case 76: precompPointsMask[3] += 54900814598111232L; precompPointsMask[4] += 3L; break; case 77: precompPointsMask[3] += 54887620458577920L; precompPointsMask[4] += 3L; break; case 78: precompPointsMask[3] += 54043195528445952L; break; } }
                if (tile.isPassable() && tile.getPaint() == PaintType.EMPTY) { switch ((diff.x + 4) * 9 + (diff.y + 4)) {case 2: precompPointsMask[0] += 16777281L; break; case 3: precompPointsMask[0] += 1073745985L; break; case 4: precompPointsMask[0] += 68719743041L; break; case 5: precompPointsMask[0] += 4398046777345L; break; case 6: precompPointsMask[0] += 281474976972801L; break; case 10: precompPointsMask[0] += 18014398526259264L; break; case 11: precompPointsMask[0] += 1090523200L; precompPointsMask[1] += 1L; break; case 12: precompPointsMask[0] += 69810262081L; precompPointsMask[1] += 64L; break; case 13: precompPointsMask[0] += 4467839995969L; precompPointsMask[1] += 4096L; break; case 14: precompPointsMask[0] += 285941742964801L; precompPointsMask[1] += 262144L; break; case 15: precompPointsMask[0] += 285873023488000L; precompPointsMask[1] += 16777216L; break; case 16: precompPointsMask[0] += 281474976972800L; precompPointsMask[1] += 1073741824L; break; case 18: precompPointsMask[0] += 18014398526259200L; precompPointsMask[1] += 68719476736L; break; case 19: precompPointsMask[0] += 18014399600001024L; precompPointsMask[1] += 4398046511105L; break; case 20: precompPointsMask[0] += 18014468319477824L; precompPointsMask[1] += 281474976710721L; break; case 21: precompPointsMask[0] += 4467856511040L; precompPointsMask[1] += 18014398509486145L; break; case 22: precompPointsMask[0] += 285942833483841L; precompPointsMask[1] += 266304L; precompPointsMask[2] += 1L; break; case 23: precompPointsMask[0] += 285942816706560L; precompPointsMask[1] += 17043456L; precompPointsMask[2] += 64L; break; case 24: precompPointsMask[0] += 285941742960640L; precompPointsMask[1] += 1090781184L; precompPointsMask[2] += 4096L; break; case 25: precompPointsMask[0] += 285873023221760L; precompPointsMask[1] += 1090519040L; precompPointsMask[2] += 262144L; break; case 26: precompPointsMask[0] += 281474976710656L; precompPointsMask[1] += 1073741824L; precompPointsMask[2] += 16777216L; break; case 27: precompPointsMask[0] += 18014398509481984L; precompPointsMask[1] += 4466765987841L; break; case 28: precompPointsMask[0] += 18014398526259200L; precompPointsMask[1] += 285941742698561L; precompPointsMask[2] += 1073741824L; break; case 29: precompPointsMask[0] += 18014399600001024L; precompPointsMask[1] += 18300271532707905L; precompPointsMask[2] += 68719476736L; break; case 30: precompPointsMask[0] += 18014468319477824L; precompPointsMask[1] += 18295873486458945L; precompPointsMask[2] += 4398046511105L; break; case 31: precompPointsMask[0] += 4467839733760L; precompPointsMask[1] += 18014398526525505L; precompPointsMask[2] += 281474976710721L; break; case 32: precompPointsMask[0] += 285941742960640L; precompPointsMask[1] += 1090785344L; precompPointsMask[2] += 18014398509486145L; break; case 33: precompPointsMask[0] += 285873023221760L; precompPointsMask[1] += 1090785280L; precompPointsMask[2] += 266304L; precompPointsMask[3] += 1L; break; case 34: precompPointsMask[0] += 281474976710656L; precompPointsMask[1] += 1090781184L; precompPointsMask[2] += 17043456L; precompPointsMask[3] += 64L; break; case 35: precompPointsMask[1] += 1090519040L; precompPointsMask[2] += 17039360L; break; case 36: precompPointsMask[0] += 18014398509481984L; precompPointsMask[1] += 285941742698496L; precompPointsMask[2] += 1073741824L; break; case 37: precompPointsMask[0] += 18014398509481984L; precompPointsMask[1] += 18300340252180481L; precompPointsMask[2] += 69793218560L; break; case 38: precompPointsMask[0] += 18014398526259200L; precompPointsMask[1] += 18300340252180545L; precompPointsMask[2] += 4467839729665L; precompPointsMask[3] += 4096L; break; case 39: precompPointsMask[0] += 1073741824L; precompPointsMask[1] += 18300271532707905L; precompPointsMask[2] += 285941742698561L; precompPointsMask[3] += 262144L; break; case 40: precompPointsMask[0] += 68719476736L; precompPointsMask[1] += 18295873486458944L; precompPointsMask[2] += 18300271532707905L; precompPointsMask[3] += 16777216L; break; case 41: precompPointsMask[0] += 4398046511104L; precompPointsMask[1] += 18014398526525440L; precompPointsMask[2] += 18295873486458945L; precompPointsMask[3] += 1073741825L; break; case 42: precompPointsMask[0] += 281474976710656L; precompPointsMask[1] += 1090781184L; precompPointsMask[2] += 18014398526525505L; precompPointsMask[3] += 68719476801L; break; case 43: precompPointsMask[1] += 1090519040L; precompPointsMask[2] += 17043520L; precompPointsMask[3] += 65L; break; case 44: precompPointsMask[1] += 1073741824L; precompPointsMask[2] += 17043456L; precompPointsMask[3] += 64L; break; case 45: precompPointsMask[1] += 4466765987840L; precompPointsMask[2] += 69793218560L; break; case 46: precompPointsMask[0] += 18014398509481984L; precompPointsMask[1] += 285941742698496L; precompPointsMask[2] += 4467839729664L; precompPointsMask[3] += 4096L; break; case 47: precompPointsMask[1] += 18300271532703745L; precompPointsMask[2] += 285942816440320L; precompPointsMask[3] += 266240L; break; case 48: precompPointsMask[1] += 18295873486192704L; precompPointsMask[2] += 18300341325922305L; precompPointsMask[3] += 4398063554560L; break; case 49: precompPointsMask[1] += 18014398509486080L; precompPointsMask[2] += 18300340252180545L; precompPointsMask[3] += 281476067491841L; break; case 50: precompPointsMask[1] += 262144L; precompPointsMask[2] += 18300271532707905L; precompPointsMask[3] += 18014468319477825L; break; case 51: precompPointsMask[1] += 16777216L; precompPointsMask[2] += 18295873486458944L; precompPointsMask[3] += 69793218625L; break; case 52: precompPointsMask[1] += 1073741824L; precompPointsMask[2] += 18014398526525440L; precompPointsMask[3] += 68719476801L; break; case 53: precompPointsMask[2] += 17039360L; precompPointsMask[3] += 65L; break; case 54: precompPointsMask[1] += 68719476736L; precompPointsMask[2] += 1073741824L; precompPointsMask[3] += 4096L; break; case 55: precompPointsMask[1] += 4398046511104L; precompPointsMask[2] += 69793218560L; precompPointsMask[3] += 266240L; break; case 56: precompPointsMask[1] += 281474976710656L; precompPointsMask[2] += 4467839729664L; precompPointsMask[3] += 4398063554560L; break; case 57: precompPointsMask[1] += 18014398509481984L; precompPointsMask[2] += 285941742698496L; precompPointsMask[3] += 285874114007040L; break; case 58: precompPointsMask[2] += 18300271532703745L; precompPointsMask[3] += 18300341342965760L; precompPointsMask[4] += 1L; break; case 59: precompPointsMask[2] += 18295873486192704L; precompPointsMask[3] += 18295943296450561L; break; case 60: precompPointsMask[2] += 18014398509486080L; precompPointsMask[3] += 18014468319477825L; break; case 61: precompPointsMask[2] += 262144L; precompPointsMask[3] += 69793218625L; break; case 62: precompPointsMask[2] += 16777216L; precompPointsMask[3] += 68719476800L; break; case 64: precompPointsMask[2] += 1073741824L; precompPointsMask[3] += 4398046515200L; break; case 65: precompPointsMask[2] += 68719476736L; precompPointsMask[3] += 285873023488000L; break; case 66: precompPointsMask[2] += 4398046511104L; precompPointsMask[3] += 18300271549747200L; precompPointsMask[4] += 1L; break; case 67: precompPointsMask[2] += 281474976710656L; precompPointsMask[3] += 18300272623484928L; precompPointsMask[4] += 1L; break; case 68: precompPointsMask[2] += 18014398509481984L; precompPointsMask[3] += 18300341342699520L; precompPointsMask[4] += 1L; break; case 69: precompPointsMask[3] += 18295943279411201L; break; case 70: precompPointsMask[3] += 18014467228958784L; break; case 74: precompPointsMask[3] += 4398046515200L; precompPointsMask[4] += 1L; break; case 75: precompPointsMask[3] += 285873023483904L; precompPointsMask[4] += 1L; break; case 76: precompPointsMask[3] += 18300271549480960L; precompPointsMask[4] += 1L; break; case 77: precompPointsMask[3] += 18295874559934464L; precompPointsMask[4] += 1L; break; case 78: precompPointsMask[3] += 18014467228958720L; precompPointsMask[4] += 1L; break; } }
            }

            for (int i = 0; i < 41; ++i) {
                int l = i / 10;
                int b = i % 10;
                precompPoints[i] = (int)((precompPointsMask[l] >> (6 * b)) & 0b11111);
            }
        }
        

        AggroStrategy() throws GameActionException {
            splashSpotIndices.add(new MapLocation(0, 4)); splashSpotIndices.add(new MapLocation(1, 3)); splashSpotIndices.add(new MapLocation(1, 4)); splashSpotIndices.add(new MapLocation(1, 5)); splashSpotIndices.add(new MapLocation(2, 2)); splashSpotIndices.add(new MapLocation(2, 3)); splashSpotIndices.add(new MapLocation(2, 4)); splashSpotIndices.add(new MapLocation(2, 5)); splashSpotIndices.add(new MapLocation(2, 6)); splashSpotIndices.add(new MapLocation(3, 1)); splashSpotIndices.add(new MapLocation(3, 2)); splashSpotIndices.add(new MapLocation(3, 3)); splashSpotIndices.add(new MapLocation(3, 4)); splashSpotIndices.add(new MapLocation(3, 5)); splashSpotIndices.add(new MapLocation(3, 6)); splashSpotIndices.add(new MapLocation(3, 7)); splashSpotIndices.add(new MapLocation(4, 0)); splashSpotIndices.add(new MapLocation(4, 1)); splashSpotIndices.add(new MapLocation(4, 2)); splashSpotIndices.add(new MapLocation(4, 3)); splashSpotIndices.add(new MapLocation(4, 4)); splashSpotIndices.add(new MapLocation(4, 5)); splashSpotIndices.add(new MapLocation(4, 6)); splashSpotIndices.add(new MapLocation(4, 7)); splashSpotIndices.add(new MapLocation(4, 8)); splashSpotIndices.add(new MapLocation(5, 1)); splashSpotIndices.add(new MapLocation(5, 2)); splashSpotIndices.add(new MapLocation(5, 3)); splashSpotIndices.add(new MapLocation(5, 4)); splashSpotIndices.add(new MapLocation(5, 5)); splashSpotIndices.add(new MapLocation(5, 6)); splashSpotIndices.add(new MapLocation(5, 7)); splashSpotIndices.add(new MapLocation(6, 2)); splashSpotIndices.add(new MapLocation(6, 3)); splashSpotIndices.add(new MapLocation(6, 4)); splashSpotIndices.add(new MapLocation(6, 5)); splashSpotIndices.add(new MapLocation(6, 6)); splashSpotIndices.add(new MapLocation(7, 3)); splashSpotIndices.add(new MapLocation(7, 4)); splashSpotIndices.add(new MapLocation(7, 5)); splashSpotIndices.add(new MapLocation(8, 4));
            computeNewTarget();
        }

        void computeNewTarget() throws GameActionException {
            MapLocation possibleTarget = null;
            if (informedEnemyPaintLoc != null && 
                informedEnemyPaintLocTimestamp >= roundNum - 30 &&
                !(rc.canSenseLocation(informedEnemyPaintLoc) && !rc.senseMapInfo(informedEnemyPaintLoc).getPaint().isEnemy())) {
                possibleTarget = informedEnemyPaintLoc;
            }
            if (possibleTarget != null) {
                target = project(locBeforeTurn, possibleTarget);
            }
            else {
                target = project(locBeforeTurn, new MapLocation(rng.nextInt(mapWidth), rng.nextInt(mapHeight)));
            }
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
            if (!inEnemyTowerRange(locBeforeTurn.add(Direction.DIRECTION_ORDER[0]))) {if (precompPoints[6] > bestPoints && attackable[6]) { bestPoints = precompPoints[6]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 6;} if (precompPoints[11] > bestPoints && attackable[11]) { bestPoints = precompPoints[11]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 11;} if (precompPoints[12] > bestPoints && attackable[12]) { bestPoints = precompPoints[12]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 12;} if (precompPoints[13] > bestPoints && attackable[13]) { bestPoints = precompPoints[13]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 13;} if (precompPoints[18] > bestPoints && attackable[18]) { bestPoints = precompPoints[18]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 18;} if (precompPoints[19] > bestPoints && attackable[19]) { bestPoints = precompPoints[19]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 19;} if (precompPoints[20] > bestPoints && attackable[20]) { bestPoints = precompPoints[20]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 20;} if (precompPoints[21] > bestPoints && attackable[21]) { bestPoints = precompPoints[21]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 21;} if (precompPoints[22] > bestPoints && attackable[22]) { bestPoints = precompPoints[22]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 22;} if (precompPoints[27] > bestPoints && attackable[27]) { bestPoints = precompPoints[27]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 27;} if (precompPoints[28] > bestPoints && attackable[28]) { bestPoints = precompPoints[28]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 28;} if (precompPoints[29] > bestPoints && attackable[29]) { bestPoints = precompPoints[29]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 29;} if (precompPoints[34] > bestPoints && attackable[34]) { bestPoints = precompPoints[34]; bestMove = Direction.DIRECTION_ORDER[0]; bestAttack = 34;} }
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
                if (bestMove != Direction.CENTER) rc.move(bestMove);
                // if (!rc.canAttack(splashSpots[bestAttack])) {
                //     rc.setTimelineMarker("splasher bad!", 255, 255, 0);
                // }
                rc.attack(splashSpots[bestAttack]);
            }
            else {
                if (chebyshevDist(locBeforeTurn, target) <= 2) { // my target is likely outdated, reset!
                    computeNewTarget();
                }
                Direction dir = BugNav.getDirectionToMove(target);
                if (dir != null && !dirInEnemyTowerRange(dir)) {
                    rc.move(dir);
                }
                else {
                    tryMoveToSafeTile();
                    tryMoveLessSafeTile();
                }
            }
            rc.setIndicatorLine(rc.getLocation(), target, 0, 255, 0);
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