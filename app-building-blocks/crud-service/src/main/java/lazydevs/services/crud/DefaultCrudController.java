package lazydevs.services.crud;

import lazydevs.mapper.utils.reflection.InitDTO;
import lazydevs.services.crud.transform.DefaultSmartTransController;
import lazydevs.services.crud.transform.NoOpsTransformer;
import lazydevs.services.crud.transform.Transformer;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Set;

public abstract class DefaultCrudController<T extends BaseEntity> extends DefaultSmartTransController<T, T> {

    public DefaultCrudController() {
        super(new NoOpsTransformer());
    }

    public DefaultCrudController(InitDTO transformerInit) {
        super(getNoOpsTransformerInit());
    }
    private static InitDTO getNoOpsTransformerInit(){
        InitDTO initDTO = new InitDTO();
        initDTO.setFqcn(NoOpsTransformer.class.getName());
        return initDTO;
    }
}
