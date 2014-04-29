package com.kbsriram.checkmention.db;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Text;
import com.kbsriram.checkmention.util.CUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CJob
{
    public final static CJob findByExternalPath(String p)
    {
        if (p == null) { return null; }

        Matcher m = s_path_pattern.matcher(p);
        if (!m.matches()) { return null; }

        return find(Long.parseLong(m.group(1), 16), m.group(2));
    }

    public final static CJob find(long ts, String nonce)
    {
        Key k = makeKey(ts, nonce);
        Entity e = E.find(k);
        if (e == null) { return null; }

        String url = (String) e.getProperty(URL);
        Text tlog = (Text) e.getProperty(LOG);
        String log;
        if (tlog != null) { log = tlog.getValue(); }
        else { log = null; }

        return new CJob(url, ts, nonce, log);
    }

    public final static CJob store
        (String url, long ts, String nonce, String log)
    {
        Key k = makeKey(ts, nonce);
        Entity e = new Entity(k);
        e.setUnindexedProperty(URL, url);
        if (log != null) {
            e.setUnindexedProperty(LOG, new Text(log));
        }

        E.store(e);
        return new CJob(url, ts, nonce, log);
    }

    public final static Key makeKey(long ts)
    { return KeyFactory.createKey(TYPE, ts); }


    public final static int deleteBefore(long cutoff)
    {
        Key cutoff_key = makeKey(cutoff);

        Query q = new Query(TYPE_NONCE)
            .setKeysOnly()
            .setFilter
            (new Query.FilterPredicate
             (Entity.KEY_RESERVED_PROPERTY,
              Query.FilterOperator.LESS_THAN,
              cutoff_key));
        Iterator<Entity> ekeys = E.query
            (q, FetchOptions.Builder.withLimit(MAX_COUNT));
        final ArrayList<Key> del_list = new ArrayList<Key>();
        while (ekeys.hasNext()) {
            del_list.add(ekeys.next().getKey());
        }
        if (del_list.size() > 0) {
            E.remove(del_list);
        }
        return del_list.size();
    }

    private final static Key makeKey(long ts, String nonce)
    {
        return new KeyFactory.Builder(TYPE, ts)
            .addChild(TYPE_NONCE, nonce)
            .getKey();
    }

    public String getURL() { return m_url; }
    public long getTimestamp() { return m_ts; }
    public String getNonce() { return m_nonce; }
    public String getLog() { return m_log; }
    public String asExternalPath()
    { return "/"+Long.toString(m_ts, 16)+"/"+m_nonce; }

    private CJob(String url, long ts, String nonce, String log)
    {
        m_url = url;
        m_ts = ts;
        m_nonce = nonce;
        m_log = log;
    }
    private final String m_url;
    private final long m_ts;
    private final String m_nonce;
    private final String m_log;

    private final static String URL = "url";
    private final static String TYPE = "job";
    private final static String LOG = "log";
    private final static String TYPE_NONCE = "nce";

    // max-deleted per run
    private final static int MAX_COUNT = 100;
    private final static Pattern s_path_pattern =
        Pattern.compile("/([0-9a-f]+)/([0-9a-f]+)");
}
