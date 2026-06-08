package com.supcon.supfusion.framework.scaffold.auditlog.repository;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexModel;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.po.AuditBusinessLogPO;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 业务审计日志持久化
 * @author caokele
 */
@Repository
public class AuditBusinessLogRepository implements AuditLogRepository<AuditBusinessLogPO> {
    private static final Logger logger = LoggerFactory.getLogger(AuditBusinessLogRepository.class);

    /**
     * 索引字段
     * key: 字段名称
     * value: 1 ASC; -1 DESC
     */
    private static Map<String, Integer> INDEX_MAP = null;

    static {
        INDEX_MAP = new LinkedHashMap<>();
        INDEX_MAP.put("traceId", DESC);
        INDEX_MAP.put("moduleName", ASC);
        INDEX_MAP.put("operateUserName", ASC);
        INDEX_MAP.put("operateTime", DESC);
        INDEX_MAP.put("operateType", ASC);
        INDEX_MAP.put("ipAddress", ASC);
        INDEX_MAP.put("companyId", ASC);
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void save(AuditBusinessLogPO auditLog) {
        createIndex(AuditBusinessLogPO.COLLECTION_NAME);
        mongoTemplate.insert(Collections.singletonList(auditLog), AuditBusinessLogPO.COLLECTION_NAME);
    }

    @Override
    public void batchSave(Collection<AuditBusinessLogPO> auditLogs) {
        createIndex(AuditBusinessLogPO.COLLECTION_NAME);
        mongoTemplate.insert(auditLogs,AuditBusinessLogPO.COLLECTION_NAME);
    }

    /**
     * 为集合创建索引
     */
    public void createIndex(String collectionName) {
        // 已存在的集合无须创建索引
        if (mongoTemplate.collectionExists(collectionName)) {
            return;
        }
        List<IndexModel> indexes = new LinkedList<>();
        INDEX_MAP.forEach((field, direction) -> {
            IndexModel indexModel = new IndexModel(new BasicDBObject(field, direction));
            indexes.add(indexModel);
        });
        try {
            MongoCollection<Document> collection = mongoTemplate.createCollection(collectionName);
            collection.createIndexes(indexes);
        } catch (UncategorizedMongoDbException e) {
            // 捕获创建索引失败的异常
            logger.info("Collection " + collectionName + " already exists.");
        }
    }
}
