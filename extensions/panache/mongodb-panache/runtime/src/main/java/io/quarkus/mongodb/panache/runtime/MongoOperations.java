package io.quarkus.mongodb.panache.runtime;

import static io.quarkus.mongodb.panache.runtime.BeanUtils.beanName;
import static io.quarkus.mongodb.panache.runtime.BeanUtils.clientFromArc;
import static io.quarkus.mongodb.panache.runtime.BeanUtils.getDatabaseName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.jboss.logging.Logger;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;

import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.mongodb.panache.PanacheUpdate;
import io.quarkus.mongodb.panache.binder.NativeQueryBinder;
import io.quarkus.mongodb.panache.binder.PanacheQlQueryBinder;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;

public class MongoOperations {
    private static final Logger LOGGER = Logger.getLogger(MongoOperations.class);
    public static final String ID = "_id";

    private static final Map<String, String> defaultDatabaseName = new ConcurrentHashMap<>();

    //
    // Instance methods

    public static void persist(Object entity) {
        MongoCollection collection = mongoCollection(entity);
        persist(collection, entity);
    }

    public static void persist(Iterable<?> entities) {
        // not all iterables are re-traversal, so we traverse it once for copying inside a list
        List<Object> objects = new ArrayList<>();
        for (Object entity : entities) {
            objects.add(entity);
        }

        if (objects.size() > 0) {
            // get the first entity to be able to retrieve the collection with it
            Object firstEntity = objects.get(0);
            MongoCollection collection = mongoCollection(firstEntity);
            persist(collection, objects);
        }
    }

    public static void persist(Object firstEntity, Object... entities) {
        MongoCollection collection = mongoCollection(firstEntity);
        if (entities == null || entities.length == 0) {
            persist(collection, firstEntity);
        } else {
            List<Object> entityList = new ArrayList<>();
            entityList.add(firstEntity);
            entityList.addAll(Arrays.asList(entities));
            persist(collection, entityList);
        }
    }

    public static void persist(Stream<?> entities) {
        List<Object> objects = entities.collect(Collectors.toList());
        if (objects.size() > 0) {
            // get the first entity to be able to retrieve the collection with it
            Object firstEntity = objects.get(0);
            MongoCollection collection = mongoCollection(firstEntity);
            persist(collection, objects);
        }
    }

    public static void update(Object entity) {
        MongoCollection collection = mongoCollection(entity);
        update(collection, entity);
    }

    public static void update(Iterable<?> entities) {
        // not all iterables are re-traversal, so we traverse it once for copying inside a list
        List<Object> objects = new ArrayList<>();
        for (Object entity : entities) {
            objects.add(entity);
        }

        if (objects.size() > 0) {
            // get the first entity to be able to retrieve the collection with it
            Object firstEntity = objects.get(0);
            MongoCollection collection = mongoCollection(firstEntity);
            update(collection, objects);
        }
    }

    public static void update(Object firstEntity, Object... entities) {
        MongoCollection collection = mongoCollection(firstEntity);
        if (entities == null || entities.length == 0) {
            update(collection, firstEntity);
        } else {
            List<Object> entityList = new ArrayList<>();
            entityList.add(firstEntity);
            entityList.addAll(Arrays.asList(entities));
            update(collection, entityList);
        }
    }

    public static void update(Stream<?> entities) {
        List<Object> objects = entities.collect(Collectors.toList());
        if (objects.size() > 0) {
            // get the first entity to be able to retrieve the collection with it
            Object firstEntity = objects.get(0);
            MongoCollection collection = mongoCollection(firstEntity);
            update(collection, objects);
        }
    }

    public static void persistOrUpdate(Object entity) {
        MongoCollection collection = mongoCollection(entity);
        persistOrUpdate(collection, entity);
    }

    public static void persistOrUpdate(Iterable<?> entities) {
        // not all iterables are re-traversal, so we traverse it once for copying inside a list
        List<Object> objects = new ArrayList<>();
        for (Object entity : entities) {
            objects.add(entity);
        }

        if (objects.size() > 0) {
            // get the first entity to be able to retrieve the collection with it
            Object firstEntity = objects.get(0);
            MongoCollection collection = mongoCollection(firstEntity);
            persistOrUpdate(collection, objects);
        }
    }

