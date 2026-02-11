package com.yqrb.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.yqrb.netty.constant.NettyConstant;
import com.yqrb.pojo.vo.ChatMessageVO;
import com.yqrb.pojo.vo.WebSocketMsgVO;
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
 * WebSocket消息编解码器（实现WebSocketMsg与WebSocketFrame的相互转换）
 * 优化：同时支持标准JSON消息和纯文本消息，补全纯文本消息核心业务字段，整合全局常量类
 * 新增：纯文本消息支持「receiverId:xxx|sessionId:xxx|真实消息」格式，兼容旧格式
 */
public class WebSocketMsgCodec extends MessageToMessageCodec<WebSocketFrame, WebSocketMsgVO> {
    // 新增：使用SLF4J日志，与处理器保持一致
    private static final Logger logger = LoggerFactory.getLogger(WebSocketMsgCodec.class);

    /**
     * 解码：将WebSocketFrame转换为WebSocketMsgVO（兼容JSON和纯文本，补全核心字段）
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
        String clientId = ctx.channel().id().asShortText();
        logger.info("【编解码器】客户端{}发送了帧类型：{}", clientId, frame.getClass().getSimpleName());

        try {
            if (frame instanceof TextWebSocketFrame) {
                String msgContent = ((TextWebSocketFrame) frame).text();
                logger.info("【解码】客户端{}发送文本帧：{}", clientId, msgContent);

                WebSocketMsgVO webSocketMsg;
                try {
                    // 第一步：尝试按JSON格式解析（支持附件字段）
                    webSocketMsg = JSON.parseObject(msgContent, WebSocketMsgVO.class);
                } catch (JSONException e) {
                    // 第二步：纯文本消息，支持附件格式扩展
                    logger.info("【解码】客户端{}发送的是纯文本消息，手动封装VO", clientId);
                    webSocketMsg = new WebSocketMsgVO();

                    // 扩展格式：receiverId:xxx|sessionId:xxx|msgType:ATTACHMENT|attachmentPath:xxx,xxx|content:xxx
                    String targetReceiverId = null;
                    String customSessionId = null;
                    String realMsgContent = msgContent;
                    String attachmentPath = null;
                    String msgType = ChatMessageVO.MSG_TYPE_TEXT; // 默认文本

                    if (msgContent.contains("|")) {
                        String[] msgParts = msgContent.split("\\|", 5); // 扩展为5部分，支持附件

                        // 新格式：receiverId:xxx|sessionId:xxx|msgType:ATTACHMENT|attachmentPath:xxx|content:xxx
                        if (msgParts.length >= 5
                                && msgParts[0].startsWith("receiverId:")
                                && msgParts[1].startsWith("sessionId:")
                                && msgParts[2].startsWith("msgType:")
                                && msgParts[3].startsWith("attachmentPath:")) {
                            targetReceiverId = msgParts[0].substring("receiverId:".length()).trim();
                            customSessionId = msgParts[1].substring("sessionId:".length()).trim();
                            msgType = msgParts[2].substring("msgType:".length()).trim();
                            attachmentPath = msgParts[3].substring("attachmentPath:".length()).trim();
                            realMsgContent = msgParts[4].trim();
                        } else if (msgParts.length >= 2 && msgParts[0].startsWith("receiverId:")) {
                            // 兼容旧格式
                            targetReceiverId = msgParts[0].substring("receiverId:".length()).trim();
                            realMsgContent = msgParts[1].trim();
                            logger.warn("【解码】客户端{}使用旧格式纯文本，无自定义sessionId，将用通道自身ID兜底", clientId);
                        }
                    }

                    // 从Channel获取上下文信息
                    String channelSelfId = ctx.channel().attr(NettyConstant.SESSION_ID_KEY).get();
                    String senderType = ctx.channel().attr(NettyConstant.SENDER_TYPE_KEY).get();
                    String userId = ctx.channel().attr(NettyConstant.USER_ID_KEY).get();

                    // 填充字段（包含附件路径）
                    webSocketMsg.setMsgContent(realMsgContent);
                    webSocketMsg.setMsgType(msgType);
                    webSocketMsg.setAttachmentPath(attachmentPath); // 赋值附件路径
                    webSocketMsg.setSendTime(new Date());
                    webSocketMsg.setReceiverId(targetReceiverId);
                    webSocketMsg.setSessionId(customSessionId != null ? customSessionId : channelSelfId);
                    webSocketMsg.setSenderType(senderType != null ? senderType : WebSocketMsgVO.SENDER_TYPE_USER);
                    webSocketMsg.setUserId(userId != null ? userId : channelSelfId);
                }

                if (webSocketMsg != null) {
                    out.add(webSocketMsg);
                }
                return;
            }

            // 二进制帧处理（可用于直接传输文件字节，可选扩展）
            if (frame instanceof BinaryWebSocketFrame) {
                ByteBuf byteBuf = ((BinaryWebSocketFrame) frame).content();
                int readerIndex = byteBuf.readerIndex();
                byte[] bytes = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(bytes);
                byteBuf.readerIndex(readerIndex);

                String jsonStr = new String(bytes, StandardCharsets.UTF_8);
                logger.info("【解码】客户端{}发送二进制帧：{}", clientId, jsonStr);
                WebSocketMsgVO webSocketMsg = JSON.parseObject(jsonStr, WebSocketMsgVO.class);
                out.add(webSocketMsg);
                return;
            }

            // 心跳/关闭帧处理
            if (frame instanceof PingWebSocketFrame) {
                logger.info("【解码】客户端{}发送Ping帧，自动回复Pong", clientId);
                ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
                return;
            }
            if (frame instanceof PongWebSocketFrame) {
                logger.info("【解码】客户端{}发送Pong帧（心跳响应）", clientId);
                return;
            }
            if (frame instanceof CloseWebSocketFrame) {
                logger.info("【解码】客户端{}发送Close帧，关闭通道", clientId);
                frame.retain();
                ctx.channel().writeAndFlush(frame);
                ctx.channel().close();
                return;
            }

            logger.error("【解码】客户端{}发送不支持的帧类型：{}", clientId, frame.getClass().getSimpleName());

        } catch (Exception e) {
            logger.error("【解码】客户端{}解码异常：{}", clientId, e.getMessage(), e);
        }
    }
    /**
     * 编码：将WebSocketMsgVO转换为WebSocketFrame（保留原有逻辑，无需修改）
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, WebSocketMsgVO msg, List<Object> out) throws Exception {
        String clientId = ctx.channel().id().asShortText();
        try {
            // 1. 将WebSocketMsgVO转换为JSON字符串
            String jsonStr = JSON.toJSONString(msg);
            logger.info("【编码】服务端向客户端{}发送文本帧：{}", clientId, jsonStr);

            // 2. 封装为文本WebSocket帧（优先使用文本帧，高效简洁）
            TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(jsonStr);
            out.add(textWebSocketFrame);

        } catch (JSONException e) {
            logger.error("【编码】服务端JSON编码失败：{}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("【编码】服务端编码异常：{}", e.getMessage(), e);
        }
    }
}