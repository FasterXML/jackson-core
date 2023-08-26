package tools.jackson.core.util;

import java.lang.ref.SoftReference;

import tools.jackson.core.TokenStreamFactory;

/**
 * Helper entity used to control access to simple buffer recycling scheme used for
 * some encoding, decoding tasks.
 *
 * @see BufferRecycler
 */
public class BufferRecyclers
{
    /*
    /**********************************************************************
    /* Accessor for default BufferRecyclerPool implementations
    /**********************************************************************
     */

    public static BufferRecyclerPool defaultRecyclerPool() {
        return ThreadLocalRecyclerPool.INSTANCE;
    }

    public static BufferRecyclerPool nopRecyclerPool() {
        return NonRecyclingRecyclerPool.INSTANCE;
    }

    /*
    /**********************************************************************
    /* Standard BufferRecyclerPool implementations
    /**********************************************************************
     */
    
    /**
     * Default {@link BufferRecyclerPool} implementation that uses
     * {@link ThreadLocal} for recycling instances.
     */
    public static class ThreadLocalRecyclerPool
        implements BufferRecyclerPool
    {
        private static final long serialVersionUID = 1L;

        /**
         * This <code>ThreadLocal</code> contains a {@link java.lang.ref.SoftReference}
         * to a {@link BufferRecycler} used to provide a low-cost
         * buffer recycling between reader and writer instances.
         */
        protected final static ThreadLocal<SoftReference<BufferRecycler>> _recyclerRef
            = new ThreadLocal<SoftReference<BufferRecycler>>();

        public final static ThreadLocalRecyclerPool INSTANCE = new ThreadLocalRecyclerPool();

        /**
         * Main accessor to call for accessing possibly recycled {@link BufferRecycler} instance.
         *
         * @return {@link BufferRecycler} to use
         */
        private  BufferRecycler getBufferRecycler()
        {
            SoftReference<BufferRecycler> ref = _recyclerRef.get();
            BufferRecycler br = (ref == null) ? null : ref.get();

            if (br == null) {
                br = new BufferRecycler();
                _recyclerRef.set(ref);
            }
            return br;
        }

        @Override
        public BufferRecycler acquireBufferRecycler(TokenStreamFactory forFactory) {
            return getBufferRecycler();
        }

        @Override
        public void releaseBufferRecycler(BufferRecycler recycler) {
            ; // nothing to do, relies on ThreadLocal
        }
    }

    /**
     * {@link BufferRecyclerPool} implementation that does not use
     * any pool but simply creates new instances when necessary.
     */
    public static class NonRecyclingRecyclerPool
        implements BufferRecyclerPool
    {
        private static final long serialVersionUID = 1L;

        public final static ThreadLocalRecyclerPool INSTANCE = new ThreadLocalRecyclerPool();

        @Override
        public BufferRecycler acquireBufferRecycler(TokenStreamFactory forFactory) {
            return new BufferRecycler();
        }

        @Override
        public void releaseBufferRecycler(BufferRecycler recycler) {
            ; // nothing to do, relies on ThreadLocal
        }
    }
}
