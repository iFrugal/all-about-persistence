package lazydevs.services.crud.transform;

import lazydevs.services.basic.exception.ValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;

public interface TransformableCrudController<T, DTO>{
    ResponseEntity<DTO> post(DTO dto);
    ResponseEntity<DTO> put(DTO dto);
    ResponseEntity<DTO> delete(DTO dto);

    default void prePersist(DTO dto, RequestMethod requestMethod){ }
    default void postPersist(T t, RequestMethod requestMethod){}
    default void preDelete(DTO dto){}
    default void postDelete(T t){}
    default void validate(DTO dto) throws ValidationException {
        if(null == dto){
            throw new IllegalArgumentException("DTO can't be null or empty");
        }
    }

    DTO convertToDTO(T t);
    T convertToEntity(DTO dto);

}