    public static void persistOrUpdate(Object firstEntity, Object... entities) {
        MongoCollection collection = mongoCollection(firstEntity);
        if (entities == null || entities.length == 0) {
            persistOrUpdate(collection, firstEntity);
        } else {
            List<Object> entityList = new ArrayList<>();
            entityList.add(firstEntity);
            entityList.addAll(Arrays.asList(entities));
            persistOrUpdate(collection, entityList);
        }
    }

    public static void persistOrUpdate(Stream<?> entities) {
        List<Object> objects = entities.collect(Collectors.toList());
        if (objects.size() > 0) {
            // get the first entity to be able to retrieve the collection with it
            Object firstEntity = objects.get(0);
            MongoCollection collection = mongoCollection(firstEntity);
            persistOrUpdate(collection, objects);
        }
    }

    public static void delete(Object entity) {
        MongoCollection collection = mongoCollection(entity);
        BsonDocument document = getBsonDocument(collection, entity);
        BsonValue id = document.get(ID);
        BsonDocument query = new BsonDocument().append(ID, id);
        collection.deleteOne(query);
    }

    public static MongoCollection mongoCollection(Class<?> entityClass) {
        MongoEntity mongoEntity = entityClass.getAnnotation(MongoEntity.class);
        MongoDatabase database = mongoDatabase(mongoEntity);
        if (mongoEntity != null && !mongoEntity.collection().isEmpty()) {
            return database.getCollection(mongoEntity.collection(), entityClass);
        }
        return database.getCollection(entityClass.getSimpleName(), entityClass);
    }

    public static MongoDatabase mongoDatabase(Class<?> entityClass) {
        MongoEntity mongoEntity = entityClass.getAnnotation(MongoEntity.class);
        return mongoDatabase(mongoEntity);
    }

    //
    // Private stuff

    private static void persist(MongoCollection collection, Object entity) {
        collection.insertOne(entity);
    }

    private static void persist(MongoCollection collection, List<Object> entities) {
        collection.insertMany(entities);
    }

    private static void update(MongoCollection collection, Object entity) {
        //we transform the entity as a document first
        BsonDocument document = getBsonDocument(collection, entity);

        //then we get its id field and create a new Document with only this one that will be our replace query
        BsonValue id = document.get(ID);
        BsonDocument query = new BsonDocument().append(ID, id);
        collection.replaceOne(query, entity);
    }

    private static void update(MongoCollection collection, List<Object> entities) {
        for (Object entity : entities) {
            update(collection, entity);
        }
    }

    private static void persistOrUpdate(MongoCollection collection, Object entity) {
        //we transform the entity as a document first
        BsonDocument document = getBsonDocument(collection, entity);

        //then we get its id field and create a new Document with only this one that will be our replace query
        BsonValue id = document.get(ID);
        if (id == null) {
            //insert with autogenerated ID
            collection.insertOne(entity);
        } else {
            //insert with user provided ID or update
            BsonDocument query = new BsonDocument().append(ID, id);
            collection.replaceOne(query, entity, new ReplaceOptions().upsert(true));
        }
    }

    private static void persistOrUpdate(MongoCollection collection, List<Object> entities) {
        //this will be an ordered bulk: it's less performant than a unordered one but will fail at the first failed write
        List<WriteModel> bulk = new ArrayList<>();
        for (Object entity : entities) {
            //we transform the entity as a document first
            BsonDocument document = getBsonDocument(collection, entity);

            //then we get its id field and create a new Document with only this one that will be our replace query
            BsonValue id = document.get(ID);
            if (id == null) {
                //insert with autogenerated ID
                bulk.add(new InsertOneModel(entity));
            } else {
                //insert with user provided ID or update
                BsonDocument query = new BsonDocument().append(ID, id);
                bulk.add(new ReplaceOneModel(query, entity,
                        new ReplaceOptions().upsert(true)));
            }
        }

        collection.bulkWrite(bulk);
    }

    private static BsonDocument getBsonDocument(MongoCollection collection, Object entity) {
        BsonDocument document = new BsonDocument();
        Codec codec = collection.getCodecRegistry().get(entity.getClass());
        codec.encode(new BsonDocumentWriter(document), entity, EncoderContext.builder().build());
        return document;
    }

