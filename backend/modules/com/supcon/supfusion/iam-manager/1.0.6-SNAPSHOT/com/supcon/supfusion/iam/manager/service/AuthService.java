package com.supcon.supfusion.iam.manager.service;

import java.util.List;

public interface AuthService {
    void create(String ak, String sk);

    void delete(List<String> aks);
}
