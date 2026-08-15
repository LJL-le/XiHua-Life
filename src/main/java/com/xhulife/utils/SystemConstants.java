package com.xhulife.utils;

import java.nio.file.Paths;

public class SystemConstants {
    public static final String IMAGE_UPLOAD_DIR = Paths.get(
            System.getenv().getOrDefault("IMAGE_UPLOAD_DIR", "mul1/imgs/uploads")
    ).toAbsolutePath().normalize().toString();
    public static final String USER_NICK_NAME_PREFIX = "user_";
    public static final int DEFAULT_PAGE_SIZE = 5;
    public static final int MAX_PAGE_SIZE = 10;
}

