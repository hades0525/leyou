package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.dto.CartDto;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import com.leyou.item.mapper.StockMapper;
import com.leyou.item.pojo.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GoodsService {
    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    /**
     * 分页查询商品信息
     * @param page
     * @param rows
     * @param saleable
     * @param key
     * @return
     */
    public PageResult<Spu> querySpuByPage(Integer page, Integer rows, Boolean saleable, String key) {
        //分页
        PageHelper.startPage(page,rows);
        //过滤
        Example example = new Example(Spu.class);
        //搜索字段过滤
        Example.Criteria criteria = example.createCriteria();
        if(StringUtils.isNotBlank(key)){
            criteria.andLike("title","%"+key+"%");
        }
        //上下架过滤
        if(saleable != null){
            criteria.andEqualTo("saleable",saleable);
        }
        //默认排序,按时间
        example.setOrderByClause("last_update_time DESC");

        //查询
        List<Spu> spus = spuMapper.selectByExample(example);
        //判断
        if (CollectionUtils.isEmpty(spus)){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //解析分类和品牌的名称
        loadCategoryAndBrandName(spus);
        //解析分页结果
        PageInfo<Spu> info = new PageInfo<>(spus);
        return new PageResult<>(info.getTotal(),spus);
    }

    /**
     * 把商品的分类和品牌名称解析出来
     * @param spus
     */
    private void loadCategoryAndBrandName(List<Spu> spus) {

        for (Spu spu : spus) {
            //处理分类名称
            //把List<Category对象>变为List<目录名称>
            List<String> names = categoryService.queryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()))
                    .stream().map(Category::getName).collect(Collectors.toList());
            spu.setCname(StringUtils.join(names,"/"));
            //处理品牌名称
            spu.setBname(brandService.queryById(spu.getBrandId()).getName());
        }
    }

    /**
     * 新增商品
     * @param spu
     */
    @Transactional
    public void saveGoods(Spu spu) {
        //新增spu
        spu.setId(null);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        spu.setSaleable(true);
        spu.setValid(false);
        int count = spuMapper.insert(spu);
        if (count != 1){
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }

        //新增spu_detail
        SpuDetail detail = spu.getSpuDetail();
        detail.setSpuId(spu.getId());
        spuDetailMapper.insert(detail);
        //新增sku和stock
        saveSkuAndStock(spu);

        //发送amqp消息
        amqpTemplate.convertAndSend("item.insert",spu.getId());

    }

    /**
     * 新增sku和stock
     * @param spu
     */
    private void saveSkuAndStock(Spu spu) {
        int count;//定义库存集合
        List<Stock> stockList = new ArrayList<>();
        //新增sku
        List<Sku> skus = spu.getSkus();
        for (Sku sku : skus) {
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            sku.setSpuId(spu.getId());
            count = skuMapper.insert(sku);
            if (count != 1){
                throw new LyException(ExceptionEnum.GOODS_SAVE_SKU_ERROR);
            }
            //新增stock
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stockList.add(stock);
        }

        //批量新增stock
        count = stockMapper.insertList(stockList);
        if (count == 0){
            throw new LyException(ExceptionEnum.GOODS_SAVE_STOCK_ERROR);
        }
    }

    /**
     * 根据spu的id查询详情detail
     * @param spuId
     * @return
     */
    public SpuDetail queryDetailById(Long spuId) {
        SpuDetail detail = spuDetailMapper.selectByPrimaryKey(spuId);
        if (detail ==null){
            throw new LyException(ExceptionEnum.GOODS_DETAIL_NOT_FOUND);
        }
        return detail;
    }

    /**
     * 根据spuid查询下面所有的sku和stock
     * @param spuId
     * @return
     */
    public List<Sku> querySkuBySpuId(Long spuId) {
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skuList = skuMapper.select(sku);
        if (CollectionUtils.isEmpty(skuList)){
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }

        //查询库存stock
        /*for (Sku s : skuList) {
            Stock stock = stockMapper.selectByPrimaryKey(s.getId());
            if (stock == null){
                throw new LyException(ExceptionEnum.GOODS_STOCK_NOT_FOUND);
            }
            s.setStock(stock.getStock());
        }*/
        loadStockInSku(skuList);

        return skuList;
    }

    /**
     * 查询库存
     * @param skuList
     */
    private void loadStockInSku(List<Sku> skuList) {
        //查询库存
        //根据skulist取得到所有id集合
        List<Long> ids = skuList.stream().map(Sku::getId).collect(Collectors.toList());
        //根据id集合查询到所有stock集合
        List<Stock> stockList = stockMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(stockList)){
            throw new LyException(ExceptionEnum.GOODS_STOCK_NOT_FOUND);
        }
        //把stock变成一个map,key是skuid，value是库存量
        Map<Long,Integer> stockMap = stockList.stream()
                .collect(Collectors.toMap(Stock::getSkuId, Stock::getStock));
        //把stockmap的库存量赋值给skulist的库存量
        skuList.forEach(s -> s.setStock(stockMap.get(s.getId())));
    }

    /**
     * 修改商品
     * @param spu
     */
    @Transactional
    public void updateGoods(Spu spu) {
        if (spu.getId()==null){
            throw new LyException(ExceptionEnum.GOODS_ID_CANNOT_BE_NULL);
        }

        Sku sku = new Sku();
        sku.setSpuId(spu.getId());
        //查询sku
        List<Sku> skuList = skuMapper.select(sku);
        if (! CollectionUtils.isEmpty(skuList)){
            //删除sku
            skuMapper.delete(sku);
            //删除stock
            List<Long> ids = skuList.stream().map(Sku::getId).collect(Collectors.toList());
            stockMapper.deleteByIdList(ids);
        }
        //修改spu
        spu.setValid(null);
        spu.setSaleable(null);
        spu.setLastUpdateTime(new Date());
        spu.setCreateTime(null);

        int count = spuMapper.updateByPrimaryKeySelective(spu);
        if (count == 0){
            throw new LyException(ExceptionEnum.UPDATE_GOODS_SPU_ERROR);
        }
        //修改detail
        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spu.getId());
        count = spuDetailMapper.updateByPrimaryKeySelective(spuDetail);
        if (count == 0){
            throw new LyException(ExceptionEnum.UPDATE_GOODS_SPUDETAIL_ERROR);
        }
        //新增sku和stock
        saveSkuAndStock(spu);

        //发送mq信息
        amqpTemplate.convertAndSend("item.update",spu.getId());
    }

    /**
     * 根据spu的id查询spu(sku,detail)
     * @param id
     * @return
     */
    public Spu querySpuById(Long id) {
        //查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (spu == null){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //查询sku
        spu.setSkus(querySkuBySpuId(id));
        //查询spudetail
        spu.setSpuDetail(queryDetailById(id));

        return spu;
    }

    /**
     * 根据spu的id集合查询下面所有sku
     * @param ids
     * @return
     */
    public List<Sku> querySkuByIds(List<Long> ids) {
        List<Sku> skus = skuMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(skus)){
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }
        //查询库存
        loadStockInSku(skus);
        return skus;
    }

    /**
     * 减少库存
     * @param cartDtos
     */
    @Transactional
    public void decreaseStock(List<CartDto> cartDtos) {
        for (CartDto cartDto : cartDtos) {
            //减库存
            int count =  stockMapper.decreaseStock(cartDto.getSkuId(),cartDto.getNum());
            if (count != 1){
                throw new LyException(ExceptionEnum.STOCK_NOT_ENOUGH);
            }
        }
    }
}
