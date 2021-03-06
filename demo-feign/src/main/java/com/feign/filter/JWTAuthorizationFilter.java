package com.feign.filter;

import com.feign.jwt.TokenProvider;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.ResponseUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 *  进行鉴权了 验证token的有效性
 *  登录成功之后走此类进行鉴权操作
 * @Date 2020/6/11 11:20
 * @name JWTAuthorizationFilter
 */

@Slf4j
public class JWTAuthorizationFilter extends BasicAuthenticationFilter {
    public JWTAuthorizationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {

        String tokenHeader = request.getHeader(TokenProvider.TOKEN_HEADER);
        // 如果请求头中没有Authorization信息则直接放行了
        if (tokenHeader == null || !tokenHeader.startsWith(TokenProvider.TOKEN_PREFIX)) {
            //chain.doFilter(request, response);
            ResUtils.responseJson(response,"没有有效的token!");
            return;
        }
        log.info("验证token>>>");
        // 如果请求头中有token，则进行解析，并且设置认证信息
        SecurityContextHolder.getContext().setAuthentication(getAuthentication(tokenHeader));
        super.doFilterInternal(request, response, chain);
    }

    // 这里从token中获取用户信息
    private UsernamePasswordAuthenticationToken getAuthentication(String tokenHeader) {
        String token = tokenHeader.replace(TokenProvider.TOKEN_PREFIX, "");
        String username = TokenProvider.getUsername(token);
        String role = TokenProvider.getUserRole(token);
        if (username != null && !Strings.isNullOrEmpty(role)) {
            log.info("token验证通过!");
            return new UsernamePasswordAuthenticationToken(username, null, Collections.singleton(new SimpleGrantedAuthority(role)));

        }else{
            log.info("{}用户没有权限",username);
        }
        return null;
    }
}
