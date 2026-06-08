
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
 * &amp;lt;element name="GetBusinessTypeResult" type="{http://www.139130.net}BusinessType" maxOccurs="unbounded"/&amp;gt;
 * &amp;lt;/sequence&amp;gt;
 * &amp;lt;/restriction&amp;gt;
 * &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "getBusinessTypeResult"
})
@XmlRootElement(name = "GetBusinessTypeResponse")
public class GetBusinessTypeResponse {

    @XmlElement(name = "GetBusinessTypeResult", required = true)
    protected List<BusinessType> getBusinessTypeResult;

    /**
     * Gets the value of the getBusinessTypeResult property.
     * <p>
     * &lt;p&gt;
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a &lt;CODE&gt;set&lt;/CODE&gt; method for the getBusinessTypeResult property.
     * <p>
     * &lt;p&gt;
     * For example, to add a new item, do as follows:
     * &lt;pre&gt;
     * getGetBusinessTypeResult().add(newItem);
     * &lt;/pre&gt;
     * <p>
     * <p>
     * &lt;p&gt;
     * Objects of the following type(s) are allowed in the list
     * {@link BusinessType }
     */
    public List<BusinessType> getGetBusinessTypeResult() {
        if (getBusinessTypeResult == null) {
            getBusinessTypeResult = new ArrayList<BusinessType>();
        }
        return this.getBusinessTypeResult;
    }

}
