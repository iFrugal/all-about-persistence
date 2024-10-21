package lazydevs.services.crud.transform;

import lazydevs.mapper.utils.reflection.InitDTO;
import lazydevs.services.crud.BaseEntity;

import static lazydevs.mapper.utils.reflection.ReflectionUtils.getInterfaceReference;

/**
 * @author Abhijeet Rai
 */
public abstract class DefaultSmartTransController<T extends BaseEntity, DTO>  extends DefaultTransCrudController<T, DTO> {
    private final Transformer<T, DTO> transformer;
    public DefaultSmartTransController(Transformer<T, DTO> transformer) {
        this.transformer = transformer;
    }

    public DefaultSmartTransController(InitDTO transformerInit) {
        this.transformer = getInterfaceReference(transformerInit, Transformer.class);
    }


    @Override
    public DTO convertToDTO(T t) {
        return transformer.convertToDTO(t);
    }

    @Override
    public T convertToEntity(DTO dto) {
        return transformer.convertToEntity(dto);
    }
}

