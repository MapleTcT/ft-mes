
package net._139130;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


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
 * &amp;lt;element name="mobiles" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded"/&amp;gt;
 * &amp;lt;element name="content" type="{http://www.w3.org/2001/XMLSchema}string"/&amp;gt;
 * &amp;lt;element name="subid" type="{http://www.w3.org/2001/XMLSchema}string"/&amp;gt;
 * &amp;lt;element name="orgCode" type="{http://www.w3.org/2001/XMLSchema}string"/&amp;gt;
 * &amp;lt;element name="msgFmt" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
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
        "mobiles",
        "content",
        "subid",
        "orgCode",
        "msgFmt"
})
@XmlRootElement(name = "PostMass")
public class PostMass {

    @XmlElement(required = true)
    protected String account;
    @XmlElement(required = true)
    protected String password;
    @XmlElement(required = true)
    protected List<String> mobiles;
    @XmlElement(required = true)
    protected String content;
    @XmlElement(required = true)
    protected String subid;
    @XmlElement(required = true)
    protected String orgCode;
    protected int msgFmt;

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
     * Gets the value of the mobiles property.
     * <p>
     * &lt;p&gt;
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a &lt;CODE&gt;set&lt;/CODE&gt; method for the mobiles property.
     * <p>
     * &lt;p&gt;
     * For example, to add a new item, do as follows:
     * &lt;pre&gt;
     * getMobiles().add(newItem);
     * &lt;/pre&gt;
     * <p>
     * <p>
     * &lt;p&gt;
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public List<String> getMobiles() {
        if (mobiles == null) {
            mobiles = new ArrayList<String>();
        }
        return this.mobiles;
    }

    /**
     * 获取content属性的值。
     *
     * @return possible object is
     * {@link String }
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置content属性的值。
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setContent(String value) {
        this.content = value;
    }

    /**
     * 获取subid属性的值。
     *
     * @return possible object is
     * {@link String }
     */
    public String getSubid() {
        return subid;
    }

    /**
     * 设置subid属性的值。
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSubid(String value) {
        this.subid = value;
    }

    /**
     * 获取orgCode属性的值。
     *
     * @return possible object is
     * {@link String }
     */
    public String getOrgCode() {
        return orgCode;
    }

    /**
     * 设置orgCode属性的值。
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setOrgCode(String value) {
        this.orgCode = value;
    }

    /**
     * 获取msgFmt属性的值。
     */
    public int getMsgFmt() {
        return msgFmt;
    }

    /**
     * 设置msgFmt属性的值。
     */
    public void setMsgFmt(int value) {
        this.msgFmt = value;
    }

}
