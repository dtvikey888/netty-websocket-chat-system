package com.yqrb.netty;


import com.alibaba.fastjson.JSON;
import com.yqrb.pojo.vo.WebSocketMsgVO;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * WebSocket消息编解码器（实现WebSocketMsg与WebSocketFrame的相互转换）
 */
public class WebSocketMsgCodec extends MessageToMessageCodec<WebSocketFrame, WebSocketMsgVO> {

    /**
     * 解码：将WebSocketFrame转换为WebSocketMsgVO
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
        // 1. 处理文本帧
        if (frame instanceof TextWebSocketFrame) {
            String jsonStr = ((TextWebSocketFrame) frame).text();
            WebSocketMsgVO webSocketMsg = JSON.parseObject(jsonStr, WebSocketMsgVO.class);
            out.add(webSocketMsg);
            return;
        }

        // 2. 处理二进制帧（简化处理，此处仅支持文本帧，二进制帧可扩展文件传输）
        if (frame instanceof BinaryWebSocketFrame) {
            ByteBuf byteBuf = ((BinaryWebSocketFrame) frame).content();
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bytes);
            String jsonStr = new String(bytes, StandardCharsets.UTF_8);
            WebSocketMsgVO webSocketMsg = JSON.parseObject(jsonStr, WebSocketMsgVO.class);
            out.add(webSocketMsg);
            return;
        }

        // 3. 不支持的帧类型
        throw new UnsupportedOperationException("不支持的WebSocket帧类型：" + frame.getClass().getSimpleName());
    }

    /**
     * 编码：将WebSocketMsgVO转换为WebSocketFrame
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, WebSocketMsgVO msg, List<Object> out) throws Exception {
        // 1. 将WebSocketMsgVO转换为JSON字符串
        String jsonStr = JSON.toJSONString(msg);

        // 2. 封装为文本WebSocket帧（优先使用文本帧，高效简洁）
        TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(jsonStr);
        out.add(textWebSocketFrame);

        // 3. 若需传输二进制数据，可封装为BinaryWebSocketFrame
        // ByteBuf byteBuf = Unpooled.copiedBuffer(jsonStr.getBytes(StandardCharsets.UTF_8));
        // BinaryWebSocketFrame binaryWebSocketFrame = new BinaryWebSocketFrame(byteBuf);
        // out.add(binaryWebSocketFrame);
    }
}
