package com.yqrb.netty.pre;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.yqrb.netty.constant.NettyConstant;
import com.yqrb.pojo.vo.PreSaleChatMessageVO;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * 售前WebSocket消息编解码器（完全独立，适配售前VO）
 */
public class PreSaleWebSocketMsgCodec extends MessageToMessageCodec<WebSocketFrame, PreSaleChatMessageVO> {
    private static final Logger logger = LoggerFactory.getLogger(PreSaleWebSocketMsgCodec.class);

    /**
     * 解码：WebSocketFrame -> PreSaleChatMessageVO
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
        String channelId = ctx.channel().id().asShortText();
        if (frame instanceof TextWebSocketFrame) {
            String msgContent = ((TextWebSocketFrame) frame).text();
            logger.info("【售前-解码】通道ID：{}，接收内容：{}", channelId, msgContent);
            PreSaleChatMessageVO vo = null;

            // 尝试JSON解析
            try {
                vo = JSON.parseObject(msgContent, PreSaleChatMessageVO.class);
            } catch (JSONException e) {
                // 纯文本格式解析：receiverId:xxx|preSaleSessionId:xxx|消息内容
                vo = new PreSaleChatMessageVO();
                String targetReceiverId = null;
                String realContent = msgContent;

                if (msgContent.contains("|")) {
                    String[] parts = msgContent.split("\\|", 3);
                    if (parts.length >= 2 && parts[0].startsWith("receiverId:")) {
                        targetReceiverId = parts[0].substring("receiverId:".length()).trim();
                        realContent = parts[1].trim();
                    }
                }

                vo.setReceiverId(targetReceiverId);
                vo.setContent(realContent);
                logger.info("【售前-解码】通道ID：{}，纯文本消息封装完成", channelId);
            }

            if (vo != null) {
                out.add(vo);
            }
            return;
        }

        // 二进制帧处理
        if (frame instanceof BinaryWebSocketFrame) {
            ByteBuf byteBuf = ((BinaryWebSocketFrame) frame).content();
            int readerIndex = byteBuf.readerIndex();
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bytes);
            byteBuf.readerIndex(readerIndex);
            String jsonStr = new String(bytes, StandardCharsets.UTF_8);
            PreSaleChatMessageVO vo = JSON.parseObject(jsonStr, PreSaleChatMessageVO.class);
            out.add(vo);
            return;
        }

        // 心跳/关闭帧处理
        if (frame instanceof PingWebSocketFrame) {
            logger.info("【售前-解码】通道ID：{}，接收Ping帧，回复Pong", channelId);
            ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (frame instanceof CloseWebSocketFrame) {
            logger.info("【售前-解码】通道ID：{}，接收Close帧，关闭通道", channelId);
            frame.retain();
            ctx.channel().writeAndFlush(frame);
            ctx.channel().close();
        }
    }

    /**
     * 编码：PreSaleChatMessageVO -> WebSocketFrame
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, PreSaleChatMessageVO msg, List<Object> out) throws Exception {
        String channelId = ctx.channel().id().asShortText();
        try {
            String jsonStr = JSON.toJSONString(msg);
            logger.info("【售前-编码】通道ID：{}，发送内容：{}", channelId, jsonStr);
            out.add(new TextWebSocketFrame(jsonStr));
        } catch (Exception e) {
            logger.error("【售前-编码失败】通道ID：{}，异常：{}", channelId, e.getMessage(), e);
        }
    }
}