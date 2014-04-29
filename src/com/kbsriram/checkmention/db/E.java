package com.kbsriram.checkmention.db;

// Utilities to manage store/retrieve from the bigtable db.

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.kbsriram.checkmention.util.CUtils;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class E
{
    // Also maintains a memcache, and a per-process
    // cache.

    // Write-through both caches
    public final static Key store(Entity e)
    {
        s_logger.log(Level.INFO, "Storing "+e);
        Key ret = s_datastore.put(e);
        String sk = KeyFactory.keyToString(ret);
        s_memcache.put(sk, e);
        synchronized (s_l1cache) {
            s_l1cache.put(sk, e);
        }
        return ret;
    }

    // bulk-delete when possible, to avoid
    // frequent datastore calls.
    public final static void remove(final List<Key> keys)
    {
        s_logger.log(Level.INFO, "Removing "+keys);
        for (Key k: keys) {
            String sk = KeyFactory.keyToString(k);
            s_memcache.delete(sk);
            synchronized (s_l1cache) {
                s_l1cache.remove(sk);
            }
        }
        s_datastore.delete
            (new Iterable<Key>() {
                public Iterator<Key> iterator() {
                    return keys.iterator();
                }
            });
    }

    public final static void remove(Key k)
    {
        s_logger.log(Level.INFO, "Removing "+k);
        String sk = KeyFactory.keyToString(k);
        s_memcache.delete(sk);
        synchronized (s_l1cache) {
            s_l1cache.remove(sk);
        }
        s_datastore.delete(k);
    }        

    public final static QueryResultIterator<Entity>
        query(Query q, FetchOptions fo)
    {
        s_logger.log(Level.INFO, "Querying "+q);
        PreparedQuery pq = s_datastore.prepare(q);
        if (fo != null) {
            return pq.asQueryResultIterator(fo);
        }
        else {
            return pq.asQueryResultIterator();
        }
    }

    public final static Entity find(Key k)
    {
        String sk = KeyFactory.keyToString(k);
        s_logger.log(Level.INFO, "Searching for "+k);

        // Step 1: try from local weakhashmap.
        Entity ret;
        synchronized (s_l1cache) {
            ret = s_l1cache.get(sk);
        }
        if (ret != null) {
            s_logger.log(Level.INFO, "Found "+k+" in L1 cache");
            return ret;
        }

        ret = (Entity) s_memcache.get(sk);
        if (ret != null) {
            s_logger.log(Level.INFO, "Found "+k+" in memcache");
            synchronized (s_l1cache) {
                s_l1cache.put(sk, ret);
            }
            return ret;
        }

        try {
            s_logger.log(Level.INFO, "Searching for "+k+" from store");
            ret = s_datastore.get(k);
            s_logger.log(Level.INFO, "Found "+k+" from store, caching");
            s_memcache.put(sk, ret);
            synchronized (s_l1cache) {
                s_l1cache.put(sk, ret);
            }
            return ret;
        }
        catch (EntityNotFoundException enfe) {
            s_logger.log(Level.INFO, "Failed to find "+k);
            return null;
        }
    }

    private final static DatastoreService s_datastore =
        DatastoreServiceFactory.getDatastoreService();
    private final static Logger s_logger = Logger.getLogger
        (CUtils.makeLogTag(E.class));
    private final static MemcacheService s_memcache =
        MemcacheServiceFactory.getMemcacheService();
    private final static WeakHashMap<String,Entity> s_l1cache =
        new WeakHashMap<String,Entity>();
}
