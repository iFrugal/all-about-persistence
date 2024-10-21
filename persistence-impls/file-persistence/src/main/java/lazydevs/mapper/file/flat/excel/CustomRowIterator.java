package lazydevs.mapper.file.flat.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.Iterator;

public class CustomRowIterator implements Iterator<Row> {

    private final Sheet sheet;
    private int rowCurrent;
    private final int rowLast;

    public CustomRowIterator(Sheet sheet) {
        this.sheet = sheet;
        this.rowCurrent=sheet.getFirstRowNum();
        this.rowLast=sheet.getLastRowNum();
    }


    @Override
    public boolean hasNext() {
        return rowCurrent<=rowLast;
    }

    @Override
    public Row next() {
        return sheet.getRow(rowCurrent++);
    }
}
