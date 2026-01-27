package com.yqrb.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.yqrb.pojo.vo.WebSocketMsgVO;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * WebSocket消息编解码器（实现WebSocketMsg与WebSocketFrame的相互转换）
 * 修复：补充调试日志、异常捕获、非业务帧处理
 */
public class WebSocketMsgCodec extends MessageToMessageCodec<WebSocketFrame, WebSocketMsgVO> {

    /**
     * 解码：将WebSocketFrame转换为WebSocketMsgVO
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
        String clientId = ctx.channel().id().asShortText();
        // 新增：全局日志，只要进入解码方法就打印（无论帧类型）
        System.out.println("【编解码器】客户端" + clientId + "发送了帧类型：" + frame.getClass().getSimpleName());

        try {
            // 1. 处理文本帧（核心业务帧）
            if (frame instanceof TextWebSocketFrame) {
                String jsonStr = ((TextWebSocketFrame) frame).text();
                // 关键：打印原始消息（调试日志，确认收到消息）
                System.out.println("【解码】客户端" + clientId + "发送文本帧：" + jsonStr);
                WebSocketMsgVO webSocketMsg = JSON.parseObject(jsonStr, WebSocketMsgVO.class);
                out.add(webSocketMsg);
                return;
            }

            // 2. 处理二进制帧（扩展用，如文件传输）
            if (frame instanceof BinaryWebSocketFrame) {
                ByteBuf byteBuf = ((BinaryWebSocketFrame) frame).content();
                // 修复：读取前记录readerIndex，读取后重置
                int readerIndex = byteBuf.readerIndex();
                byte[] bytes = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(bytes);
                byteBuf.readerIndex(readerIndex); // 重置索引

                String jsonStr = new String(bytes, StandardCharsets.UTF_8);
                System.out.println("【解码】客户端" + clientId + "发送二进制帧：" + jsonStr);
                WebSocketMsgVO webSocketMsg = JSON.parseObject(jsonStr, WebSocketMsgVO.class);
                out.add(webSocketMsg);
                return;
            }

            // 3. 处理心跳/关闭帧（非业务帧，避免抛异常）
            if (frame instanceof PingWebSocketFrame) {
                System.out.println("【解码】客户端" + clientId + "发送Ping帧，自动回复Pong");
                ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
                return;
            }
            if (frame instanceof PongWebSocketFrame) {
                System.out.println("【解码】客户端" + clientId + "发送Pong帧（心跳响应）");
                return;
            }
            if (frame instanceof CloseWebSocketFrame) {
                System.out.println("【解码】客户端" + clientId + "发送Close帧，关闭通道");
                frame.retain();
                ctx.channel().writeAndFlush(frame);
                ctx.channel().close();
                return;
            }

            // 4. 不支持的帧类型（仅打印日志，不抛异常）
            System.err.println("【解码】客户端" + clientId + "发送不支持的帧类型：" + frame.getClass().getSimpleName());

        } catch (JSONException e) {
            // 关键：捕获JSON解析异常，打印日志（避免通道关闭）
            System.err.println("【解码】客户端" + clientId + "JSON解析失败：" + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("【解码】客户端" + clientId + "解码异常：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 编码：将WebSocketMsgVO转换为WebSocketFrame
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, WebSocketMsgVO msg, List<Object> out) throws Exception {
        String clientId = ctx.channel().id().asShortText();
        try {
            // 1. 将WebSocketMsgVO转换为JSON字符串
            String jsonStr = JSON.toJSONString(msg);
            System.out.println("【编码】服务端向客户端" + clientId + "发送文本帧：" + jsonStr);

            // 2. 封装为文本WebSocket帧（优先使用文本帧，高效简洁）
            TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(jsonStr);
            out.add(textWebSocketFrame);

        } catch (JSONException e) {
            System.err.println("【编码】服务端JSON编码失败：" + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("【编码】服务端编码异常：" + e.getMessage());
            e.printStackTrace();
        }
    }
}