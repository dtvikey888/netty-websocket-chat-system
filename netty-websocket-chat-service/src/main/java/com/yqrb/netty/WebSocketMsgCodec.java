package com.yqrb.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.yqrb.netty.constant.NettyConstant; // 新增：导入全局常量类
import com.yqrb.pojo.vo.WebSocketMsgVO;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger; // 新增：SLF4J日志导入
import org.slf4j.LoggerFactory; // 新增：SLF4J日志导入

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * WebSocket消息编解码器（实现WebSocketMsg与WebSocketFrame的相互转换）
 * 优化：同时支持标准JSON消息和纯文本消息，补全纯文本消息核心业务字段，整合全局常量类
 */
public class WebSocketMsgCodec extends MessageToMessageCodec<WebSocketFrame, WebSocketMsgVO> {
    // 新增：使用SLF4J日志，与处理器保持一致
    private static final Logger logger = LoggerFactory.getLogger(WebSocketMsgCodec.class);

    // 移除：删除本地自定义的AttributeKey，改用NettyConstant中的全局常量

    /**
     * 解码：将WebSocketFrame转换为WebSocketMsgVO（兼容JSON和纯文本，补全核心字段）
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
        String clientId = ctx.channel().id().asShortText();
        // 修改：替换System.out为SLF4J日志
        logger.info("【编解码器】客户端{}发送了帧类型：{}", clientId, frame.getClass().getSimpleName());

        try {
            // 1. 处理文本帧（核心业务帧：JSON + 纯文本）
            if (frame instanceof TextWebSocketFrame) {
                String msgContent = ((TextWebSocketFrame) frame).text();
                // 修改：替换System.out为SLF4J日志
                logger.info("【解码】客户端{}发送文本帧：{}", clientId, msgContent);

                WebSocketMsgVO webSocketMsg = null;
                try {
                    // 第一步：尝试按JSON格式解析（兼容原有标准JSON消息）
                    webSocketMsg = JSON.parseObject(msgContent, WebSocketMsgVO.class);
                } catch (JSONException e) {
                    // 第二步：JSON解析失败，判定为纯文本消息，手动封装WebSocketMsgVO（补全核心字段）
                    logger.info("【解码】客户端{}发送的是纯文本消息，手动封装VO", clientId);
                    webSocketMsg = new WebSocketMsgVO();

                    // 修改：使用NettyConstant全局常量，从Channel上下文获取绑定的业务字段
                    String receiverId = ctx.channel().attr(NettyConstant.RECEIVER_ID_KEY).get();
                    String sessionId = ctx.channel().attr(NettyConstant.SESSION_ID_KEY).get();
                    String senderType = ctx.channel().attr(NettyConstant.SENDER_TYPE_KEY).get();
                    String userId = ctx.channel().attr(NettyConstant.USER_ID_KEY).get();

                    // 填充纯文本消息的完整字段（解决receiverId为空问题）
                    webSocketMsg.setMsgContent(msgContent); // 纯文本内容
                    webSocketMsg.setMsgType(WebSocketMsgVO.MSG_TYPE_TEXT); // 文本消息类型
                    webSocketMsg.setSendTime(new Date()); // 发送时间（当前时间）
                    webSocketMsg.setReceiverId(receiverId); // 补全接收者ID（核心：解决为空报错）
                    webSocketMsg.setSessionId(sessionId); // 补全会话ID（保持业务数据完整）
                    webSocketMsg.setSenderType(senderType != null ? senderType : WebSocketMsgVO.SENDER_TYPE_USER); // 补全发送者类型（默认用户）
                    webSocketMsg.setUserId(userId != null ? userId : sessionId); // 优化：userId为空时，用sessionId兜底（避免null）
                }

                // 无论JSON还是纯文本，最终将VO加入输出列表，传递给后续业务处理器
                if (webSocketMsg != null) {
                    out.add(webSocketMsg);
                }
                return;
            }

            // 2. 处理二进制帧（扩展用，如文件传输，保留原有逻辑）
            if (frame instanceof BinaryWebSocketFrame) {
                ByteBuf byteBuf = ((BinaryWebSocketFrame) frame).content();
                // 修复：读取前记录readerIndex，读取后重置
                int readerIndex = byteBuf.readerIndex();
                byte[] bytes = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(bytes);
                byteBuf.readerIndex(readerIndex); // 重置索引

                String jsonStr = new String(bytes, StandardCharsets.UTF_8);
                logger.info("【解码】客户端{}发送二进制帧：{}", clientId, jsonStr);
                WebSocketMsgVO webSocketMsg = JSON.parseObject(jsonStr, WebSocketMsgVO.class);
                out.add(webSocketMsg);
                return;
            }

            // 3. 处理心跳/关闭帧（非业务帧，避免抛异常，保留原有逻辑）
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

            // 4. 不支持的帧类型（仅打印日志，不抛异常，保留原有逻辑）
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