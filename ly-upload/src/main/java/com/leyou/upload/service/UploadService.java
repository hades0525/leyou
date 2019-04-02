package com.leyou.upload.service;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.upload.config.UploadProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@EnableConfigurationProperties(UploadProperties.class)
public class UploadService {

    //使用自定义配置
    @Autowired
    private UploadProperties prop;

    //上传到fdfs
    @Autowired
    private FastFileStorageClient storageClient;
    public  String uploadImage(MultipartFile file) {
        try {
            //校验文件类型
            String contentType = file.getContentType();
            if (!prop.getAllowTypes().contains(contentType)){
                throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
            }

            //校验文件内容
            BufferedImage image = ImageIO.read(file.getInputStream());
            if(image==null){
                throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
            }
            //上传到FastDFS
            //取得后缀
            String extension = StringUtils.substringAfterLast(file.getOriginalFilename(),".");
            StorePath storePath = storageClient.uploadFile(file.getInputStream(), file.getSize(), extension, null);

            //返回路径
            return prop.getBaseUrl()+storePath.getFullPath();
        } catch (IOException e) {
            //上传失败
            log.error("[文件上传] 上传文件失败",e);
            throw new LyException(ExceptionEnum.UPLOAD_FILE_ERROR);
        }
    }
}
