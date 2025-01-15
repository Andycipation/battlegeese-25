package template;

import battlecode.common.*;

public class RobotPlayer {

    static Robot robot;

    public static void run(RobotController rc) throws GameActionException {
        
        Globals.init(rc);
        
        robot = switch (rc.getType()) {
            case SOLDIER -> new Soldier();
            case SPLASHER -> new Splasher();
            case MOPPER -> new Mopper();
            default -> new Tower();
        };
        while (true) {
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
            Clock.yield();
        }
    }
}
