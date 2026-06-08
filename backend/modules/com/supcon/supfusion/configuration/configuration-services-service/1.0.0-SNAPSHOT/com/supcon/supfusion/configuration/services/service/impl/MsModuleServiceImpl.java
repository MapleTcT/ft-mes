package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.base.i18n.InternationalBaseResource;
import com.supcon.supfusion.configuration.services.dao.MsModuleDaoImpl;
import com.supcon.supfusion.configuration.services.dao.MsModuleIpAdressDaoImpl;
import com.supcon.supfusion.configuration.services.dao.MsModuleRelationDaoImpl;
import com.supcon.supfusion.configuration.services.entity.MsModule;
import com.supcon.supfusion.configuration.services.entity.MsModuleIpAdress;
import com.supcon.supfusion.configuration.services.entity.MsModuleRelation;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.service.ModuleService;
import com.supcon.supfusion.configuration.services.service.MsModuleService;
import com.supcon.supfusion.configuration.services.utils.EcUtils;
import com.supcon.supfusion.configuration.services.utils.XmlUtils;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 * @Description:
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/1/12
 */
@Slf4j
@Service
@Transactional
public class MsModuleServiceImpl implements MsModuleService {

    @Autowired
    private MsModuleDaoImpl msModuleDAO;
    @Autowired
    private MsModuleRelationDaoImpl relationDAO;
    @Autowired
    private ModuleService moduleService;
    @Autowired
    private MsModuleIpAdressDaoImpl ipAdressDAO;

    @Override
    public Map<String, Object> saveMsModule(String xmlStr) {
        Map<String, Object> jsonMap = new HashMap<String, Object>(16);
        jsonMap.put("success", true);
//        jsonMap.put("message", InternationalResource.get("ec.view.codeIs", getCurrentLanguage()));
        try {
            String ecEnv = XmlUtils.getTagContent(xmlStr, "ecEnv");
            String code = XmlUtils.getTagContent(xmlStr, "code");
            String[] codeInfo = code.split("_");
            String relationstr = XmlUtils.getTagContent(xmlStr, "relations");
            String[] relations = relationstr.split(",");

            String msServiceCode = XmlUtils.getTagContent(xmlStr, "msServiceCode");
            if (msServiceCode == null || "".equals(msServiceCode)) {
                return jsonMap;
            }
            String msServiceRamNum = XmlUtils.getTagContent(xmlStr, "msServiceRamNum");
            String msServiceName = XmlUtils.getTagContent(xmlStr, "msServiceName");
            String msServiceDescription = XmlUtils.getTagContent(xmlStr, "msServiceDescription");
            String msServiceIPHosts = XmlUtils.getTagContent(xmlStr, "msServiceIPHosts");

            List<MsModule> msModuleList = null;
            //不包含版本信息的模块编码 找到 微服务的模块依赖
            List<MsModuleRelation> relationList = relationDAO.findByCriteria(Restrictions.like("code", "%" + codeInfo[0] + "%"), Restrictions.eq("valid", true));

            msModuleList = msModuleDAO.findByCriteria(Restrictions.eq("code", msServiceCode), Restrictions.eq("valid", true));
            //如果模块强依赖被服务使用，改为该服务
            List<MsModuleRelation> relationServiceList = relationDAO.findByCriteria(Restrictions.in("code", relations), Restrictions.eq("valid", true));
            for (MsModuleRelation relService : relationServiceList) {
                msModuleList = new ArrayList<>();
                msModuleList.add(relService.getMsModule());
            }

            //服务编码找到对应的模块依赖
            List<MsModuleRelation> relationMsList = relationDAO.findByCriteria(Restrictions.eq("msModule.code", msServiceCode), Restrictions.eq("valid", true));
            List<String> relationMsCodeList = new ArrayList<>();
            for (MsModuleRelation relation : relationMsList) {
                relationMsCodeList.add(relation.getCode().split("_")[0]);
            }
            //上载包中强依赖

            if (msModuleList != null && msModuleList.size() > 0) {
                //如果存在相同服务
                if (relationList != null && relationList.size() > 0) {
                    //服务编码找到对应的模块依赖
                    relationMsList = relationDAO.findByCriteria(Restrictions.eq("msModule", relationList.get(0).getMsModule()), Restrictions.eq("valid", true));
                    relationMsCodeList = new ArrayList<>();
                    for (MsModuleRelation relation : relationMsList) {
                        relationMsCodeList.add(relation.getCode().split("_")[0]);
                    }
                    //如果模块被使用
                    if (msModuleList.get(0).getCode().equals(relationList.get(0).getMsModule().getCode())) {
                        //如果是同一个服务  （服务信息不修改）
                        this.sameServiceUpload(code, relationList.get(0), relations, relationMsCodeList, relationList.get(0).getMsModule());
                        return null;
                    }
                    //如果是不同服务模块关系加到该服务中 （原环境服务信息不修改）
                    this.sameServiceUpload(code, relationList.get(0), relations, relationMsCodeList, relationList.get(0).getMsModule());
                    return null;
                }
                //如果原环境中模块没有被使用创建关系(将模块关联加到原环境服务中)
                this.sameServiceUpload(code, null, relations, relationMsCodeList, msModuleList.get(0));
                return null;
            }
            //如果不存在服务，创建服务创建依赖关系
            if (relationList != null && relationList.size() > 0) {
                //如果模块存在（上载到原环境的服务中）
                this.updateRelationTableByRelations(code, relations, relationList.get(0).getMsModule());
                return null;
            }
            this.undefineServiceUpload(code, msServiceCode, msServiceRamNum, msServiceName, msServiceDescription, msServiceIPHosts, relations);
            return null;

        } catch (Exception e) {
            log.error("=========存储服务信息失败" + e.getMessage(), e);
            jsonMap.put("success", false);
            jsonMap.put("message", InternationalResource.get("ec.dataclassific.submitfailure") + ":" + e.getMessage());
        }
        return jsonMap;

    }

