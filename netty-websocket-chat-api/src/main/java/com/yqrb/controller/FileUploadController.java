package com.yqrb.controller;

import com.yqrb.pojo.vo.NewspaperFileRecordVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.FileUploadService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/newspaper/upload")
@Api(tags = "压缩图片上传接口")
public class FileUploadController {

    @Resource
    private FileUploadService fileUploadService;

    @PostMapping("/compress/image")
    @ApiOperation("上传图片（仅保留压缩版本，自动删除原图）")
    public Result<NewspaperFileRecordVO> uploadCompressImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("appId") String appId,
            @RequestParam("userId") String userId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return fileUploadService.uploadCompressImage(file, appId, userId, receiverId);
    }

    @GetMapping("/compress/list/{appId}")
    @ApiOperation("查询申请对应的压缩图片列表")
    public Result<List<NewspaperFileRecordVO>> getCompressFileList(
            @PathVariable String appId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return fileUploadService.getCompressFileListByAppId(appId, receiverId);
    }

    @GetMapping("/compress/detail/{fileId}")
    @ApiOperation("查询压缩图片详情")
    public Result<NewspaperFileRecordVO> getCompressFileDetail(
            @PathVariable String fileId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return fileUploadService.getCompressFileByFileId(fileId, receiverId);
    }

    @DeleteMapping("/compress/delete/{fileId}")
    @ApiOperation("删除压缩图片")
    public Result<Boolean> deleteCompressFile(
            @PathVariable String fileId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return fileUploadService.deleteCompressFile(fileId, receiverId);
    }
}