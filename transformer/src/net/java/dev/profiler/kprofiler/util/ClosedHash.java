package net.java.dev.profiler.kprofiler.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.util.Iterator;

/**
 * Closed hash that uses ID as the comparison.
 */
public final class ClosedHash<V extends ClosedHash.Entry> implements Iterable<V>  {
    public static abstract class Entry {
        public final int id;

        protected Entry(int id) {
            this.id = id;
        }
    }

    /** The hash table data. */
    private Object[] table;

    /** The total number of mappings in the hash table. */
    private int count;

    /**
     * The table is rehashed when its size exceeds this threshold.  (The
     * value of this field is (int)(capacity * loadFactor).)
     */
    private int threshold;

    /** The load factor for the hashtable. */
    private static final float loadFactor = 0.3f;
    private static final int initialCapacity = 191;

    public ClosedHash() {
        this(initialCapacity);
    }

    public ClosedHash(int initialCapacity) {
        table = new Object[(int)(initialCapacity/loadFactor)];
        threshold = initialCapacity;
    }

    public V get(int id) {
        Object[] tab = table;
        int index = (id & 0x7FFFFFFF) % tab.length;

        while (true) {
            final V e = (V)tab[index];
            if (e == null)
                return null;
            if (e.id==id) {
                return e;
            }
            index = (index + 1) % tab.length;
        }
    }

    /**
     * Gets the number of objects in this hash table.
     */
    public int size() {
        return count;
    }


    /**
     * rehash.
     *
     * It is possible for one thread to call get method
     * while another thread is performing rehash.
     * Keep this in mind.
     */
    private void rehash() {
        // create a new table first.
        // meanwhile, other threads can safely access get method.
        int oldCapacity = table.length;
        Object[] oldMap = table;

        int newCapacity = oldCapacity * 2 + 1;
        Object[] newMap = new Object[newCapacity];

        for (int i = oldCapacity; i-- > 0;)
            if (oldMap[i] != null) {
                int index = (((V)oldMap[i]).id & 0x7FFFFFFF) % newMap.length;
                while (newMap[index] != null)
                    index = (index + 1) % newMap.length;
                newMap[index] = oldMap[i];
            }

        // threshold is not accessed by get method.
        threshold = (int) (newCapacity * loadFactor);
        // switch!
        table = newMap;
    }

    /**
     * put method. No two threads can call this method simulatenously,
     * and it's the caller's responsibility to enforce it.
     *
     * <p>
     * Once a value is set, it can be replaced by another but it can never be reset
     * back to null.
     *
     * @return
     *      the old value
     */
    public V put(V value) {
        if (count >= threshold)
            rehash();

        Object[] tab = table;
        int id = value.id;
        int index = (id & 0x7FFFFFFF) % tab.length;

        V old;
        while(true) {
            old = (V)tab[index];
            if(old==null || old.id==id)
                break;
            index = (index + 1) % tab.length;
        }

        tab[index] = value;

        if(old==null)
            count++;

        return old;
    }

    public Iterator<V> iterator() {
        return new Iterator<V>() {
            private int index = 0;
            private V next;

            public boolean hasNext() {
                if(next==null)
                    fetch();

                return next!=null;
            }

            private void fetch() {
                while(index<table.length && table[index]==null)
                    index++;

                if(index<table.length)
                    next = (V)table[index++];
            }

            public V next() {
                V r = next;
                next = null;
                return r;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }



    // serialization support
    private static final long serialVersionUID = -2924295970572669668L;

    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("count", Integer.TYPE),
        new ObjectStreamField("streamVersion", Byte.TYPE),
    };

    private void writeObject(ObjectOutputStream s) throws IOException {
        ObjectOutputStream.PutField fields = s.putFields();
        fields.put("count",count);
        fields.put("streamVersion",(byte)1);
        s.writeFields();

        for( int i=0; i<table.length; i++ )
            if( table[i]!=null )
                s.writeObject(table[i]);
    }

    private void readObject(ObjectInputStream s) throws IOException,ClassNotFoundException {
        // prepare to read the alternate persistent fields
        ObjectInputStream.GetField fields = s.readFields();

        byte version = fields.get("streamVersion",(byte)0);

        if( version==1 ) {
            // read the new format
            int objCnt = fields.get("count",0);

            int size = (int)(objCnt/loadFactor)*2+10;
            threshold = count*2;
            count = 0;
            table = (V[])new Object[size];
            for( int i=0; i<count; i++ )
                put( (V)s.readObject() );
        }
    }
}
