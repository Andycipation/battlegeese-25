package bobtesting;

import battlecode.common.*;

import java.lang.reflect.Array;
import java.util.*;


/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public class RobotPlayer {
    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static Random rng = new Random(6147);

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };


    // previous direction robot is walking in
    static Direction prevDirection = directions[0];

    // previous ruin robot was working on
    static MapLocation prevRuinMapLocation = null;

    // number of each thingy produced
    static Integer[] robotTypeCount = {0, 0, 0};

    // number of turns working on ruin
    static Integer timeRuin = 0;

    // number of turns travelling 
    static Integer timeTravel = 0;

    // work on ruin or not
    static Boolean ruin = false;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        rng = new Random(rc.getID());

        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!
        System.out.println("I'm alive");

        // You can also use indicators to save debug notes in replays.
        rc.setIndicatorString("Hello world!");

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // The same run() function is called for every robot on your team, even if they are
                // different types. Here, we separate the control depending on the UnitType, so we can
                // use different strategies on different robots. If you wish, you are free to rewrite
                // this into a different control structure!
                switch (rc.getType()){
                    case SOLDIER: runSoldier(rc); break; 
                    case MOPPER: runMopper(rc); break;
                    case SPLASHER: runSplasher(rc);; // Consider upgrading examplefuncsplayer to use splashers!
                    default: runTower(rc); break;
                    }
                }
             catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }


    public static Integer pickRandom(ArrayList<Integer> choices) {
        int idx = rng.nextInt(choices.size());
        return choices.get(idx);
    }

    public static Integer pickEqual(ArrayList<Integer>  choices) {
        int min = 10000;
        int cur = 10000;
        for (int i = 0; i < choices.size(); i++) {
            if (robotTypeCount[choices.get(i)] < min) {
                min = robotTypeCount[choices.get(i)];
                cur = choices.get(i);
            }
        }
        return cur;
    }

    /**
     * Run a single turn for towers.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    public static void runTower(RobotController rc) throws GameActionException{

        if (rc.getChips() < 1400) {
            return;
        }

        // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);

        // Pick a random robot type to build.
        ArrayList<Integer> choices = new ArrayList<>();
        choices.add(0);
        if (rc.getChips() > 1e4) {
            choices.add(1);
        }
        int robotType = pickRandom(choices);
        if (robotType == 0 && rc.canBuildRobot(UnitType.SOLDIER, nextLoc)){
            rc.buildRobot(UnitType.SOLDIER, nextLoc);
            System.out.println("BUILT A SOLDIER");
        }
        else if (robotType == 1 && rc.canBuildRobot(UnitType.MOPPER, nextLoc)){
            rc.buildRobot(UnitType.MOPPER, nextLoc);
            System.out.println("BUILT A MOPPER");
        }
        else if (robotType == 2 && rc.canBuildRobot(UnitType.SPLASHER, nextLoc)){
            rc.buildRobot(UnitType.SPLASHER, nextLoc);
            System.out.println("BUILT A SPLASHER");
            // rc.setIndicatorString("SPLASHER NOT IMPLEMENTED YET");
        }

        robotTypeCount[robotType]++;

        // Read incoming messages
        Message[] messages = rc.readMessages(-1);
        for (Message m : messages) {
            System.out.println("Tower received message: '#" + m.getSenderID() + " " + m.getBytes());
        }

        // TODO: can we attack other bots?
        // AOE attack
        rc.attack(null);

        // Single target attack lowest hp enemy in range
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
        Optional<RobotInfo> target = Arrays.stream(nearbyEnemies).min((a, b) -> Integer.compare(a.getHealth(), b.getHealth()));
        if (target.isPresent()) {
            rc.attack(target.get().getLocation());
        }
    }

    /**
     * Run a single turn for a Soldier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    public static void runSoldier(RobotController rc) throws GameActionException{
        boolean moved = false;
        boolean findNewRuin = false;
        int canAttackCnt = 0;

        // Sense information about all visible nearby tiles.
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        // Search for a nearby ruin to complete.
        MapLocation curRuinLocation = null;

        for (MapInfo tile : nearbyTiles) {
            RobotInfo robot = rc.senseRobotAtLocation(tile.getMapLocation());
            boolean isTower = robot != null && robot.getType().isTowerType();
            if (prevRuinMapLocation == null 
            || (tile.getMapLocation().equals(prevRuinMapLocation) && isTower)) {
                findNewRuin = true;
            } 
            if (tile.hasRuin() && !isTower) {
                curRuinLocation = tile.getMapLocation();
            }
        }

        if (prevRuinMapLocation == null || !rc.getLocation().isWithinDistanceSquared(prevRuinMapLocation, 20)) {
            findNewRuin = true;
        }

        if (findNewRuin != true) {
            curRuinLocation = prevRuinMapLocation;
        }

        prevRuinMapLocation = curRuinLocation;

        if (timeRuin > 15) {
            ruin = false;
            timeRuin = 0;
        } else if (timeTravel > 10){
            ruin = true;
            timeTravel = 0;
        }

        MapLocation targetLoc = curRuinLocation;

        if (curRuinLocation != null) {
            targetLoc = curRuinLocation;
            boolean ruinHasTower = false;

            // Complete the ruin if we can.
            UnitType[] towersToConsider = {UnitType.LEVEL_ONE_PAINT_TOWER, UnitType.LEVEL_ONE_MONEY_TOWER};
            for (UnitType towerType : towersToConsider) {
                if (rc.canCompleteTowerPattern(towerType, targetLoc)){
                    ruinHasTower = true;
                    rc.completeTowerPattern(towerType, targetLoc);
                    rc.setTimelineMarker("Tower built", 0, 255, 0);
                }
            }
            // // // remove ruin pattern after done
            // if (ruinHasTower) {
            //     MapLocation ruinLoc = targetLoc;
            //     for (int dx = -GameConstants.PATTERN_SIZE / 2; dx < (GameConstants.PATTERN_SIZE + 1) / 2; dx++) {
            //         for (int dy = -GameConstants.PATTERN_SIZE / 2; dy < (GameConstants.PATTERN_SIZE + 1) / 2; dy++) {
            //             MapLocation loc = ruinLoc.translate(dx, dy);
            //             if (!rc.canSenseLocation(loc)) continue;
            //             MapInfo tile = rc.senseMapInfo(ruinLoc.translate(dx, dy));
            //             if (tile.getMark() != PaintType.EMPTY && rc.canRemoveMark(loc)) {
            //                  rc.removeMark(loc);
            //             }
            //         }
            //     }
            // }

        }


        // upgrade tower if possible

        for (MapInfo tile : nearbyTiles) {
            // Check if the tile contains a tower of your team
            RobotInfo tower = rc.senseRobotAtLocation(tile.getMapLocation());
            if (tower != null && tower.getType().isTowerType() && tower.getTeam() == rc.getTeam()) {
                // Check if the tower can be upgraded
                if (rc.canUpgradeTower(tower.getLocation())) {
                    rc.upgradeTower(tower.getLocation());
                    System.out.println("Upgraded tower at: " + tower.getLocation());
                }
            }
        }

        if (targetLoc != null && ruin == true){
            timeRuin++;
            timeTravel = 0;
            System.out.println("Current ruin location " + targetLoc);

            Direction dir = rc.getLocation().directionTo(targetLoc);
                


            // Mark the pattern we need to draw to build a tower here if we haven't already.
            MapLocation shouldBeMarked = curRuinLocation.subtract(dir);
            if (rc.senseMapInfo(shouldBeMarked).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc)){
                int ok = rng.nextInt(12);
                if (ok < 4) {
                    rc.markTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
                } else {
                    rc.setTimelineMarker("Tower marked", 255, 0, 0);
                    rc.markTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, targetLoc);
                }
                System.out.println("Trying to build a tower at " + targetLoc);
            }




            ArrayList<MapLocation> attackLocs = new ArrayList<>();
            ArrayList<Boolean> useSec = new ArrayList<>();
            // Fill in any spots in the pattern with the appropriate paint.
            for (MapInfo patternTile : rc.senseNearbyMapInfos(targetLoc, 9)){
                if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY && !patternTile.getPaint().isEnemy()){
                    boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                    if (rc.canAttack(patternTile.getMapLocation())) {
                        attackLocs.add(patternTile.getMapLocation());
                        useSec.add(useSecondaryColor);
                    }
                }
            }

            canAttackCnt = attackLocs.size();

            if (canAttackCnt > 0) {
                rc.attack(attackLocs.get(0), useSec.get(0));
            }

            if (canAttackCnt < 2) {
                if (rc.canMove(dir)) {
                    rc.move(dir);
                    prevDirection = null;
                    moved = true;
                }
            } 
        } else {
            timeTravel++;
            timeRuin = 0;
        }




        // if badarded robot painted mark wrong
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
        if (rc.canAttack(rc.getLocation()) 
        && (currentTile.getPaint().isAlly() && (currentTile.getPaint().isSecondary() != currentTile.getMark().isSecondary()))){
            rc.attack(rc.getLocation());
        }


        // try to paint resource pattern packing pattern
        for (Direction dir : directions) {
            MapLocation loc = rc.getLocation().add(dir);
            boolean useSecondaryColor = false;
            if ((loc.x + loc.y) % 2 == 0 && (loc.x + loc.y * 3) % 10 != 0) {
                useSecondaryColor = true;
            }
            PaintType colorToPaint = (useSecondaryColor ? PaintType.ALLY_SECONDARY : PaintType.ALLY_PRIMARY);
            if (!rc.canSenseLocation(loc)) continue;
            MapInfo tile = rc.senseMapInfo(loc);
            if (tile.getMark().isAlly()) continue;
            if (tile.getPaint() != colorToPaint && rc.canPaint(loc) && rc.canAttack(loc)) {
                rc.attack(loc, useSecondaryColor);
            }
        }

        for (Direction dir : directions) {
            MapLocation loc = rc.getLocation().add(dir);
            if ((loc.x + 3 * loc.y) % 10 == 0 && rc.canCompleteResourcePattern(loc)) {
                rc.setIndicatorDot(loc, 0, 0, 255);
                rc.completeResourcePattern(loc);
            }
        }

        System.out.println("canAttackCnt: " + canAttackCnt + " moved: " + moved);
        System.out.println("prevDirection: " + prevDirection);

        if (moved == false && canAttackCnt < 2) {
            Direction dir = directions[rng.nextInt(directions.length)];
            System.out.println(rng.nextInt(directions.length));

            // keep moving in same direction, otherwise move in random direction
            if (prevDirection != null && rc.canMove(prevDirection)) {
                dir = prevDirection;
            }
    
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
            prevDirection = dir;
        }

    }


    /**
     * Run a single turn for a Mopper.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    public static void runMopper(RobotController rc) throws GameActionException {

        boolean moved = false;
        ArrayList<MapLocation> attackLocs = new ArrayList<>();
        ArrayList<Boolean> useSec = new ArrayList<>();

        if (!rc.isActionReady()) return;
    
        // Attempt to mop enemy paint
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        for (MapInfo tile : nearbyTiles) {
            if (tile.getPaint().isEnemy() && rc.canAttack(tile.getMapLocation()) && tile.getMapLocation().isWithinDistanceSquared(rc.getLocation(), 2)) {
                attackLocs.add(tile.getMapLocation());
                useSec.add(tile.getMark().isSecondary());

                // rc.attack(tile.getMapLocation());
                // System.out.println("Attack To: " + tile.getMapLocation());
                // return;
            }
        }
        
        
        int canMopCnt = attackLocs.size();

        if (canMopCnt > 0) {
            rc.attack(attackLocs.get(0), useSec.get(0));
        }

        if (moved == false) {
            int bro = rng.nextInt(directions.length);
            Direction dir = directions[bro];
            System.out.println("bro: " + bro + " - " + dir);

            // keep moving in same direction, otherwise move in random direction
            if (prevDirection != null && rc.canMove(prevDirection)) {
                dir = prevDirection;
            }
    
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
            prevDirection = dir;
        }


    
        // // Paint beneath and ahead
        // MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
        // if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation())) {
        //     rc.attack(rc.getLocation());
        // }
    
        // MapLocation nextLoc = rc.getLocation().add(dir);
        // if (rc.canAttack(nextLoc)) {
        //     rc.attack(nextLoc);
        // }
    }
    

    /*
     * Run single turn for a splasher
     */
    public static void runSplasher(RobotController rc) throws GameActionException{
       // Sense information about all visible nearby tiles.
       MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
       // Search for a nearby ruin to complete.
       MapInfo curRuin = null;
       for (MapInfo tile : nearbyTiles){
           if (tile.hasRuin()){
               curRuin = tile;
           }
       }
       if (false && curRuin != null){
           MapLocation targetLoc = curRuin.getMapLocation();
           Direction dir = rc.getLocation().directionTo(targetLoc);
           if (rc.canMove(dir))
               rc.move(dir);
           // Mark the pattern we need to draw to build a tower here if we haven't already.
           MapLocation shouldBeMarked = curRuin.getMapLocation().subtract(dir);
           if (rc.senseMapInfo(shouldBeMarked).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc)){
               rc.markTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
               System.out.println("Trying to build a tower at " + targetLoc);
           }
           // Fill in any spots in the pattern with the appropriate paint.
           for (MapInfo patternTile : rc.senseNearbyMapInfos(targetLoc, 8)){
               if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY){
                   boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                   if (rc.canAttack(patternTile.getMapLocation()))
                       rc.attack(patternTile.getMapLocation(), useSecondaryColor);
               }
           }
           // Complete the ruin if we can.
           if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc)){
               rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
               rc.setTimelineMarker("Tower built", 0, 255, 0);
               System.out.println("Built a tower at " + targetLoc + "!");
           }
       }

       Direction dir = directions[rng.nextInt(directions.length)];

       // keep moving in same direction, otherwise move in random direction
       if (rc.canMove(prevDirection)) {
           dir = prevDirection;
       } 

       if (rc.canMove(dir)) {
           rc.move(dir);
       }

       prevDirection = dir;

       // Try to paint beneath us as we walk to avoid paint penalties.
       // Avoiding wasting paint by re-painting our own tiles.
       MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
       if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation())){
           rc.attack(rc.getLocation());
       }
    }

    public static void updateEnemyRobots(RobotController rc) throws GameActionException{
        // Sensing methods can be passed in a radius of -1 to automatically 
        // use the largest possible value.
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemyRobots.length != 0){
            rc.setIndicatorString("There are nearby enemy robots! Scary!");
            // Save an array of locations with enemy robots in them for possible future use.
            MapLocation[] enemyLocations = new MapLocation[enemyRobots.length];
            for (int i = 0; i < enemyRobots.length; i++){
                enemyLocations[i] = enemyRobots[i].getLocation();
            }
            RobotInfo[] allyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
            // Occasionally try to tell nearby allies how many enemy robots we see.
            if (rc.getRoundNum() % 20 == 0){
                for (RobotInfo ally : allyRobots){
                    if (rc.canSendMessage(ally.location, enemyRobots.length)){
                        rc.sendMessage(ally.location, enemyRobots.length);
                    }
                }
            }
        }
    }
}
