package com.leyou.page.client;

import com.leyou.item.api.SpecifictionApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("item-service")
public interface SpecifictionClient extends SpecifictionApi {
}
