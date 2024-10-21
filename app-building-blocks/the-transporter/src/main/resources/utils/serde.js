var SerDe = Java.type('lazydevs.mapper.utils.SerDe');
var ClassUtils = Java.type('lazydevs.mapper.utils.reflection.ClassUtils');

function deserialize(jsonString, typeFqcn){
    return SerDe.JSON.deserialize(jsonString, ClassUtils.loadClass(typeFqcn))
}

function serialize(object){
    return SerDe.JSON.serialize(object);
}

function serializePretty(object, pretty){
    return SerDe.JSON.serialize(object, pretty);
}

