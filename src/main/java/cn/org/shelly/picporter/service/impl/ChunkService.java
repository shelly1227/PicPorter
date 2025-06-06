package cn.org.shelly.picporter.service.impl;

import cn.org.shelly.picporter.mapper.ChunkMapper;
import cn.org.shelly.picporter.model.po.Chunk;
import cn.org.shelly.picporter.service.IChunkService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ChunkService extends ServiceImpl<ChunkMapper, Chunk>
    implements IChunkService{

}


