package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.pojo.Brand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class BrandService {
    @Autowired
    private BrandMapper brandMapper;

    /**
     * 分页查询品牌信息
     * @param page
     * @param rows
     * @param sortBy
     * @param desc
     * @param key
     * @return
     */
    public PageResult<Brand> queryBrandByPage(Integer page, Integer rows, String sortBy, boolean desc, String key) {

        /*
        * where 'name' like '%x%' or letter == 'x'
        * order by id desc
        * */
        //过滤
        //通用mapper提供的example，用来自定义查询条件
        Example example = new Example(Brand.class);
        if(StringUtils.isNotBlank(key)){
           example.createCriteria().orLike("name","%"+key+"%")
           .orEqualTo("letter",key.toUpperCase());
        }
        //排序
        if(StringUtils.isNotBlank(sortBy)){
            String oderByClause = sortBy+(desc ? " DESC" : " ASC");
            example.setOrderByClause(oderByClause);
        }

        //分页
        PageHelper.startPage(page,rows);
        //查询
        List<Brand> list = brandMapper.selectByExample(example);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        //解析分页结果,自动封装总页数
        //pagehelper提供的分页类PageInfo
        PageInfo<Brand> info = new PageInfo<>(list);
        return new PageResult<>(info.getTotal(),list);
    }

    /**
     * 新增品牌
     * @param brand
     * @param cids
     */
    @Transactional
    public void saveBrand(Brand brand, List<Long> cids) {
        //新增品牌
        brand.setId(null);
        int count = brandMapper.insert(brand);
        if(count !=1 ){
            //新增失败
            throw new LyException(ExceptionEnum.BRAND_SAVE_ERROR);
        }
        for (Long cid : cids) {
            //把品牌的分类保存
           count = brandMapper.insertCategoryBrand(cid,brand.getId());
            if(count != 1){
                throw new LyException(ExceptionEnum.BRAND_SAVE_ERROR);
            }
        }
    }

    /**
     * 修改品牌信息
     * @param brand
     * @param cids
     */
    @Transactional
    public void editBrand(Brand brand, List<Long> cids) {
        //先把数据库里的品牌的分类全部删除
        int count = brandMapper.deleteCategoryBrand(brand.getId());
        if(count < 1){
            throw new LyException(ExceptionEnum.UPDATE_BRAND_ERROR);
        }
        //修改品牌的信息
        count = brandMapper.updateByPrimaryKey(brand);
        if(count != 1){
            throw new LyException(ExceptionEnum.UPDATE_BRAND_ERROR);
        }
        //增加品牌的分类
        for (Long cid : cids) {
            count = brandMapper.insertCategoryBrand(cid,brand.getId());

            if(count != 1){
                throw new LyException(ExceptionEnum.UPDATE_BRAND_ERROR);
            }
        }
    }

    public Brand queryById(Long id){
        Brand brand = brandMapper.selectByPrimaryKey(id);
        if (brand == null){
            throw new LyException(ExceptionEnum.BRAND_SAVE_ERROR);
        }
        return brand;
    }

    /**
     * 根据cid查询品牌
     * @param cid
     * @return
     */
    public List<Brand> queryBrandByCid(Long cid) {
        List<Brand> brands = brandMapper.queryByCategoryId(cid);
        if (CollectionUtils.isEmpty(brands)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brands;
    }

    /**
     *根据ids找到brandlist
     * @param ids
     * @return
     */
    public List<Brand> queryBrandByIds(List<Long> ids) {
        List<Brand> brands = brandMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(brands)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brands;
    }
}
