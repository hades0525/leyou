package com.leyou.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author: HuYi.Zhang
 * @create: 2018-04-24 17:20
 **/
public class JsonUtils {

    public static final ObjectMapper mapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    @Nullable
    //序列化toString
    public static String toString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj.getClass() == String.class) {
            return (String) obj;
        }
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("json序列化出错：" + obj, e);
            return null;
        }
    }

    @Nullable
    //反序列化toBean
    public static <T> T toBean(String json, Class<T> tClass) {
        try {
            return mapper.readValue(json, tClass);
        } catch (IOException e) {
            logger.error("json解析出错：" + json, e);
            return null;
        }
    }

    @Nullable
    //反序列化 toList
    public static <E> List<E> toList(String json, Class<E> eClass) {
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, eClass));
        } catch (IOException e) {
            logger.error("json解析出错：" + json, e);
            return null;
        }
    }

    @Nullable
    //反序列化 toMap
    public static <K, V> Map<K, V> toMap(String json, Class<K> kClass, Class<V> vClass) {
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructMapType(Map.class, kClass, vClass));
        } catch (IOException e) {
            logger.error("json解析出错：" + json, e);
            return null;
        }
    }

    @Nullable
    public static <T> T nativeRead(String json, TypeReference<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (IOException e) {
            logger.error("json解析出错：" + json, e);
            return null;
        }
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class User{
        String name;
        Integer age;
    }
    public static void main(String[] args) {
      /*  User user = new User("jack",21);
        //toString
        String jason = toString(user);
        System.out.println(jason);

        //反序列化
        User user1 = toBean(jason,User.class);
        System.out.println(user1);*/

        //toList
//        String jason = "[20,10,2,12]";
//        List<Integer> list = toList(jason, Integer.class);
//        System.out.println(list);

        //toMap
        //language=JSON
//        String jason = "{ \"name\":\"jack\",\"age\": \"21\"}";
//        Map<String, String> map = toMap(jason, String.class, String.class);
//        System.out.println(map);

        String jason ="[{\"name\":\"jack\",\"age\": \"21\"},{\"name\":\"rose\",\"age\": \"22\"}]";
        List<Map<String, String>> maps = nativeRead(jason, new TypeReference<List<Map<String, String>>>() {
        });
       for(Map<String,String> map: maps){
           System.out.println(map);
       }
    }
}
