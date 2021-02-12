package com.leyou.updownload.service;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class UploadService {

    @Resource
    private FastFileStorageClient fastFileStorageClient;

    //定义允许的图片类型
    private static final List<String> CONTENT_TYPES = Arrays.asList("image/jpeg","image/gif");

    //获取logger对象
    private static final Logger LOGGER =LoggerFactory.getLogger(UploadService.class);

    public String upload(MultipartFile file) {

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        if(!CONTENT_TYPES.contains(contentType)){
            LOGGER.info("文件内容不合法!+originalFilename");
            return null;
        }

        try {
            // 校验文件的内容
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            if (bufferedImage == null){
                LOGGER.info("文件内容不合法"+originalFilename);
                return null;
            }

            // 保存到本地服务器
            //file.transferTo(new File("C:\\Users\\13275\\Documents\\Uploads\\leyou" ,originalFilename));
            // 生成url地址，返回
            //return "http://image.leyou.com/" + originalFilename;

            //保存到图片服务器
            String ext = StringUtils.substringAfter(originalFilename,".");
            StorePath storePath = fastFileStorageClient.uploadFile(file.getInputStream(),file.getSize(),ext,null);
            // 生成url地址，返回
            return "http://image.leyou.com/"+storePath.getFullPath();

        } catch (IOException e) {
            LOGGER.info("服务器内部错误!+originalFilename");
            e.printStackTrace();
        }
        return null;

    }
}
