local previousStopKey = redis.call('GET', KEYS[2])
if previousStopKey and previousStopKey ~= KEYS[1] then
    redis.call('ZREM', previousStopKey, ARGV[3])
end

redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])

local rateAllowed = redis.call('SET', KEYS[3], '1', 'NX', 'EX', ARGV[7])
if rateAllowed or previousStopKey ~= KEYS[1] then
    redis.call('ZADD', KEYS[1], ARGV[2], ARGV[3])
    redis.call('SET', KEYS[2], ARGV[4], 'EX', ARGV[5])
end

redis.call('EXPIRE', KEYS[1], ARGV[6])
return redis.call('ZCARD', KEYS[1])
