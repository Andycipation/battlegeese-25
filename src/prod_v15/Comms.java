package prod_v15;

import battlecode.common.*;

public class Comms {
    /**
     * THE FIRST FIELD OF ANY MESSAGE MUST SPECIFY THE PROTOCOL WE ARE ON
     */
    final public static int IDENTIFIER_SZ = 16;
    
    // to get index of specific Protocol p, use p.ordinal()
    public enum Protocol {
        TOWER_TO_TOWER_V1,
        TOWER_TO_TOWER_V2,
        TOWER_NETWORK_REQUEST,
        TOWER_NETWORK_RESPONSE,
        TOWER_NETWORK_INFORM,
    };
    public static Comms towerToTowerCommsV2 = new Comms(new int[]{
        Comms.IDENTIFIER_SZ, // protocol id
        Comms.BIT_SZ, // which network (1 for enemy network, 0 for empty network)
        Comms.ROUND_NUM_SZ, // round number the information came from
        Comms.MAP_ENCODE_SZ // returned location
    });
    public static Comms towerNetworkRequestComms = new Comms(new int[]{
        Comms.IDENTIFIER_SZ, // protocol id
        Comms.BIT_SZ, // move direction (1 for forward, 0 for backward)
        Comms.BIT_SZ, // which network (1 for enemy network, 0 for empty network)
        Comms.ID_SZ // requestor ID
    });
    public static Comms towerNetworkResponseComms = new Comms(new int[]{
        Comms.IDENTIFIER_SZ, // protocol id
        Comms.BIT_SZ, // successful?
        Comms.MAP_ENCODE_SZ, // returned location
    });
    public static Comms towerNetworkInformComms = new Comms(new int[]{
        Comms.IDENTIFIER_SZ, // protocol id
        Comms.BIT_SZ, // which network (1 for enemy network, 0 for empty network)
        Comms.ROUND_NUM_SZ, // round number the information came from
        Comms.MAP_ENCODE_SZ, // informed location
    });
    
    public static Protocol getProtocol(int msg) {
        return Protocol.values()[msg % IDENTIFIER_SZ];
    }

    final public static int BIT_SZ = 2;
    final public static int ROUND_NUM_SZ = 2001;

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

    final public static int ID_SZ = (1 << 16);
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