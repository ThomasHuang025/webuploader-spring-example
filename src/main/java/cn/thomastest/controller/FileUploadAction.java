package cn.thomastest.controller;

import cn.thomastest.controller.request.MultipartFileParam;
import cn.thomastest.util.MultipartFileUploadUtil;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Administrator on 2018/1/10/010.
 */
@Controller
@RequestMapping("/file")
public class FileUploadAction {
    private static AtomicLong counter = new AtomicLong(0L);

    @RequestMapping(method = {RequestMethod.POST}, value = {"test-upload2"})
    public void uploadv2(HttpServletRequest request) throws Exception {

        String prefix = "req_count:" + counter.incrementAndGet() + ":";
        System.out.println(prefix + "start !!!");
        //使用 工具类解析相关参数，工具类代码见下面
        MultipartFileParam param = MultipartFileUploadUtil.parse(request);
        System.out.println(prefix + "chunks= " + param.getChunks());
        System.out.println(prefix + "chunk= " + param.getChunk());
        System.out.println(prefix + "chunkSize= " + param.getParam().get("chunkSize"));
        //这个必须与前端设定的值一致
        long chunkSize = 512 * 1024;

        if (param.isMultipart()) {

            String finalDirPath = "/data0/uploads/";
            String tempDirPath = finalDirPath + param.getId();
            String tempFileName = param.getFileName() + "_tmp";
            File confFile = new File(tempDirPath, param.getFileName() + ".conf");
            File tmpDir = new File(tempDirPath);
            File tmpFile = new File(tempDirPath, tempFileName);
            if (!tmpDir.exists()) {
                tmpDir.mkdirs();
            }

            RandomAccessFile accessTmpFile = new RandomAccessFile(tmpFile, "rw");
            RandomAccessFile accessConfFile = new RandomAccessFile(confFile, "rw");

            long offset = chunkSize * param.getChunk();
            //定位到该分片的偏移量
            accessTmpFile.seek(offset);
            //写入该分片数据
            accessTmpFile.write(param.getFileItem().get());

            //把该分段标记为 true 表示完成
            System.out.println(prefix + "set part " + param.getChunk() + " complete");
            accessConfFile.setLength(param.getChunks());
            accessConfFile.seek(param.getChunk());
            accessConfFile.write(Byte.MAX_VALUE);

            //completeList 检查是否全部完成,如果数组里是否全部都是(全部分片都成功上传)
            byte[] completeList = FileUtils.readFileToByteArray(confFile);
            byte isComplete = Byte.MAX_VALUE;
            for (int i = 0; i < completeList.length && isComplete==Byte.MAX_VALUE; i++) {
                //与运算, 如果有部分没有完成则 isComplete 不是 Byte.MAX_VALUE
                isComplete = (byte)(isComplete & completeList[i]);
                System.out.println(prefix + "check part " + i + " complete?:" + completeList[i]);
            }

            if (isComplete == Byte.MAX_VALUE) {
                System.out.println(prefix + "upload complete !!");
            }
            accessTmpFile.close();
            accessConfFile.close();
        }
        System.out.println(prefix + "end !!!");
    }
}