    /**
     * 更新关系中新增强依赖
     *
     * @param code
     * @param relations
     * @param msModule
     */
    private void updateRelationTableByRelations(String code, String[] relations, MsModule msModule) {
        Map<String, String> addRelationMap = new HashMap<>();
        addRelationMap.put(code.split("_")[0], code);
        for (String relation : relations) {
            if ("".equals(relation)) {
                continue;
            }
            addRelationMap.put(relation.split("_")[0], relation);
        }
        this.updateRelationTable(addRelationMap, msModule);
    }

    /***
     * 服务不存在
     * 新增服务信息
     * @param msServiceCode
     * @param msServiceRamNum
     * @param msServiceName
     * @param msServiceDescription
     * @param msServiceIPHosts
     */
    private void undefineServiceUpload(String code, String msServiceCode, String msServiceRamNum, String msServiceName, String msServiceDescription, String msServiceIPHosts, String[] relations) {
        //新建服务
        MsModule msModule = new MsModule();
        msModule.setCode(msServiceCode);
        msModule.setRamNUM(msServiceRamNum);
        msModule.setName(msServiceName);
        msModule.setShowname(msServiceName);
        msModule.setDescription(msServiceDescription);
        msModule.setArtifact(msServiceCode);
        msModuleDAO.merge(msModule);
        msModuleDAO.flush();
        msModuleDAO.clear();
        //新建服务的ip地址
        for (String iphost : msServiceIPHosts.split(",")) {
            iphost = this.checkHostValidate(iphost);
            MsModuleIpAdress msModuleIpAdress = new MsModuleIpAdress();
            msModuleIpAdress.setIpadress(iphost);
            msModuleIpAdress.setMsModule(msModule);
            msModuleIpAdress.setCode(UUID.randomUUID().toString());
            ipAdressDAO.merge(msModuleIpAdress);
        }
        ipAdressDAO.flush();
        ipAdressDAO.clear();

        //新建服务关联的模块
        Map<String, String> addRelationMap = new HashMap<>();
        addRelationMap.put(code.split("_")[0], code);
        for (String relation : relations) {
            if ("".equals(relation)) {
                continue;
            }
            addRelationMap.put(relation.split("_")[0], relation);
        }
        this.updateRelationTable(addRelationMap, msModule);
    }


    /***
     * 服务存在
     * 如果模块新增了一个强依赖 (服务添加新增依赖信息 如果更新了依赖模块版本 不更新依赖模块版本信息，等依赖模块上载）
     * @param code
     * @param msModuleRelation
     * @param relations
     * @param relationMsCodeList
     * @param msModule
     */
    private void sameServiceUpload(String code, MsModuleRelation msModuleRelation, String[]
            relations, List<String> relationMsCodeList, MsModule msModule) {
        Map<String, String> addRelationMap = new HashMap<>();
        //模块和服务无关联时
        if (msModuleRelation == null) {
            addRelationMap.put(code.split("_")[0], code);
        }
        //模块版本不同时并且为新版本时，删除旧关联 添加新关联
        if (msModuleRelation != null && !code.equals(msModuleRelation.getCode())) {
            if (code.split("_")[1].compareTo(msModuleRelation.getCode().split("_")[1]) > 0) {
                relationDAO.delete(msModuleRelation.getCode());
                relationDAO.flush();
                relationDAO.clear();
                addRelationMap.put(code.split("_")[0], code);
            }
        }
        // 验证模块是否已被使用
        for (String relation : relations) {
            if ("".equals(relation)) {
                continue;
            }
            String relationCode = relation.split("_")[0];
            boolean flag = false;
            for (String relmscode : relationMsCodeList) {
                if (relationCode.equals(relmscode)) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                addRelationMap.put(relation.split("_")[0], relation);
            }
        }
        // 验证这些新增模块依赖是否被其他服务使用
        if (addRelationMap.size() > 0) {
            this.updateRelationTable(addRelationMap, msModule);
        }
    }

