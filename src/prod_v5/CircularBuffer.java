package prod_v5;

/**
 * Not really like an actual circular buffer
 * Keeps two seperate write and read ptrs,
 * the write ptr writes new data to buffer,
 * overwriting oldest data if full, read ptr
 * loops around the circular buffer on poll
 */
public class CircularBuffer<T> {

    public T[] arr;
    public int write_ptr, read_ptr, cur_sz, cap;

    CircularBuffer(int _cap) {
        cap = _cap;
        arr = (T[]) new Object[cap];
        write_ptr = 0;
        read_ptr = 0;
    }

    public void push(T elem) {
        arr[write_ptr] = elem;
        write_ptr = (write_ptr + 1) % cap;
        if (cur_sz < arr.length) {
            cur_sz++;
        }
    }

    public T poll() {
        T ret = arr[read_ptr];
        read_ptr = (read_ptr + 1) % cur_sz;
        return ret;
    }

    public void clear() {
        arr = (T[]) new Object[cap];
        write_ptr = 0;
        read_ptr = 0;
        cur_sz = 0;
    }

    public int size() {
        return cur_sz;
    }

    public boolean empty() {
        return cur_sz == 0;
    }
}