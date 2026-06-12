/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.utils.PropertyHolder;
import com.supcon.supfusion.configuration.services.entity.CustomCode;
import com.supcon.supfusion.configuration.services.entity.Entity;
import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.utils.StringUtils;
import com.supcon.supfusion.configuration.services.dao.CustomCodeDaoImpl;
import com.supcon.supfusion.configuration.services.service.GenerateService;
import com.supcon.supfusion.configuration.services.service.CustomCodeService;
import com.supcon.supfusion.configuration.services.service.EntityService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import flexjson.JSONSerializer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.hibernate.Query;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * 
 * @author fangzhibin
 * @version $Id$
 */
@Slf4j
@Transactional
@ServiceApiService("ec_CustomCodeService")
public class CustomCodeServiceImpl implements CustomCodeService {

	private static final String[] EXCLUDE_FILE = new String[] { "bin", ".project", ".classpath", ".settings", "build.properties", "target", "MANIFEST.MF", ".deployMethod" };

	private static final String[] SUFFIX_EXCLUDE = new String[] { "jpg", "png", "gif", "cab", "exe", "crx", "xpi" };
	
	@Autowired
	private CustomCodeDaoImpl customCodeDao;
	@Autowired
	private EntityService entityService;

	private List<String> excludeList;

	private List<String> suffixExclude;
	@Autowired
	private JdbcTemplate jdbcTemplate;

//	@Autowired
	private GenerateService generateService;
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.supcon.orchid.entityconf.services.CustomCodeService#getCustomCode(java.lang.String)
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public CustomCode getCustomCode(String code) {
		CustomCode customCode = customCodeDao.findEntityByHql("from CustomCode where code = ?", code);
		return customCode;
	}
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<CustomCode> getCustomCodes(String moduleCode){
		return customCodeDao.findByProperty("moduleCode", moduleCode);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<CustomCode> getCustomCodesByModelCode(String modelCode){
		return customCodeDao.findByProperty("modelCode", modelCode);
	}
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<CustomCode> getCustomCodes(Criterion... criterions){
		return customCodeDao.findByCriteria(criterions);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.supcon.orchid.entityconf.services.CustomCodeService#save(com.supcon.supfusion.configuration.services.pojo.CustomCode[])
	 */
	@Override
	public void save(CustomCode... customCodes) {
		if (null != customCodes && customCodes.length > 0) {
			//TODO  需要优化---------------先删除当前模块所有，后批量插入
			for (CustomCode cc : customCodes) {
				CustomCode c;
				if (null == cc.getModelCode() || cc.getModelCode().length() == 0) {
					c = customCodeDao.findEntityByCriteria(Restrictions.eq("moduleCode", cc.getModuleCode()), Restrictions.isNull("modelCode"),
							Restrictions.eq("type", cc.getType()), Restrictions.eq("subType", cc.getSubType()));
				} else
					// c =
					// customCodeDao.findEntityByHql("from CustomCode where modelCode = ? and type = ? and subType = ?",
					// cc.getModelCode(), cc.getType(),
					// cc.getSubType());

                {
                    c = customCodeDao.findEntityByCriteria(Restrictions.eq("modelCode", cc.getModelCode()), Restrictions.eq("type", cc.getType()),
                            Restrictions.eq("subType", cc.getSubType()));
                }
				if (c == null && cc.getCode() != null) {
					c = customCodeDao.get(cc.getCode());
				}
				if (null != c) {
					// cc.setId(c.getId());
					c.setCustomCode(cc.getCustomCode());
					customCodeDao.merge(c);
				} else {
					customCodeDao.merge(cc);
				}
			}
		}
	}

	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.supcon.orchid.entityconf.services.CustomCodeService#getCustomCode(java.lang.String, java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public CustomCode getCustomCode(String moduleCode, String modelCode, String type, String subType) {
		String hql = "from CustomCode where type = ? and subType = ?";
		List<Object> list = new ArrayList<Object>();
		list.add(type);
		list.add(subType);
		if (null != moduleCode && moduleCode.length() > 0) {
			hql += " and moduleCode = ?";
			list.add(moduleCode);
		}
		if (null != modelCode && modelCode.length() > 0) {
			hql += " and modelCode = ?";
			list.add(modelCode);
		}
		CustomCode c = customCodeDao.findEntityByHql(hql, list.toArray());
		return c;
	}
	
	public static CustomCode getCustomCode(List<CustomCode> customCodes, String modelCode, String type, String subType) {
		for(CustomCode code : customCodes) {
			if(type.equals(code.getType()) && subType.equals(code.getSubType())) {
				if( StringUtils.isEmpty(modelCode) || modelCode.equals(code.getModelCode())) {
					return code;
				}
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.supcon.orchid.entityconf.services.CustomCodeService#findCustomCodes(java.lang.String)
	 */
	@Override
	public List<CustomCode> findCustomCodes(String entityCode) {
		String hql = "from CustomCode where entityCode = ?";
		return customCodeDao.findByHql(hql, entityCode);
	}

	private String getBasePath(String base, Module module) {
		StringBuilder builder = new StringBuilder();
		builder.append(base);
		builder.append(File.separator).append(module.getCode()).append(File.separator);
		return builder.toString();
	}

	private String getBasePath(String base, String ...addPaths) {
		StringBuilder builder = new StringBuilder();
		builder.append(base);
		for(String addpath : addPaths){
			builder.append(File.separator).append(addpath).append(File.separator);	
		}		
		return builder.toString();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.supcon.orchid.entityconf.services.CustomCodeService#buildCustomCodeTree(java.lang.String,
	 * java.lang.String, java.lang.Long, int, String)
	 */
	@Override
	public String buildCustomCodeTree(String entityCode, String basePath, Long id, int type, String filterName) {
		Entity entity = entityService.getEntity(entityCode);
		if (null == basePath || "-1".equals(basePath)) {
			basePath = getBasePath(PropertyHolder.get().getGeneratePath(), entity.getModule());
			//type=1 表示返回所有的内容，type=2表示返回service层的内容，type=3表示返回view层的内容
			if(type == 2){
				basePath = getBasePath(basePath, "service", "src", "main", "java", "com", "supcon", "orchid", entity.getModule().getArtifact());
			} else if(type == 3){
				basePath = getBasePath(basePath, "service", "src", "main", "resources", "views", entity.getModule().getArtifact());
			}
		}		
		List customFiles = new ArrayList();
		if (null != basePath && !basePath.isEmpty()) {
			if(null == filterName || "".equals(filterName)){
				findFiles(basePath, customFiles, id);	
			}else{
				findFiles(new File(basePath), customFiles,filterName);
			}
		}
		JSONSerializer serializer = new JSONSerializer();
		String excludes = "*.class*";
		if(null != filterName){
			//FIXME 查询时，会返回大量无关的属性，目前没找到怎么include对象下面的List
			excludes = "*.*";
		}
		String includes = "*.id,*.name,*.path,*.isDir,*.layRec,*.isParent,*.parentId,*.children,*.children2";
		if (includes != null) {
			StringTokenizer tokenizer = new StringTokenizer(includes, ",");
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				serializer = serializer.include(token.trim());
			}
		}
		if (excludes != null) {
			StringTokenizer tokenizer = new StringTokenizer(excludes, ",");
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				serializer = serializer.exclude(token.trim());
			}
		}
		if(null != filterName && !"".equals(filterName) && customFiles.size() > 10){
			customFiles = customFiles.subList(0, 10);
		}
		String jsonStr = serializer.deepSerialize(customFiles);
		log.info(jsonStr);
		return jsonStr;
	}

	private void findFiles(String basePath, List<CustomFile> customFiles, Long id) {
		File baseDir = new File(basePath);
		Long parentId = id;
		if (!baseDir.exists() || !baseDir.isDirectory()) {
			log.info("{} is not a directory", basePath);
		} else {
			String[] fileTuple = baseDir.list();
			List<CustomFile> dirList = new ArrayList<CustomFile>();
			List<CustomFile> fileList = new ArrayList<CustomFile>();
			Long tempId = id + 1;
			if(fileTuple !=null){
			for (String str : fileTuple) {
				if (excludeList.contains(str) || suffixExclude.contains(str.substring(str.lastIndexOf(".") + 1))) {
					continue;
				}
				File tempFile = new File(basePath + File.separator + str);
				CustomFile cf = new CustomFile();
				cf.setId(tempId++);
				String layRec = parentId + "-" + id;
				cf.setLayRec(layRec);
				cf.setName(tempFile.getName());
				cf.setPath(tempFile.getPath());
				cf.setParentId(parentId);
				if (tempFile.isDirectory()) {
					List<CustomFile> children = cf.getChildren();
					findFiles(cf.getPath(), children, cf.getId());
					cf.setIsDir(true);
					cf.setIsParent(true);
					dirList.add(cf);
				} else {
					cf.setIsDir(false);
					fileList.add(cf);
				}
			}
			}
			Collections.sort(dirList, new Comparator<CustomFile>() {
				@Override
				public int compare(CustomFile o1, CustomFile o2) {
					return o1.getName().compareTo(o2.getName());
				}

			});
			Collections.sort(fileList, new Comparator<CustomFile>() {
				@Override
				public int compare(CustomFile o1, CustomFile o2) {
					return o1.getName().compareTo(o2.getName());
				}

			});
			customFiles.addAll(dirList);
			customFiles.addAll(fileList);
		}
	}

	private void findFiles(File targetFile, List<File> allFile, final String filterName) {
		if (excludeList.contains(targetFile.getName()) || suffixExclude.contains(targetFile.getName().substring(targetFile.getName().lastIndexOf(".") + 1))) {
			return;
		}
		if (targetFile.isDirectory()) {
			File[] files = targetFile.listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					if (file.isDirectory()) {
						return true;
					}
					String[] fileNameTuple = filterName.split("\\*");	//兼容*号模糊匹配
					if (fileNameTuple.length == 0 || "" == fileNameTuple[0] || file.getName().toLowerCase().startsWith(fileNameTuple[0].toLowerCase())) {
						for (int i = 1; i < fileNameTuple.length; i++) {
							if (file.getName().toLowerCase().indexOf(fileNameTuple[i].toLowerCase()) < 0) {
								return false;
							}
						}
						return true;
					}else{
						return false;	
					}					
				}
			});
			for (File file : files) {
				findFiles(file, allFile, filterName);
			}
		} else {
			allFile.add(targetFile);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.supcon.orchid.entityconf.services.CustomCodeService#getFileContent(java.lang.String)
	 */
	@Override
	public String getFileContent(String filePath) {
		if (null != filePath && !filePath.isEmpty()) {
			filePath = filePath.replaceAll("\\|\\|", "\\" + File.separator);
			File file = new File(filePath);
			if (!file.exists()) {
				return "";
			}
			String source;
			try {
				source = FileUtils.readFileToString(file, "UTF-8");
				return source;
			} catch (IOException e) {
				throw new EcException(EcException.Code.FILE_EXISTS);
			}
		}
		return "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.supcon.orchid.entityconf.services.CustomCodeService#saveFileContent(java.lang.String)
	 */
	@Override
	public void saveFileContent(String codeContent, String jses5, String originalPath, String entityCode, Boolean publishEnabled) {
		List<CustomCode> ccs = new ArrayList<CustomCode>();
		List<CustomCode> saveList = new ArrayList<CustomCode>();
		try {
			if (null != originalPath) {
				if (originalPath.endsWith(".java")) {
					ccs.addAll(fetchCustomCode(codeContent, "java", entityCode));
				} else {
					ccs.addAll(fetchCustomCode(codeContent, "html", entityCode));
					if (originalPath.endsWith(".ftl")) {
						ccs.addAll(fetchCustomCode(codeContent, "java", entityCode));
					}
				}
			}
		} catch (IOException e) {
			throw new EcException("add the customCode error");
		}
		List<String> codes=new ArrayList<String>();
		for(CustomCode c : ccs){
			codes.add(c.getCode());
			if(!("// 自定义代码".equals(c.getCustomCode()) || "<!-- 自定义代码 -->".equals(c.getCustomCode()))){
				saveList.add(c);
			}
		}
		if(null!=codes&&codes.size()>0){
			batchDeleteByCodes(codes);
		}
		batchSave(saveList);
		
	//	save(ccs.toArray(new CustomCode[ccs.size()]));
		if (null != originalPath && !originalPath.isEmpty()) {
			originalPath = originalPath.replaceAll("\\|\\|", "\\" + File.separator);
			String fileName = originalPath.substring(originalPath.lastIndexOf(File.separator) + 1);
			String es5FileName = fileName.substring(0, fileName.lastIndexOf(".")) + "-es5"
					+ fileName.substring(fileName.lastIndexOf("."));
			String es5FilePath = originalPath.substring(0, originalPath.lastIndexOf(File.separator) + 1) + es5FileName;
			File file = new File(originalPath);
			File es5File = new File(es5FilePath);
			FileUtils.deleteQuietly(file);
			try {
				FileUtils.writeStringToFile(file, codeContent, "UTF-8");
				if (originalPath.endsWith(".js")) {
					FileUtils.deleteQuietly(es5File);
					FileUtils.writeStringToFile(es5File, jses5, "UTF-8");
				}
			} catch (IOException e) {
				throw new EcException("write the file error");
			}
			if (publishEnabled) {
				String targetPath = null;
				File f = null;
				File es5F = null;
				if (originalPath.endsWith(".ftl") && originalPath.indexOf(File.separator + "views") > -1) {
					targetPath = originalPath.substring(originalPath.indexOf(File.separator + "views") + 1);
					f = new File(PropertyHolder.get().getViewPath() + File.separator + targetPath);
				} else if (!originalPath.endsWith(".jsx") && originalPath.indexOf(File.separator + "static") > -1) {
					targetPath = originalPath.substring(originalPath.indexOf(File.separator + "static") + 7);
					if(originalPath.endsWith(".html")){
						f = new File(PropertyHolder.get().getMisStaticPath() + File.separator + "views" + targetPath);
					}else{
						f = new File(PropertyHolder.get().getStaticPath() + File.separator + targetPath);
					}
				} else if ((originalPath.endsWith(".js") || originalPath.endsWith(".css")) && originalPath.indexOf(File.separator + "custom") > -1) {
					targetPath = originalPath.substring(originalPath.indexOf(File.separator + "custom") + 7);
					f = new File(PropertyHolder.get().getMisStaticPath() + File.separator + "greenDill" + File.separator + "static" + File.separator + targetPath);
					if (originalPath.endsWith(".js")) {
						String es5TargetPath = es5FilePath.substring(es5FilePath.indexOf(File.separator + "custom") + 7);
						es5F = new File(PropertyHolder.get().getMisStaticPath() + File.separator + "greenDill" + File.separator + "static" + File.separator + es5TargetPath);
					}
				} else if (originalPath.endsWith(".html")  && originalPath.indexOf(File.separator + "custom") > -1) {
					File srcFile = new File(originalPath);
					targetPath = originalPath.substring(originalPath.indexOf(File.separator + "custom") + 7);
					File destFile = new File(PropertyHolder.get().getMisStaticPath() + File.separator + "custom" + File.separator + targetPath);
					try {
						FileUtils.copyFile(srcFile, destFile);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					int endIndex = originalPath.lastIndexOf('\\');
					targetPath = originalPath.substring(originalPath.indexOf(File.separator + "custom") + 7, endIndex) + ".html";
					f = new File(PropertyHolder.get().getMisStaticPath() + File.separator + "views" + targetPath);
					codeContent = alterHtmlCustomCode(f, file, codeContent);
				}
				FileUtils.deleteQuietly(f);
				FileUtils.deleteQuietly(es5F);
				try {
					FileUtils.writeStringToFile(f, codeContent, "UTF-8");
					if (originalPath.endsWith(".js")) {
						FileUtils.writeStringToFile(es5F, jses5, "UTF-8");
					}
				} catch (IOException e) {
					throw new EcException("write the file error");
				}
			}
		}
		/*if(entityCode!=null && !"".equals(entityCode)){
			Entity entity = entityService.getEntity(entityCode);
			if(null!=entity){
				//开始保存模块信息数据的最后修改时间
				ModuleGenerateInfo generateInfo = moduleGenerateInfoService.getModuleGenerateInfoByModuleCode(entity.getModule().getCode());
				if(generateInfo!=null){
					generateInfo.setLastModifyTime(new Date());
					moduleGenerateInfoService.save(generateInfo,Thread.currentThread());
				}else{
					generateInfo = new ModuleGenerateInfo();
					generateInfo.setLastModifyTime(new Date());
					generateInfo.setModuleCode(entity.getModule().getCode());
					moduleGenerateInfoService.create(generateInfo,Thread.currentThread());
				}
			}
		}*/
	}

	private String alterHtmlCustomCode(File file, File staticFile, String codeContent){
		//先读取文件中的内容
		StringBuilder temp = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String s = null;
			while ((s = br.readLine()) != null) {// 使用readLine方法，一次读一行
				temp.append(System.lineSeparator() + s);
			}
			br.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		//替换内容
		return temp.toString().replaceAll("<!-- " + staticFile.getName()
				+ "--start -->([\\s\\S]*?)<!-- " + staticFile.getName()
				+ "--end -->", "<!-- " + staticFile.getName()
				+ "--start -->\n\n" + codeContent + "\n\t<!-- " + staticFile.getName()
				+ "--end -->");
	}

	private List<CustomCode> fetchCustomCode(String source, String type, String entityCode) throws IOException {
		String start, end;
		if ("html".equals(type) || "xml".equals(type)) {
			start = "<!-- ";
			end = " -->";
		} else {
			start = "/\\* ";
			end = " \\*/";
		}
		String s = start + "CUSTOM CODE START";
		String e = start + "CUSTOM CODE END" + end;
		Pattern p = Pattern.compile(s + "\\((.+),(.+),(.*),(.+)\\)" + end + "([\\s\\S]*?)" + e);
		Matcher m = p.matcher(source);
		List<CustomCode> ccs = new ArrayList<CustomCode>();
		while (m.find()) {
			
			String customCode=m.group(5).trim();
			/*if("// 自定义代码".equals(customCode) || "<!-- 自定义代码 -->".equals(customCode)){
				continue;
			}*/
			CustomCode cc = new CustomCode();
			if (null == m.group(3) || m.group(3).length() == 0) {
				cc.setCode(m.group(4) + "_" + m.group(1) + "_" + m.group(2));
			} else {
				cc.setCode(m.group(4) + "_" + m.group(3) + "_" + m.group(1) + "_" + m.group(2));
			}
			cc.setType(m.group(1));
			cc.setSubType(m.group(2));
			cc.setModelCode(m.group(3));
			cc.setModuleCode(m.group(4));
			cc.setCustomCode(customCode);
			cc.setEntityCode(entityCode);
			ccs.add(cc);
		}
		return ccs;
	}

	@Data
	class CustomFile {
		private static final long serialVersionUID = 1359475749334513341L;
		private Long id;
		private String name;
		private String path;
		private String layRec;
		private Boolean isDir = false;
		private Long parentId;
		private Boolean isParent;
		private List<CustomFile> children = new ArrayList<CustomFile>();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@PostConstruct
	public void afterPropertiesSet() throws Exception {
		try {
			excludeList = new ArrayList<String>();
			for (String str : EXCLUDE_FILE) {
				excludeList.add(str);
			}
			suffixExclude = new ArrayList<String>();
			for (String str : SUFFIX_EXCLUDE) {
				suffixExclude.add(str);
			}
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void batchSave(final List<CustomCode> customCodes) {
		final String sql = "INSERT INTO "
				+ CustomCode.TABLE_NAME
				+ "(CODE,EC_ENV,VERSION,CUSTOM_CODE,SUB_TYPE,TYPE,MODEL_CODE,ENTITY_CODE,MODULE_CODE) VALUES (?,?,?,?,?,?,?,?,?)";
	
		jdbcTemplate.batchUpdate(sql,  new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
					ps.setString(1, customCodes.get(i).getCode());
					ps.setString(2, customCodes.get(i).getEcEnv().toString());
					ps.setInt(3, 0);
						String customCode = customCodes.get(i).getCustomCode();
						if (customCode == null) {
							customCode = "";
						}
						ps.setCharacterStream(4, new StringReader(customCode), customCode.length());
					ps.setString(5, customCodes.get(i).getSubType());
					ps.setString(6, customCodes.get(i).getType());
					ps.setString(7, customCodes.get(i).getModelCode());
					ps.setString(8, customCodes.get(i).getEntityCode());
					ps.setString(9, customCodes.get(i).getModuleCode());
			}
			
			@Override
			public int getBatchSize() {
				return customCodes.size();
			}
		});
	}

	@Override
	public void batchDelete(String moduleCode) {
		customCodeDao.createNativeQuery("delete from " + CustomCode.TABLE_NAME + " where MODULE_CODE=?", moduleCode).executeUpdate();
		
	}
	@Override
	public void batchDeleteByCodes(List<String> list) {
		if(null != list && !list.isEmpty()){
			Query sqlQuery=customCodeDao.createNativeQuery("delete from " + CustomCode.TABLE_NAME + " where code in (:codes)");
			sqlQuery.setParameterList("codes", list.toArray());
			sqlQuery.executeUpdate();
		}
		
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public void compileAndDeploy(String path,String entityCode){
		 Entity entity = entityService.getEntity(entityCode);
		 if(null != entity){
			 String moduleCode = entity.getModule().getCode();
//			 mvnCompile(moduleCode);
//			 codeDeploy(path, entity.getModule());
		 }else{
			 log.error("entity " + entityCode + " is empty list");
		 }
	}

	/**
	 * 判断代理包是否存在
	 * @return
	 */
	private boolean checkAgentExists(String path){
		if(new File(path).exists()){
			return true;
		}
		log.error("jar包不存在" + path);
		return false;
	}
	
	/**
	 * BAP代码简单热部署
	 * @param
	 * @param
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public void bapCodeDeploy(String targetPath,String packagePath) {

	}
}
