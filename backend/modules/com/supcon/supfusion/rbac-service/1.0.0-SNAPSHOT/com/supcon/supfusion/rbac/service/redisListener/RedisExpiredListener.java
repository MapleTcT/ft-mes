package com.supcon.supfusion.rbac.service.redisListener;

import com.supcon.supfusion.rbac.service.bo.ExportFileStatusBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

@Slf4j
@Component
public class RedisExpiredListener implements MessageListener {

    @Qualifier("rbacRedisTemplate")
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 客户端监听订阅的topic，当有消息的时候，会触发该方法;
     * 并不能得到value, 只能得到key。
     * 姑且理解为: redis服务在key失效时(或失效后)通知到java服务某个key失效了, 那么在java中不可能得到这个redis-key对应的redis-value。
     * * 解决方案:
     * 创建copy/shadow key, 例如 set vkey "vergilyn"; 对应copykey: set copykey:vkey "" ex 10;
     * 真正的key是"vkey"(业务中使用), 失效触发key是"copykey:vkey"(其value为空字符为了减少内存空间消耗)。
     * 当"copykey:vkey"触发失效时, 从"vkey"得到失效时的值, 并在逻辑处理完后"del vkey"
     * <p>
     * 缺陷:
     * 1: 存在多余的key; (copykey/shadowkey)
     * 2: 不严谨, 假设copykey在 12:00:00失效, 通知在12:10:00收到, 这间隔的10min内程序修改了key, 得到的并不是 失效时的value.
     * (第1点影响不大; 第2点貌似redis本身的Pub/Sub就不是严谨的, 失效后还存在value的修改, 应该在设计/逻辑上杜绝)
     * 当"copykey:vkey"触发失效时, 从"vkey"得到失效时的值, 并在逻辑处理完后"del vkey"
     */
    @Override
    public void onMessage(Message message, byte[] bytes) {
        String prefix = "export_template:";
        String expiredKey = message.toString();
        if (expiredKey.startsWith("export_template:")) {
            //export_Template:开头的key，进行处理
            String id = expiredKey.split(prefix)[1];
            HashOperations<String, Object, Object> hash = redisTemplate.opsForHash();
            Object o = hash.get("keyObj", id);
            if (o != null) {
                ExportFileStatusBO exportFile = (ExportFileStatusBO) o;
                //删除文件
                File file = new File(exportFile.getFilePath());
                if (file.exists() && file.delete()) {
                    log.info("删除成功");
                } else {
                    log.info("删除失败");
                }
            }
            //删除数据信息
            hash.delete("keyObj", id);
        }
    }
}
