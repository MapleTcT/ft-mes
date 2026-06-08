package com.supcon.supfusion.auth.service.excel.entity;


import com.alibaba.excel.metadata.CellData;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lifangyuan
 */
@Slf4j
public class CustomCellWriteHandler implements CellWriteHandler {

    private Table<Integer, Integer, String> weightedGraph = HashBasedTable.create();

    private Map<Integer, String> headMap = new HashMap<>();

    public void put(Integer row, Integer column, String remark) {
        weightedGraph.put(row, column, remark);
    }

    public void put(Map<Integer, String> headMap) {
        this.headMap = headMap;
    }


    @Override
    public void beforeCellCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Row row,
                                 Head head, Integer columnIndex, Integer relativeRowIndex, Boolean isHead) {

    }

    @Override
    public void afterCellCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Cell cell,
                                Head head, Integer relativeRowIndex, Boolean isHead) {

    }

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                 List<CellData> cellDataList, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
        // 这里可以对cell进行任何操作
        log.info("第{}行，第{}列写入完成。", cell.getRowIndex(), cell.getColumnIndex());
        Map<Integer, String> column = weightedGraph.row(cell.getRowIndex());
        if (!column.isEmpty()) {
            for (Map.Entry<Integer, String> entry : column.entrySet()) {
                if (cell.getColumnIndex() == entry.getKey()) {
                    Drawing<?> drawing = writeSheetHolder.getSheet().createDrawingPatriarch();
                    Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, cell.getColumnIndex(), cell.getRowIndex(), cell.getColumnIndex(), cell.getRowIndex()));
                    comment.setString(new XSSFRichTextString(entry.getValue()));
                    cell.setCellComment(comment);
                    if (cell.getRowIndex() == 0) {
                        cell.setCellValue(headMap.get(cell.getColumnIndex()));
                    }
                    Workbook workbook = writeSheetHolder.getSheet().getWorkbook();
                    //只能创建实例才能对单元格进行操作.阿里暂时不支持
                    CellStyle cellStyle = workbook.createCellStyle();
                    //设置前景填充样式
                    cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    //设置前景色为红色
                    cellStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
                    //设置垂直居中
                    cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                    cell.setCellStyle(cellStyle);
                }
            }
        }

    }

    @Override
    public void afterCellDataConverted(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, CellData cellData, Cell cell, Head head, Integer integer, Boolean aBoolean) {

    }
}
