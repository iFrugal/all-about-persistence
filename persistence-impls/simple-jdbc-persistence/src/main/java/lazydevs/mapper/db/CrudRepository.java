package lazydevs.mapper.db;

import lazydevs.persistence.reader.Page;
import lazydevs.persistence.reader.Param;

import java.util.List;

public interface CrudRepository<T> {

    T findOne(Param... params) ;

    List<T> findAll(Param... params) ;

    Page<T> findAll(Page.PageRequest pageRequest, Param... params) ;

    long count(Param... params);

    T save(T t);

    List<T> save(List<T> list);

    T insert(T t);

    List<T> insert(List<T> list);

    T update(T t);

    T patch(T t);

    List<T> update(List<T> list);

    long delete(Param... params) ;

    T delete(T t);
}
