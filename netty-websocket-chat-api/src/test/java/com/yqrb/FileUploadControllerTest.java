package com.yqrb;

import com.yqrb.controller.FileUploadController;
import com.yqrb.pojo.vo.NewspaperFileRecordVO;
import com.yqrb.pojo.vo.ReceiverIdSessionVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.ReceiverIdService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

/**
 * 压缩图片上传接口全量测试
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
@Rollback(true)
public class FileUploadControllerTest {

    @Autowired
    private FileUploadController fileUploadController;

    @Autowired
    private ReceiverIdService receiverIdService;

    // 通用生成测试ReceiverId
    private String getTestReceiverId(String userId, String userName) {
        ReceiverIdSessionVO session = receiverIdService.generateReceiverId(userId, userName);
        return session.getReceiverId();
    }

    // 模拟测试图片文件
    private MultipartFile getMockImageFile() {
        // 模拟1KB的测试图片（字节数组）
        byte[] imageBytes = new byte[1024];
        try {
            return new MockMultipartFile(
                    "file",                  // 参数名
                    "test_image.jpg",        // 文件名
                    "image/jpeg",            // 文件类型
                    new ByteArrayInputStream(imageBytes) // 文件流
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 测试：上传压缩图片 /newspaper/upload/compress/image
     */
    @Test
    public void testUploadCompressImage() {
        // 1. 构建参数
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        MultipartFile mockFile = getMockImageFile();
        String appId = "APP_TEST_001";
        String userId = "user_001";

        // 2. 调用接口
        Result<NewspaperFileRecordVO> result = fileUploadController.uploadCompressImage(mockFile, appId, userId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "上传压缩图片返回码非200";
        assert result.getData() != null : "文件上传返回数据为空";
        assert result.getData().getFileId() != null : "文件ID为空";
        System.out.println("上传压缩图片接口测试通过，文件ID：" + result.getData().getFileId());
    }

    /**
     * 测试：查询申请对应的压缩图片列表 /newspaper/upload/compress/list/{appId}
     */
    @Test
    public void testGetCompressFileList() {
        // 1. 先上传一张测试图片
        testUploadCompressImage();
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        String appId = "APP_TEST_001";

        // 2. 调用接口
        Result<List<NewspaperFileRecordVO>> result = fileUploadController.getCompressFileList(appId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "查询压缩图片列表返回码非200";
        assert result.getData() != null && !result.getData().isEmpty() : "图片列表为空";
        System.out.println("查询申请压缩图片列表接口测试通过，共" + result.getData().size() + "张图片");
    }

    /**
     * 测试：查询压缩图片详情 /newspaper/upload/compress/detail/{fileId}
     */
    @Test
    public void testGetCompressFileDetail() {
        // 1. 先上传测试图片并获取fileId
        Result<NewspaperFileRecordVO> uploadResult = testUploadCompressImageReturnResult();
        String fileId = uploadResult.getData().getFileId();
        String receiverId = getTestReceiverId("user_001", "测试用户1");

        // 2. 调用接口
        Result<NewspaperFileRecordVO> result = fileUploadController.getCompressFileDetail(fileId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "查询压缩图片详情返回码非200";
        assert result.getData() != null : "图片详情为空";
        assert result.getData().getFileId().equals(fileId) : "文件ID不一致";
        System.out.println("查询压缩图片详情接口测试通过");
    }

    /**
     * 测试：删除压缩图片 /newspaper/upload/compress/delete/{fileId}
     */
    @Test
    public void testDeleteCompressFile() {
        // 1. 先上传测试图片并获取fileId
        Result<NewspaperFileRecordVO> uploadResult = testUploadCompressImageReturnResult();
        String fileId = uploadResult.getData().getFileId();
        String receiverId = getTestReceiverId("user_001", "测试用户1");

        // 2. 调用接口
        Result<Boolean> result = fileUploadController.deleteCompressFile(fileId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "删除压缩图片返回码非200";
        assert result.getData() == true : "删除图片失败";
        System.out.println("删除压缩图片接口测试通过");
    }

    // 辅助方法：上传图片并返回结果（用于获取fileId）
    private Result<NewspaperFileRecordVO> testUploadCompressImageReturnResult() {
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        MultipartFile mockFile = getMockImageFile();
        String appId = "APP_TEST_001";
        String userId = "user_001";
        return fileUploadController.uploadCompressImage(mockFile, appId, userId, receiverId);
    }
}