package com.supcon.supfusion.signature.services.service;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * @author zhang yafei
 */
public interface ElectronicSignatureService  {
    Map<String, Object> signatureAuthenticate(Boolean isFirstSigner, String username, String password, String buttonCode);

}
