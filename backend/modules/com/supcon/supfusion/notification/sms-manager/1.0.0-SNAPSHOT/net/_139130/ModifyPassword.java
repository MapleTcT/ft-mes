
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
 * &amp;lt;element name="account" type="{http://www.w3.org/2001/XMLSchema}string"/&amp;gt;
 * &amp;lt;element name="old_password" type="{http://www.w3.org/2001/XMLSchema}string"/&amp;gt;
 * &amp;lt;element name="new_password" type="{http://www.w3.org/2001/XMLSchema}string"/&amp;gt;
 * &amp;lt;/sequence&amp;gt;
 * &amp;lt;/restriction&amp;gt;
 * &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "account",
        "oldPassword",
        "newPassword"
})
@XmlRootElement(name = "ModifyPassword")
public class ModifyPassword {

    @XmlElement(required = true)
    protected String account;
    @XmlElement(name = "old_password", required = true)
    protected String oldPassword;
    @XmlElement(name = "new_password", required = true)
    protected String newPassword;

    /**
     * 获取account属性的值。
     *
     * @return possible object is
     * {@link String }
     */
    public String getAccount() {
        return account;
    }

    /**
     * 设置account属性的值。
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setAccount(String value) {
        this.account = value;
    }

    /**
     * 获取oldPassword属性的值。
     *
     * @return possible object is
     * {@link String }
     */
    public String getOldPassword() {
        return oldPassword;
    }

    /**
     * 设置oldPassword属性的值。
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setOldPassword(String value) {
        this.oldPassword = value;
    }

    /**
     * 获取newPassword属性的值。
     *
     * @return possible object is
     * {@link String }
     */
    public String getNewPassword() {
        return newPassword;
    }

    /**
     * 设置newPassword属性的值。
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setNewPassword(String value) {
        this.newPassword = value;
    }

}
