
package net._139130;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * &lt;p&gt;BindChannel complex type的 Java 类。
 * <p>
 * &lt;p&gt;以下模式片段指定包含在此类中的预期内容。
 * <p>
 * &lt;pre&gt;
 * &amp;lt;complexType name="BindChannel"&amp;gt;
 * &amp;lt;complexContent&amp;gt;
 * &amp;lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt;
 * &amp;lt;sequence&amp;gt;
 * &amp;lt;element name="ChannelNum" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="Carrier" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="SendType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;/sequence&amp;gt;
 * &amp;lt;/restriction&amp;gt;
 * &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BindChannel", propOrder = {
        "channelNum",
        "carrier",
        "sendType"
})
public class BindChannel {

    @XmlElementRef(name = "ChannelNum", namespace = "http://www.139130.net", type = JAXBElement.class, required = false)
    protected JAXBElement<String> channelNum;
    @XmlElementRef(name = "Carrier", namespace = "http://www.139130.net", type = JAXBElement.class, required = false)
    protected JAXBElement<String> carrier;
    @XmlElementRef(name = "SendType", namespace = "http://www.139130.net", type = JAXBElement.class, required = false)
    protected JAXBElement<String> sendType;

    /**
     * 获取channelNum属性的值。
     *
     * @return possible object is
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public JAXBElement<String> getChannelNum() {
        return channelNum;
    }

    /**
     * 设置channelNum属性的值。
     *
     * @param value allowed object is
     *              {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public void setChannelNum(JAXBElement<String> value) {
        this.channelNum = value;
    }

    /**
     * 获取carrier属性的值。
     *
     * @return possible object is
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public JAXBElement<String> getCarrier() {
        return carrier;
    }

    /**
     * 设置carrier属性的值。
     *
     * @param value allowed object is
     *              {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public void setCarrier(JAXBElement<String> value) {
        this.carrier = value;
    }

    /**
     * 获取sendType属性的值。
     *
     * @return possible object is
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public JAXBElement<String> getSendType() {
        return sendType;
    }

    /**
     * 设置sendType属性的值。
     *
     * @param value allowed object is
     *              {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public void setSendType(JAXBElement<String> value) {
        this.sendType = value;
    }

}
