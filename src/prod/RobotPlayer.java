package prod;

import battlecode.common.*;

public class RobotPlayer {

    static Robot robot;

    public static void run(RobotController rc) throws GameActionException {
        switch (rc.getType()) {
            case SOLDIER: robot = new Soldier(rc); break;
            case SPLASHER: robot = new Splasher(rc); break;
            case MOPPER: robot = new Mopper(rc); break;
            default: robot = new Tower(rc); break;
        }
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
