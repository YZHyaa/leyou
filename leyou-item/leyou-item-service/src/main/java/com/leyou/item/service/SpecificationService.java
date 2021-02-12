package com.leyou.item.service;

import com.leyou.item.dao.SpecGroupDao;
import com.leyou.item.dao.SpecParamDao;
import com.leyou.pojo.SpecGroup;
import com.leyou.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class SpecificationService {

    @Resource
    SpecGroupDao specGroupDao;

    @Resource
    SpecParamDao specParamDao;

    public List<SpecGroup> queryGroupByCid(Long cid) {

        SpecGroup specGroup = new SpecGroup();
        specGroup.setCid(cid);
        return specGroupDao.select(specGroup);
    }

    /**
     * 根据条件查询规格参数
     * @param gid
     * @return
     */
    public List<SpecParam> queryParams(Long gid, Long cid, Boolean generic, Boolean searching) {
        SpecParam record = new SpecParam();
        record.setGroupId(gid);
        record.setCid(cid);
        record.setGeneric(generic);
        record.setSearching(searching);
        List<SpecParam> specParams = this.specParamDao.select(record);
        return specParams;
    }

    public List<SpecGroup> querySpecsByCid(Long cid) {
        // 查询规格组
        List<SpecGroup> groups = this.queryGroupByCid(cid);
        groups.forEach(g -> {
            // 查询组内参数
            g.setParams(this.queryParams(g.getId(), null, null, null));
        });
        return groups;
    }
}
