package com.leyou.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum ExceptionEnum {
    CATEGORY_NOT_FIND(404,"商品分类没有查到"),
    BRAND_NOT_FOUND(404,"品牌不存在"),
    BRAND_SAVE_ERROR(500,"新增品牌失败" ),
    UPLOAD_FILE_ERROR(500,"文件上传失败" ),
    INVALID_FILE_TYPE(400,"非法的文件类型" ),
    SPEC_GROUP_NOT_FOUND(404,"商品规格组没查到"),
    BRAND_CATEGORY_NOT_FOUND(404,"品牌的分类没有查到"),
    UPDATE_BRAND_ERROR(500,"更新品牌失败"),
    GOODS_NOT_FOUND(404, "商品不存在"),
    GOODS_SAVE_ERROR(500,"新增商品失败"),
    GOODS_DETAIL_NOT_FOUND(404,"商品详情没查到"),
    GOODS_SKU_NOT_FOUND(404,"商品SKU不存在"),
    GOODS_STOCK_NOT_FOUND(404,"商品库存不存在"),
    UPDATE_GOODS_ERROR(500,"更新商品失败"),
    GOODS_ID_CANNOT_BE_NULL(400,"商品id不能为空"),
    UPDATE_GOODS_SPU_ERROR(500,"更新商品SPU失败"),
    UPDATE_GOODS_SPUDETAIL_ERROR(500,"更新商品SPUDETAIL失败"),
    GOODS_SAVE_SKU_ERROR(500,"新增sku失败"),
    GOODS_SAVE_STOCK_ERROR(500,"新增stock失败"),
    INVALID_USER_DATA_TYPE(400,"无效的用户数据类型"),
    INVALID_VERIFY_CODE(400,"无效的验证码"),
    INVALID_USERNAME_PASSWORD(400,"用户名或密码错误" ),
    CREATE_TOKEN_ERROR(500,"用户凭证生成失败"),
    UNAUTHRIZED(403,"未授权"),
    CART_NOT_FOUND(404,"购物车为空"),
    CREATE_ORDER_ERROR(500,"订单创建失败"),
    STOCK_NOT_ENOUGH(500,"库存不足"),
    ORDER_NOT_FOUND(404,"订单不存在" ),
    ORDER_DETAIL_NOT_FOUND(404,"订单详情不存在" ),
    ORDER_STATUS_NOT_FOUND(404,"订单状态不存在" ),
    WX_PAY_ORDER_FALI(500,"微信下单失败"),
    ORDER_STATUS_ERROR(400, "订单状态有误"),
    INVALID_SIGN_ERROR(400, "无效的签名异常"),
    INVALID_ORDER_PARAM(400, "订单参数异常"),
    UPDATE_ORDERSTATUS_ERROR(500, "更新订单状态失败"),
    ;
    private int code;
    private String msg;


}
