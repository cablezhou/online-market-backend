package com.zhoucable.marketbackend.modules.user.VO;

import lombok.Data;

/**
 * 登录成功返回给前端的数据
 * @author 周开播
 * @Date 2025/10/21 16:45
 */

@Data
public class UserLoginVO {
    private Long userId;
    private String nickname;
    private String token;
}
