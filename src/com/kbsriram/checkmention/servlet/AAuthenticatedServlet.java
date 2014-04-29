package com.kbsriram.checkmention.servlet;

import com.kbsriram.checkmention.util.CUtils;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.tdom.TDom;
import static org.tdom.TDom.*;

@SuppressWarnings("serial")
public abstract class AAuthenticatedServlet extends HttpServlet
{
    @Override
    public void service
        (HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        try {
            m_info = CServletUtils.getUserInfo(req);

            if (m_info.getUserID() == null) {
                showLogin(req, resp, m_info);
                return;
            }
            else {
                // proceed with normal flow.
                super.service(req, resp);
            }
        }
        finally {
            m_info = null;
        }
    }

    private final static void showLogin
        (HttpServletRequest req, HttpServletResponse resp,
         CServletUtils.UserInfo info)
        throws ServletException, IOException
    {
        String nonce = info.getNonce();
        if (nonce == null) {
            info.setNonce(CUtils.makeNonce());
            nonce = info.getNonce();
            CServletUtils.commitUserInfo(req, info);
        }

        String iaurl = "https://indieauth.com/auth";
        String redurl;
        if (CServletUtils.isProduction()) {
            redurl = "https://checkmention.appspot.com/auth/"+nonce;
        }
        else {
            redurl = "https://localhost:8080/auth/"+nonce;
        }

        TNode page =
            n("div",
              n("h2", t("Please login via indieauth")),
              n("form",
                a("class", "large"),
                a("action", iaurl),
                a("method", "get"),
                n("input",
                  a("type", "text"),
                  a("name", "me"),
                  a("autocomplete", "off"),
                  a("spellcheck", "false"),
                  a("required", ""),
                  a("placeholder", "yourdomain.com")),
                n("input",
                  a("type", "hidden"),
                  a("name", "redirect_uri"),
                  a("value", redurl)),
                n("input",
                  a("type", "submit"),
                  a("value", " Sign in "))));

        CServletUtils.write
            (resp, page, "Login");
    }

    protected CServletUtils.UserInfo getUserInfo()
    { return m_info; }

    private CServletUtils.UserInfo m_info;
}
