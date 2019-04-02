package com.leyou.item.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;
import java.util.List;

@Table(name = "tb_spu")
@Data
public class Spu {
    @Id
    @KeySql(useGeneratedKeys = true)
    private Long id;
    private Long brandId;
    private Long cid1;// 1级类目
    private Long cid2;// 2级类目
    private Long cid3;// 3级类目
    private String title;// 标题
    private String subTitle;// 子标题
    private Boolean saleable;// 是否上架

    @JsonIgnore  //页面忽略的字段
    private Boolean valid;// 是否有效，逻辑删除用

    private Date createTime;// 创建时间

    @JsonIgnore  //页面忽略字段
    private Date lastUpdateTime;// 最后修改时间


    @Transient  //spu表里没有的字段，但页面查询要用
    private String cname;
    @Transient  //spu表里没有的字段，但页面查询要用
    private String bname;
    @Transient  //spu表里没有的字段，但页面新增,修改要用
    private List<Sku> skus;
    @Transient  //spu表里没有的字段，但页面新增,修改要用
    private SpuDetail spuDetail;
}