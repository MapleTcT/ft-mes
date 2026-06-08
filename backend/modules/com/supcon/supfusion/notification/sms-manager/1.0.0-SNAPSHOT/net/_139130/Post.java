
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
 * &amp;lt;element name="password" type="{http://www.w3.org/2001/XMLSchema}string"/&amp;gt;
 * &amp;lt;element name="mtpack" type="{http://www.139130.net}MTPacks"/&amp;gt;
 * &amp;lt;/sequence&amp;gt;
 * &amp;lt;/restriction&amp;gt;
 * &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "account",
        "password",
        "mtpack"
})
@XmlRootElement(name = "Post")
public class Post {

    @XmlElement(required = true)
    protected String account;
    @XmlElement(required = true)
    protected String password;
    @XmlElement(required = true)
    protected MTPacks mtpack;

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
     * 获取password属性的值。
     *
     * @return possible object is
     * {@link String }
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置password属性的值。
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setPassword(String value) {
        this.password = value;
    }

    /**
     * 获取mtpack属性的值。
     *
     * @return possible object is
     * {@link MTPacks }
     */
    public MTPacks getMtpack() {
        return mtpack;
    }

    /**
     * 设置mtpack属性的值。
     *
     * @param value allowed object is
     *              {@link MTPacks }
     */
    public void setMtpack(MTPacks value) {
        this.mtpack = value;
    }

}
