package com.zhoucable.marketbackend.modules.pay.VO;

import lombok.Data;

/**
 * 发起支付成功后，返回给前端调取支付SDK所需的信息
 * FR-PAY-001的响应
 */
@Data
public class PaymentSubmitVO {
    // 这里的字段将根据所选支付方式（微信、支付宝）动态填充
    // 例如微信支付可能需要：
    // private String prepayId;
    // private String nonceStr;
    // private String timeStamp;
    // private String sign;

    // 开发阶段先返回一个模拟的 "payUrl"
    private String mockPayUrl;
}
