package cn.org.shelly.picporter.service.impl;

import cn.org.shelly.picporter.mapper.FileMapper;
import cn.org.shelly.picporter.model.po.File;


import cn.org.shelly.picporter.service.IFileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
* @author Shelly6
* @description 针对表【file(用户文件表)】的数据库操作Service实现
* @createDate 2025-06-05 21:16:21
*/
@Service
public class FileService extends ServiceImpl<FileMapper, File>
    implements IFileService {

}




