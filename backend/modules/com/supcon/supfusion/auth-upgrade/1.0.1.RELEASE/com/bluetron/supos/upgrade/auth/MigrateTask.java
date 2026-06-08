package com.bluetron.supos.upgrade.auth;

import com.bluetron.supos.upgrade.log.TaskLogger;
import com.bluetron.supos.upgrade.task.Task;
import com.bluetron.supos.upgrade.task.TaskExecutor;

import java.util.Map;

public class MigrateTask implements TaskExecutor {
    @Override
    public String getTaskName() {
        return null;
    }

    @Override
    public void upgrade(Task task, Map<String, Object> map, TaskLogger taskLogger) {

    }

    //
//    private static DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//
//
//    private static String MYSQL_TBL_USER = "select * from tbl_user where accountType !=1";
//
//    private static String NOMYSQL_TBL_USER = "select * from auth_user where accountType !=1";
//
//    private static String MYSQL_AUTH_USER_ROLE = "select * from tbl_user_role";
//
//
//    private static String NOMYSQL_AUTH_USER_ROLE = "select * from auth_user_role";
//
//    private static String INSERT_INTO_USER = "insert into auth_user (id,user_name,company_id,password,person_id,has_lock,user_type,time_zone,description,login_first,face_url,creator,modifier,create_time,modify_time) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
//
//    private static String SELECT_ORG_PERSON = "select * from org_person where code = ?";
//
//    private static String INSERT_INTO_USER_ROLE = "insert into auth_user_role (id,user_id,role_id,role_type,creator,modifier,create_time,modify_time) values (?,?,?,?,?,?,?,?)";
//
//    private static String MYSQL_PASSWORD_RULES = "select * from tbl_passwd_rules";
//
//    private static String NOMYSQL_PASSWORD_RULES = "select * from auth_passwd_rules";
//
//    private static String PASSWORD_RULES = "insert into auth_passwd_rules (id,min_length,max_length,contain_letter_case,contain_numbers,contain_special_char,creator,modifier,create_time,modify_time) values(?,?,?,?,?,?,?,?,?,?)";
//
//    private static String MYSQL_IP_BLACK_WHITE = "select * from tbl_ip_black_white";
//
//    private static String NOMYSQL_IP_BLACK_WHITE = "select * from auth_ip_black_white";
//
//    private static String IP_BLACK_WHITE = "insert into auth_ip_black_white (id,company_id,ip,control_type,creator,modifier,create_time,modify_time) values(?,?,?,?,?,?,?,?)";
//
//    private static String MYSQL_CONFIG_DASHBOR = "select * from tbl_user_config_dashboard";
//
//    private static String NOMYSQL_CONFIG_DASHBOR = "select * from auth_user_config_dashboard";
//
//    private static String CONFIG_DASHBOR = "insert into auth_user_config_dashboard (user_id,mkey,fields,config_info) values(?,?,?,?)";
//
//    private static String VITUAL_PERSON_ID = "select op.* from org_person op join org_person_company opc on op.id = opc.person_id WHERE opc.company_id = ? AND op.sys_flag = TRUE";
//
//    private static String DELETE_PASSWD_RULES = "delete from auth_passwd_rules";
//
//    @Override
//    public String getTaskName() {
//        return "auth";
//    }
//
//
//    @SneakyThrows
//    public void upgrade(Task task, Map<String, Object> params, TaskLogger taskLogger) {
//        try (Connection originConnection = task.getSrc().getConnection();
//             Connection targetConnection = task.getTarget().getConnection()) {
//            try {
//                targetConnection.setAutoCommit(false);
//                migrateAuthUser(originConnection, targetConnection, task.getSrcDbType(), task.getTargetDbType(), taskLogger);
//                migrateAuthRole(originConnection, targetConnection, task.getSrcDbType(), task.getTargetDbType(), taskLogger);
//                migrateAuthPasswordRule(originConnection, targetConnection, task.getSrcDbType(), task.getTargetDbType(), taskLogger);
//                migrateAuthIpBlackWhite(originConnection, targetConnection, task.getSrcDbType(), task.getTargetDbType(), taskLogger);
//                migrateConfigDashboard(originConnection, targetConnection, task.getSrcDbType(), task.getTargetDbType(), taskLogger);
//                targetConnection.commit();
//            } catch (Exception e) {
//                targetConnection.rollback();
//                taskLogger.log("auth error ====>", e.getMessage());
//                throw e;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            taskLogger.log("auth error ====>", e.getMessage());
//            throw e;
//        }
//
//    }
//
//    private void migrateAuthUser(final Connection originConnection, final Connection targetConnection, String srcDbType, String targetDbType, TaskLogger taskLogger) throws Exception {
//        taskLogger.log("迁移{}开始","auth_user");
//        String authUserSql = "";
//        if (srcDbType.equals("mariadb") || srcDbType.equals("mysql")) {
//            authUserSql = MYSQL_TBL_USER;
//        } else {
//            authUserSql = NOMYSQL_TBL_USER;
//        }
//        PreparedStatement originStatement = originConnection.prepareStatement(authUserSql);
//        ResultSet rs = originStatement.executeQuery();
//        ResultSetMapper<OldUser> resultSetMapper = new ResultSetMapper<OldUser>();
//        PreparedStatement authUser = targetConnection.prepareStatement(INSERT_INTO_USER);
//        List<OldUser> oldUsers = resultSetMapper.mapRersultSetToObject(rs, OldUser.class);
//        if (oldUsers != null && !oldUsers.isEmpty()) {
//            for (int i = 0; i < oldUsers.size(); i++) {
//                OldUser oldUser = oldUsers.get(i);
//                if (oldUser.getUsername().equals("admin")) {
//                    continue;
//                }
//                NewUser newUser = new NewUser();
//                newUser.setId(oldUser.getUserId());
//                newUser.setUserName(oldUser.getUsername());
//                if (oldUser.getCid() == null || oldUser.getCid().longValue() == 1) {
//                    newUser.setCompanyId(1000L);
//                } else {
//                    newUser.setCompanyId(oldUser.getCid());
//                }
//                newUser.setDescription(oldUser.getUserDesc());
//                newUser.setFaceUrl(oldUser.getUploadUrl());
//                newUser.setHasLock(oldUser.getLockStatus());
//                newUser.setUserType(oldUser.getAccountType());
//                newUser.setPassword(oldUser.getPassword());
//                newUser.setLoginFirst(oldUser.getNeedChangePassword());
//                newUser.setDescription(oldUser.getUserDesc());
//                newUser.setTimeZone("CST+08:00");
//                if (StringUtils.isNotEmpty(oldUser.getStaffCode())) {
//                    PreparedStatement preparedStatement = targetConnection.prepareStatement(SELECT_ORG_PERSON);
//                    preparedStatement.setString(1, oldUser.getStaffCode());
//                    ResultSet resultSet = preparedStatement.executeQuery();
//                    if (resultSet.next()) {
//                        long id = resultSet.getLong("id");
//                        newUser.setPersonId(id);
//                    }
//                } else {
//                    PreparedStatement preparedStatement = targetConnection.prepareStatement(VITUAL_PERSON_ID);
//                    preparedStatement.setLong(1, newUser.getCompanyId());
//                    ResultSet resultSet = preparedStatement.executeQuery();
//                    if (resultSet.next()) {
//                        long id = resultSet.getLong("id");
//                        newUser.setPersonId(id);
//                    }
//                }
//                authUser.setLong(1, newUser.getId());
//                authUser.setString(2, newUser.getUserName());
//                authUser.setLong(3, newUser.getCompanyId());
//                authUser.setString(4, newUser.getPassword());
//                authUser.setLong(5, newUser.getPersonId());
//                authUser.setBoolean(6, newUser.getHasLock());
//                authUser.setLong(7, newUser.getUserType());
//                authUser.setString(8, newUser.getTimeZone());
//                authUser.setString(9, newUser.getDescription());
//                authUser.setBoolean(10, newUser.getLoginFirst());
//                authUser.setString(11, newUser.getFaceUrl());
//                authUser.setString(12, "admin");
//                authUser.setString(13, "admin");
//                if (StringUtils.isEmpty(oldUser.getCreateTime())) {
//                    LocalDateTime ldt = LocalDateTime.ofInstant(new java.util.Date().toInstant(), ZoneId.of("UTC"));
//                    authUser.setTimestamp(14, Timestamp.valueOf(ldt));
//                } else {
//                    Date date = sdf.(oldUser.getCreateTime());
//                    OffsetDateTime offsetDateTime = date.toInstant().atOffset(ZoneOffset.ofHours(0));
//                    LocalDateTime ldt = LocalDateTime.ofInstant(offsetDateTime.toInstant(), ZoneId.of("UTC"));
//                    authUser.setTimestamp(14, Timestamp.valueOf(ldt));
//                }
//                if (StringUtils.isEmpty(oldUser.getUpdateTime())) {
//                    LocalDateTime ldt = LocalDateTime.ofInstant(new java.util.Date().toInstant(), ZoneId.of("UTC"));
//                    authUser.setTimestamp(15, Timestamp.valueOf(ldt));
//                } else {
//                    java.util.Date date = sdf.parse(oldUser.getUpdateTime());
//                    OffsetDateTime offsetDateTime = date.toInstant().atOffset(ZoneOffset.ofHours(0));
//                    LocalDateTime ldt = LocalDateTime.ofInstant(offsetDateTime.toInstant(), ZoneId.of("UTC"));
//                    authUser.setTimestamp(15, Timestamp.valueOf(ldt));
//                }
//                authUser.addBatch();
//                if (i % 100 == 0) {
//                    authUser.executeBatch();
//                }
//                if (i == oldUsers.size() - 1 && i % 100 != 0) {
//                    authUser.executeBatch();
//                }
//            }
//        }
//        taskLogger.log("迁移{}结束","auth_user");
//    }
//
//    private void migrateAuthRole(final Connection originConnection, final Connection targetConnection, String srcDbType, String targetDbType, TaskLogger taskLogger) throws Exception {
//        taskLogger.log("迁移{}开始","auth_user_role");
//        String authUserRoleSql = "";
//        if (srcDbType.equals("mariadb") || srcDbType.equals("mysql")) {
//            authUserRoleSql = MYSQL_AUTH_USER_ROLE;
//        } else {
//            authUserRoleSql = NOMYSQL_AUTH_USER_ROLE;
//        }
//        PreparedStatement originStatement = originConnection.prepareStatement(authUserRoleSql);
//        ResultSet rs = originStatement.executeQuery();
//        ResultSetMapper<OldUserRole> resultSetMapper = new ResultSetMapper<OldUserRole>();
//        PreparedStatement authUser = targetConnection.prepareStatement(INSERT_INTO_USER_ROLE);
//        List<OldUserRole> oldUserRoles = resultSetMapper.mapRersultSetToObject(rs, OldUserRole.class);
//        if (oldUserRoles != null && !oldUserRoles.isEmpty()) {
//            for (int i = 0; i < oldUserRoles.size(); i++) {
//                OldUserRole oldUserRole = oldUserRoles.get(i);
//                authUser.setLong(1, oldUserRole.getUrid());
//                authUser.setLong(2, oldUserRole.getUserId());
//                authUser.setLong(3, getRoleId(oldUserRole.getRoleId()));
//                authUser.setLong(4, 1);
//                authUser.setString(5, "admin");
//                authUser.setString(6, "admin");
//                LocalDateTime ldt = LocalDateTime.ofInstant(new java.util.Date().toInstant(), ZoneId.of("UTC"));
//                authUser.setTimestamp(7, Timestamp.valueOf(ldt));
//                authUser.setTimestamp(8, Timestamp.valueOf(ldt));
//                authUser.addBatch();
//                if (i % 100 == 0) {
//                    authUser.executeBatch();
//                }
//                if (i == oldUserRoles.size() - 1 && i % 100 != 0) {
//                    authUser.executeBatch();
//                }
//            }
//
//        }
//        taskLogger.log("迁移{}结束","auth_user_role");
//    }
//
//    private void migrateAuthPasswordRule(final Connection originConnection, final Connection targetConnection, String srcDbType, String targetDbType, TaskLogger taskLogger) throws Exception {
//        taskLogger.log("迁移{}开始","auth_passwd_rules");
//        String passwdSql = "";
//        if (srcDbType.equals("mariadb") || srcDbType.equals("mysql")) {
//            passwdSql = MYSQL_PASSWORD_RULES;
//        } else {
//            passwdSql = NOMYSQL_PASSWORD_RULES;
//        }
//        PreparedStatement originStatement = originConnection.prepareStatement(passwdSql);
//        ResultSet rs = originStatement.executeQuery();
//        if (rs.next()) {
//            String regularExpression = rs.getString("regularExpression");
//            if(StringUtils.isEmpty(regularExpression)){
//                PreparedStatement preparedStatement = targetConnection.prepareStatement(DELETE_PASSWD_RULES);
//                preparedStatement.execute();
//                long id = rs.getLong("id");
//                long minLength = rs.getLong("minLength");
//                long maxLength = rs.getLong("maxLength");
//                boolean containLetterCase = rs.getBoolean("containLetterCase");
//                boolean containNumbers = rs.getBoolean("containNumbers");
//                boolean containSpecialChar = rs.getBoolean("containSpecialChar");
//                PreparedStatement targetStatement = targetConnection.prepareStatement(PASSWORD_RULES);
//                targetStatement.setLong(1, 1L);
//                targetStatement.setLong(2, minLength);
//                targetStatement.setLong(3, maxLength);
//                targetStatement.setBoolean(4, containLetterCase);
//                targetStatement.setBoolean(5, containNumbers);
//                targetStatement.setBoolean(6, containSpecialChar);
//                targetStatement.setString(7, "admin");
//                targetStatement.setString(8, "admin");
//                LocalDateTime ldt = LocalDateTime.ofInstant(new java.util.Date().toInstant(), ZoneId.of("UTC"));
//                targetStatement.setTimestamp(9, Timestamp.valueOf(ldt));
//                targetStatement.setTimestamp(10, Timestamp.valueOf(ldt));
//                targetStatement.execute();
//            }
//        }
//        taskLogger.log("迁移{}结束","auth_passwd_rules");
//    }
//
//    private void migrateAuthIpBlackWhite(final Connection originConnection, final Connection targetConnection, String srcDbType, String targetDbType, TaskLogger taskLogger) throws Exception {
//        taskLogger.log("迁移{}开始","auth_ip_black_white");
//        String ipBlackWhiteSql = "";
//        if (srcDbType.equals("mariadb") || srcDbType.equals("mysql")) {
//            ipBlackWhiteSql = MYSQL_IP_BLACK_WHITE;
//        } else {
//            ipBlackWhiteSql = NOMYSQL_IP_BLACK_WHITE;
//        }
//        ResultSetMapper<OldIpWhite> resultSetMapper = new ResultSetMapper<OldIpWhite>();
//        PreparedStatement originStatement = originConnection.prepareStatement(ipBlackWhiteSql);
//        ResultSet rs = originStatement.executeQuery();
//        List<OldIpWhite> oldIpWhites = resultSetMapper.mapRersultSetToObject(rs, OldIpWhite.class);
//        PreparedStatement targetStatement = targetConnection.prepareStatement(IP_BLACK_WHITE);
//        if (oldIpWhites != null && !oldIpWhites.isEmpty()) {
//            for (int i = 0; i < oldIpWhites.size(); i++) {
//                OldIpWhite oldIpWhite = oldIpWhites.get(i);
//                targetStatement.setLong(1, oldIpWhite.getId());
//                if (oldIpWhite.getCid().longValue() == 1) {
//                    targetStatement.setLong(2, 1000L);
//                } else {
//                    targetStatement.setLong(2, oldIpWhite.getCid());
//                }
//                targetStatement.setString(3, oldIpWhite.getIP());
//                targetStatement.setLong(4, Long.valueOf(oldIpWhite.getType()));
//                targetStatement.setString(5, oldIpWhite.getCreatedBy());
//                targetStatement.setString(6, oldIpWhite.getCreatedBy());
//                Date date = new Date(oldIpWhite.getCreationTime());
//                OffsetDateTime offsetDateTime = date.toInstant().atOffset(ZoneOffset.ofHours(0));
//                LocalDateTime ldt = LocalDateTime.ofInstant(offsetDateTime.toInstant(), ZoneId.of("UTC"));
//                targetStatement.setTimestamp(7, Timestamp.valueOf(ldt));
//                targetStatement.setTimestamp(8, Timestamp.valueOf(ldt));
//                targetStatement.addBatch();
//                if (i % 100 == 0) {
//                    targetStatement.executeBatch();
//                }
//                if (i == oldIpWhites.size() - 1 && i % 100 != 0) {
//                    targetStatement.executeBatch();
//                }
//            }
//        }
//        taskLogger.log("迁移{}结束","auth_ip_black_white");
//    }
//
//    private void migrateConfigDashboard(final Connection originConnection, final Connection targetConnection, String srcDbType, String targetDbType, TaskLogger taskLogger) throws Exception {
//        taskLogger.log("迁移{}开始","auth_user_config_dashboard");
//        String configDashborSql = "";
//        if (srcDbType.equals("mariadb") || srcDbType.equals("mysql")) {
//            configDashborSql = MYSQL_CONFIG_DASHBOR;
//        } else {
//            configDashborSql = NOMYSQL_CONFIG_DASHBOR;
//        }
//        ResultSetMapper<OldConfigDashboard> resultSetMapper = new ResultSetMapper<OldConfigDashboard>();
//        PreparedStatement originStatement = originConnection.prepareStatement(configDashborSql);
//        ResultSet rs = originStatement.executeQuery();
//        List<OldConfigDashboard> oldConfigDashboards = resultSetMapper.mapRersultSetToObject(rs, OldConfigDashboard.class);
//        PreparedStatement targetStatement = targetConnection.prepareStatement(CONFIG_DASHBOR);
//        if (oldConfigDashboards != null && !oldConfigDashboards.isEmpty()) {
//            for (int i = 0; i < oldConfigDashboards.size(); i++) {
//                OldConfigDashboard oldConfigDashboard = oldConfigDashboards.get(i);
//                targetStatement.setLong(1, oldConfigDashboard.getUserId());
//                targetStatement.setString(2, oldConfigDashboard.getMkey());
//                targetStatement.setString(3, oldConfigDashboard.getFields());
//                if (targetDbType.equals("oracle")) {
//                    CLOB clob = oracle.sql.CLOB.createTemporary(targetConnection, false, CLOB.DURATION_SESSION);
//                    clob.setString(1, oldConfigDashboard.getConfigInfo());
//                    targetStatement.setClob(4, clob);
//                } else {
//                    targetStatement.setString(4, oldConfigDashboard.getConfigInfo());
//                }
//                targetStatement.addBatch();
//                if (i % 100 == 0) {
//                    targetStatement.executeBatch();
//                }
//                if (i == oldConfigDashboards.size() - 1 && i % 100 != 0) {
//                    targetStatement.executeBatch();
//                }
//            }
//        }
//        taskLogger.log("迁移{}结束","auth_user_config_dashboard");
//    }
//
//
//    private Long getRoleId(Long roleId) {
//        if (-1 == roleId) {
//            return 2L;
//        } else if (1 == roleId) {
//            return 1L;
//        } else if (2 == roleId) {
//            return 3L;
//        } else if (3 == roleId) {
//            return -3L;
//        } else {
//            return roleId;
//        }
//    }
//
//    public static void main(String[] args) throws ParseException {
//
//    }
}
