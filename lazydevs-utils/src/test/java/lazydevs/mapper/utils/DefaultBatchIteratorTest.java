package lazydevs.mapper.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;


public class DefaultBatchIteratorTest {

    @Test
    public void x(){
        BatchIterator<String> it = new DefaultBatchIterator(Arrays.asList(1, 2, 3, 4, 5, 6), 2, (i) -> String.valueOf(i));
        int batchNumber = 0;
        while(it.hasNext()){
            List<String> list = it.next();
            Assert.assertTrue(2 >= list.size());
            System.out.println(list);
            batchNumber++;
        }
        Assert.assertEquals(batchNumber, 4);
    }

}