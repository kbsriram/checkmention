package com.kbsriram.checkmention.servlet;

import com.kbsriram.checkmention.util.CUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("serial")
public final class CAuthServlet extends HttpServlet
{
    @Override
    public void doGet
        (HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        CServletUtils.UserInfo info = CServletUtils.getUserInfo(req);
        String nonce = info.getNonce();
        if (nonce == null) {
            // Unexpected.
            resp.sendRedirect(CJobServlet.SPATH);
            return;
        }

        if (!("/"+nonce).equals(req.getPathInfo())) {
            s_logger.log(Level.INFO, "bad nonce:"+req.getPathInfo()+","+
                         nonce);
            resp.sendRedirect(CJobServlet.SPATH);
            return;
        }

        String token = CUtils.nullIfEmpty(req.getParameter("token"));
        if (token == null) {
            // Also unexpected.
            s_logger.log(Level.INFO, "missing token");
            resp.sendRedirect(CJobServlet.SPATH);
            return;
        }

        String me = CUtils.nullIfEmpty(req.getParameter("me"));
        if (me == null) {
            s_logger.log(Level.INFO, "missing me");
            resp.sendRedirect(CJobServlet.SPATH);
            return;
        }

        String verify_me;
        // Launch bg check
        BufferedReader reader = null;
        try {
            URL url = new URL("https://indieauth.com/verify?token="+token);
            URLConnection con = url.openConnection();
            con.setReadTimeout(15*1000);
            con.setConnectTimeout(15*1000);
            reader = new BufferedReader
                (new InputStreamReader(con.getInputStream()));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            
            JSONObject json = new JSONObject(sb.toString());
            verify_me = json.optString("me");
        }
        catch (JSONException jse) {
            s_logger.log(Level.INFO, "Barfed on fetch: ", jse);
            throw new IOException(jse);
        }
        finally {
            if (reader != null) {
                try { reader.close(); }
                catch (IOException ign) {}
            }
        }

        if (!me.equals(verify_me)) {
            s_logger.log(Level.INFO, "mismatch: "+me+","+verify_me);
            resp.sendRedirect(CJobServlet.SPATH);
            return;
        }

        // Ok, good. Stuff id into session, nuke nonce and carry on.
        info.setUserID(me).setNonce(null);
        CServletUtils.commitUserInfo(req, info);
        resp.sendRedirect(CJobServlet.SPATH);
    }

    final static String SPATH = "/auth";

    private final static Logger s_logger = Logger.getLogger
        (CUtils.makeLogTag(CAuthServlet.class));
}
