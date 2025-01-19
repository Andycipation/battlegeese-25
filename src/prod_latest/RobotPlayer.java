package prod_latest;

import battlecode.common.*;

public class RobotPlayer extends Globals {

    static Robot robot;
    static int bytecode_limit;

    public static void run(RobotController rc) throws GameActionException {
        
        Globals.init(rc);
        bytecode_limit = unitType.isTowerType() ? GameConstants.TOWER_BYTECODE_LIMIT : GameConstants.ROBOT_BYTECODE_LIMIT;

        robot = switch (rc.getType()) {
            case SOLDIER -> new Soldier();
            case SPLASHER -> new Splasher();
            case MOPPER -> new Mopper();
            default -> new Tower();
        };

        while (true) {
            act();
            Logger.flush();
            Clock.yield();
        }
    }

    private static void act() {

        int startRound = rc.getRoundNum();
        int startBytecodes = Clock.getBytecodeNum();

        try {
            robot.initTurn();
            robot.play();
            robot.endTurn();
        } catch (Exception e) {
            // All exceptions should be handled by robot. There is something
            // wrong if an exception reaches here. We will mark red on the timeline
            // if an exception reaches here.
            System.out.println("Exception -- this should not happen. FIX THIS!");
            e.printStackTrace();
            rc.setTimelineMarker("Exception", 255, 0, 0);
        }

        int endRound = rc.getRoundNum();
        int endBytecodes = Clock.getBytecodeNum();

        int usedBytecodes = startRound == endRound
            ? endBytecodes - startBytecodes
            : (bytecode_limit - startBytecodes) + Math.max(0, endRound - startRound - 1) * bytecode_limit + endBytecodes;

        double bytecodePercentage = (double) usedBytecodes / (double) bytecode_limit * 100.0;

        if (startRound != endRound) {
            System.out.println(rc.getLocation() + " Bytecode overflow: " + usedBytecodes + " (" + bytecodePercentage + "%)");
        } else if (bytecodePercentage > 80) {
            System.out.println(rc.getLocation() + " High bytecode usage: " + usedBytecodes + " (" + bytecodePercentage + "%)");
        }

    }
}
