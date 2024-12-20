package lazydevs.mapper.rest;

import lazydevs.mapper.utils.BatchIterator;


import java.util.List;
import java.util.concurrent.Future;


public interface RestMapper {

    //isSSLCheckRequired, sync/async, closeResponse/notClose

    RestOutput call(RestInput restInput);

    List<RestOutput> call(List<RestInput> restInputs);

    List<RestOutput> call(BatchIterator<RestInput> restInputBatchIterator);

    Future<RestOutput> callAysnc(RestInput restInput);


}
