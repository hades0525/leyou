package com.leyou.user.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "user:verify:phone";

    /**
     * 验证用户数据
     * @param data
     * @param type
     * @return
     */
    public Boolean checkData(String data, Integer type) {
        //判断数据类型
        User record = new User();

        switch (type){
            case 1:
                record.setUsername(data);
                break;
            case 2:
                record.setPhone(data);
                break;
            default:
                throw new LyException(ExceptionEnum.INVALID_USER_DATA_TYPE);
        }


        return userMapper.selectCount(record) == 0;
    }

    /**
     * 发送验证码
     * @param phone
     */
    public void sendCode(String phone) {
        //生成key
        String key = KEY_PREFIX+phone;
        //生成验证码
        String code = NumberUtils.generateCode(6);

        Map<String,String> msg = new HashMap<>();
        msg.put("phone",phone);
        msg.put("code",code);

        //发送验证码
        amqpTemplate.convertAndSend("ly.sms.exchange","sms.verify.code",msg);

        //保存验证码
        redisTemplate.opsForValue().set(key,code,5, TimeUnit.MINUTES);
    }

    /**
     * 注册
     * @param user
     * @param code
     */
    public void register(User user, String code) {
        //校验验证码
        //从redis中取出验证码
        String cacheCode = redisTemplate.opsForValue().get(KEY_PREFIX + user.getPhone());
        if (!StringUtils.equals(code,cacheCode)){
            throw new LyException(ExceptionEnum.INVALID_VERIFY_CODE);
        }
        //生成盐
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);
        //对密码加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(),salt));

        //数据写入数据库
        user.setCreated(new Date());
        userMapper.insert(user);
    }

    /**
     * 查询用户
     * @param username
     * @param password
     * @return
     */
    public User queryUserByUsernameAndPassword(String username, String password) {
        //查询用户
        User record = new User();
        record.setUsername(username);
        User user = userMapper.selectOne(record);
        //校验
        if (user == null){
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
        //校验密码
        if (!StringUtils.equals(user.getPassword(),CodecUtils.md5Hex(password,user.getSalt()))){
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }

        //用户名和密码正确
        return user;
    }
}
