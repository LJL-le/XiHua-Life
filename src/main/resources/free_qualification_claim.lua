local activity = redis.call('HMGET', KEYS[1], 'status', 'beginAt', 'endAt')
if not activity[1] or activity[1] ~= '1' then
    return 3
end

local now = redis.call('TIME')
local nowMs = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
if nowMs < tonumber(activity[2]) then
    return 1
end
if nowMs > tonumber(activity[3]) then
    return 2
end

local stock = tonumber(redis.call('GET', KEYS[2]) or '-1')
if stock <= 0 then
    return 4
end
if redis.call('SISMEMBER', KEYS[3], ARGV[1]) == 1 then
    return 5
end

redis.call('DECR', KEYS[2])
redis.call('SADD', KEYS[3], ARGV[1])
redis.call('XADD', KEYS[4], '*',
    'recordId', ARGV[2],
    'userId', ARGV[1],
    'activityId', ARGV[3])
return 0
