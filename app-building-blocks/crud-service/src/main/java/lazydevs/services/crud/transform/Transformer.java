package lazydevs.services.crud.transform;

import lazydevs.services.crud.BaseEntity;

/**
 * @author Abhijeet Rai
 */
public interface Transformer<T extends BaseEntity, DTO> {
    DTO convertToDTO(T t);
    T convertToEntity(DTO dto);
}
