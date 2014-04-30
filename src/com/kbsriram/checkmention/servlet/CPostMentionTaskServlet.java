package com.kbsriram.checkmention.servlet;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Builder;
import com.kbsriram.checkmention.db.CJob;
import com.kbsriram.checkmention.util.CUtils;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

@SuppressWarnings("serial")
public final class CPostMentionTaskServlet extends HttpServlet
{
    public final static void enqueueJob(CJob job)
    {
        Queue q = QueueFactory.getQueue("postmention");
        q.add(Builder
              .withUrl("/tasks/postmention")
              .param(TS, String.valueOf(job.getTimestamp()))
              .param(NONCE, job.getNonce())
              .param(URL, job.getURL()));
    }

    @Override
    public void doPost
        (HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        String nonce = CUtils.nullIfEmpty(req.getParameter(NONCE));
        String tsS = CUtils.nullIfEmpty(req.getParameter(TS));

        if ((nonce == null) || (tsS == null)) {
            bail(resp, "skipped, missing params");
            return;
        }
        long ts;
        try { ts = Long.parseLong(tsS); }
        catch (NumberFormatException nfe) {
            bail(resp, "bad number", nfe);
            return;
        }

        CJob job = CJob.find(ts, nonce);
        if (job == null) {
            bail(resp, "skipped, missing job: "+ts+"/"+nonce);
            return;
        }

        StringBuilder log = new StringBuilder();

        URL mention = findMention(resp, job, log);
        if (mention == null) {
            // Update log and be done.
            CJob.store
                (job.getURL(), job.getTimestamp(), job.getNonce(),
                 log.toString());
            return;
        }

        // Just do the success servlet for now.
        String[] checks = { "success", "hcardxss", "xss" };

        for (int i=0; i<checks.length; i++) {
            postMention
                (resp, mention, "https://checkmention.appspot.com"+
                 CContentServlet.SPATH+"/"+Long.toString(ts, 16)+"/"+
                 nonce+"/"+checks[i], job.getURL(), log);
        }

        CJob.store
            (job.getURL(), job.getTimestamp(), job.getNonce(),
             log.toString());
    }

    private URL findMention
        (HttpServletResponse resp, CJob job, StringBuilder log)
        throws IOException
    {
        BufferedInputStream bin = null;
        URL ret = null;
        try {
            addLog(log, (new Date()).toString());
            addLog(log, "Connecting to "+job.getURL());
            URL url = new URL(job.getURL());
            URLConnection con = url.openConnection();
            con.setReadTimeout(30*1000);
            con.setConnectTimeout(30*1000);
            bin = new BufferedInputStream(con.getInputStream());
            Document doc = Jsoup.parse(bin, "utf-8", job.getURL());
            Element el = doc.select("html>head>link[rel*=webmention]").first();
            if (el == null) {
                addLog(log, "ERROR: Woops! Could not find a webmention");
            }
            else {
                String href = CUtils.nullIfEmpty(el.attr("href"));
                if (href == null) {
                    addLog(log, "ERROR: Missing href in webmention: "+el);
                }
                else {
                    ret = new URL(url, href);
                    addLog(log, "Webmention-url is "+ret);
                }
            }
        }
        catch (IOException ioe) {
            addLog(log, "ERROR: "+ioe.getMessage());
            bail(resp, "Unable to handle webmention", ioe);
            return null;
        }
        finally {
            if (bin != null) {
                try { bin.close(); }
                catch (IOException ign) {}
            }
        }
        if (ret == null) {
            bail(resp, log.toString());
        }
        return ret;
    }

    private void postMention
        (HttpServletResponse resp, URL mention, String src, String target,
         StringBuilder log)
        throws IOException
    {
        BufferedReader br = null;
        PrintWriter pw = null;
        try {
            s_logger.log(Level.INFO, "post to "+mention+", "+src);
            addLog(log, "Posting source="+src+"&target="+target);
            URLConnection con = mention.openConnection();
            con.setReadTimeout(30*1000);
            con.setConnectTimeout(30*1000);
            con.setDoOutput(true);
            pw = new PrintWriter
                (new OutputStreamWriter(con.getOutputStream()));
            pw.print("source="+src+"&target="+target);
            pw.flush();
            br = new BufferedReader
                (new InputStreamReader(con.getInputStream()));
            String line;
            addLog(log, "RESPONSE:");
            while ((line = br.readLine()) != null) {
                addLog(log, line);
            }
            finish(resp, "OK! "+log.toString());
        }
        catch (IOException ioe) {
            addLog(log, "ERROR: "+ioe.getMessage());
            bail(resp, "Unable to post webmention", ioe);
        }
        finally {
            if (pw != null) {
                pw.close();
            }
            if (br != null) {
                try { br.close(); } catch (IOException ign) {}
            }
        }
    }

    private static void addLog(StringBuilder log, String m)
    {
        log.append(m);
        log.append("\n");
    }

    private static void bail(HttpServletResponse resp, String msg)
        throws IOException
    {
        s_logger.log(Level.WARNING, msg);
        finish(resp, msg);
    }

    private static void bail(HttpServletResponse resp, String msg, Throwable th)
        throws IOException
    {
        s_logger.log(Level.WARNING, msg, th);
        finish(resp, msg);
    }

    private static void finish(HttpServletResponse resp, String msg)
        throws IOException
    {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain");
        resp.getWriter().println(msg);
        s_logger.log(Level.INFO, msg);
    }

    final static String SPATH = "/tasks/postmention";

    private final static String TS = "ts";
    private final static String NONCE = "nonce";
    private final static String URL = "url";

    private final static Logger s_logger = Logger.getLogger
        (CUtils.makeLogTag(CPostMentionTaskServlet.class));
}
