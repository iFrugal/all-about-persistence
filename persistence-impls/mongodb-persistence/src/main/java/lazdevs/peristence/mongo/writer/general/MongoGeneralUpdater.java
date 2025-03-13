package lazdevs.peristence.mongo.writer.general;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.result.UpdateResult;
import lazdevs.peristence.mongo.common.MongoQuery;
import lazydevs.persistence.connection.ConnectionProvider;

import lazydevs.persistence.writer.general.GeneralUpdater;
import lombok.NonNull;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;

import static com.mongodb.client.model.ReturnDocument.AFTER;
import static java.util.stream.Collectors.toList;
import static lazdevs.peristence.mongo.common.ReadMode.FIND;
import static lazydevs.persistence.writer.general.TemplatisedWriteInstruction.process;

public class MongoGeneralUpdater extends MongoGeneralAppender implements GeneralUpdater<MongoQuery, MongoWriteInstruction> {

    public MongoGeneralUpdater(@NonNull ConnectionProvider<MongoDatabase> connectionProvider, @NonNull String defaultCollectionName) {
       super(connectionProvider, defaultCollectionName);
    }

    public MongoGeneralUpdater(@NonNull MongoDatabase mongoDatabase, @NonNull String defaultCollectionName) {
        this(() -> mongoDatabase, defaultCollectionName);
    }

    public MongoGeneralUpdater(@NonNull MongoClient mongoClient, @NonNull String dbName, @NonNull String defaultCollectionName) {
        this(() -> mongoClient.getDatabase(dbName), defaultCollectionName);
    }

    @Override
    public Map<String, Object> replace(Map<String, Object> t, MongoWriteInstruction writeInstruction) {
        writeInstruction = process(t, writeInstruction);
        return getCollection(writeInstruction)
                .findOneAndReplace(getDocumentWithId(t), new Document(t), new FindOneAndReplaceOptions().upsert(false).returnDocument(AFTER));
    }




    @Override
    public Map<String, Object> update(Map<String, Object> t, MongoWriteInstruction writeInstruction) {
        writeInstruction = process(t, writeInstruction);
        Map<String, Object> tWithoutId = new HashMap<>(t);
        tWithoutId.remove("_id");
        Bson update = null == writeInstruction || null == writeInstruction.getDocument() ? new Document("$set", tWithoutId) :  new Document(writeInstruction.getDocument());
        return getCollection(writeInstruction)
                .findOneAndUpdate(getDocumentWithId(t), update, new FindOneAndUpdateOptions().returnDocument(AFTER));
    }


    @Override
    public Map<String, Object> createOrReplace(Map<String, Object> t, MongoWriteInstruction writeInstruction) {
        writeInstruction = process(t, writeInstruction);
        return getCollection(writeInstruction)
                .findOneAndReplace(getDocumentWithId(t), new Document(t), new FindOneAndReplaceOptions().upsert(true).returnDocument(AFTER));
    }

    @Override
    public Map<String, Object> updateOne(String id, Map<String, Object> fieldsToUpdate, MongoWriteInstruction writeInstruction) {
        return getCollection(writeInstruction).findOneAndUpdate(new Document("_id", id), new Document(fieldsToUpdate));
    }

    @Override
    public long updateMany(MongoQuery query, Map<String, Object> fieldsToUpdate, MongoWriteInstruction writeInstruction) {
        Objects.equals(FIND, query.getReadMode());
        UpdateResult updateResult = getCollection(writeInstruction).updateMany(new Document(query.getFind().getFilter()), new Document(fieldsToUpdate));
        return updateResult.getModifiedCount();
    }


    @Override
    public Map<String, Object> delete(Map<String, Object> t, MongoWriteInstruction writeInstruction) {
        writeInstruction = process(t, writeInstruction);
        return getCollection(writeInstruction)
                .findOneAndDelete(getDocumentWithId(t));
    }

    @Override
    public Map<String, Object> delete(@NonNull String id, MongoWriteInstruction writeInstruction) {
        return getCollection(writeInstruction).findOneAndDelete(new Document("_id", id));
    }

    @Override
    public List<Map<String, Object>> delete(List<Map<String, Object>> list, MongoWriteInstruction writeInstruction) {
        return list.stream()
                   .map(e -> delete(e, null))
                   .collect(toList());
    }

    private Document getDocumentWithId(Map<String, Object> t){
        return new Document("_id", Optional.ofNullable(t.get("_id"))
                                           .orElseThrow(() -> new IllegalArgumentException("Missing field '_id' in the given map: " + t)));
    }

}
