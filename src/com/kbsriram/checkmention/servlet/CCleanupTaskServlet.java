package com.kbsriram.checkmention.servlet;

import com.kbsriram.checkmention.db.CJob;
import com.kbsriram.checkmention.util.CUtils;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class CCleanupTaskServlet
    extends HttpServlet
{
    @Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws IOException
    {
        long cutoff = System.currentTimeMillis() - CUTOFF_MSEC;

        int delete_count = CJob.deleteBefore(cutoff);
        s_logger.log(Level.INFO, "deleted: "+delete_count);

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain");
        resp.getWriter().println("Deleted: "+delete_count);
    }

    // 24 hours
    private final static long CUTOFF_MSEC = 24l*60l*60l*1000l;
    private final static Logger s_logger = Logger.getLogger
        (CUtils.makeLogTag(CCleanupTaskServlet.class));

}
