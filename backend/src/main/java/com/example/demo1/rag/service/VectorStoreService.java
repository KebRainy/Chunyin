package com.example.demo1.rag.service;

import com.example.demo1.config.RAGConfig;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.grpc.SearchResultData;
import io.milvus.grpc.SearchResults;
import io.milvus.response.SearchResultsWrapper;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.collection.LoadCollectionParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Milvus向量数据库服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {
    
    private final RAGConfig ragConfig;
    private MilvusServiceClient milvusClient;
    
    @PostConstruct
    public void init() {
        try {
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withHost(ragConfig.getMilvus().getHost())
                    .withPort(ragConfig.getMilvus().getPort())
                    .build();
            
            milvusClient = new MilvusServiceClient(connectParam);
            
            // 检查集合是否存在，不存在则创建
            String collectionName = ragConfig.getMilvus().getCollectionName();
            if (!hasCollection(collectionName)) {
                createCollection();
            } else {
                // 如果集合已存在，确保已加载
                loadCollection(collectionName);
            }
            
            log.info("Milvus连接成功");
        } catch (Exception e) {
            log.error("Milvus连接失败", e);
        }
    }
    
    /**
     * 检查集合是否存在
     */
    public boolean hasCollection(String collectionName) {
        try {
            R<Boolean> response = milvusClient.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            return response.getData();
        } catch (Exception e) {
            log.error("检查集合失败", e);
            return false;
        }
    }
    
    /**
     * 创建集合
     */
    public void createCollection() {
        try {
            String collectionName = ragConfig.getMilvus().getCollectionName();
            int dimension = ragConfig.getVectorDimension();
            
            // 定义字段
            List<FieldType> fields = Arrays.asList(
                    FieldType.newBuilder()
                            .withName("id")
                            .withDataType(DataType.Int64)
                            .withPrimaryKey(true)
                            .withAutoID(true)
                            .build(),
                    FieldType.newBuilder()
                            .withName("beverage_id")
                            .withDataType(DataType.Int64)
                            .build(),
                    FieldType.newBuilder()
                            .withName("vector")
                            .withDataType(DataType.FloatVector)
                            .withDimension(dimension)
                            .build()
            );
            
            // 创建集合
            CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withDescription("酒类向量集合")
                    .withShardsNum(2)
                    .withFieldTypes(fields)
                    .build();
            
            R<RpcStatus> response = milvusClient.createCollection(createParam);
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                // 创建索引
                createIndex();
                // 创建后加载集合
                loadCollection(collectionName);
                log.info("集合创建成功: {}", collectionName);
            } else {
                log.error("集合创建失败: {}", response.getMessage());
            }
        } catch (Exception e) {
            log.error("创建集合异常", e);
        }
    }
    
    /**
     * 创建索引
     */
    private void createIndex() {
        try {
            String collectionName = ragConfig.getMilvus().getCollectionName();
            
            R<RpcStatus> response = milvusClient.createIndex(
                    CreateIndexParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withFieldName("vector")
                            .withIndexType(IndexType.HNSW)
                            .withMetricType(MetricType.L2)
                            .withExtraParam("{\"M\":16,\"efConstruction\":200}")
                            .build()
            );
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("索引创建成功");
            }
        } catch (Exception e) {
            log.error("创建索引异常", e);
        }
    }
    
    /**
     * 插入向量（关联酒类ID）
     */
    public void insertVectors(List<Long> beverageIds, List<List<Float>> vectors) {
        if (beverageIds == null || vectors == null || beverageIds.size() != vectors.size()) {
            log.warn("插入向量参数不匹配");
            return;
        }
        
        try {
            String collectionName = ragConfig.getMilvus().getCollectionName();
            
            List<List<Float>> vectorList = new ArrayList<>(vectors);
            
            List<InsertParam.Field> fields = Arrays.asList(
                    new InsertParam.Field("beverage_id", beverageIds),
                    new InsertParam.Field("vector", vectorList)
            );
            
            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build();
            
            R<?> response = milvusClient.insert(insertParam);
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("插入向量成功，数量: {}", beverageIds.size());
            } else {
                log.error("插入向量失败: {}", response.getMessage());
            }
        } catch (Exception e) {
            log.error("插入向量异常", e);
        }
    }
    
    /**
     * 插入外部文档向量（不关联具体酒类，beverage_id为null或0）
     */
    public void insertExternalDocumentVectors(List<List<Float>> vectors) {
        if (vectors == null || vectors.isEmpty()) {
            log.warn("向量列表为空");
            return;
        }
        
        try {
            String collectionName = ragConfig.getMilvus().getCollectionName();
            
            // 外部文档使用0作为beverage_id标识
            List<Long> beverageIds = new ArrayList<>();
            for (int i = 0; i < vectors.size(); i++) {
                beverageIds.add(0L);
            }
            
            List<List<Float>> vectorList = new ArrayList<>(vectors);
            
            List<InsertParam.Field> fields = Arrays.asList(
                    new InsertParam.Field("beverage_id", beverageIds),
                    new InsertParam.Field("vector", vectorList)
            );
            
            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build();
            
            R<?> response = milvusClient.insert(insertParam);
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("插入外部文档向量成功，数量: {}", vectors.size());
            } else {
                log.error("插入外部文档向量失败: {}", response.getMessage());
            }
        } catch (Exception e) {
            log.error("插入外部文档向量异常", e);
        }
    }
    
    /**
     * 加载集合到内存
     */
    private void loadCollection(String collectionName) {
        try {
            R<RpcStatus> response = milvusClient.loadCollection(
                    LoadCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("集合加载成功: {}", collectionName);
            } else {
                log.warn("集合加载失败: {}, 消息: {}", collectionName, response.getMessage());
            }
        } catch (Exception e) {
            log.error("加载集合异常: {}", collectionName, e);
        }
    }
    
    /**
     * 向量检索（返回Milvus ID和相似度分数）
     */
    public static class SearchResult {
        private Long milvusId;
        private Long beverageId;
        private Double score;
        
        public SearchResult(Long milvusId, Long beverageId, Double score) {
            this.milvusId = milvusId;
            this.beverageId = beverageId;
            this.score = score;
        }
        
        public Long getMilvusId() { return milvusId; }
        public Long getBeverageId() { return beverageId; }
        public Double getScore() { return score; }
    }
    
    /**
     * 向量检索（返回详细结果）
     */
    public List<SearchResult> searchSimilarWithScore(List<Float> queryVector, int topK) {
        if (queryVector == null || queryVector.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            String collectionName = ragConfig.getMilvus().getCollectionName();
            
            // 确保集合已加载
            loadCollection(collectionName);
            
            List<List<Float>> searchVectors = Collections.singletonList(queryVector);
            
            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withMetricType(MetricType.L2)
                    .withOutFields(Collections.singletonList("beverage_id"))
                    .withTopK(topK)
                    .withVectors(searchVectors)
                    .withVectorFieldName("vector")
                    .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                    .build();
            
            R<?> response = milvusClient.search(searchParam);
            
            if (response.getStatus() == R.Status.Success.getCode() && response.getData() != null) {
                try {
                    // 解析检索结果
                    Object data = response.getData();
                    SearchResultsWrapper wrapper;
                    
                    if (data == null) {
                        log.warn("检索结果数据为null");
                        return new ArrayList<>();
                    }
                    
                    // 根据实际返回类型处理
                    if (data instanceof SearchResults) {
                        try {
                            java.lang.reflect.Method getResultsMethod = SearchResults.class.getMethod("getResults");
                            Object results = getResultsMethod.invoke(data);
                            if (results instanceof SearchResultData) {
                                wrapper = new SearchResultsWrapper((SearchResultData) results);
                            } else {
                                log.error("SearchResults.getResults()返回的不是SearchResultData类型: {}", 
                                    results != null ? results.getClass().getName() : "null");
                                return new ArrayList<>();
                            }
                        } catch (NoSuchMethodException e) {
                            log.error("SearchResults没有getResults方法", e);
                            return new ArrayList<>();
                        } catch (Exception e) {
                            log.error("调用getResults方法失败", e);
                            return new ArrayList<>();
                        }
                    } else if (data instanceof SearchResultData) {
                        wrapper = new SearchResultsWrapper((SearchResultData) data);
                    } else {
                        log.error("检索结果数据类型不匹配: {}", data.getClass().getName());
                        return new ArrayList<>();
                    }
                    
                    List<SearchResult> results = new ArrayList<>();
                    List<?> idScores = wrapper.getIDScore(0);
                    if (idScores != null) {
                        int index = 0;
                        for (Object item : idScores) {
                            if (item instanceof Map) {
                                Map<?, ?> map = (Map<?, ?>) item;
                                Object id = map.get("beverage_id");
                                Long beverageId = null;
                                if (id instanceof Long) {
                                    beverageId = (Long) id;
                                } else if (id instanceof Number) {
                                    beverageId = ((Number) id).longValue();
                                }
                                
                                // 从Map中获取Milvus ID和相似度分数
                                // 注意：SearchResultsWrapper 的 getIDScore 返回的 Map 可能包含不同的键
                                // 根据实际 Milvus SDK 版本调整
                                Object milvusIdObj = map.get("id");
                                Long milvusId = null;
                                if (milvusIdObj instanceof Long) {
                                    milvusId = (Long) milvusIdObj;
                                } else if (milvusIdObj instanceof Number) {
                                    milvusId = ((Number) milvusIdObj).longValue();
                                } else {
                                    // 如果Map中没有id字段，使用索引
                                    milvusId = (long) index;
                                }
                                
                                Object scoreObj = map.get("score") != null ? map.get("score") : map.get("distance");
                                Double score = null;
                                if (scoreObj instanceof Double) {
                                    score = (Double) scoreObj;
                                } else if (scoreObj instanceof Number) {
                                    score = ((Number) scoreObj).doubleValue();
                                } else {
                                    // 如果Map中没有score字段，使用默认值
                                    score = 0.0;
                                }
                                
                                results.add(new SearchResult(milvusId, beverageId, score));
                                index++;
                            }
                        }
                    }
                    
                    return results;
                } catch (Exception e) {
                    log.error("解析检索结果失败", e);
                    return new ArrayList<>();
                }
            } else {
                log.error("向量检索失败: {}", response.getMessage());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("向量检索异常", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 向量检索
     */
    public List<Long> searchSimilar(List<Float> queryVector, int topK) {
        if (queryVector == null || queryVector.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            String collectionName = ragConfig.getMilvus().getCollectionName();
            
            // 确保集合已加载
            loadCollection(collectionName);
            
            List<List<Float>> searchVectors = Collections.singletonList(queryVector);
            
            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withMetricType(MetricType.L2)
                    .withOutFields(Collections.singletonList("beverage_id"))
                    .withTopK(topK)
                    .withVectors(searchVectors)
                    .withVectorFieldName("vector")
                    .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                    .build();
            
            R<?> response = milvusClient.search(searchParam);
            
            if (response.getStatus() == R.Status.Success.getCode() && response.getData() != null) {
                try {
                    // 解析检索结果
                    Object data = response.getData();
                    SearchResultsWrapper wrapper;
                    
                    if (data == null) {
                        log.warn("检索结果数据为null");
                        return new ArrayList<>();
                    }
                    
                    // 根据实际返回类型处理
                    if (data instanceof SearchResults) {
                        // SearchResults类型，需要获取results字段（SearchResultData）
                        try {
                            java.lang.reflect.Method getResultsMethod = SearchResults.class.getMethod("getResults");
                            Object results = getResultsMethod.invoke(data);
                            if (results instanceof SearchResultData) {
                                wrapper = new SearchResultsWrapper((SearchResultData) results);
                            } else {
                                log.error("SearchResults.getResults()返回的不是SearchResultData类型: {}", 
                                    results != null ? results.getClass().getName() : "null");
                                return new ArrayList<>();
                            }
                        } catch (NoSuchMethodException e) {
                            log.error("SearchResults没有getResults方法", e);
                            return new ArrayList<>();
                        } catch (Exception e) {
                            log.error("调用getResults方法失败", e);
                            return new ArrayList<>();
                        }
                    } else if (data instanceof SearchResultData) {
                        wrapper = new SearchResultsWrapper((SearchResultData) data);
                    } else {
                        log.error("检索结果数据类型不匹配: {}", data.getClass().getName());
                        return new ArrayList<>();
                    }
                    
                    List<Long> beverageIds = new ArrayList<>();
                    List<?> idScores = wrapper.getIDScore(0);
                    if (idScores != null) {
                        for (Object item : idScores) {
                            if (item instanceof Map) {
                                Map<?, ?> map = (Map<?, ?>) item;
                                Object id = map.get("beverage_id");
                                if (id instanceof Long) {
                                    beverageIds.add((Long) id);
                                } else if (id instanceof Number) {
                                    beverageIds.add(((Number) id).longValue());
                                }
                            }
                        }
                    }
                    
                    return beverageIds;
                } catch (Exception e) {
                    log.error("解析检索结果失败", e);
                    return new ArrayList<>();
                }
            } else {
                log.error("向量检索失败: {}", response.getMessage());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("向量检索异常", e);
            return new ArrayList<>();
        }
    }
}

