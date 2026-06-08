package com.supcon.supfusion.auth.common.useragent;

public class UserAgentUtil {
    public static UserAgent parse(String userAgentString) {
        return UserAgentParser.parse(userAgentString);
    }
}