    private static MongoCollection mongoCollection(Object entity) {
        Class<?> entityClass = entity.getClass();
        return mongoCollection(entityClass);
    }

    private static MongoDatabase mongoDatabase(MongoEntity entity) {
        MongoClient mongoClient = clientFromArc(entity, MongoClient.class);
        if (entity != null && !entity.database().isEmpty()) {
            return mongoClient.getDatabase(entity.database());
        }
        String databaseName = getDefaultDatabaseName(entity);
        return mongoClient.getDatabase(databaseName);
    }

    private static String getDefaultDatabaseName(MongoEntity entity) {
        return defaultDatabaseName.computeIfAbsent(beanName(entity), new Function<String, String>() {
            @Override
            public String apply(String beanName) {
                return getDatabaseName(entity, beanName);
            }
        });
    }

    //
    // Queries

    public static Object findById(Class<?> entityClass, Object id) {
        MongoCollection collection = mongoCollection(entityClass);
        return collection.find(new Document(ID, id)).first();
    }

    public static Optional findByIdOptional(Class<?> entityClass, Object id) {
        return Optional.ofNullable(findById(entityClass, id));
    }

    public static PanacheQuery<?> find(Class<?> entityClass, String query, Object... params) {
        return find(entityClass, query, null, params);
    }

    @SuppressWarnings("rawtypes")
    public static PanacheQuery<?> find(Class<?> entityClass, String query, Sort sort, Object... params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        Document docSort = sortToDocument(sort);
        MongoCollection collection = mongoCollection(entityClass);
        return new PanacheQueryImpl(collection, docQuery, docSort);
    }

    /**
     * We should have a query like <code>{'firstname': ?1, 'lastname': ?2}</code> for native one
     * and like <code>firstname = ?1</code> for PanacheQL one.
     */
    static String bindFilter(Class<?> clazz, String query, Object[] params) {
        String bindQuery = bindQuery(clazz, query, params);
        LOGGER.debug(bindQuery);
        return bindQuery;
    }

    /**
     * We should have a query like <code>{'firstname': :firstname, 'lastname': :lastname}</code> for native one
     * and like <code>firstname = :firstname and lastname = :lastname</code> for PanacheQL one.
     */
    static String bindFilter(Class<?> clazz, String query, Map<String, Object> params) {
        String bindQuery = bindQuery(clazz, query, params);
        LOGGER.debug(bindQuery);
        return bindQuery;
    }

    /**
     * We should have a query like <code>{'firstname': ?1, 'lastname': ?2}</code> for native one
     * and like <code>firstname = ?1 and lastname = ?2</code> for PanacheQL one.
     * As update document needs a <code>$set</code> operator we add it if needed.
     */
    static String bindUpdate(Class<?> clazz, String query, Object[] params) {
        String bindUpdate = bindQuery(clazz, query, params);
        if (!bindUpdate.contains("$set")) {
            bindUpdate = "{'$set':" + bindUpdate + "}";
        }
        LOGGER.debug(bindUpdate);
        return bindUpdate;
    }

    /**
     * We should have a query like <code>{'firstname': :firstname, 'lastname': :lastname}</code> for native one
     * and like <code>firstname = :firstname and lastname = :lastname</code> for PanacheQL one.
     * As update document needs a <code>$set</code> operator we add it if needed.
     */
    static String bindUpdate(Class<?> clazz, String query, Map<String, Object> params) {
        String bindUpdate = bindQuery(clazz, query, params);
        if (!bindUpdate.contains("$set")) {
            bindUpdate = "{'$set':" + bindUpdate + "}";
        }
        LOGGER.debug(bindUpdate);
        return bindUpdate;
    }

    private static String bindQuery(Class<?> clazz, String query, Object[] params) {
        String bindQuery = null;
        //determine the type of the query
        if (query.charAt(0) == '{') {
            //this is a native query
            bindQuery = NativeQueryBinder.bindQuery(query, params);
        } else {
            //this is a PanacheQL query
            bindQuery = PanacheQlQueryBinder.bindQuery(clazz, query, params);
        }
        return bindQuery;
    }

