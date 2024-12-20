package lazydevs.persistence.impl.rest.writer;

import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.engine.TemplateEngine;
import lazydevs.persistence.impl.rest.reader.RestGeneralReader;
import lazydevs.persistence.impl.rest.reader.RestGeneralReader.RestInstruction;
import lazydevs.persistence.reader.GeneralTransformer;
import lazydevs.persistence.writer.general.GeneralAppender;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class RestGeneralAppender implements GeneralAppender<RestInstruction> {

    private final RestGeneralReader restGeneralReader;

    @Override
    public Map<String, Object> create(Map<String, Object> t, RestInstruction restInstruction) {
        GeneralTransformer transformer = restInstruction.getTransformer();
        if (null != transformer) {
            Map<String, Object> payloadMap = transformer.convert(t);
            restInstruction.getRequest().setPayload(SerDe.JSON.serialize(payloadMap));
            restInstruction.setTransformer(null);
        }
        if(restInstruction.isSkipCallOnNullPayload() && restInstruction.getRequest().getPayload() == null) {
            return t;
        }
        String str = TemplateEngine.getInstance().generate(SerDe.JSON.serialize(restInstruction), t);
        restInstruction.setTransformer(transformer);
        return restGeneralReader.findOne(SerDe.JSON.deserialize(str, RestInstruction.class), (Map<String, Object>) null);
    }

    @Override
    public Class<RestInstruction> getWriteInstructionType() {
        return RestInstruction.class;
    }
}
