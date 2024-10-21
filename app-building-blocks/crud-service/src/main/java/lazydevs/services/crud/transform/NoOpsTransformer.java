package lazydevs.services.crud.transform;

import lazydevs.services.crud.BaseEntity;

/**
 * @author Abhijeet Rai
 */
public class NoOpsTransformer<T extends BaseEntity> implements Transformer<T, T> {

    @Override
    public T convertToDTO(T t) {
        return t;
    }

    @Override
    public T convertToEntity(T dto) {
        return dto;
    }
}
