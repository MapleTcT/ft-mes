
package net._139130;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * &lt;p&gt;MOMsg complex type的 Java 类。
 * <p>
 * &lt;p&gt;以下模式片段指定包含在此类中的预期内容。
 * <p>
 * &lt;pre&gt;
 * &amp;lt;complexType name="MOMsg"&amp;gt;
 * &amp;lt;complexContent&amp;gt;
 * &amp;lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt;
 * &amp;lt;sequence&amp;gt;
 * &amp;lt;element name="Phone" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="Content" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="MsgType" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 * &amp;lt;element name="SpecNumber" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="ServiceType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="ReceiveTime" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&amp;gt;
 * &amp;lt;element name="Reserve" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;/sequence&amp;gt;
 * &amp;lt;/restriction&amp;gt;
 * &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MOMsg", propOrder = {
        "phone",
        "content",
        "msgType",
        "specNumber",
        "serviceType",
        "receiveTime",
        "reserve"
})
public class MOMsg {

    @XmlElementRef(name = "Phone", namespace = "http://www.139130.net", type = JAXBElement.class, required = false)
    protected JAXBElement<String> phone;
    @XmlElementRef(name = "Content", namespace = "http://www.139130.net", type = JAXBElement.class, required = false)
    protected JAXBElement<String> content;
    @XmlElement(name = "MsgType")
    protected int msgType;
    @XmlElementRef(name = "SpecNumber", namespace = "http://www.139130.net", type = JAXBElement.class, required = false)
    protected JAXBElement<String> specNumber;
    @XmlElementRef(name = "ServiceType", namespace = "http://www.139130.net", type = JAXBElement.class, required = false)
    protected JAXBElement<String> serviceType;
    @XmlElement(name = "ReceiveTime", required = true, nillable = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar receiveTime;
    @XmlElementRef(name = "Reserve", namespace = "http://www.139130.net", type = JAXBElement.class, required = false)
    protected JAXBElement<String> reserve;

    /**
     * 获取phone属性的值。
     *
     * @return possible object is
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public JAXBElement<String> getPhone() {
        return phone;
    }

    /**
     * 设置phone属性的值。
     *
     * @param value allowed object is
     *              {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public void setPhone(JAXBElement<String> value) {
        this.phone = value;
    }

    /**
     * 获取content属性的值。
     *
     * @return possible object is
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public JAXBElement<String> getContent() {
        return content;
    }

    /**
     * 设置content属性的值。
     *
     * @param value allowed object is
     *              {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public void setContent(JAXBElement<String> value) {
        this.content = value;
    }

    /**
     * 获取msgType属性的值。
     */
    public int getMsgType() {
        return msgType;
    }

    /**
     * 设置msgType属性的值。
     */
    public void setMsgType(int value) {
        this.msgType = value;
    }

    /**
     * 获取specNumber属性的值。
     *
     * @return possible object is
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public JAXBElement<String> getSpecNumber() {
        return specNumber;
    }

    /**
     * 设置specNumber属性的值。
     *
     * @param value allowed object is
     *              {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public void setSpecNumber(JAXBElement<String> value) {
        this.specNumber = value;
    }

    /**
     * 获取serviceType属性的值。
     *
     * @return possible object is
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public JAXBElement<String> getServiceType() {
        return serviceType;
    }

    /**
     * 设置serviceType属性的值。
     *
     * @param value allowed object is
     *              {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public void setServiceType(JAXBElement<String> value) {
        this.serviceType = value;
    }

    /**
     * 获取receiveTime属性的值。
     *
     * @return possible object is
     * {@link XMLGregorianCalendar }
     */
    public XMLGregorianCalendar getReceiveTime() {
        return receiveTime;
    }

    /**
     * 设置receiveTime属性的值。
     *
     * @param value allowed object is
     *              {@link XMLGregorianCalendar }
     */
    public void setReceiveTime(XMLGregorianCalendar value) {
        this.receiveTime = value;
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
