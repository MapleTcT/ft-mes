
package net._139130;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.*;
import java.math.BigDecimal;


/**
 * &lt;p&gt;AccountInfo complex type的 Java 类。
 * <p>
 * &lt;p&gt;以下模式片段指定包含在此类中的预期内容。
 * <p>
 * &lt;pre&gt;
 * &amp;lt;complexType name="AccountInfo"&amp;gt;
 * &amp;lt;complexContent&amp;gt;
 * &amp;lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt;
 * &amp;lt;sequence&amp;gt;
 * &amp;lt;element name="Account" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="Identify" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="BizNames" type="{http://www.139130.net}ArrayOfString" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="Userbrief" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="Balance" type="{http://www.w3.org/2001/XMLSchema}decimal"/&amp;gt;
 * &amp;lt;element name="Reserve" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;/sequence&amp;gt;
 * &amp;lt;/restriction&amp;gt;
 * &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AccountInfo", propOrder = {
        "account",
        "name",
        "identify",
        "bizNames",
        "userbrief",
        "balance",
        "reserve"
})
public class AccountInfo {

    @XmlElementRef(name = "Account", namespace = "http://www.139130.net", type = JAXBElement.class, required = false)
    protected JAXBElement<String> account;
    @XmlElementRef(name = "Name", namespace = "http://www.139130.net", type = JAXBElement.class, required = false)
    protected JAXBElement<String> name;
    @XmlElementRef(name = "Identify", namespace = "http://www.139130.net", type = JAXBElement.class, required = false)
    protected JAXBElement<String> identify;
    @XmlElementRef(name = "BizNames", namespace = "http://www.139130.net", type = JAXBElement.class, required = false)
    protected JAXBElement<ArrayOfString> bizNames;
    @XmlElementRef(name = "Userbrief", namespace = "http://www.139130.net", type = JAXBElement.class, required = false)
    protected JAXBElement<String> userbrief;
    @XmlElement(name = "Balance", required = true, nillable = true)
    protected BigDecimal balance;
    @XmlElementRef(name = "Reserve", namespace = "http://www.139130.net", type = JAXBElement.class, required = false)
    protected JAXBElement<String> reserve;

    /**
     * 获取account属性的值。
     *
     * @return possible object is
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public JAXBElement<String> getAccount() {
        return account;
    }

    /**
     * 设置account属性的值。
     *
     * @param value allowed object is
     *              {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public void setAccount(JAXBElement<String> value) {
        this.account = value;
    }

    /**
     * 获取name属性的值。
     *
     * @return possible object is
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public JAXBElement<String> getName() {
        return name;
    }

    /**
     * 设置name属性的值。
     *
     * @param value allowed object is
     *              {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public void setName(JAXBElement<String> value) {
        this.name = value;
    }

    /**
     * 获取identify属性的值。
     *
     * @return possible object is
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public JAXBElement<String> getIdentify() {
        return identify;
    }

    /**
     * 设置identify属性的值。
     *
     * @param value allowed object is
     *              {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public void setIdentify(JAXBElement<String> value) {
        this.identify = value;
    }

    /**
     * 获取bizNames属性的值。
     *
     * @return possible object is
     * {@link JAXBElement }{@code <}{@link ArrayOfString }{@code >}
     */
    public JAXBElement<ArrayOfString> getBizNames() {
        return bizNames;
    }

    /**
     * 设置bizNames属性的值。
     *
     * @param value allowed object is
     *              {@link JAXBElement }{@code <}{@link ArrayOfString }{@code >}
     */
    public void setBizNames(JAXBElement<ArrayOfString> value) {
        this.bizNames = value;
    }

    /**
     * 获取userbrief属性的值。
     *
     * @return possible object is
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public JAXBElement<String> getUserbrief() {
        return userbrief;
    }

    /**
     * 设置userbrief属性的值。
     *
     * @param value allowed object is
     *              {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public void setUserbrief(JAXBElement<String> value) {
        this.userbrief = value;
    }

    /**
     * 获取balance属性的值。
     *
     * @return possible object is
     * {@link BigDecimal }
     */
    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * 设置balance属性的值。
     *
     * @param value allowed object is
     *              {@link BigDecimal }
     */
    public void setBalance(BigDecimal value) {
        this.balance = value;
    }

    /**
     * 获取reserve属性的值。
     *
     * @return possible object is
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public JAXBElement<String> getReserve() {
        return reserve;
    }

    /**
     * 设置reserve属性的值。
     *
     * @param value allowed object is
     *              {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public void setReserve(JAXBElement<String> value) {
        this.reserve = value;
    }

}
