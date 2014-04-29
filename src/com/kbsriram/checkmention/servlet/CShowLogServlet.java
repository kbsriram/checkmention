package com.kbsriram.checkmention.servlet;

import com.kbsriram.checkmention.db.CJob;
import com.kbsriram.checkmention.util.CUtils;
import java.io.IOException;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public final class CShowLogServlet extends AAuthenticatedServlet
{
    @Override
    public void doGet
        (HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        String pi = CUtils.nullIfEmpty(req.getPathInfo());
        if (pi == null) {
            CServletUtils.notfound(req, resp, "not found");
            return;
        }

        CJob job = CJob.findByExternalPath(pi);
        if (job == null) {
            CServletUtils.notfound(req, resp, "not found");
            return;
        }

        CServletUtils.owaspProtect(resp);
        resp.setContentType("text/plain");

        resp.getWriter().println
            ("TESTING : "+job.getURL());
        resp.getWriter().println
            ("CREATED : "+(new Date(job.getTimestamp())));
        resp.getWriter().println();

        String log = CUtils.nullIfEmpty(job.getLog());
        if (log == null) {
            resp.getWriter().println("Not yet run");
            return;
        }
        else {
            resp.getWriter().println(log);
        }
    }

    final static String SPATH = "/log";
}
