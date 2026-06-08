package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.base.entities.Department;
import com.supcon.supfusion.base.entities.Position;
import com.supcon.supfusion.base.entities.Staff;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 
 * @author songjiawei
 * 
 */
@Data
public class CreatorInfo implements Serializable {

	private static final long serialVersionUID = -6307543342155639540L;

	private Staff staff;
	private List<Position> positions;
	private Position position;
	private Department department;
	private Department mainDepartment;
	private Position mainPosition;

}