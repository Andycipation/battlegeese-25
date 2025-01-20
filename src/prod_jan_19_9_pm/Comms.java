package prod_jan_19_9_pm;

import battlecode.common.*;

public class Comms {
    /**
     * THE FIRST FIELD OF ANY MESSAGE MUST SPECIFY THE PROTOCAL WE ARE ON
     */
    final public static int IDENTIFIER_SZ = 16;

    // to get index of specific Protocal p, use p.ordinal()
    public enum Protocal {
        TOWER_TO_TOWER
    };
    public static Protocal getProtocol(int msg) {
        return Protocal.values()[msg % IDENTIFIER_SZ];
    }

    public int n;
    public int lims[];

    Comms (int[] lims) {
        n = lims.length;
        this.lims = lims;
    }

    public int encode(int[] data) {
        int ret = 0;
        for (int i = n; --i >= 0; ) {
            ret = ret * lims[i] + data[i];
        }
        return ret;
    }

    public int[] decode(int msg) {
        int[] ret = new int[n];
        for (int i = -1; ++i < n;) {
            ret[i] = msg % lims[i];
            msg /= lims[i];
        }
        return ret;
    }

    final public static int MAP_ENCODE_SZ = 3600;
    public static int encodeMapLocation(MapLocation loc) {
        return 60 * loc.x + loc.y;
    }

    public static MapLocation decodeMapLocation(int m) {
        return new MapLocation(m / 60, m % 60);
    }

    final public static int MAP_ENCODE_SCALE_2 = 3600 / 2 / 2;
    public static int encodeMapLocationScale2(MapLocation loc) {
        return 30 * (loc.x / 2) + (loc.y / 2);
    }

    public static MapLocation decodeMapLocationScale2(int m) {
        return new MapLocation(2 * (m / 30), 2 * (m % 30));
    }

}
