package com.leyou.item.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.CategoryMapper;
import com.leyou.item.pojo.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;

@Service
public class CategoryService{
    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 根据父节点的id查询商品分类
     * @param pid
     * @return
     */
    public List<Category> queryCategoryListByPid(Long pid) {
        //查询条件，mapper把对象中的非空属性做查询条件
        Category t = new Category();
        t.setParentId(pid);
        List<Category> list = categoryMapper.select(t);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FIND);
        }
        return list;
    }

    /**
     * 根据品牌id查询分类
     * @param bid
     * @return
     */
    public List<Category> queryByBrandId(Long bid) {
        List<Category> list = categoryMapper.queryByBrandId(bid);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnum.BRAND_CATEGORY_NOT_FOUND);
        }
        return list;
    }

    /**
     * 根据分类id的集合 List<>ids 查询分类对象
     * @param ids
     * @return
     */
    public List<Category> queryByIds(List<Long> ids) {
        //通用mapper继承 IdListMapper<Category,Long> 类
        final List<Category> list = categoryMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FIND);
        }
        return list;
    }

    /**
     * 根据3级分类id，查询1~3级的分类
     * @param id
     * @return
     */
    public List<Category> queryAllByCid3(Long id) {
        Category c3 = categoryMapper.selectByPrimaryKey(id);
        Category c2 = categoryMapper.selectByPrimaryKey(c3.getParentId());
        Category c1 = categoryMapper.selectByPrimaryKey(c2.getParentId());
        List<Category> list = Arrays.asList(c1, c2, c3);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FIND);
        }
        return list;
    }
}
