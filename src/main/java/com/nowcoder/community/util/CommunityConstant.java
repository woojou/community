package com.nowcoder.community.util;

public interface CommunityConstant {

    /**
     * 激活成功
     */
    int ACTIVATION_SUCCESS = 0;

    /**
     * 重复激活
     */
    int ACTIVATION_REPEAT = 1;

    /**
     * 激活失败
     */
    int ACTIVATION_FAILURE = 2;

    /**
     * 默认登录凭证超时时间
     */
    int DEFAULT_EXPIRED_SECONDS = 3600 * 12;

    /**
     * 勾选记住我的凭证超时时间
     */
    int REMEMBER_EXPIRED_SECONDS = 3600 * 24 * 100;

    /**
     * 评论
     */
    int ENTITY_TYPE_POST = 1;

    /**
     * 回复
     */
    int ENTITY_TYPE_REPLY = 2;

    /**
     * 实体类型: 用户
     */
    int ENTITY_TYPE_USER = 3;

}
