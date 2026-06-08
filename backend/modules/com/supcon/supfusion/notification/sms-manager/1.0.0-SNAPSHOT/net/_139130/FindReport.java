
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
 * &amp;lt;element name="batchid" type="{http://www.w3.org/2001/XMLSchema}string"/&amp;gt;
 * &amp;lt;element name="mobile" type="{http://www.w3.org/2001/XMLSchema}string"/&amp;gt;
 * &amp;lt;element name="pageindex" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 * &amp;lt;element name="flag" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
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
        "batchid",
        "mobile",
        "pageindex",
        "flag"
})
@XmlRootElement(name = "FindReport")
public class FindReport {

    @XmlElement(required = true)
    protected String account;
    @XmlElement(required = true)
    protected String password;
    @XmlElement(required = true)
    protected String batchid;
    @XmlElement(required = true)
    protected String mobile;
    protected int pageindex;
    protected int flag;

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
     * 获取batchid属性的值。
     *
     * @return possible object is
     * {@link String }
     */
    public String getBatchid() {
        return batchid;
    }

    /**
     * 设置batchid属性的值。
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setBatchid(String value) {
        this.batchid = value;
    }

    /**
     * 获取mobile属性的值。
     *
     * @return possible object is
     * {@link String }
     */
    public String getMobile() {
        return mobile;
    }

    /**
     * 设置mobile属性的值。
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setMobile(String value) {
        this.mobile = value;
    }

    /**
     * 获取pageindex属性的值。
     */
    public int getPageindex() {
        return pageindex;
    }

    /**
     * 设置pageindex属性的值。
     */
    public void setPageindex(int value) {
        this.pageindex = value;
    }

    /**
     * 获取flag属性的值。
     */
    public int getFlag() {
        return flag;
    }

    /**
     * 设置flag属性的值。
     */
    public void setFlag(int value) {
        this.flag = value;
    }

}
