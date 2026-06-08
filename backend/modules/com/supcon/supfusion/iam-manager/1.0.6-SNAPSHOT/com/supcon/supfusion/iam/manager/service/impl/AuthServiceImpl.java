package com.supcon.supfusion.iam.manager.service.impl;

import com.supcon.supfusion.auth.keycloak.client.api.ClientRegistrationApiService;
import com.supcon.supfusion.auth.keycloak.client.api.dto.ClientDTO;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiReference;
import com.supcon.supfusion.iam.manager.service.AuthService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {
    @ServiceApiReference
    private ClientRegistrationApiService clientRegistrationApiService;

    @Override
    public void create(String ak, String sk) {
        ClientDTO clientDTO = new ClientDTO();
        clientDTO.setClientId(ak);
        clientDTO.setSecret(sk);
        clientRegistrationApiService.create(clientDTO);
    }

    @Override
    public void delete(List<String> aks) {
        clientRegistrationApiService.delete(aks);
    }
}
