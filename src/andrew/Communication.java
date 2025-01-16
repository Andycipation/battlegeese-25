package andrew;

import battlecode.common.*;

public class Communication extends Globals {
    // public enum GeeseMessageType {
    //     TARGET_CELL,
    // }

    // public class GeeseMessage {
    //     GeeseMessageType type;

    // }

    // public static int encodeMessage(GeeseMessage gm) {

    // }

    public static int encodeTowerMessage(MapLocation loc) {
        return (loc.x << 6) | loc.y;
    }

    public static MapLocation decodeTowerMessage(int m) {
        return new MapLocation(m >> 6, m & 63);
    }
    
}
