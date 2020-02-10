package ifrugal.persistence.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;

@RequiredArgsConstructor @ToString @Getter
public class Page<T> {
    private final PageRequest pageRequest;
    private final int nextPageIndex;
    private final int totalNoOfPages;
    private final int totalNoOfRecords;
    private final List<T> data;

    public boolean hasNextPage(){
        return !(nextPageIndex == -1);
    }
}
