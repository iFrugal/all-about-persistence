package lazydevs.mapper.file.flat.excel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import lazydevs.mapper.file.flat.FlatFileMapper;
import lazydevs.mapper.file.utils.FileBatchIterator;
import lazydevs.mapper.utils.BatchIterator;
import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.engine.TemplateEngine;
import lombok.Setter;

public class ExcelFileMapper<R> extends FlatFileMapper<Row> {
    @Setter
    private DataFormatter dataFormatter=new DataFormatter();


    private abstract class ExcelFileBatchIterator<T> extends FileBatchIterator<T, Row> {
        private final Workbook workbook;
        
        public ExcelFileBatchIterator(Workbook workbook, int batchSize, int noOfLinesToIgnore) {
            super(new CustomRowIterator(workbook.getSheetAt(0)), batchSize, noOfLinesToIgnore);
            this.workbook = workbook;
        }
        
		public ExcelFileBatchIterator(Sheet sheet, int batchSize, int noOfLinesToIgnore) {
			  super(new CustomRowIterator(sheet), batchSize, noOfLinesToIgnore);	
			  this.workbook=sheet.getWorkbook();
		}

		@Override
        public boolean isRowToIgnore(Row row) {
            return false;
        }

        @Override
        public void close() {
            if (null != workbook) {
                try {
                    workbook.close();
                } catch (Exception e) {
                    throw new RuntimeException("Error while closing workbook.", e);
                }
            }
        }
    }



