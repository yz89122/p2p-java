package tw.imyz.util;

import java.util.Collection;
import java.util.Iterator;

public class EmptyCollection {

    public static final Object[] array = {};

    public static final Iterator iterator = new Iterator() {
        @Override
        public boolean hasNext() { return false; }

        @Override
        public Object next() { return null; }
    };

    public static final Iterable iterable = () -> iterator;

    public static final Collection collection = new Collection() {
        @Override
        public int size() { return 0; }

        @Override
        public boolean isEmpty() { return true; }

        @Override
        public boolean contains(Object o) { return false; }

        @Override
        public Iterator iterator() { return iterator; }

        @Override
        public Object[] toArray() { return array; }

        @Override
        public Object[] toArray(Object[] a) { throw new UnsupportedOperationException(); }

        @Override
        public boolean add(Object o) { throw new UnsupportedOperationException(); }

        @Override
        public boolean remove(Object o) { throw new UnsupportedOperationException(); }

        @Override
        public boolean containsAll(Collection c) { return false; }

        @Override
        public boolean addAll(Collection c) { throw new UnsupportedOperationException(); }

        @Override
        public boolean removeAll(Collection c) { throw new UnsupportedOperationException(); }

        @Override
        public boolean retainAll(Collection c) { throw new UnsupportedOperationException(); }

        @Override
        public void clear() { }
    };

}
