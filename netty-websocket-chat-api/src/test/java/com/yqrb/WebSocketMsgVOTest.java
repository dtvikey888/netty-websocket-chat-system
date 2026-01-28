package com.yqrb;

import com.alibaba.fastjson.JSON;
import com.yqrb.pojo.vo.WebSocketMsgVO;

import java.util.Date;

public class WebSocketMsgVOTest {
    public static void main(String[] args) {
        // 1. 模拟客户端发送的JSON字符串
        String clientJson = "{\n" +
                "  \"receiverId\": \"R_FIXED_0000_LYQY_CS_5fc5bff4b77d2e6436a618aa\",\n" +
                "  \"userId\": \"LYQY_USER_5fbb6357b77d2e6436a46336\",\n" +
                "  \"msgContent\": \"请问登报遗失声明需要多少费用？\",\n" +
                "  \"msgType\": \"TEXT\",\n" +
                "  \"sessionId\": \"SESSION_cf22da7ebff04caa9b40f61a41d0f465\"\n" +
                "}";

        // 2. 反序列化为VO（模拟编解码器的decode过程）
        WebSocketMsgVO vo = JSON.parseObject(clientJson, WebSocketMsgVO.class);
        System.out.println("反序列化结果：");
        System.out.println("receiverId: " + vo.getReceiverId());
        System.out.println("userId: " + vo.getUserId());
        System.out.println("msgContent: " + vo.getMsgContent());

        // 3. 补充sendTime并序列化（模拟编解码器的encode过程）
        vo.setSendTime(new Date());
        String serverJson = JSON.toJSONString(vo);
        System.out.println("\n序列化结果（含sendTime）：");
        System.out.println(serverJson);
    }
}