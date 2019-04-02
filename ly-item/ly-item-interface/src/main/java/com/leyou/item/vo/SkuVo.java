package com.leyou.item.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkuVo {

    private Long id;
    private String title;
    private Long price;
    private String image;
}
