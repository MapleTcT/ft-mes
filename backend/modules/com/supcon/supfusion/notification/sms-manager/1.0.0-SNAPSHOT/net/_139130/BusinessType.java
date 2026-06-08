
package net._139130;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.*;


/**
 * &lt;p&gt;BusinessType complex type的 Java 类。
 * <p>
 * &lt;p&gt;以下模式片段指定包含在此类中的预期内容。
 * <p>
 * &lt;pre&gt;
 * &amp;lt;complexType name="BusinessType"&amp;gt;
 * &amp;lt;complexContent&amp;gt;
 * &amp;lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt;
 * &amp;lt;sequence&amp;gt;
 * &amp;lt;element name="Id" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 * &amp;lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="Priority" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 * &amp;lt;element name="StartTime" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="EndTime" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="ExtendFlag" type="{http://www.w3.org/2001/XMLSchema}boolean"/&amp;gt;
 * &amp;lt;element name="state" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 * &amp;lt;element name="bindChs" type="{http://www.139130.net}ArrayOfBindChannel" minOccurs="0"/&amp;gt;
 * &amp;lt;/sequence&amp;gt;
 * &amp;lt;/restriction&amp;gt;
 * &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BusinessType", propOrder = {
        "id",
        "name",
        "priority",
        "startTime",
        "endTime",
        "extendFlag",
        "state",
        "bindChs"
})
public class BusinessType {

    @XmlElement(name = "Id")
    protected int id;
    @XmlElementRef(name = "Name", namespace = "http://www.139130.net", type = JAXBElement.class, required = false)
    protected JAXBElement<String> name;
    @XmlElement(name = "Priority")
    protected int priority;
    @XmlElementRef(name = "StartTime", namespace = "http://www.139130.net", type = JAXBElement.class, required = false)
    protected JAXBElement<String> startTime;
    @XmlElementRef(name = "EndTime", namespace = "http://www.139130.net", type = JAXBElement.class, required = false)
    protected JAXBElement<String> endTime;
    @XmlElement(name = "ExtendFlag")
    protected boolean extendFlag;
    protected int state;
    protected ArrayOfBindChannel bindChs;

    /**
     * 获取id属性的值。
     */
    public int getId() {
        return id;
    }

    /**
     * 设置id属性的值。
     */
    public void setId(int value) {
        this.id = value;
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
     * 获取priority属性的值。
     */
    public int getPriority() {
        return priority;
    }

    /**
     * 设置priority属性的值。
     */
    public void setPriority(int value) {
        this.priority = value;
    }

    /**
     * 获取startTime属性的值。
     *
     * @return possible object is
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public JAXBElement<String> getStartTime() {
        return startTime;
    }

    /**
     * 设置startTime属性的值。
     *
     * @param value allowed object is
     *              {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public void setStartTime(JAXBElement<String> value) {
        this.startTime = value;
    }

    /**
     * 获取endTime属性的值。
     *
     * @return possible object is
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public JAXBElement<String> getEndTime() {
        return endTime;
    }

    /**
     * 设置endTime属性的值。
     *
     * @param value allowed object is
     *              {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public void setEndTime(JAXBElement<String> value) {
        this.endTime = value;
    }

    /**
     * 获取extendFlag属性的值。
     */
    public boolean isExtendFlag() {
        return extendFlag;
    }

    /**
     * 设置extendFlag属性的值。
     */
    public void setExtendFlag(boolean value) {
        this.extendFlag = value;
    }

    /**
     * 获取state属性的值。
     */
    public int getState() {
        return state;
    }

    /**
     * 设置state属性的值。
     */
    public void setState(int value) {
        this.state = value;
    }

    /**
     * 获取bindChs属性的值。
     *
     * @return possible object is
     * {@link ArrayOfBindChannel }
     */
    public ArrayOfBindChannel getBindChs() {
        return bindChs;
    }

    /**
     * 设置bindChs属性的值。
     *
     * @param value allowed object is
     *              {@link ArrayOfBindChannel }
     */
    public void setBindChs(ArrayOfBindChannel value) {
        this.bindChs = value;
    }

}
