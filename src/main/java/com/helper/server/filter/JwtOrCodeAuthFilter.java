package com.helper.server.filter;

import com.helper.server.entity.UserSession;
import com.helper.server.registry.UserSessionRegistry;
import com.helper.server.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtOrCodeAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwt;
    private final UserDetailsService uds;
    private final UserSessionRegistry sessions;

    public JwtOrCodeAuthFilter(JwtUtil jwt, UserDetailsService uds, UserSessionRegistry sessions) {
        this.jwt = jwt; this.uds = uds; this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String username = null;

            String auth = req.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring(7).trim();
                UserSession entry = sessions.byToken(token);
                if (entry != null) {
                    username = jwt.validateAndGetUser(token);
                }
            }

            if (username == null) {
                String code = req.getHeader("X-Auth-Code");
                if (code == null && auth != null && auth.regionMatches(true, 0, "Code ", 0, 5)) {
                    code = auth.substring(5).trim();
                }
                if (code != null && !code.isEmpty()) {
                    UserSession entry = sessions.byCode(code); // reusable until expiry
                    if (entry != null) {
                        username = jwt.validateAndGetUser(entry.token);
                    }
                }
            }

            if (username != null) {
                UserDetails u = uds.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authn = new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authn);
            }
        }

        chain.doFilter(req, res);
    }
}
