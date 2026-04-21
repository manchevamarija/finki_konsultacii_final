package mk.ukim.finki.konsultacii.web.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;


@Controller
public class LogoutController {

    @GetMapping("/logout-basic")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        response.setHeader("WWW-Authenticate", "Basic realm=\"logout\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().write(
                "<html><body>" +
                        "<script>" +
                        "  try {" +
                        "    var xhr = new XMLHttpRequest();" +
                        "    xhr.open('GET', '/consultations', false, 'logout', 'logout');" +
                        "    xhr.send();" +
                        "  } catch(e) {}" +
                        "  window.location.href = '/consultations';" +
                        "</script>" +
                        "<p>Одјавување... <a href='/consultations'>Кликни овде</a></p>" +
                        "</body></html>"
        );
    }
}