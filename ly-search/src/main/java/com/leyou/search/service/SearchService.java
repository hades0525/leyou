package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.utils.NumberUtils;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.item.vo.SkuVo;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecifictionClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchService {

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private SpecifictionClient specifictionClient;

    @Autowired
    private GoodsRepository repository;

    @Autowired
    private ElasticsearchTemplate template;

    public Goods buildGoods(Spu spu) {
        Long spuId = spu.getId();

        // 获取all字段的拼接
        String all = this.getall(spu);

        // 需要对sku过滤把不需要的数据去掉
        List<Sku> skus = goodsClient.querySkuBySpuId(spuId);
        List<SkuVo> skuVoList = this.getSkuVo(skus);

        // 获取sku的价格列表
        Set<Long> prices = this.getPrices(skus);

        // 获取specs
        HashMap<String, Object> specs = getSpecs(spu); //  数据不全导致的 bug

        Goods goods = new Goods();
        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setCreateTime(spu.getCreateTime());
        goods.setId(spuId);
        goods.setSubTitle(spu.getSubTitle());
        // 搜索条件 拼接：标题、分类、品牌
        goods.setAll(all);
        goods.setPrice(prices);
        goods.setSkus(JsonUtils.toString(skuVoList));
        goods.setSpecs(specs); // 数据不全导致的 bug

        return goods;
    }

    /**
     * 对all字段进行拼接
     *
     * @param spu
     * @return
     */
    private String getall(Spu spu) {
        // 查询分类
        List<Category> categories = categoryClient.queryCategoryByIds(
                Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        if (CollectionUtils.isEmpty(categories)) {
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FIND);
        }
        // 查询品牌
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        if (brand == null) {
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        //品牌名
        List<String> names = categories.stream().map(Category::getName).collect(Collectors.toList());
        // 搜索字段
        String all = spu.getTitle() + StringUtils.join(names, ",") + brand.getName();
        return all;
    }

    /**
     * 对sku进行处理，去掉不需要的字段
     *
     * @param skus
     * @return
     */
    private List<SkuVo> getSkuVo(List<Sku> skus) {
        List<SkuVo> skuVoList = new ArrayList<>();
        for (Sku sku : skus) {
            SkuVo skuVo = new SkuVo();
            skuVo.setId(sku.getId());
            skuVo.setPrice(sku.getPrice());
            skuVo.setTitle(sku.getTitle());
            skuVo.setImage(StringUtils.substringBefore(sku.getImages(), ","));
            skuVoList.add(skuVo);
        }
        return skuVoList;

    }

    /**
     * 获取sku的price
     *
     * @param skuList
     * @return
     */
    private Set<Long> getPrices(List<Sku> skuList) {
        // 查询sku
        if (CollectionUtils.isEmpty(skuList)) {
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }
        return skuList.stream().map(Sku::getPrice).collect(Collectors.toSet());
    }

    /**
     * 获取规格参数
     *
     * @param spu
     * @return
     */
    private HashMap<String, Object> getSpecs(Spu spu) {
        // 获取规格参数
        List<SpecParam> params = specifictionClient.queryParamByList(null, spu.getCid3(), true);
        if (CollectionUtils.isEmpty(params)) {
            throw new LyException(ExceptionEnum.SPEC_GROUP_NOT_FOUND);
        }
        // 查询商品详情
        SpuDetail spuDetail = goodsClient.queryDetailById(spu.getId());

        // 获取通用规格参数
        Map<Long, String> genericSpec = JsonUtils.toMap(
                spuDetail.getGenericSpec(), Long.class, String.class);

        //获取特有规格参数
        Map<Long, List<String>> specialSpec = JsonUtils.nativeRead(
                spuDetail.getSpecialSpec(), new TypeReference<Map<Long, List<String>>>() {
                });

        //定义spec对应的map
        HashMap<String, Object> map = new HashMap<>();
        //对规格进行遍历，并封装spec，其中spec的key是规格参数的名称，value是商品详情中的值
        for (SpecParam param : params) {
            //key是规格参数的名称
            String key = param.getName();
            Object value = "";

            if (param.getGeneric()) {
                //参数是通用属性，通过规格参数的ID从商品详情存储的规格参数中查出值
                value = genericSpec.get(param.getId());
                if (param.getNumeric()) {
                    //参数是数值类型，处理成段，方便后期对数值类型进行范围过滤
                    value = chooseSegment(value.toString(), param);
                }
            } else {
                //参数不是通用类型
                value = specialSpec.get(param.getId());
            }
            value = (value == null ? "其他" : value);
            //存入map
            map.put(key, value);
        }
        return map;
    }

    /**
     * 对数值进行分段
     *
     * @param value
     * @param p
     * @return
     */
    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + p.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + p.getUnit() + "以下";
                } else {
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    /**
     * 搜索功能
     * @param searchRequest
     * @return
     */
    public PageResult<Goods> search(SearchRequest searchRequest) {
        String key = searchRequest.getKey();
        if (StringUtils.isBlank(key)){
            return null;
        }

        int page = searchRequest.getPage() - 1;
        int size = searchRequest.getSize();

        //创建查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //0.结果过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "subTitle", "skus"}, null));

        //1.分页,从0开始
        queryBuilder.withPageable(PageRequest.of(page, size));

        //2.过滤
        //查询条件
        //MatchQueryBuilder basicQuery = QueryBuilders.matchQuery("all", searchRequest.getKey());
        QueryBuilder basicQuery =buildBasicQuery(searchRequest);
        queryBuilder.withQuery(basicQuery);

        //3.聚合分类和品牌
        //3.1 聚合分类
        String CategoryAggName = "category_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(CategoryAggName).field("cid3"));
        //3.2聚合品牌
        String BrandAggName = "brand_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(BrandAggName).field("brandId"));

        //4.查询
        //Page<Goods> result = repository.search(queryBuilder.build());
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);

        //5.解析结果
        //5.1解析分页结果
        long total = result.getTotalElements();
        int totalPages = result.getTotalPages();
        List<Goods> goodsList = result.getContent();
        //5.2解析聚合结果
        Aggregations aggs = result.getAggregations();
        List<Category> categories = parseCategoryAgg(aggs.get(CategoryAggName));
        List<Brand> brands = parseBrandAgg(aggs.get(BrandAggName));

        //6.完成规格参数聚合
        List<Map<String,Object>> specs = null;
        if (categories != null && categories.size() ==1){
            //商品分类存在并且数量为1，可以聚合规格参数
            specs = buildSpecifictionAgg(categories.get(0).getId(),basicQuery);
        }


        return new SearchResult(total, goodsList, totalPages,categories,brands,specs);
    }

    /**
     * 过滤条件筛选
     * @param searchRequest
     * @return
     */
    private QueryBuilder buildBasicQuery(SearchRequest searchRequest) {
        //创建bool查询
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        //查询条件
        queryBuilder.must(QueryBuilders.matchQuery("all",searchRequest.getKey()));
        //过滤条件
        Map<String, String> map = searchRequest.getFilter();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            //key有两种，分类品牌和规格参数
            if (!"cid3".equals(key) && !"brandId".equals(key)){
                key = "specs."+key+".keyword";
            }
            String value = entry.getValue();
            queryBuilder.filter(QueryBuilders.termQuery(key,value));
        }
        return queryBuilder;
    }


    /**
     * 聚合规格参数
     * @param cid
     * @param basicQuery
     * @return
     */
    private List<Map<String, Object>> buildSpecifictionAgg(Long cid, QueryBuilder basicQuery) {
        List<Map<String,Object>> specs = new ArrayList<>();
        //1.查询需要聚合的规格参数,根据cid查询规格
        List<SpecParam> params = specifictionClient.queryParamByList(null, cid, true);
        //2.聚合
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //2.1 带上查询条件
        queryBuilder.withQuery(basicQuery);
        //2.2聚合
        for (SpecParam param : params) {
            String name = param.getName();
            queryBuilder.addAggregation(
                    //规格参数保存时不做分词，因此其名称会自动带上一个.keyword后缀
                    AggregationBuilders.terms(name).field("specs."+name+".keyword"));
        }
        //3.获取结果
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);
        //4.解析结果
        Aggregations aggs = result.getAggregations();
        for (SpecParam param : params) {
            //规格参数名称
            String name = param.getName();
            StringTerms terms = aggs.get(name);
            //待选项
            List<String> options = terms.getBuckets().stream().map(
                    b -> b.getKeyAsString()).collect(Collectors.toList());
            //准备map
            Map<String,Object> map =new HashMap<>();
            map.put("k",name);
            map.put("options",options);
            specs.add(map);
        }
        return specs;
    }

    /**
     * 解析分类聚合
     * @param terms
     * @return
     */
    private List<Category> parseCategoryAgg(LongTerms terms) {
        try {
            List<Long> ids = terms.getBuckets().stream()
                    .map(b -> b.getKeyAsNumber().longValue())
                    .collect(Collectors.toList());
            List<Category> categories = categoryClient.queryCategoryByIds(ids);
            return categories;
        }catch (Exception e){
            log.error("[搜索服务]查询分类异常：",e);
            return null;
        }
    }

    /**
     * 解析品牌聚合
     * @param terms
     * @return
     */
    private List<Brand> parseBrandAgg(LongTerms terms) {
        try {
            List<Long> ids = terms.getBuckets().stream()
                    .map(b -> b.getKeyAsNumber().longValue())
                    .collect(Collectors.toList());
            List<Brand> brands = brandClient.queryBrandByIds(ids);
            return brands;
        } catch (Exception e) {
            log.error("[搜索服务]查询品牌异常：",e);
            return null;
        }
    }

    public void insertOrUpdateIndex(Long spuId) {
        //查询spu
        Spu spu = goodsClient.querySpuById(spuId);
        //构建goods对象
        Goods goods = buildGoods(spu);
        //存入索引库
        repository.save(goods);
    }

    public void deleteIndex(Long spuId) {
        repository.deleteById(spuId);
    }
}