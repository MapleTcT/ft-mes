
package net._139130;

import javax.xml.bind.annotation.*;


/**
 * &lt;p&gt;anonymous complex type的 Java 类。
 * <p>
 * &lt;p&gt;以下模式片段指定包含在此类中的预期内容。
 * <p>
 * &lt;pre&gt;
 * &amp;lt;complexType&amp;gt;
 * &amp;lt;complexContent&amp;gt;
 * &amp;lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt;
 * &amp;lt;sequence&amp;gt;
 * &amp;lt;element name="GetAccountInfoResult" type="{http://www.139130.net}AccountInfo"/&amp;gt;
 * &amp;lt;/sequence&amp;gt;
 * &amp;lt;/restriction&amp;gt;
 * &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "getAccountInfoResult"
})
@XmlRootElement(name = "GetAccountInfoResponse")
public class GetAccountInfoResponse {

    @XmlElement(name = "GetAccountInfoResult", required = true)
    protected AccountInfo getAccountInfoResult;

    /**
     * 获取getAccountInfoResult属性的值。
     *
     * @return possible object is
     * {@link AccountInfo }
     */
    public AccountInfo getGetAccountInfoResult() {
        return getAccountInfoResult;
    }

    /**
     * 设置getAccountInfoResult属性的值。
     *
     * @param value allowed object is
     *              {@link AccountInfo }
     */
    public void setGetAccountInfoResult(AccountInfo value) {
        this.getAccountInfoResult = value;
    }

}
