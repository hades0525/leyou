package com.leyou.auth.web;

import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.service.AuthService;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.CookieUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@EnableConfigurationProperties(JwtProperties.class)
public class AuthController {
    @Autowired
    private AuthService authService;

    @Value("${ly.jwt.cookieName}")
    private String cookieName;

    @Autowired
    private JwtProperties prop;
    /**
     * 用户登录
     * @param username
     * @param password
     * @param response
     * @param request
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @RequestParam("username")String username, @RequestParam("password")String password,
            HttpServletResponse response,HttpServletRequest request){
        //登录
        String token = authService.login(username, password);
        //写入cookie
        CookieUtils.newBuilder(response).httpOnly().request(request)
                    .build("LY_TOKEN",token);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    /**
     * 校验用户登录状态
     */
    @GetMapping("verify")
    public ResponseEntity<UserInfo> verify(
        @CookieValue("LY_TOKEN") String token,
        HttpServletResponse response,HttpServletRequest request
    ){
        try {
            //解析token
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, prop.getPublicKey());

            //刷新token,重新生成token
            String newToken = JwtUtils.generateToken(userInfo, prop.getPrivateKey(), prop.getExpire());

            //写入cookie
            CookieUtils.newBuilder(response).httpOnly().request(request)
                    .build("LY_TOKEN",newToken);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            //token已过期，或者token篡改了
            throw new LyException(ExceptionEnum.UNAUTHRIZED);
        }
    }
}
