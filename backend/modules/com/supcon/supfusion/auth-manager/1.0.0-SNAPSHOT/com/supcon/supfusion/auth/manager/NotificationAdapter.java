package com.supcon.supfusion.auth.manager;

import com.supcon.supfusion.notification.apiserver.api.dto.SendWithMessageRequestDTO;

public interface NotificationAdapter {

    void sendMessage(SendWithMessageRequestDTO sendWithMessageRequestDTO);
}