    private static String bindQuery(Class<?> clazz, String query, Map<String, Object> params) {
        String bindQuery = null;
        //determine the type of the query
        if (query.charAt(0) == '{') {
            //this is a native query
            bindQuery = NativeQueryBinder.bindQuery(query, params);
        } else {
            //this is a PanacheQL query
            bindQuery = PanacheQlQueryBinder.bindQuery(clazz, query, params);
        }
        return bindQuery;
    }

    public static PanacheQuery<?> find(Class<?> entityClass, String query, Map<String, Object> params) {
        return find(entityClass, query, null, params);
    }

    @SuppressWarnings("rawtypes")
    public static PanacheQuery<?> find(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        Document docSort = sortToDocument(sort);
        MongoCollection collection = mongoCollection(entityClass);
        return new PanacheQueryImpl(collection, docQuery, docSort);
    }

    public static PanacheQuery<?> find(Class<?> entityClass, String query, Parameters params) {
        return find(entityClass, query, null, params.map());
    }

    public static PanacheQuery<?> find(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return find(entityClass, query, sort, params.map());
    }

    @SuppressWarnings("rawtypes")
    public static PanacheQuery<?> find(Class<?> entityClass, Document query, Sort sort) {
        MongoCollection collection = mongoCollection(entityClass);
        Document sortDoc = sortToDocument(sort);
        return new PanacheQueryImpl(collection, query, sortDoc);
    }

    public static PanacheQuery<?> find(Class<?> entityClass, Document query, Document sort) {
        MongoCollection collection = mongoCollection(entityClass);
        return new PanacheQueryImpl(collection, query, sort);
    }

    public static PanacheQuery<?> find(Class<?> entityClass, Document query) {
        return find(entityClass, query, (Document) null);
    }

    public static List<?> list(Class<?> entityClass, String query, Object... params) {
        return find(entityClass, query, params).list();
    }

    public static List<?> list(Class<?> entityClass, String query, Sort sort, Object... params) {
        return find(entityClass, query, sort, params).list();
    }

    public static List<?> list(Class<?> entityClass, String query, Map<String, Object> params) {
        return find(entityClass, query, params).list();
    }

    public static List<?> list(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return find(entityClass, query, sort, params).list();
    }

    public static List<?> list(Class<?> entityClass, String query, Parameters params) {
        return find(entityClass, query, params).list();
    }

    public static List<?> list(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return find(entityClass, query, sort, params).list();
    }

    //specific Mongo query
    public static List<?> list(Class<?> entityClass, Document query) {
        return find(entityClass, query).list();
    }

    //specific Mongo query
    public static List<?> list(Class<?> entityClass, Document query, Document sort) {
        return find(entityClass, query, sort).list();
    }

    public static Stream<?> stream(Class<?> entityClass, String query, Object... params) {
        return find(entityClass, query, params).stream();
    }

    public static Stream<?> stream(Class<?> entityClass, String query, Sort sort, Object... params) {
        return find(entityClass, query, sort, params).stream();
    }

    public static Stream<?> stream(Class<?> entityClass, String query, Map<String, Object> params) {
        return find(entityClass, query, params).stream();
    }

    public static Stream<?> stream(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return find(entityClass, query, sort, params).stream();
    }

    public static Stream<?> stream(Class<?> entityClass, String query, Parameters params) {
        return find(entityClass, query, params).stream();
    }

    public static Stream<?> stream(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return find(entityClass, query, sort, params).stream();
    }

    //specific Mongo query
    public static Stream<?> stream(Class<?> entityClass, Document query) {
        return find(entityClass, query).stream();
    }

    //specific Mongo query
    public static Stream<?> stream(Class<?> entityClass, Document query, Document sort) {
        return find(entityClass, query, sort).stream();
    }

    @SuppressWarnings("rawtypes")
    public static PanacheQuery<?> findAll(Class<?> entityClass) {
        MongoCollection collection = mongoCollection(entityClass);
        return new PanacheQueryImpl(collection, null, null);
    }

    @SuppressWarnings("rawtypes")
    public static PanacheQuery<?> findAll(Class<?> entityClass, Sort sort) {
        MongoCollection collection = mongoCollection(entityClass);
        Document sortDoc = sortToDocument(sort);
        return new PanacheQueryImpl(collection, null, sortDoc);
    }

