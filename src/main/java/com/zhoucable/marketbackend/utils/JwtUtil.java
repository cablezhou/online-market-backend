package com.zhoucable.marketbackend.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT工具类，用于生成、校验Token
 * @author 周开播
 * @Date 2025/10/21 16:59
 */
@Component
public class JwtUtil {

    //从application.yml中注入密钥
    @Value("${jwt.secret}")
    private String secret;

    /**
     * -- GETTER --
     *  获取配置的过期时间（秒）
     *
     * @return expireTime
     */
    //注入过期时间（单位：秒）
    @Getter
    @Value("${jwt.expire-time}")
    private long expireTime;

    /**
     * 根据用户ID生成Token
     * @param userId 用户ID
     * @return 生成的JWT Token字符串
     */
    public String generateToken(Long userId){

        //计算过期时间点
        Date expireDate = new Date(System.currentTimeMillis() + expireTime * 1000);

        //使用HMAC256算法和密钥创建签名
        Algorithm algorithm = Algorithm.HMAC256(secret);

        return JWT.create()
                //将userId存入JWT的payload（载荷）中
                .withClaim("userId", userId)
                //设置Token的过期时间
                .withExpiresAt(expireDate)
                //使用指定的算法进行签名
                .sign(algorithm);
    }

    /**
     * 验证Token的合法性并解析出用户ID
     * @param token 待验证的Token字符串
     * @return 若验证成功，返回用户ID；若失败，返回null或抛出异常
     */
    public Long validateTokenAndGetUserId(String token){
        try{
            //使用相同的算法和密钥创建验证器
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm).build();

            //验证Token
            DecodedJWT jwt = verifier.verify(token);

            //从payload中提取userId
            return jwt.getClaim("userId").asLong();
        }catch (JWTVerificationException exception){
            //Token验证失败（例如签名不匹配、过期了等等）
            //生产环境要记录日志
            return null;
        }
    }


}
