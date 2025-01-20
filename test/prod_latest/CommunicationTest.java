package prod_latest;

import static org.junit.Assert.*;
import org.junit.Test;

public class CommunicationTest {
    @Test
	public void testEncodeDecode() {
        Comms comm = new Comms(new int[]{64, 12, 96});
        int[] data = new int[]{12, 4, 15};
        int encoded = comm.encode(data);
        int[] decoded = comm.decode(encoded);
        assertArrayEquals(data, decoded);
	}
}
