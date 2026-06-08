
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
 * &amp;lt;element name="fullPath" type="{http://www.w3.org/2001/XMLSchema}string"/&amp;gt;
 * &amp;lt;/sequence&amp;gt;
 * &amp;lt;/restriction&amp;gt;
 * &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "fullPath"
})
@XmlRootElement(name = "SetMedias")
public class SetMedias {

    @XmlElement(required = true)
    protected String fullPath;

    /**
     * 获取fullPath属性的值。
     *
     * @return possible object is
     * {@link String }
     */
    public String getFullPath() {
        return fullPath;
    }

    /**
     * 设置fullPath属性的值。
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setFullPath(String value) {
        this.fullPath = value;
    }

}
