package com.leyou.page.service;

import com.leyou.item.pojo.*;
import com.leyou.page.client.BrandClient;
import com.leyou.page.client.CategoryClient;
import com.leyou.page.client.GoodsClient;
import com.leyou.page.client.SpecifictionClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PageService {

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private SpecifictionClient specClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private TemplateEngine templateEngine;

    /**
     * 加载模型
     * @param spuId
     * @return
     */
    public Map<String, Object> loadModel(Long spuId) {
        Map<String,Object> model = new HashMap<>();
        //查询spu
        Spu spu = goodsClient.querySpuById(spuId);
        //查询skus
        List<Sku> skus = spu.getSkus();
        //查询detail
        SpuDetail detail = spu.getSpuDetail();
        //查询brand
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        //查询categories
        List<Category> categories = categoryClient.queryCategoryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        //查询specs
        List<SpecGroup> specs = specClient.queryListByCid(spu.getCid3());

        model.put("title",spu.getTitle());
        model.put("subTitle",spu.getSubTitle());
        model.put("skus",skus);
        model.put("detail",detail);
        model.put("brand",brand);
        model.put("categories",categories);
        model.put("specs",specs);
        return model;
    }

    /**
     * 创建静态页面
     * @param spuId
     */
    public void createHtml(Long spuId)  {
        // 创建上下文，
        Context context = new Context();
        // 把数据加入上下文
        context.setVariables(loadModel(spuId));

        // 创建输出流，关联到一个临时文件
        File dest = new File("D:\\MY_IDEA\\upload",spuId + ".html");
        if (dest.exists()){
            dest.delete();
        }

        try (PrintWriter writer = new PrintWriter(dest, "UTF-8")) {
            // 利用thymeleaf模板引擎生成静态页面
            templateEngine.process("item", context, writer);
        }catch (Exception e){
            log.error("[静态页服务] 生成静态页异常");
        }
    }

    /**
     * 删除静态页
     * @param spuId
     */
    public void deleteHtml(Long spuId) {
        File dest = new File("D:\\MY_IDEA\\upload",spuId + ".html");
        if (dest.exists()){
            dest.delete();
        }
    }

}