    @Override
    protected Map<String, Object> convert(Row row, String template) {
        try {
            Map<Integer, String> columnIndexToValueMap = getColumnIndexToValueMap(row);
            Map<String, Object> datapoints = columnIndexToValueMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> "i" + entry.getKey(),
                            Map.Entry::getValue
                    ));
            if(template != null) {
                return SerDe.JSON.deserializeToMap(TemplateEngine.getInstance().generate(template, datapoints));
            }else {
                return datapoints;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Integer, String> getColumnIndexToValueMap(Row row) {
        if (null == row) {
            return new HashMap<>();
        }
        int noOfColumns = Integer.valueOf(row.getLastCellNum());
        Map<Integer, String> map = new HashMap<>(noOfColumns);
        for (int i = 0; i < noOfColumns; i++) {
            Cell cell = row.getCell(i);
            map.put(i, null == cell ? "" : dataFormatter.formatCellValue(cell).replace("\n", "\\n ").replace("\t", "\\t").replace("\"", "\\\""));
        }
        return map;
    }

    @Override
    protected <T> T convert(Row row, String template, Class<T> type) {
        try {
            Map<Integer, String> columnIndexToValueMap = getColumnIndexToValueMap(row);
            if(template!=null)
                return (T) convert(columnIndexToValueMap,template,type);
            return (T) convert(columnIndexToValueMap, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected <T> T convert(Row row, Class<T> type) {
        return convert(row,null,type);
    }


    private Workbook getWorkbook(File file) {
        try {
            return WorkbookFactory.create(file);
        } catch (Exception e) {
            throw new RuntimeException("Exception while parsing the file. FileName1 = " + file.getAbsolutePath() + ". " + e.getMessage() + ". " + e.getCause(), e);
        }
    }

    private Workbook getWorkbook(InputStream is) {
        try {
            return WorkbookFactory.create(is);
        } catch (Exception e) {
            throw new RuntimeException("Exception while parsing the fileInputStream. " + e.getMessage() + ". " + e.getCause(), e);
        }
    }

    @Override
    protected <T> FileBatchIterator<T, Row> getBatchIterator(File file, Charset charset, int batchSize, int noOfLinesToIgnore, Class<T> type,String template) {
        return getBatchIterator(getWorkbook(file),batchSize,noOfLinesToIgnore,type,template);
    }

    @Override
    protected <T> FileBatchIterator<T, Row> getBatchIterator(InputStream inputStream, Charset charset, int batchSize, int noOfLinesToIgnore, Class<T> type,String template) {
        return getBatchIterator(getWorkbook(inputStream),batchSize,noOfLinesToIgnore,type,template);
    }

    @Override
    protected FileBatchIterator<Map<String, Object>, Row> getBatchIterator(File file, Charset charset, int batchSize, int noOfLinesToIgnore, String template) {
        return getBatchIterator(getWorkbook(file),batchSize,noOfLinesToIgnore,template);
    }

    @Override
    protected FileBatchIterator<Map<String, Object>, Row> getBatchIterator(InputStream inputStream, Charset charset, int batchSize, int noOfLinesToIgnore, String template) {
        return getBatchIterator(getWorkbook(inputStream),batchSize,noOfLinesToIgnore,template);
    }

    private <T> FileBatchIterator<T, Row> getBatchIterator(Workbook workbook, int batchSize, int noOfLinesToIgnore, Class<T> type,String template) {
        return new ExcelFileBatchIterator<T>(workbook, batchSize, noOfLinesToIgnore) {

            @Override
            public T map(Row row) {
              return null==template?convert(row,type):convert(row,template,type);

            }
        };
    }


    private FileBatchIterator<Map<String,Object>, Row> getBatchIterator(Workbook workbook, int batchSize, int noOfLinesToIgnore,String template) {
        return new ExcelFileBatchIterator<Map<String,Object>>(workbook, batchSize, noOfLinesToIgnore) {
            @Override
            public Map<String, Object> map(Row row) {
                return convert(row,template);
            }
        };
    }

    private Sheet writeHeaders(String[] headers, ClassAttributes classAttributes){
        Workbook workbook = new SXSSFWorkbook(null, 1000, true);
        Sheet sheet = workbook.createSheet();
        String[] headerArray = null == headers || headers.length == 0 ? classAttributes.getHeaders() : headers;
        if(null != headerArray && headerArray.length != 0){
            Row row = sheet.createRow(0);
            int columnCount = 0;
            for (String header : headerArray) {
                if(null != header){
                    Cell cell = row.createCell(columnCount++);
                    cell.setCellValue(header);
                }
            }
        }
        return sheet;
    }

    private  void writeBodyWithTemplate(int rowCounter,Sheet sheet,List<Map<String, Object>> list,String template,String[] headers,String delimiter)
    {
        for (Map<String, Object> t : list) {
            Row rowData = sheet.createRow(rowCounter++);
            int columnCount1 = 0;
            String csv = TemplateEngine.getInstance().generate(template, t);
            //a1|b1|c1
            String[] arr = csv.split(delimiter);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = rowData.createCell(columnCount1++);
                cell.setCellValue(String.valueOf(arr[i].trim()));
            }
        }


    }


    private <T> void writeBody(int rowCounter, Sheet sheet, List<T> list, BiFunction<String, T , Object> valueFunction, ClassAttributes classAttributes){
        for (T t : list) {
            Row rowData = sheet.createRow(rowCounter++);
            int columnCount1 = 0;
            for (int i = 0; i <= classAttributes.getMaxIndex(); i++) {
                Cell cell = rowData.createCell(columnCount1++);
                if (classAttributes.getColumnIndexMap().containsKey(i)) {
                    String fieldName = classAttributes.getColumnIndexMap().get(i);
                    Object value = valueFunction.apply(fieldName, t);
                    if (value != null) {
                        if (value instanceof String) {
                            cell.setCellValue((String) value);
                        } else if (value instanceof Long) {
                            cell.setCellValue((Long) value);
                        } else if (value instanceof Integer) {
                            cell.setCellValue((Integer) value);
                        } else if (value instanceof Double) {
                            cell.setCellValue((Double) value);
                        }else {
                            cell.setCellValue(String.valueOf(value));
                        }
                    }
                }else{
                    cell.setCellValue("");
                }
            }
        }

    }
    private File writeToFile(String absoluteFilePath, Sheet sheet){
        File file = new File(absoluteFilePath);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            sheet.getWorkbook().write(outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }




    @Override
    public <T> File writeFile(List<T> list, String absoluteFilePath, String[] headers, Class<T> type)  {
        ClassAttributes classAttributes = getClassAttributes(type);
        Sheet sheet = writeHeaders(headers, classAttributes);
        writeBody(1, sheet, list, (fieldName, t) -> {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(t);
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        }, classAttributes);
        return writeToFile(absoluteFilePath, sheet);
    }

    @Override
    public File writeFile(List<Map<String, Object>> list, String absoluteFilePath, Map<Integer, String> columnIndexMap, String[] headers) {
        ClassAttributes classAttributes = getClassAttributes(columnIndexMap);
        Sheet sheet = writeHeaders(headers, classAttributes);
        writeBody(1, sheet, list, (fieldName, t) -> t.get(fieldName), classAttributes);
        return writeToFile(absoluteFilePath, sheet);
    }

    @Override
    public  File writeFile(List<Map<String, Object>> list, String absoluteFilePath, String[] headers ,String template,String delimiter) {
        Sheet sheet = writeHeaders(headers, null);
        writeBodyWithTemplate(1, sheet, list,template,headers,delimiter) ;
        return writeToFile(absoluteFilePath, sheet);
    }

    @Override
    public File writeFile(BatchIterator<Map<String, Object>> batchIterator, String absoluteFilePath, Map<Integer, String> columnIndexMap, String[] headers) {
        ClassAttributes classAttributes = getClassAttributes(columnIndexMap);
        Sheet sheet = writeHeaders(headers, classAttributes);
        int rowCounter = 1;
        while(batchIterator.hasNext()){
            List<Map<String, Object>> list = batchIterator.next();
            writeBody(rowCounter, sheet, list, (fieldName, t) -> t.get(fieldName), classAttributes);
            rowCounter = rowCounter + list.size() - 1;
        }
        return writeToFile(absoluteFilePath, sheet);
    }

    @Override
    public File writeFile(List<Map<String, Object>> list, String absoluteFilePath, Map<Integer, String> columnIndexMap) {
        return writeFile(list, absoluteFilePath, columnIndexMap, null);
    }

    @Override
    public <T> OutputStreamProperties writeFileToOutputStream(List<T> list, String[] headers, Class<T> type, OutputStream outputStream) {
        return writeToOutputStream(list,headers,type,outputStream,"xlsx");
    }
    
    public <T> List<T> readFile(String filePath,String template, Class<T> type,int noOflinesToIgnore,boolean skipFormula) {
    	 try (FileBatchIterator<T,R> batchIterator = (FileBatchIterator<T, R>) getBatchIterator(new File(filePath), Charset.forName("UTF-8"), 10000,noOflinesToIgnore,type,template,skipFormula)) {
             List<T> list = new ArrayList<>();
             batchIterator.forEachRemaining(batch -> {
                 list.addAll(batch);
             });
             return list;
         }
    }
    
	@SuppressWarnings("unchecked")
	public <T> List<T> readFileWithSheet(String filePath, String sheetName, String template, Class<T> type,
			int noOflinesToIgnore, boolean skipFormula) {
		final File file = new File(filePath);
		final Workbook workbook = getWorkbook(file);
		final Sheet sheet = workbook.getSheet(sheetName);
		try (FileBatchIterator<T, R> batchIterator = (FileBatchIterator<T, R>) getBatchIterator(sheet,
				Charset.forName("UTF-8"), 10000, noOflinesToIgnore, type, template, skipFormula)) {
			List<T> list = new ArrayList<>();
			batchIterator.forEachRemaining(batch -> {
				list.addAll(batch);
			});
			return list;
		}
	}
    
    public <T> Map<String,List<T>> readFileWithAllSheet(String filePath,String template, Class<T> type,int noOflinesToIgnore,boolean skipFormula) {
    	 final File file = new File(filePath);
    	 final Map<String,List<T>> map = new HashMap<String, List<T>>();
    	 final Workbook workbook = getWorkbook(file);
    	 Iterator<Sheet> sheetItrator = workbook.sheetIterator();
    	 while (sheetItrator.hasNext()) {
    		 final Sheet sheet = sheetItrator.next();
    		 try (FileBatchIterator<T,R> batchIterator = (FileBatchIterator<T, R>) getBatchIterator(sheet, Charset.forName("UTF-8"), 10_000,noOflinesToIgnore,type,template,skipFormula)) {
    	            List<T> list = new ArrayList<>();
    	            batchIterator.forEachRemaining(batch -> {
    	                list.addAll(batch);
    	            });
    	            map.put(sheet.getSheetName(), list);
    	      }
		}
		return map;
   }
    
    private <T> ExcelFileBatchIterator<T> getBatchIterator(Sheet sheet, Charset forName, int batchSize, int noOfLinesToIgnore,
			Class<T> type, String template, boolean skipFormula) {
    	return getBatchIterator(sheet,batchSize,noOfLinesToIgnore,type,template,skipFormula);
	}

	private <T> ExcelFileBatchIterator<T> getBatchIterator(Sheet sheet, int batchSize, int noOfLinesToIgnore, Class<T> type,
			String template, boolean skipFormula) {
		 return new ExcelFileBatchIterator<T>(sheet, noOfLinesToIgnore, noOfLinesToIgnore) {
   		  @Override
             public T map(Row row) {
               return   null==template?convert(row,type):convert(row,template,type,skipFormula);
             }
   	  };
	}

	public <T> FileBatchIterator<T, Row> getBatchIterator(File file, Charset charset, int batchSize, int noOfLinesToIgnore, Class<T> type,String template,boolean skipFormula) {
        return getBatchIterator(getWorkbook(file),batchSize,noOfLinesToIgnore,type,template,skipFormula);
    }
    
    public <T> FileBatchIterator<T, Row> getBatchIterator(Workbook workbook, int batchSize, int noOfLinesToIgnore,
			Class<T> type, String template, boolean skipFormula) {
    	  return new ExcelFileBatchIterator<T>(workbook, noOfLinesToIgnore, noOfLinesToIgnore) {
    		  @Override
              public T map(Row row) {
                return   null==template?convert(row,type):convert(row,template,type,skipFormula);
              }
    	  };
	}

	public <T> T convert(Row row, String template, Class<T> type, boolean skipFormula) {
        try {
            Map<Integer, String> columnIndexToValueMap = getColumnIndexToValueMap(row,skipFormula);
            if(template!=null)
                return (T) convert(columnIndexToValueMap,template,type);
            return (T) convert(columnIndexToValueMap, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    
	}
	
	private Map<Integer, String> getColumnIndexToValueMap(Row row,boolean skipFormula) {
        if (null == row) {
            return new HashMap<>();
        }
        int noOfColumns = Integer.valueOf(row.getLastCellNum());
        Map<Integer, String> map = new HashMap<>(noOfColumns);
        for (int i = 0; i < noOfColumns; i++) {
            Cell cell = row.getCell(i);
            if (skipFormula) {
            	map.put(i, null == cell ? "" : getCellValue(cell));
			}else {
				map.put(i, null == cell ? "" : dataFormatter.formatCellValue(cell).replace("\n", "\\n ").replace("\t", "\\t").replace("\"", "\\\""));
			}
        }
        return map;
    }
	
	private String getCellValue(Cell cell) {
		Object value = null;
		if (cell.getCellType() == CellType.FORMULA) {
			switch (cell.getCachedFormulaResultType()) {
                case BOOLEAN:
				value = cell.getBooleanCellValue();
				break;
                case NUMERIC :
				value = cell.getNumericCellValue();
				break;
                case STRING :
				value = cell.getStringCellValue();
				break;
			default:
				value = cell.getStringCellValue();
				break;
			}
		} else {
			value = dataFormatter.formatCellValue(cell).replace("\n", "\\n ").replace("\t", "\\t").replace("\"", "\\\"");
		}
		return value.toString();

	}
}
