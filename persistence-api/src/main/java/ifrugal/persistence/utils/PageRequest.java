package ifrugal.persistence.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@ToString
@Getter
public class PageRequest{
    private final int pageNum;
    private final int pageSize;
}
