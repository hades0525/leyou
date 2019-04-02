package com.leyou.item.api;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface SpecifictionApi {
    /**
     * 查询参数集合
     * @param gid 组id
     * @param cid 分类id
     * @param searching 是否搜索
     * @return
     */
    @GetMapping("spec/params")
    List<SpecParam> queryParamByList(@RequestParam(value = "gid",required = false) Long gid,
                                     @RequestParam(value = "cid",required = false)Long cid,
                                     @RequestParam(value = "searching",required = false)Boolean searching
    );

    /**
     * 根据cid查询规格组及组内参数
     * @param cid
     * @return
     */
    @GetMapping("spec/group")
    List<SpecGroup> queryListByCid(@RequestParam("cid") Long cid);
}
