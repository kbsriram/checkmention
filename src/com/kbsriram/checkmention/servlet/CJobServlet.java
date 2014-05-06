package com.kbsriram.checkmention.servlet;

// Schedule a new job, ie a set of webmentions to a provided target.

import com.kbsriram.checkmention.db.CJob;
import com.kbsriram.checkmention.util.CUtils;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.tdom.TDom;
import static org.tdom.TDom.*;

@SuppressWarnings("serial")
public final class CJobServlet extends AAuthenticatedServlet
{
    @Override
    public void doGet
        (HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        String url = CUtils.nullIfEmpty(req.getParameter(URL));
        if (url == null) {
            showForm(req, resp, null);
            return;
        }

        CServletUtils.UserInfo info = getUserInfo();

        String uid = info.getUserID();

        if (!url.startsWith("http://") &&
            !url.startsWith("https://")) {
            if (uid.startsWith("https://")) {
                url = "https://"+url;
            }
            else {
                url = "http://"+url;
            }
        }

        URL candidate;
        try { candidate = new URL(url); }
        catch (MalformedURLException mfe) {
            showForm(req, resp, url+": "+mfe.toString());
            return;
        }

        // Must match - prefix.
        /*
         * debugging.
         */
        //if (!uid.equals("http://kbsriram.com")) {
        if (!candidate.toString().startsWith(uid)) {
            showForm(req, resp, "URLs must start with "+uid);
            return;
        }
        //}

        CJob job = CJob.store
            (url, System.currentTimeMillis(), CUtils.makeNonce(), null);

        CPostMentionTaskServlet.enqueueJob(job);

        CServletUtils.write
            (resp,
             n("div",
               n("h2", t("Test scheduled")),
               n("p",
                 t("I am going to send three test mentions, which takes about a minute to finish. One mention should be accepted with no problems, while the other two try to embed some XSS."),
                 n("br"),
                 n("a",
                   a("href", url),
                   a("target", "_blank"),
                   t("Open your page")),
                 t(" in a new window to see how your site processes them.")),

               n("p",
                 n("a",
                 a("href", CShowLogServlet.SPATH+job.asExternalPath()),
                   t("Check the status of your job."))),
               
               n("p",
                 n("a",
                   a("href", SPATH),
                   t("Or run a new test")))),
             "Test scheduled");
    }

    private final void showForm
        (HttpServletRequest req, HttpServletResponse resp, String err)
        throws ServletException, IOException
    {

        CServletUtils.UserInfo info = getUserInfo();
        String prefix = info.getUserID();

        TNode page =
            n("div",
              n("h2", t("Check a page on your site")),
              n("form",
                a("class", "large"),
                a("action", SPATH),
                a("method", "get"),
                n("input",
                  a("type", "text"),
                  a("name", URL),
                  a("autocomplete", "off"),
                  a("spellcheck", "false"),
                  a("required", ""),
                  a("placeholder", prefix+"/yourpage")),
                n("input",
                  a("type", "submit"),
                  a("value", " Run tests "))));

        if (err != null) {
            page.append(n("div", a("class", "error"), t(err)));
        }
        page.append(n("div", a("class", "spacer")));
        CServletUtils.write(resp, page, "Run tests");
    }

    private final static Logger s_logger = Logger.getLogger
        (CUtils.makeLogTag(CJobServlet.class));
    final static String SPATH = "/job";
    final static String URL = "url";
}
