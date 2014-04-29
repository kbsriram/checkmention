package com.kbsriram.checkmention.servlet;

import com.google.appengine.api.utils.SystemProperty;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.tdom.TDom;
import static org.tdom.TDom.*;

public class CServletUtils
{
    // Cheap in-memory-session-based data.
    final static UserInfo getUserInfo(HttpServletRequest req)
    {
        HttpSession session = req.getSession(true);
        return new UserInfo
            ((String) session.getAttribute(USER_ID),
             (String) session.getAttribute(NONCE));
    }

    final static void commitUserInfo(HttpServletRequest req, UserInfo info)
    {
        HttpSession session = req.getSession(true);
        if (info.getUserID() == null) {
            session.removeAttribute(USER_ID);
        }
        else {
            session.setAttribute(USER_ID, info.getUserID());
        }
        if (info.getNonce() == null) {
            session.removeAttribute(NONCE);
        }
        else {
            session.setAttribute(NONCE, info.getNonce());
        }
    }

    final static void notfound
        (HttpServletRequest req, HttpServletResponse resp, String msg)
        throws IOException
    { err(HttpServletResponse.SC_NOT_FOUND, resp, msg); }

    final static void notimplemented
        (HttpServletRequest req, HttpServletResponse resp, String msg)
        throws IOException
    { err(HttpServletResponse.SC_NOT_IMPLEMENTED, resp, msg); }

    final static void notready
        (HttpServletRequest req, HttpServletResponse resp, String msg)
        throws IOException
    { err(HttpServletResponse.SC_SERVICE_UNAVAILABLE, resp, msg); }

    final static void forbid
        (HttpServletRequest req, HttpServletResponse resp, String msg)
        throws IOException
    { err(HttpServletResponse.SC_FORBIDDEN, resp, msg); }

    final static void err
        (int code, HttpServletResponse resp, String msg)
        throws IOException
    {
        owaspProtect(resp);
        resp.sendError(code, msg);
    }

    final static void owaspProtect(HttpServletResponse resp)
        throws IOException
    {
        resp.addHeader("X-Frame-Options", "deny");
        resp.addHeader("Frame-Options", "deny");
        resp.addHeader("X-XSS-Protection", "1; mode=block");
        resp.addHeader("X-Content-Type-Options", "nosniff");
        resp.addHeader("Content-Security-Policy", "default-src 'none'; style-src *; img-src *");
        resp.addHeader("X-Content-Security-Policy", "default-src 'none'; style-src *; img-src *");
        resp.addHeader("X-WebKit-CSP", "default-src 'none'; style-src *; img-src *");
        if (isProduction()) {
            // force ssl for six months.
            resp.addHeader("Strict-Transport-Security", "max-age=15768000");
        }
    }

    final static boolean isProduction()
    {
        return
            SystemProperty.environment.value() ==
            SystemProperty.Environment.Value.Production;
    }

    final static void write
        (HttpServletResponse resp, TNode content, String title)
        throws IOException
    {
        owaspProtect(resp);
        resp.setContentType("text/html;charset=utf-8");

        resp.getWriter().println("<!DOCTYPE html>");
        TNode html =
            n("html",
              a("lang", "en"),
              a("xmlns", "http://www.w3.org/1999/xhtml"),
              n("head",
                n("title", t(title)),
                n("meta", a("charset", "utf-8")),
                n("meta",
                  a("name", "viewport"),
                  a("content", "device-width,initial-scale=1.0")),
                n("link",
                  a("rel", "stylesheet"),
                  a("href", "/static/css/main.css"))),
              n("body", content));
        html.dump(resp.getWriter());
    }
    final static class UserInfo
    {
        private UserInfo(String id, String nonce)
        {
            m_id = id;
            m_nonce = nonce;
        }
        String getUserID() { return m_id; }
        String getNonce() { return m_nonce; }
        UserInfo setUserID(String id) {
            m_id = id;
            return this;
        }
        UserInfo setNonce(String nonce) {
            m_nonce = nonce;
            return this;
        }
        private String m_id;
        private String m_nonce;
    }
    private final static String USER_ID = "uid";
    private final static String NONCE = "nonce";
}
