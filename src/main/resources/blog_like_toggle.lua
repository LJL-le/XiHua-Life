if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then
    redis.call('SREM', KEYS[1], ARGV[1])
    return -1
end
redis.call('SADD', KEYS[1], ARGV[1])
return 1
