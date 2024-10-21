package lazydevs.persistence.reader;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@ToString @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Page<T> {
    PageRequest pageRequest;
    int totalNoOfPages;
    long totalNoOfRecords;
    List<T> data;

    public static <T> PageBuilder<T> builder(@NonNull  PageRequest pageRequest) {
        return new PageBuilder<T>(pageRequest);
    }

    @ToString @Getter @Setter @RequiredArgsConstructor@Builder
    public static class PageRequest{
        @NonNull private final int pageNum;
        @NonNull private final int pageSize;
    }

    @RequiredArgsConstructor
    public static class PageBuilder<T> {
        private final PageRequest pageRequest;
        private int totalNoOfPages;
        private long totalNoOfRecords;
        private List<T> data;


        public PageBuilder<T> totalNoOfRecords(long totalNoOfRecords) {
            this.totalNoOfRecords = totalNoOfRecords;
            int noOfPages = (int)(totalNoOfRecords/pageRequest.pageSize);
            this.totalNoOfPages=  totalNoOfRecords % pageRequest.pageSize == 0 ? noOfPages : noOfPages + 1;
            return this;
        }

        public PageBuilder<T> data(List<T> data) {
            this.data = data;
            return this;
        }

        public Page<T> build() {
            return new Page<T>(pageRequest, totalNoOfPages, totalNoOfRecords, data);
        }
    }
}