    /***
     * 先查询关系表中是否存在这个模块信息再执行插入，保持模块使用唯一
     * @param addRelationMap
     * @param msModule
     */
    private void updateRelationTable(Map<String, String> addRelationMap, MsModule msModule) {
        Criteria criteria = relationDAO.createCriteria();
        for (Map.Entry<String, String> entry : addRelationMap.entrySet()) {
            criteria.add(Restrictions.like("code", entry.getValue()));
        }
        List<MsModuleRelation> removeEntityList = criteria.list();
        for (MsModuleRelation removeEntity : removeEntityList) {
            String key = removeEntity.getCode().split("_")[0];
            if (addRelationMap.get(key) != null) {
                addRelationMap.remove(key);
            }
        }
        //存储服务-模块关系表
        for (Map.Entry<String, String> entry : addRelationMap.entrySet()) {
            MsModuleRelation addRelationEntity = new MsModuleRelation();
            addRelationEntity.setMsModule(msModule);
            addRelationEntity.setCode(entry.getValue());
            relationDAO.merge(addRelationEntity);
        }
        relationDAO.flush();
        relationDAO.clear();

    }


    /***
     * 确认host是否可用
     * @param host
     * @return
     */
    private String checkHostValidate(String host) {
        String ip = host.split(":")[0];
        int port = Integer.valueOf(host.split(":")[1]);
        List<MsModuleIpAdress> relationMsList = ipAdressDAO.findByCriteria(Restrictions.eq("ipadress",host));
        if(null!=relationMsList && relationMsList.size()>0){
            String newHost="";
            try {
                newHost=getNewHost(ip,port);
//                EcUtils.uploadSimpleLogger.info("上载模块服务ip地址："+host+"和"+relationMsList.get(0).getMsModule().getName()+"服务地址冲突，现重新生成ip地址"+newHost);
                EcUtils.uploadLogger.info("上载模块服务ip地址："+host+"和"+relationMsList.get(0).getMsModule().getName()+"服务地址冲突，现重新生成ip地址"+newHost);
            } catch (Exception e) {
                newHost="";
                log.error("创建随机端口失败",e);
            }
            return newHost;
        }
//        Boolean flag = true;
//        if (ip != null && !"".equals(ip)) {
//            if ("127.0.0.1".equals(ip)) {
//                ip = "0.0.0.0";
//            }
//            try (Socket socket = new Socket()) {
//                bindPort(socket, ip, port);
//                flag = true;
//            } catch (Exception e) {
//                flag = false;
//            }
//        }
//        try {
//            if (!flag) {
//                ServerSocket ss = new ServerSocket(0, 1, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
//                port = ss.getLocalPort();
//                return ip + port;
//            }
//        } catch (Exception e) {
//            log.error("创建随机端口失败");
//        }
        return host;
    }

    /***
     * 获取可用的端口
     * @param
     * @return
     */
    public String getNewHost( String ip,int port) throws UnknownHostException, IOException{
        ServerSocket ss;
        ss = new ServerSocket(0, 1, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
        port = ss.getLocalPort();
        String host =ip+":"+port;
        List<MsModuleIpAdress> relationMsList = ipAdressDAO.findByCriteria(Restrictions.eq("ipadress",host));
        if(relationMsList.size()>0){
            getNewHost(ip,port);
        }
        return host;
    }

    /**
     * 绑定 IP 和 端口
     *
     * @param socket 套接字
     * @param host   IP
     * @param port   端口
     * @throws IOException 抛出异常
     * @author shx
     * @date 2020年7月9日20:46:12
     * @since 1.0
     */
    void bindPort(Socket socket, String host, int port) throws IOException {
        socket.bind(new InetSocketAddress(host, port));
    }

    /**
     * 查看是否能绑定ip端口
     *
     * @param host
     * @param port
     * @return
     * @throws Exception
     */
    @SuppressWarnings("finally")
    Boolean bindPort(String host, int port) throws Exception {
        boolean flag = false;
        try {
            Socket s = new Socket();
            s.bind(new InetSocketAddress(host, port));
            s.close();
            flag = true;
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        } finally {
            return flag;
        }
    }


}
