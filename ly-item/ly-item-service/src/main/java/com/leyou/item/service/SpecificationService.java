package com.leyou.item.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecPareamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SpecificationService {

    @Autowired
    private SpecGroupMapper specGroupMapper;
    @Autowired
    private SpecPareamMapper specPareamMapper;
    /**
     * 按商品分类查询商品规格组
     * @param cid
     * @return
     */
    public List<SpecGroup> queryGroupByCid(Long cid) {
        //查询条件
        SpecGroup group = new SpecGroup();
        group.setCid(cid);
        //查询
        List<SpecGroup> list = specGroupMapper.select(group);
        if(CollectionUtils.isEmpty(list)){
            //没查到
            throw new LyException(ExceptionEnum.SPEC_GROUP_NOT_FOUND);
        }
        return list;
    }

    /**
     * 据组id查询参数
     *
     * @param gid
     * @param cid
     * @param searching
     * @return
     */
    public List<SpecParam> queryParamByList(Long gid, Long cid, Boolean searching) {
        SpecParam param = new SpecParam();
        param.setGroupId(gid);
        param.setCid(cid);
        param.setSearching(searching);
        List<SpecParam> list = specPareamMapper.select(param);
        if(CollectionUtils.isEmpty(list)){
            //没查到
            throw new LyException(ExceptionEnum.SPEC_GROUP_NOT_FOUND);
        }
        return list;
    }

    /**
     * 根据cid查询规格组及组内参数
     * @param cid
     * @return
     */
    public List<SpecGroup> queryListByCid(Long cid) {
        //查询规格组
        List<SpecGroup> specGroups = queryGroupByCid(cid);
        //查询当前分类下的参数
        List<SpecParam> specParams = queryParamByList(null, cid, null);
        //把规格参数变为map key为规格组id,value为组下所有参数
        Map<Long,List<SpecParam>> map = new HashMap<>();
        for (SpecParam param : specParams) {
            if (!map.containsKey(param.getId())){
                //组id在map中不存在,新增一个list
                map.put(param.getGroupId(),new ArrayList<>());
            }
            map.get(param.getGroupId()).add(param);
        }

        //填充param到group
        for (SpecGroup specGroup : specGroups) {
            specGroup.setParams(map.get(specGroup.getId()));
        }
        return specGroups;
    }
}
