package com.supcon.supfusion.rbac.dao.po;

import com.supcon.supfusion.framework.cloud.common.pojo.PO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author tomcat
 * @date 21-6-1 上午9:10
 */
@ToString
@SuperBuilder
public class MenuInfoSimplePO extends PO {
    private static final long serialVersionUID = -8960080859084119528L;

    /**
     * id
     */
    @Setter
    @Getter
    private Long id;
    /**
     * 父id
     */
    @Setter
    @Getter
    private Long parentId;
    /**
     * 排序码
     */
    @Setter
    @Getter
    private Double sort;
    /**
     * 层级
     */
    @Setter
    @Getter
    private Integer layNo;
    /**
     * 层级编码
     */
    @Getter
    private String layRec;

    @Setter
    @Getter
    private List<Long> fullPathIds;

    public MenuInfoSimplePO() {

    }

    public MenuInfoSimplePO(Long id, Long parentId, Double sort, Integer layNo, String layRec) {
        this.id = id;
        this.parentId = parentId;
        this.sort = sort;
        this.layNo = layNo;
        setLayRec(layRec);
    }

    public void setLayRec(String layRec) {
        this.layRec = layRec;
        this.fullPathIds = Stream.of(layRec.split("-")).map(Long::valueOf).collect(Collectors.toList());
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MenuInfoSimplePO) {
            return ((MenuInfoSimplePO) o).getId().equals(this.id);
        }
        return false;
    }
}
