package com.leyou.order.service;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.common.dto.CartDto;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.IdWorker;
import com.leyou.item.pojo.Sku;
import com.leyou.order.client.AddressClient;
import com.leyou.order.client.GoodsClient;
import com.leyou.order.dto.AddressDTO;
import com.leyou.order.dto.OrderDto;
import com.leyou.order.enums.OrderStatusEnum;
import com.leyou.order.enums.PayState;
import com.leyou.order.interceptor.UserInterceptor;
import com.leyou.order.mapper.OrderDetailMapper;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderDetail;
import com.leyou.order.pojo.OrderStatus;
import com.leyou.order.utils.PayHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper detailMapper;

    @Autowired
    private OrderStatusMapper statusMapper;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private PayHelper payHelper;

    /**
     * 创建订单
     * @param orderDto
     * @return
     */
    @Transactional
    public Long createOrder(OrderDto orderDto) {
        //1.新增订单
        Order order = new Order();
        //1.1 订单编号  基本信息
        long orderId = idWorker.nextId();
        order.setOrderId(orderId);
        order.setCreateTime(new Date());
        order.setPaymentType(orderDto.getPaymentType());

        //1.2 用户信息
        UserInfo user = UserInterceptor.getUser();
        order.setUserId(user.getId());
        order.setBuyerNick(user.getUsername());
        order.setBuyerRate(false);

        //1.3 收货人地址
        AddressDTO addr = AddressClient.findById(orderDto.getAddressId());
        order.setReceiver(addr.getName());
        order.setReceiverAddress(addr.getAddress());
        order.setReceiverDistrict(addr.getDistrict());
        order.setReceiverCity(addr.getCity());
        order.setReceiverState(addr.getState());
        order.setReceiverMobile(addr.getPhone());
        order.setReceiverZip(addr.getZipCode());

        //1.4 金额
        //把cartdto转为一个map，key是skuId,value是num
        Map<Long, Integer> numMap = orderDto.getCarts().stream()
                .collect(Collectors.toMap(CartDto::getSkuId, CartDto::getNum));
        //获取所有sku的id
        Set<Long> ids = numMap.keySet();
        //根据id查询sku
        List<Sku> skus = goodsClient.querySkuByIds(new ArrayList<>(ids));

        //准备orderDtail集合
        List<OrderDetail> details = new ArrayList<>();

        Long totalPay = 0L;
        for (Sku sku : skus) {
            totalPay = sku.getPrice() * numMap.get(sku.getId());

            //封装orderDtail
            OrderDetail detail = new OrderDetail();
            detail.setOrderId(orderId);
            detail.setImage(StringUtils.substringBefore(sku.getImages(),","));
            detail.setNum(numMap.get(sku.getId()));
            detail.setOwnSpec(sku.getOwnSpec());
            detail.setSkuId(sku.getId());
            detail.setTitle(sku.getTitle());
            detail.setPrice(sku.getPrice().longValue());
            details.add(detail);
        }

        order.setTotalPay(totalPay);
        //实付金额： 总金额 + 邮费 - 优惠金额
        order.setActualPay(totalPay + order.getPostFee() - 0);

        //1.5 写入数据库
        int count =  orderMapper.insertSelective(order);
        if (count != 1){
            log.error("[创建订单服务order] 创建订单失败,orderId:{}",orderId);
            throw new LyException(ExceptionEnum.CREATE_ORDER_ERROR);
        }
        //2.新增订单详情
        count = detailMapper.insertList(details);

        if (count != details.size()){
            log.error("[创建订单服务detail] 创建订单失败,orderId:{}",orderId);
            throw new LyException(ExceptionEnum.CREATE_ORDER_ERROR);
        }
        //3.新增订单状态
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setCreateTime(order.getCreateTime());
        orderStatus.setOrderId(orderId);
        orderStatus.setStatus(OrderStatusEnum.UN_PAY.value());
        count =  statusMapper.insertSelective(orderStatus);
        if (count != 1){
            log.error("[创建订单服务status] 创建订单失败,orderId:{}",orderId);
            throw new LyException(ExceptionEnum.CREATE_ORDER_ERROR);
        }

        //4.减库存   采用同步，在数据库判断
        List<CartDto> cartDtos = orderDto.getCarts();
        goodsClient.decreaseStock(cartDtos);

        return orderId;

    }

    /**
     * 根据订单id查询订单
     * @param id
     * @return
     */
    public Order queryOrderById(Long id) {
        //查询订单
        Order order = orderMapper.selectByPrimaryKey(id);
        if (order == null){
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }

        //查询订单详情
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(id);
        List<OrderDetail> details = detailMapper.select(detail);
        if (CollectionUtils.isEmpty(details)){
            throw new LyException(ExceptionEnum.ORDER_DETAIL_NOT_FOUND);
        }
        order.setOrderDetails(details);

        //查询订单状态
        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(id);
        if (orderStatus == null){
            throw new LyException(ExceptionEnum.ORDER_STATUS_NOT_FOUND);
        }
        order.setOrderStatus(orderStatus);
        return order;
    }

    /**
     * 获取支付url
     * @param orderId
     * @return
     */
    public String createPayUrl(Long orderId) {
        //查询订单
        Order order = queryOrderById(orderId);
        //判断订单状态
        Integer status = order.getOrderStatus().getStatus();
        if (status != OrderStatusEnum.UN_PAY.value()){
            throw new LyException(ExceptionEnum.ORDER_STATUS_ERROR);
        }
        //支付金额
        Long actualPay = order.getActualPay();
        //商品描述
        OrderDetail detail = order.getOrderDetails().get(0);
        String desc = detail.getTitle();
        //return payHelper.createOrder(orderId,actualPay,desc);
        return "weixin://wxpay/bizpayurl?pr=IEukvBd";
    }

    /**
     * 支付结果通知
     * @param result
     */
    public void handleNotify(Map<String, String> result) {
        //1 数据校验
        payHelper.isSuccess(result);

        //2 校验签名
        payHelper.isValidSign(result);

        //3 校验金额
        String totalFeeStr = result.get("total_fee");
        //订单号
        String tradeNo = result.get("out_trade_no");
        if (StringUtils.isBlank(totalFeeStr) || StringUtils.isBlank(tradeNo)){
            throw new LyException(ExceptionEnum.INVALID_ORDER_PARAM);
        }
        //3.1 获取结果中的金额
        long totalFee = Long.valueOf(totalFeeStr);
        //获取订单号
        Long orderId = Long.valueOf(tradeNo);

        //4 查询订单
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (totalFee != order.getActualPay()){
            //金额不符
            throw new LyException(ExceptionEnum.INVALID_ORDER_PARAM);
        }

        //5 修改订单状态
        OrderStatus status = new OrderStatus();
        status.setStatus(OrderStatusEnum.PAYED.value());
        status.setOrderId(orderId);
        status.setPaymentTime(new Date());
         int count =  statusMapper.updateByPrimaryKeySelective(status);
         if (count != 1){
             throw new LyException(ExceptionEnum.UPDATE_ORDERSTATUS_ERROR);
         }

         log.info("[订单回调] 订单支付成功！订单编号:{}",orderId);
    }

    /**
     * 查询订单状态
     * @param orderId
     * @return
     */
    public PayState queryOrderStatus(Long orderId) {
        try {
            //查询订单状态
            OrderStatus orderStatus = statusMapper.selectByPrimaryKey(orderId);
            Integer state = orderStatus.getStatus();
            //判断是否支付
            if(state != OrderStatusEnum.UN_PAY.value()){
                //如果已支付，就是真的支付了
                return PayState.SUCCESS;
            }
            Thread.sleep(2000L);
            orderStatus.setPaymentTime(new Date());
            statusMapper.updateByPrimaryKeySelective(orderStatus);
            return PayState.SUCCESS;
            //如果未支付，需要到微信查询支付状态
            //return payHelper.queryPayState(orderId);
        } catch (InterruptedException e) {
            return null;
        }
    }
}
