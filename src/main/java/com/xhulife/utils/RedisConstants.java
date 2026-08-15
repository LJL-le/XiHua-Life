package com.xhulife.utils;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 36000L;

    public static final Long CACHE_NULL_TTL = 2L;

    public static final Long CACHE_SHOP_TTL = 30L;
    public static final Long CACHE_SHOP_PHYSICAL_TTL = 24L;
    public static final String CACHE_SHOP_KEY = "cache:shop:v2:";

    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String FREE_QUALIFICATION_ACTIVITY_KEY = "free:qualification:activity:";
    public static final String FREE_QUALIFICATION_STOCK_KEY = "free:qualification:stock:";
    public static final String FREE_QUALIFICATION_USERS_KEY = "free:qualification:users:";
    public static final String FREE_QUALIFICATION_STREAM_KEY = "stream.free.qualification.records";
    public static final String LOCK_FREE_QUALIFICATION_KEY = "lock:free:qualification:";
    public static final String LOCK_FREE_QUALIFICATION_INIT_KEY = "lock:free:qualification:init:";
    public static final String FREE_QUALIFICATION_USERS_INITIALIZED_MEMBER = "__initialized__";
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String FEED_KEY = "feed:";
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";
}

