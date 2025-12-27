package com.paypal.user_service.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
//This filter executes before your controller and decides: “Is this request authenticated based on JWT?”
public class JWTrequestFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;
    public JWTrequestFilter(JWTUtil jwtUtil){
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        final String authorizationHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")){
            jwt = authorizationHeader.substring(7);
            try{
                username = jwtUtil.extractUsername(jwt); // this line is responisble for token validation as well.
            }catch (Exception e){
                //log
            }
        }
        // User is already authenitcated here.
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(username, null, null);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            // This line tells Spring Security: “This request is authenticated. Trust this user.”
        }

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            if (jwt == null || jwt.isBlank()) {
                chain.doFilter(request, response);
                return; // skip processing if token empty
            }
            try {
                username = jwtUtil.extractUsername(jwt);
                // only extract role if JWT is valid and present
                String role = jwtUtil.extractRole(jwt);
                // use role for authorities as needed
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                        );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);


                chain.doFilter(request, response);
                // chain.doFilter(request, response) calls the next filter in the chain.
                // If there are no more filters, it calls the target servlet/controller.
            } catch (Exception e) {
                // log error if you want
            }
        } else {
            chain.doFilter(request, response);
            return;
        }


    }
}