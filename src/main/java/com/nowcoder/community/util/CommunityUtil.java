package com.nowcoder.community.util;

import com.alibaba.fastjson.JSONObject;
import com.mysql.jdbc.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.Map;
import java.util.UUID;

public class CommunityUtil {

    //生成随机字符串
    public static String generateUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    //MD5加密(对用户密码加密）
    //只能加密不能解密，相同字符串加密每次都会得到相同的密码
    //利用salt（一个随机字符串）,将密码和salt拼接后传入key,之后再加密
    public static String md5(String key) {
        if(StringUtils.isNullOrEmpty(key)) {
            return null;
        }
        return DigestUtils.md5DigestAsHex(key.getBytes());
    }

    public static String getJSONString(int code, String msg, Map<String, Object> map) {
        JSONObject json = new JSONObject();
        json.put("code", code);
        json.put("msg", msg);
        if(map != null) {
            for(Map.Entry<String, Object> entry : map.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
        }

        return json.toJSONString();
    }

    public static String getJSONString(int code, String msg) {
        return getJSONString(code, msg, null);
    }

    public static String getJSONString(int code) {
        return getJSONString(code, null, null);
    }

}
