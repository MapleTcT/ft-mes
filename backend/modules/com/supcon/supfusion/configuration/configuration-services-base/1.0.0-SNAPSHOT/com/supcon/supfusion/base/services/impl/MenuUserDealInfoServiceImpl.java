package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.MenuUserDealInfoDaoImpl;
import com.supcon.supfusion.base.entities.*;
import com.supcon.supfusion.base.services.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/8/17
 */
@Slf4j
@Service
@Transactional
public class MenuUserDealInfoServiceImpl extends BaseServiceImpl implements MenuUserDealInfoService {

    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private PositionService positionService;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private MenuUserDealInfoDaoImpl menuUserDealInfoDao;

    @Override
    public void savePermissionChangesLoggerForWorkFlow(List<Map<String, Object>> permissionDatas) {
        User user = getCurrentUser();
        Staff staff = null;
        if (user != null) {
//            staff = userService.load(this.getCurrentUser().getId()).getStaff();
        }

        if (permissionDatas.size() > 0) {
            MenuUserDealInfo newDealInfo = null;
            Role role = null;
            Position position = null;
            Department department = null;
            Date date = new Date();
            for (Map<String, Object> permissionData : permissionDatas) {
                newDealInfo = new MenuUserDealInfo();
                MenuOperate menuOperate = (MenuOperate)permissionData.get("menuOperate");
                MenuInfo menuInfo = menuOperate.getMenuInfo();
                newDealInfo.setDealer(staff);
                newDealInfo.setDealReason(5);
                newDealInfo.setDealTime(date);
                newDealInfo.setDealType(Integer.parseInt(permissionData.get("dealType").toString()));
                newDealInfo.setMenuOperate(menuOperate);
                newDealInfo.setMenuInfo(menuInfo);
                int type = Integer.parseInt(permissionData.get("type").toString());
                Long id = Long.parseLong(permissionData.get("id").toString());
                if (type == 0) {
                    role = roleService.load(id);
                    newDealInfo.setTargetRole(role);
                } else if (type == 1) {
                    user = userService.load(id);
                    newDealInfo.setTargetUser(user);
                } else if (type == 2) {
                    position = positionService.load(id);
                    newDealInfo.setTargetPosition(position);
                } else if (type == 3) {
                    department = departmentService.load(id);
                    newDealInfo.setTargetDepartment(department);
                }
                String dealInfo = permissionData.get("dealInfo").toString();
                if (dealInfo != null && dealInfo.length() > 0) {
                    newDealInfo.setDealInfo(dealInfo);
                }
                newDealInfo.setType(type);
                newDealInfo.setCid(getCurrentCompany());
//                menuUserDealInfoDao.save(newDealInfo);
            }
        }
    }

    /**
     * 根据菜单操作物理删除处理记录
     * @param menuOperate
     */
    @Override
    @Transactional
    public void deletePhysicalByMenuOperate(MenuOperate menuOperate){
        if(menuOperate!=null){
            List<MenuUserDealInfo> dealInfos = menuUserDealInfoDao.findByCriteria(Restrictions.eq("menuOperate", menuOperate));
            if(dealInfos!=null && !dealInfos.isEmpty()){
                for(MenuUserDealInfo dealInfo : dealInfos){
                    menuUserDealInfoDao.deletePhysical(dealInfo);
                }
            }
        }
    }
}
