package com.yqrb;

import com.yqrb.util.UUIDUtil;
import org.junit.Test;

/**
 * 测试乐音清扬动态appId的固定ReceiverId生成
 */
public class LyqyReceiverIdTest {

    @Test
    public void testGenerateLyqyFixedReceiverId() {
        // 测试不同用户专属appId的固定ID生成
        String user1Id = UUIDUtil.generateLyqyFixedReceiverId("USER", "001");
        String user2Id = UUIDUtil.generateLyqyFixedReceiverId("USER", "abc123");
        String admin1Id = UUIDUtil.generateLyqyFixedReceiverId("ADMIN", "002");
        String cs1Id = UUIDUtil.generateLyqyFixedReceiverId("CS", "888xyz");

        // 打印结果（固定不变，仅appId不同）
        System.out.println("乐音清扬用户1（appId=001）ReceiverId：" + user1Id);
        System.out.println("乐音清扬用户2（appId=abc123）ReceiverId：" + user2Id);
        System.out.println("乐音清扬管理员（appId=002）ReceiverId：" + admin1Id);
        System.out.println("乐音清扬客服（appId=888xyz）ReceiverId：" + cs1Id);
    }
}