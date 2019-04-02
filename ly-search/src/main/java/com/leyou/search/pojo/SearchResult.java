package com.leyou.search.pojo;

import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.Brand;
import com.leyou.item.pojo.Category;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SearchResult extends PageResult<Goods> {

    //分类待选项
    private List<Category> categories;

    //品牌待选项
    private List<Brand> brands;

    //规格参数 key及待选项
    private List<Map<String,Object>> specs;

    public SearchResult() {
    }


    public SearchResult(Long total, List<Goods> items, Integer totalPage, List<Category> categories, List<Brand> brands, List<Map<String, Object>> specs) {
        super(total, items, totalPage);
        this.categories = categories;
        this.brands = brands;
        this.specs = specs;
    }
}