    private static Document sortToDocument(Sort sort) {
        if (sort == null) {
            return null;
        }

        Document sortDoc = new Document();
        for (Sort.Column col : sort.getColumns()) {
            sortDoc.append(col.getName(), col.getDirection() == Sort.Direction.Ascending ? 1 : -1);
        }
        return sortDoc;
    }

    public static List<?> listAll(Class<?> entityClass) {
        return findAll(entityClass).list();
    }

    public static List<?> listAll(Class<?> entityClass, Sort sort) {
        return findAll(entityClass, sort).list();
    }

    public static Stream<?> streamAll(Class<?> entityClass) {
        return findAll(entityClass).stream();
    }

    public static Stream<?> streamAll(Class<?> entityClass, Sort sort) {
        return findAll(entityClass, sort).stream();
    }

    public static long count(Class<?> entityClass) {
        MongoCollection collection = mongoCollection(entityClass);
        return collection.countDocuments();
    }

    public static long count(Class<?> entityClass, String query, Object... params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        MongoCollection collection = mongoCollection(entityClass);
        return collection.countDocuments(docQuery);
    }

    public static long count(Class<?> entityClass, String query, Map<String, Object> params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        MongoCollection collection = mongoCollection(entityClass);
        return collection.countDocuments(docQuery);
    }

    public static long count(Class<?> entityClass, String query, Parameters params) {
        return count(entityClass, query, params.map());
    }

    //specific Mongo query
    public static long count(Class<?> entityClass, Document query) {
        MongoCollection collection = mongoCollection(entityClass);
        return collection.countDocuments(query);
    }

    public static long deleteAll(Class<?> entityClass) {
        MongoCollection collection = mongoCollection(entityClass);
        return collection.deleteMany(new Document()).getDeletedCount();
    }

    public static boolean deleteById(Class<?> entityClass, Object id) {
        MongoCollection collection = mongoCollection(entityClass);
        Document query = new Document().append(ID, id);
        DeleteResult results = collection.deleteOne(query);
        return results.getDeletedCount() == 1;
    }

    public static long delete(Class<?> entityClass, String query, Object... params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        MongoCollection collection = mongoCollection(entityClass);
        return collection.deleteMany(docQuery).getDeletedCount();
    }

    public static long delete(Class<?> entityClass, String query, Map<String, Object> params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        MongoCollection collection = mongoCollection(entityClass);
        return collection.deleteMany(docQuery).getDeletedCount();
    }

    public static long delete(Class<?> entityClass, String query, Parameters params) {
        return delete(entityClass, query, params.map());
    }

    //specific Mongo query
    public static long delete(Class<?> entityClass, Document query) {
        MongoCollection collection = mongoCollection(entityClass);
        return collection.deleteMany(query).getDeletedCount();
    }

    public static PanacheUpdate update(Class<?> entityClass, String update, Map<String, Object> params) {
        return executeUpdate(entityClass, update, params);
    }

    public static PanacheUpdate update(Class<?> entityClass, String update, Parameters params) {
        return update(entityClass, update, params.map());
    }

    public static PanacheUpdate update(Class<?> entityClass, String update, Object... params) {
        return executeUpdate(entityClass, update, params);
    }

    private static PanacheUpdate executeUpdate(Class<?> entityClass, String update, Object... params) {
        String bindUpdate = bindUpdate(entityClass, update, params);
        BsonDocument docUpdate = BsonDocument.parse(bindUpdate);
        MongoCollection collection = mongoCollection(entityClass);
        return new PanacheUpdateImpl(entityClass, docUpdate, collection);
    }

    private static PanacheUpdate executeUpdate(Class<?> entityClass, String update, Map<String, Object> params) {
        String bindUpdate = bindUpdate(entityClass, update, params);
        BsonDocument docUpdate = BsonDocument.parse(bindUpdate);
        MongoCollection collection = mongoCollection(entityClass);
        return new PanacheUpdateImpl(entityClass, docUpdate, collection);
    }

    public static IllegalStateException implementationInjectionMissing() {
        return new IllegalStateException(
                "This method is normally automatically overridden in subclasses");
    }

}
