package com.kbsriram.checkmention.servlet;

// Send back a suitable page, based on saved info and
// a template.

import com.kbsriram.checkmention.db.CJob;
import com.kbsriram.checkmention.util.CUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public final class CContentServlet extends HttpServlet
{
    @Override
    public void doGet
        (HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        String pinfo = CUtils.nullIfEmpty(req.getPathInfo());
        if (pinfo == null) {
            CServletUtils.notfound(req, resp, "missing path");
            return;
        }

        // /timestamp/nonce/template
        Matcher m = s_content_pattern.matcher(pinfo);
        if (!m.matches()) {
            CServletUtils.notfound(req, resp, "bad path");
            return;
        }

        long ts = Long.parseLong(m.group(1), 16);
        String nonce = m.group(2);
        String template = m.group(3);

        // See if we have a valid db entry here.
        CJob job = CJob.find(ts, nonce);
        if (job == null) {
            CServletUtils.notfound(req, resp, "not available");
            return;
        }

        cheapTrick(req, resp, template, job.getURL(), job.getTimestamp());
    }

    private final void cheapTrick
        (HttpServletRequest req, HttpServletResponse resp,
         String tpl, String url, long ts)
        throws IOException
    {
        BufferedReader br = null;
        Date date = new Date(ts);

        String tstr = s_date_format.format(date);
        String nice = date.toString();

        try {
            InputStream is = getServletContext().getResourceAsStream
                ("/WEB-INF/checks/"+tpl);
            if (is == null) {
                CServletUtils.notfound(req, resp, "No such template");
                return;
            }

            br = new BufferedReader(new InputStreamReader(is));
            resp.setContentType("text/html");
            String line;
            while ((line = br.readLine()) != null) {
                if (line.equals("\"%%target\"")) {
                    line = "\""+url+"\"";
                }
                else if (line.equals("\"%%time\"")) {
                    line = "\""+tstr+"\"";
                }
                else if (line.equals("%%nice_time")) {
                    line = nice;
                }
                resp.getWriter().println(line);
            }
        }
        finally {
            if (br != null) {
                try { br.close(); }
                catch (IOException ioe) {}
            }
        }
    }

    final static String SPATH = "/content";

    private final static Pattern s_content_pattern =
        Pattern.compile("/([0-9a-f]+)/([0-9a-f]+)/([a-z_]+)");
    private final static Logger s_logger = Logger.getLogger
        (CUtils.makeLogTag(CContentServlet.class));
    private final static SimpleDateFormat s_date_format;
    static
    {
        s_date_format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ");
    }
}
