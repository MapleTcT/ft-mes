package com.supcon.supfusion.signature.services.service;


/**
 * @author zhang yafei
 */
public interface UserService {
    Boolean checkUserName(String username);

    Boolean checkUserPassword(String username, String password);

}
